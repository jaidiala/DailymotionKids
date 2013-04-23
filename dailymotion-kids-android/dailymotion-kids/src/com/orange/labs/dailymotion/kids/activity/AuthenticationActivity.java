package com.orange.labs.dailymotion.kids.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.dailymotion.kids.R;
import com.orange.labs.dailymotion.kids.activity.asynctasks.MembershipCheckingTask;
import com.orange.labs.dailymotion.kids.config.ConfigurationManager;
import com.orange.labs.dailymotion.kids.dependency.DependencyResolver;
import com.orange.labs.dailymotion.kids.dependency.DependencyResolverImpl;
import com.orange.labs.dailymotion.kids.utils.KidsLogger;
import com.orangelabs.dailymotion.DailyException;
import com.orangelabs.dailymotion.Dailymotion;
import com.orangelabs.dailymotion.Dailymotion.AuthMode;
import com.orangelabs.dailymotion.Dailymotion.AuthorizationListener;

public class AuthenticationActivity extends SherlockActivity implements OnClickListener {

	private static final String LOG_TAG = "Authentication Activity";

	private DependencyResolver mResolver;
	private TextView mTitleTv;
	private TextView mDescriptionTv;
	private TextView mStatusTv;
	private Button mSubscButton;
	private Button mLogInButton;
	private Button mContinueButton;

	private ConfigurationManager mConfiguration;
	private AsyncTask<String, Void, Boolean> mPremiumTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.authenticate_activity);

		final ActionBar actionBar = getSherlock().getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		mResolver = DependencyResolverImpl.getInstance();
		mConfiguration = mResolver.getConfiguration();
		mTitleTv = (TextView) findViewById(R.id.auth_title_tv);
		mDescriptionTv = (TextView) findViewById(R.id.auth_introduction_tv);
		mStatusTv = (TextView) findViewById(R.id.auth_status_tv);

		mLogInButton = (Button) findViewById(R.id.auth_dailymotion_choice_b);
		mSubscButton = (Button) findViewById(R.id.auth_subscription_choice_b);
		mContinueButton = (Button) findViewById(R.id.auth_continue_choice_b);
		mLogInButton.setOnClickListener(this);
		mSubscButton.setOnClickListener(this);
		mContinueButton.setOnClickListener(this);
		
		// Check if the user has subscribe to the premium offer and if true, set the corresponding views
		if (mConfiguration.hasSubscribed()) {
			mDescriptionTv.setText(R.string.auth_desc_premium);
			mLogInButton.setText(R.string.auth_dailymotion_other_account_choice);
			mSubscButton.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public void onClick(View v) {
		if (v == mLogInButton) {
			Bundle extras = new Bundle();
			extras.putString("display", "kids");
			final Dailymotion dailymotion = mResolver.getDailymotion();
			dailymotion.authorize(this, AuthMode.USER, null, new PremiumAuthorizationListener(),
					extras);
		} else if (v == mSubscButton) {
			if (mConfiguration.hasSubscribed()) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("market://details?id="+getApplicationContext().getPackageName()));
				startActivity(intent);
			} else {
				Intent intent = new Intent(v.getContext(), SubscriptionActivity.class);
				startActivity(intent);
			}
		} else if (v == mContinueButton) {
			finish();
		}
	}

	/**
	 * Configure the view depending of the status of the user. If he is a premium user, we
	 * congratulate him for subscribing. Otherwise, we propose him to subscribe to Dailymotion Kids
	 * using the in-app purchase.
	 * 
	 * @param premiumUser
	 *            boolean indicating if the user is premium.
	 */
	private void onPremiumVerificationSucceeded(boolean premiumUser) {
		mConfiguration.setHasSubscribed(premiumUser);

		// configure views.
		mLogInButton.setVisibility(View.GONE);
		if (premiumUser) {
			mTitleTv.setVisibility(View.INVISIBLE);
			mTitleTv.setText(R.string.auth_title_welcome);
			mDescriptionTv.setText(R.string.auth_desc_login_thanks);
			mSubscButton.setVisibility(View.GONE);
			mContinueButton.setVisibility(View.INVISIBLE);
			setResult(RESULT_OK);
		} else {
			mTitleTv.setText(R.string.auth_title_subscribe);
			mDescriptionTv.setText(R.string.auth_desc_not_premium);
			mSubscButton.setText(R.string.auth_subscription_choice);
			mSubscButton.setVisibility(View.VISIBLE);
			mContinueButton.setVisibility(View.GONE);
			setResult(RESULT_CANCELED);
		}
	}

	/**
	 * Authentication has failed. We notify the user and allows him to subscribe, or try
	 * authenticating again.
	 */
	private void onAuthenticationFailed() {
		mLogInButton.setVisibility(View.VISIBLE);
		mSubscButton.setVisibility(View.VISIBLE);
		mContinueButton.setVisibility(View.GONE);
		mTitleTv.setText(R.string.auth_title_error);
		mDescriptionTv.setText(R.string.auth_desc_no_account);
		setResult(RESULT_CANCELED);
	}

	private void checkIfUserPremium(String token) {
		if (mPremiumTask == null) {
			mPremiumTask = new SubscriptionCheckingTask().execute(token);
		}
	}

	private void authenticationComplete(final boolean success, final String accessToken) {
		if (success && !TextUtils.isEmpty(accessToken)) {
			checkIfUserPremium(accessToken);
		} else {
			onAuthenticationFailed();
		}
	}

	private class PremiumAuthorizationListener implements AuthorizationListener {

		@Override
		public void onSuccess(final Bundle result) {
			KidsLogger.i(LOG_TAG, "Authentication succeeded.");
			final String accessToken = result.getString("access_token");
			final String refreshToken = result.getString("refresh_token");
			final long expiresIn = result.getLong("expires_in");

			mConfiguration.storeUserAccessToken(accessToken, refreshToken, expiresIn);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					authenticationComplete(true, accessToken);
				}
			});
		}

		@Override
		public void onFailure(DailyException e) {
			KidsLogger.w(LOG_TAG, "Authentication failed: " + e.toString());
			final int message;
			switch (e.getErrorCode()) {
			case DailyException.INVALID_GRANT_TYPE:
				message = R.string.auth_invalid_username_password;
				break;
			case DailyException.IO_EXCEPTION:
				message = R.string.auth_communication_failed;
				break;
			default:
				message = R.string.auth_authentication_failed;
				break;
			}

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					authenticationComplete(false, null);
				}
			});
		}

	}

	private class SubscriptionCheckingTask extends MembershipCheckingTask {

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			onPremiumVerificationSucceeded(result);
		}

	}
}
