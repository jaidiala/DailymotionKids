package com.orangelabs.dailymotion;

import java.util.concurrent.ExecutorService;

import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.orangelabs.dailymotion.DailymotionDialog.DailymotionDialogListener;
import com.orangelabs.dailymotion.DailymotionRequestor.DailymotionRequestListener;

public class Dailymotion {

	private static final String LOG_TAG = "Dailymotion";

	public static final String USERNAME_PARAM = "username";
	public static final String PASSWORD_PARAM = "password";

	/**
	 * Client ID of the application accessing the Dailymotion GRAPH API.
	 */
	private final String mClientId;

	/**
	 * Client Secret : Allows the application to identify itself against the Dailymotion Graph API.
	 */
	private final String mClientSecret;

	private final Context mContext;
	private final ExecutorService mExecutor;

	/**
	 * Access Token
	 */
	private String mAccessToken;

	/**
	 * Expiration Time of the Access Token
	 */
	private long mExpirationTime = -1;

	/**
	 * Token used to refresh the access token without asking user's credentials.
	 */
	private String mRefreshToken;

	/**
	 * Build a new Dailymotion object used to interact with the Facebook Graph API.
	 * 
	 * @param clientId
	 *            Application client ID that has been registered against Dailymotion.
	 */
	public Dailymotion(final Context context, final ExecutorService executor,
			final String clientId, final String clientSecret) {
		mContext = context;
		mExecutor = executor;
		mClientId = clientId;
		mClientSecret = clientSecret;

		// Set up environment
		DailymotionUtils.disableConnectionReuseIfNecessary();
//		DailymotionUtils.enableHttpResponseCache(mContext, Constants.CACHE_SIZE);
	}

	public ExecutorService getExecutor() {
		return mExecutor;
	}

	/**
	 * Return the client ID used to identify the application against the Dailymotion API.
	 * 
	 * @return Client ID as a String.
	 */
	public String getClientId() {
		return mClientId;
	}

	/**
	 * Return the Client Secret used to identify the application against the Dailymotion Graph API.
	 * 
	 * @return Client Secret as a {@link String}
	 */
	public String getClientSecret() {
		return mClientSecret;
	}

	/**
	 * Return the token used to refresh the access token when it expires.
	 * 
	 * @return Refresh Token as a String.
	 */
	protected String getRefreshToken() {
		return mRefreshToken;
	}

	/**
	 * Set the Dailymotion Access Token.
	 * 
	 * @param accessToken
	 *            Access Token
	 */
	public void setAccessToken(String accessToken) {
		mAccessToken = accessToken;
	}

	/**
	 * Set the expiration time of the Dailymotion access token
	 * 
	 * @param expirationTime
	 *            Long value pointing at the time the token won't be valid anymore.
	 */
	public void setExpirationTime(long expirationTime) {
		mExpirationTime = expirationTime;
	}

	/**
	 * Store the refresh token value, that allows to refresh the access token
	 * 
	 * @param refreshToken
	 *            Refresh Token as a String.
	 */
	public void setRefreshToken(String refreshToken) {
		mRefreshToken = refreshToken;
	}

	/**
	 * Return the expiration time of the Access Token
	 * 
	 * @return Long representing the expiration time of the access token since unix epoch.
	 */
	public long getExpirationTime() {
		return mExpirationTime;
	}

	/**
	 * Return the current access token if it exists and if it is still valid.
	 * 
	 * @return Current functional Access Token if it exists.
	 */
	public String getAccessToken() {
		return mAccessToken;
	}

	/**
	 * Indicate if the current token is valid and can be used at the present time.
	 * 
	 * @return boolean indicating if the token is still valid.
	 */
	public boolean isSessionValid() {
		long expirationTime = getExpirationTime();
		return ((mAccessToken != null) && (expirationTime != -1) && ((expirationTime == 0) || (expirationTime > System
				.currentTimeMillis())));
	}

	/**
	 * Indicate if the current token should be updated (inexistent, expires soon, or expired).
	 * 
	 * @return boolean indicating if the session should be updated, requesting a new token to
	 *         dailymotion.
	 */
	public boolean shouldRefreshToken() {
		return (!isSessionValid() || getExpirationTime() - System.currentTimeMillis() < Constants.REFRESH_TOKEN_THRESHOLD);
	}

	/**
	 * Show the authentication dialog to the user, requesting the default permission.
	 * 
	 * @param context
	 *            Context used to display the dialog containing the authentication webpage.
	 */
	public void authorize(Context context, AuthMode mode, final AuthorizationListener listener) {
		authorize(context, mode, new String[] { "read" }, listener);
	}

	/**
	 * Show the authentication dialog to the user, requesting the provided permissions.
	 * 
	 * @param context
	 *            Context used to display the dialog containing the authentication webpage.
	 * @param permissions
	 *            Array of {@link String} containing a list of permissions to request.
	 * @param extras
	 *            Extras parameters to be sent to the server, typically to provide a
	 *            username/password when using the Password method.
	 */
	// TODO: add scope using permissions.
	public void authorize(Context context, AuthMode mode, String[] permissions,
			final AuthorizationListener listener, final Bundle extras) {
		final DailymotionRequestor requestor = new DailymotionRequestor(this);
		final Bundle params = (extras == null) ? new Bundle() : new Bundle(extras);
		final AuthenticationRequestListener authListener = new AuthenticationRequestListener(
				listener);
		switch (mode) {
		case APPLICATION:
			params.putString("grant_type", "client_credentials");
			params.putString("client_id", getClientId());
			params.putString("client_secret", getClientSecret());
			requestor.requestToken(params, authListener);
			break;
		case USER:
			showDialog(context, permissions, extras, new DailymotionDialogListener() {
				@Override
				public void onSuccess(Bundle values) {
					String code = values.getString("code");
					if (!TextUtils.isEmpty(code)) {
						// Now that we have a code, we can request an access
						// token using the oauth
						// API.
						params.putString("grant_type", "authorization_code");
						params.putString("code", code);
						requestor.requestToken(params, authListener);
					} else {
						listener.onFailure(new DailyException(DailyException.INVALID_RESPONSE,
								"No authentication code has been returned by Dailymotion."));
					}
				}

				@Override
				public void onFailure(DailyException e) {
					listener.onFailure(e);
				}
			});
			break;
		case PASSWORD:
			params.putString("grant_type", "password");
			params.putString("client_id", getClientId());
			params.putString("client_secret", getClientSecret());
			requestor.requestToken(params, authListener);
			break;
		case REFRESH_TOKEN:
			String token = getRefreshToken();
			if (!TextUtils.isEmpty(token)) {
				params.putString("grant_type", "refresh_token");
				params.putString("refresh_token", token);
				requestor.requestToken(params, authListener);
			} else {
				listener.onFailure(new DailyException(DailyException.NO_TOKEN,
						"Please make sure you have a valid refresh token."));
			}
			break;
		case ANONYMOUS:
			listener.onFailure(new DailyException(DailyException.INVALID_PARAMETER,
					"Requested authentication for Anonymous access. Should not happen"));
			break;
		}
	}

	/**
	 * Authorization without providing any extra parameter to be sent to the platform.
	 * 
	 * @param context
	 *            Application context.
	 * @param mode
	 *            Authentication Mode, @see {@link AuthMode}
	 * @param permissions
	 *            Extras permissions
	 * @param listener
	 *            Listener to get a callback after the authentication has been performed, or has
	 *            failed.
	 */
	public void authorize(Context context, AuthMode mode, String[] permissions,
			final AuthorizationListener listener) {
		authorize(context, mode, permissions, listener, null);
	}

	/**
	 * Show an authentication dialog to the user. A {@link DailymotionDialogListener} is provided to
	 * process the result of the interaction of the user with the dialog.
	 * 
	 * @param context
	 *            Context used to display the dialog.
	 * @param permissions
	 *            Permissions to request for the user logging in.
	 * @param listener
	 *            Listener that will be used by the dialog to communicate the result back.
	 */
	private void showDialog(Context context, String[] permissions, Bundle extras,
			DailymotionDialogListener listener) {
		Bundle parameters = (extras != null) ? new Bundle(extras) : new Bundle();
		parameters.putString("response_type", "code");
		parameters.putString("client_id", getClientId());
		parameters.putString("redirect_uri", Constants.REDIRECT_URI);
		parameters.putString("scope", DailymotionUtils.formatPermissions(permissions));

		String url = Constants.DAILYMOTION_URI + Constants.AUTH_ENDPOINT + "?"
				+ DailymotionUtils.encodeUrl(parameters);

		// Show the dialog to the user.
		new DailymotionDialog(context, url, listener).show();
	}

	/**
	 * Default {@link DailymotionRequestListener} when authenticating the user or the user against
	 * the Dailymotion API. This listener takes an {@link AuthorizationListener} as a parameter that
	 * is used to give back the authentication result to the application using the Dailymotion SDK.
	 * 
	 * @author Jean-Francois Moy
	 * 
	 */
	private class AuthenticationRequestListener implements DailymotionRequestListener {

		/**
		 * Original listener passed by the caller that should be used to give back the result of the
		 * authentication request.
		 */
		private final AuthorizationListener mListener;

		public AuthenticationRequestListener(AuthorizationListener listener) {
			mListener = listener;
		}

		/**
		 * Dailymotion can return a 'null' String when the value is inexistent, it is important to
		 * dismiss those values.
		 */
		@Override
		public void onSuccess(JSONObject json) {
			String accessToken = json.optString("access_token");
			String refreshToken = json.optString("refresh_token");
			long validity = json.optLong("expires_in", -1);

			if (!TextUtils.isEmpty(accessToken)) {
				if (accessToken.equals("null")) {
					accessToken = "";
				}
				setAccessToken(accessToken);
			}

			if (!TextUtils.isEmpty(refreshToken)) {
				if (refreshToken.equals("null")) {
					refreshToken = "";
				}
				setRefreshToken(refreshToken);
			}

			long expiration = -1;
			if (validity != -1) {
				expiration = (validity == 0) ? 0 : System.currentTimeMillis() + validity * 1000L;
				setExpirationTime(expiration);
			}

			Bundle result = new Bundle();
			result.putString("refresh_token", refreshToken);
			result.putString("access_token", accessToken);
			result.putLong("expires_in", expiration);
			mListener.onSuccess(result);
		}

		@Override
		public void onFailure(DailyException e) {
			DailymotionLogger.w(LOG_TAG, "Authentication Request Error : " + e.getMessage());
			mListener.onFailure(e);
		}
	}

	/**
	 * Listener for the authorization process. Allows the client application using the SDK to know
	 * if the authentication succeeded or not, and the reason of failure if one happened.
	 * 
	 * @author Jean-Francois Moy
	 * 
	 */
	public interface AuthorizationListener {

		/**
		 * Executed when the authentication succceeds. The result bundle contains the values
		 * returned by the Dailymotion API. Such information are the access token, the expiration
		 * date, or the refresh token.
		 * 
		 * @param result
		 *            Bundle containing the result returned by Dailymotion after authenticating.
		 */
		public void onSuccess(Bundle result);

		/**
		 * Executed if an error related to Dailymotion occurs. The exception provided gives more
		 * information about the error.
		 * 
		 * @param e
		 *            Exception that occured while authentication against the Dailymotion API.
		 */
		public void onFailure(DailyException e);

	}

	/**
	 * Authorization modes that can be used while authenticating against the Dailymotion Graph API.
	 */
	public enum AuthMode {
		/**
		 * Mode used to perform anonymous request and access publicly available resources.
		 */
		ANONYMOUS,
		/**
		 * Mode used when there is no need to authenticate as the user, but the application requires
		 * special privileges for accessing private resources.
		 */
		APPLICATION,
		/**
		 * Authenticate on behalf of the user using a the Dailymotion OAUTH method.
		 */
		USER,
		/**
		 * Authenticate using the username/password of the user.
		 */
		PASSWORD,
		/**
		 * Refresh the access token (available only if an access token has been previously retrieved
		 */
		REFRESH_TOKEN
	}
}
