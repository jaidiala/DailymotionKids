package com.orangelabs.dailymotion;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.orange.labs.dailymotion.R;

/**
 * Dialog shown to the user to authenticate against the Dailymotion Graph API. Once the user has
 * entered his/her credentials, Dailymotion responds with a code that can be used later on to
 * retrieve an access token.
 * 
 * This dialog uses a DailymotionDialogListener to send back the result to the caller.
 * 
 * @author Jean-Francois Moy
 * 
 */
public class DailymotionDialog extends Dialog {

	private static final String LOG_TAG = "Dailymotion Dialog";

	static LayoutParams FILL_PARENT = new LayoutParams(LayoutParams.FILL_PARENT,
			LayoutParams.FILL_PARENT);

	/**
	 * Frame Layout that contains the WebView required to display the authentication page.
	 */
	private FrameLayout mContent;

	/**
	 * URL used to authenticate.
	 */
	private String mUrl;

	/**
	 * Web View used to show the authentication webpage.
	 */
	private WebView mWebView;

	/**
	 * Spinner displayed while the webpage is loading.
	 */
	private ProgressDialog mSpinner;

	/**
	 * Listener used to give a callback to the calling object.
	 */
	private DailymotionDialogListener mListener;

	/**
	 * Creates a new Dailymotion Dialog that displays the authentication dialog.
	 * 
	 * @param context
	 *            Android context used to create the Dialog.
	 * @param url
	 *            URL to display in the web view contained by the Dialog.
	 */
	public DailymotionDialog(Context context, String url, DailymotionDialogListener listener) {
		super(context, android.R.style.Theme_Translucent_NoTitleBar);
		mUrl = url;
		mListener = listener;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Create spinner used when the page is loading in the background.
		mSpinner = new ProgressDialog(getContext());
		mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
		mSpinner.setIndeterminate(true);
		mSpinner.setTitle(getContext().getString(R.string.auth_dialog_title));
		mSpinner.setMessage(getContext().getString(R.string.auth_dialog_message));

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Build Dialog
		mContent = new FrameLayout(getContext());
		createWebView();

		addContentView(mContent, FILL_PARENT);
	}

	/**
	 * Creates the web view and adds it to the Dialog content.
	 */
	private void createWebView() {
		mWebView = new WebView(getContext());
		mWebView.setLayoutParams(FILL_PARENT);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.getSettings().setUserAgentString("dailymotion_android_sdk");
		mWebView.getSettings().setSavePassword(false);
		mWebView.setWebViewClient(new DailymotionWebViewClient());
		mWebView.loadUrl(mUrl);
		mWebView.setVisibility(View.INVISIBLE);
		mContent.addView(mWebView);
	}

	/**
	 * Extending {@link WebViewClient} to process manually the response from the Dailymotion website
	 * while authenticating. It also manages the displaying/hiding of graphical elements such as the
	 * spinner while the page is loading.
	 */
	private class DailymotionWebViewClient extends WebViewClient {

		/**
		 * Executed when the Dailymotion authentication page starts to load, we show a spinner
		 * instead while it is loading.
		 */
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			super.onPageStarted(view, url, favicon);
			mSpinner.show();
		}

		/**
		 * Executed when the Dailymotion authentication page has finished loading, we thus hide the
		 * spinner and show the authentication form instead.
		 */
		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			DailymotionLogger.d(LOG_TAG, "Page finished: " + url);

			// Hide spinner and show web view.
			mSpinner.dismiss();
			mWebView.setVisibility(View.VISIBLE);
		}

		/**
		 * Each time a URL should be loaded by the Web Client, this method gets executed instead.
		 * The objective is to obtain the authentication code, given back by the Dailymotion API
		 * after the authentication has succeeded.
		 */
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			DailymotionLogger.d(LOG_TAG, "Overriding.. " + url);
			if (url.startsWith(Constants.REDIRECT_URI)) {
				// Check if an error has occurred.
				Bundle result = DailymotionUtils.decodeUrl(url);
				DailymotionLogger.d(LOG_TAG, result.toString());

				String error = result.getString("error");
				if (error != null) {
					mListener.onFailure(DailyException.getException(error,
							result.getString("error_description")));
				} else {
					DailymotionLogger.d(LOG_TAG, "Success: " + result.getString("code"));
					mListener.onSuccess(result);
				}

				dismiss();
				return true;
			}

			return super.shouldOverrideUrlLoading(view, url);
		}
	}

	/**
	 * Interface that defines a listener that can be used by a Dialog to give results back to the
	 * caller.
	 * 
	 * <ul>
	 * <li>The onSuccess method indicates that the authentication was successful, and gives the
	 * result as key/value pairs in a {@link Bundle}.
	 * <li>The onFailure method indicates that the authentication has failed, the reason is given as
	 * a {@link DailyException}.
	 * 
	 * @author Jean_Francois Moy
	 * 
	 */
	public interface DailymotionDialogListener {

		/**
		 * Callback method that should be called when the dialog operation has succeeded, providing
		 * the response from Dailymotion as a {@link Bundle}.
		 * 
		 * @param values
		 *            Bundle containing the values returned by Dailymotion.
		 */
		public void onSuccess(Bundle values);

		/**
		 * Callback method called when the dialog operation fails. It provides the description of
		 * the error sent by Dailymotion.
		 * 
		 * @param e
		 *            Exception describing the error that provoked the failure.
		 */
		public void onFailure(DailyException e);

	}

}
