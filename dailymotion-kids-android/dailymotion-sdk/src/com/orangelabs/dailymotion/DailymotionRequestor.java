package com.orangelabs.dailymotion;

import java.io.IOException;

import org.json.JSONObject;

import android.os.Bundle;

public class DailymotionRequestor {

	private final Dailymotion mDailymotion;

	public DailymotionRequestor(Dailymotion dailymotion) {
		mDailymotion = dailymotion;
	}

	/**
	 * Request the provided endpoint and give the result of the request back through the provided
	 * {@link DailymotionRequestListener}.
	 * 
	 * @param endpoint
	 *            Endpoint to interrogate.
	 * @param listener
	 *            Listener used for providing the result back, or any error that could occur.
	 */
	public void request(final String endpoint, final Bundle bundle,
			final DailymotionRequestListener listener) {
		if (mDailymotion.isSessionValid()) {
			String url = Constants.DAILYMOTION_URI + endpoint;

			Bundle params = new Bundle(bundle);
			params.putString("access_token", mDailymotion.getAccessToken());

			request(url, "GET", params, listener);
		} else {
			listener.onFailure(new DailyException(DailyException.EXPIRED_TOKEN,
					"Token has expired, should be refreshed"));
		}
	}

	/**
	 * Execute an anonymous request : the request is sent without using an access token. Access to
	 * public resources is typically done this way.
	 * 
	 * @param endpoint
	 *            Endpoint to request anonymously.
	 * @param listener
	 *            Listener used for providing the result back, or any error that could occur.
	 */
	public void anonymousRequest(final String endpoint, final Bundle bundle,
			final DailymotionRequestListener listener) {
		String url = Constants.DAILYMOTION_URI + endpoint;
		request(url, "GET", bundle, listener);
	}

	/**
	 * Request the Token Endpoint using the extras {@link Bundle} as parameters. Results will be
	 * provided via the listener.
	 * 
	 * @param extras
	 *            - Extras to include in the body of the request.
	 * @param listener
	 *            Listener used to provide the result back to the caller.
	 */
	public void requestToken(Bundle extras, DailymotionRequestListener listener) {
		String url = Constants.DAILYMOTION_URI + Constants.TOKEN_ENDPOINT;
		Bundle bundle = commonParameters();
		bundle.putAll(extras);
		request(url, "POST", bundle, listener);
	}

	/**
	 * Request the Dailymotion Graph API endpoint specified by the provided URL using a HTTP Post
	 * request, including the parameters contained in the provided bundle in the body.
	 * 
	 * @param url
	 *            Dailymotion endpoint to request.
	 * @param parameters
	 *            Request parameters that will be included in the body of the request.
	 * @param listener
	 *            Listener used to provide the results back to the caller.
	 */
	private void request(final String url, final String method, final Bundle parameters,
			final DailymotionRequestListener listener) {
		mDailymotion.getExecutor().execute(new Runnable() {

			@Override
			public void run() {
				try {
					String response = DailymotionUtils.request(url, parameters, method);
					DailymotionLogger.v("Requestor", response);
					JSONObject json = DailymotionUtils.parseJson(response);
					listener.onSuccess(json);
				} catch (DailyException e) {
					listener.onFailure(e);
				} catch (IOException e) {
					listener.onFailure(new DailyException(DailyException.IO_EXCEPTION,
							"Input/Output exception while communicating with Dailymotion.", e));
				}
			}

		});
	}

	/**
	 * Add to the provided bundle the common parameters to every request to Dailymotion Grqph API
	 * including the application information and redirect URI.
	 * 
	 * @param bundle
	 *            Bundle we should add the parameters to.
	 */
	private Bundle commonParameters() {
		Bundle bundle = new Bundle();
		bundle.putString("client_secret", mDailymotion.getClientSecret());
		bundle.putString("client_id", mDailymotion.getClientId());
		bundle.putString("redirect_uri", Constants.REDIRECT_URI);
		return bundle;
	}

	/**
	 * Listener used when requesting Dailymotion Graph API.
	 * 
	 * @author Jean-Francois Moy
	 */
	public interface DailymotionRequestListener {

		/**
		 * Request has succeeded and the response is given back as a {@link JSONObject}.
		 * 
		 * @param response
		 *            Response as a {@link JSONObject}.
		 */
		public void onSuccess(JSONObject response);

		/**
		 * Request has failed. The reason of failure is provided as a {@link DailyException}.
		 * 
		 * @param e
		 *            Reason of failure as a {@link DailyException}.
		 */
		public void onFailure(DailyException e);

	}
}
