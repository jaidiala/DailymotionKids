package com.orange.labs.dailymotion.kids.activity;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.amazon.inapp.purchasing.PurchasingManager;
import com.dailymotion.kids.R;
import com.orange.labs.dailymotion.kids.activity.adapters.HeroGridAdapter;
import com.orange.labs.dailymotion.kids.activity.asynctasks.MembershipCheckingTask;
import com.orange.labs.dailymotion.kids.billing.amazon.UpdateObserver;
import com.orange.labs.dailymotion.kids.callback.Callback;
import com.orange.labs.dailymotion.kids.config.ConfigurationManager;
import com.orange.labs.dailymotion.kids.dependency.DependencyResolver;
import com.orange.labs.dailymotion.kids.dependency.DependencyResolverImpl;
import com.orange.labs.dailymotion.kids.user.User;
import com.orange.labs.dailymotion.kids.user.User.State;
import com.orange.labs.dailymotion.kids.user.UserContract;
import com.orange.labs.dailymotion.kids.user.UsersProvider;
import com.orange.labs.dailymotion.kids.utils.HeroesCacheUpdater;
import com.orange.labs.dailymotion.kids.utils.KidsLogger;
import com.orangelabs.dailymotion.DailyException;
import com.orangelabs.dailymotion.Dailymotion;
import com.orangelabs.dailymotion.Dailymotion.AuthorizationListener;
import com.orangelabs.dailymotion.DailymotionRequestor;
import com.orangelabs.dailymotion.DailymotionRequestor.DailymotionRequestListener;

/**
 * Home screen activity displayed to the user once he launches the application.
 */
@SuppressLint("NewApi")
public class HeroesActivity extends SherlockFragmentActivity implements
		LoaderCallbacks<Cursor> {

	private static final String LOG_TAG = "HeroesActivity";

	private static final String EDITION_MODE_KEY = "edition_mode";

	private GridView mGridView;
	private HeroGridAdapter mGridAdapter;
	private ConfigurationManager mConfiguration;
	private String currentUser;
	private boolean mUpdating = false;

	/**
	 * Dailymotion object used to access remote content and to authenticate
	 * against Dailymotion Graph API when the user selects the menu item.
	 */
	private Dailymotion mDailymotion;

	private DependencyResolver mResolver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.heroes_activity);

		mResolver = DependencyResolverImpl.getInstance();
		mDailymotion = mResolver.getDailymotion();
		mConfiguration = mResolver.getConfiguration();

		// Create the Homescreen Gridview
		mGridAdapter = new HeroGridAdapter(this, null,
				CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

		mGridView = (GridView) findViewById(R.id.homescreen_gridview);
		mGridView.setAdapter(mGridAdapter);
		mGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				if (mGridAdapter.inEditMode()) {
					switchEditMode(false);
					return;
				}

				User hero = User.fromCursor((Cursor) mGridAdapter
						.getItem(position));
				if (hero.isNew())
					markHeroAsOld(hero);
				Intent intent = new Intent(HeroesActivity.this,
						VideosListActivity.class);
				intent.putExtra(VideosListActivity.USER_ID,
						hero.getDailymotionId());
				startActivity(intent);
			}
		});
		mGridView
				.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

					@Override
					public boolean onItemLongClick(AdapterView<?> parent,
							View v, int position, long id) {
						switchEditMode(!mGridAdapter.inEditMode());
						return true;
					}

				});

		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(EDITION_MODE_KEY)) {
				switchEditMode(savedInstanceState.getBoolean(EDITION_MODE_KEY));
				return; // no need to initialize the loader in that case.
			}
		}

		// Load the cursor asynchronously
		getSupportLoaderManager().initLoader(0, null, this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (mGridAdapter != null)
			outState.putBoolean(EDITION_MODE_KEY, mGridAdapter.inEditMode());
	}

	/**
	 * Responsible for checking if the user is premium on Google Play at start
	 */
	@Override
	protected void onStart() {

		// we check if the useer is subscribed or not via Amazon
		UpdateObserver buttonClickerObserver = new UpdateObserver(this);
		PurchasingManager.registerObserver(buttonClickerObserver);
		PurchasingManager.initiateGetUserIdRequest();


		super.onStart();
	}


	/**
	 * Responsible of refreshing (or getting) the access token, and if already
	 * present, of updating the local content.
	 */
	@Override
	protected void onResume() {
		super.onResume();

		// Refresh the action bar menu
		invalidateOptionsMenu();
		if (!refreshTokenIfNecessary()) {
			updateLocalCache(false);
		}
		if (shouldTestUserMembership()) {
			if (shouldRefreshUserToken()) {
				if (TextUtils.isEmpty(mConfiguration.getUserRefreshToken())) {
					Toast.makeText(this,
							getString(R.string.homescreen_token_error),
							Toast.LENGTH_LONG).show();
					mConfiguration.storeAccessToken(null, null, -1); // reset
																		// token
				} else {
					refreshUserToken();
				}
			} else {
				checkUserMembership();
			}
		}
	}


	/**
	 * Responsible of stopping the billing service if it is running
	 */
	@Override
	protected void onStop() {
		
		super.onStop();
	}

	/**
	 * Switch the mode of the page, between edit and normal mode.
	 */
	private void switchEditMode(boolean editMode) {
		mGridAdapter.setEditMode(editMode);
		getSupportLoaderManager().restartLoader(0, null, HeroesActivity.this);
	}

	/**
	 * Mark a hero as old (meaning seen) in the database.
	 */
	private void markHeroAsOld(User hero) {
		hero.setNew(false);
		getContentResolver().update(
				ContentUris.withAppendedId(UsersProvider.CONTENT_URI,
						hero.getId()), hero.toContentValues(), null, null);
	}

	/**
	 * Update the local cache if necessary.
	 */
	private void updateLocalCache(boolean forceUpdate) {
		boolean updating = false;
		HeroesCacheUpdater updater = mResolver.getCacheUpdater();
		if (forceUpdate || updater.shouldUpdate()) {
			updating = updater.update(new LocalCacheUpdateCallback());
		}

		if (updating || updater.isRunning()) {
			setupUpdatingViews(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.heroes_activity, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem updateIndicatorItem = menu
				.findItem(R.id.homescreen_update_indic_item);
		MenuItem refreshItem = menu.findItem(R.id.homescreen_refresh_item);
		MenuItem subscriptionItem = menu.findItem(R.id.homescreen_auth_item);
		updateIndicatorItem.setVisible(mUpdating);
		refreshItem.setEnabled(!mUpdating);

		// final boolean connected =
		// !TextUtils.isEmpty(mConfiguration.getUserAccessToken());
		// if (connected && mConfiguration.hasSubscribed()) {
		if (mConfiguration.hasSubscribed()) {
			subscriptionItem.setTitle(R.string.homescreen_subscribed_menu_item);
		} else {
			subscriptionItem.setTitle(R.string.homescreen_auth_menu_item);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.homescreen_auth_item:
			Intent authIntent = new Intent(this, AuthenticationActivity.class);
			startActivity(authIntent);
			break;
		case R.id.homescreen_refresh_item:
			updateLocalCache(true); // forcing update
			break;
		case R.id.homescreen_info_item:
			Intent infoIntent = new Intent(this, AboutActivity.class);
			startActivity(infoIntent);
			break;
		default:
			// not used.
			break;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		final String sortOrder = UserContract.NEW + " DESC, "
				+ UserContract.STATE + " ASC, " + UserContract.SCREEN_NAME
				+ " ASC";
		if (mGridAdapter.inEditMode()) {
			return new CursorLoader(this, UsersProvider.CONTENT_URI, null,
					null, null, sortOrder);
		} else {
			return new CursorLoader(this, UsersProvider.CONTENT_URI, null,
					UserContract.STATE + "!=?",
					new String[] { String.valueOf(State.HIDDEN.ordinal()) },
					sortOrder);
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mGridAdapter.swapCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mGridAdapter.swapCursor(null);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			if (mGridAdapter.inEditMode()) {
				mGridAdapter.setEditMode(false);
				mGridAdapter.notifyDataSetChanged();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Refresh the Dailymotion access token if necessary and returns a boolean
	 * indicating if it was.
	 */
	private boolean refreshTokenIfNecessary() {
		String accessToken = mConfiguration.getAccessToken();
		String refreshToken = mConfiguration.getRefreshToken();
		long expirationDate = mConfiguration.getExpirationDate();
		mDailymotion.setAccessToken(accessToken);
		mDailymotion.setRefreshToken(refreshToken);
		mDailymotion.setExpirationTime(expirationDate);

		if (!mDailymotion.isSessionValid()) {
			KidsLogger.v(LOG_TAG, "Session is invalid, should we refresh?");
			final DailymotionAuthorizationListener listener = new DailymotionAuthorizationListener();
			if (!TextUtils.isEmpty(refreshToken)
					&& mDailymotion.shouldRefreshToken()) {
				KidsLogger.v(LOG_TAG, "Should refresh... Start refreshing...");
				mDailymotion.authorize(this,
						Dailymotion.AuthMode.REFRESH_TOKEN, listener);
			} else {
				// We get an application access token to be able to play the
				// videos without the user
				// logging in.
				KidsLogger.v(LOG_TAG, "Retrieving an application access token");
				mDailymotion.authorize(this, Dailymotion.AuthMode.APPLICATION,
						listener);
			}
			return true;
		} else {
			KidsLogger
					.v(LOG_TAG,
							String.format(
									"Session is valid. Access Token: %s, Refresh Token: %s, Expiration Time: %d",
									accessToken, refreshToken, expirationDate));
		}

		return false;
	}

	/**
	 * Indicate if the user is or has logged in and if we should thus check his
	 * membership.
	 */
	public boolean shouldTestUserMembership() {
		String accessToken = mConfiguration.getUserAccessToken();
		String refreshToken = mConfiguration.getUserRefreshToken();
		return (!TextUtils.isEmpty(accessToken) || !TextUtils
				.isEmpty(refreshToken));
	}

	/**
	 * If the user token has expired, we need to refresh it before being able to
	 * check his membership.
	 */
	public boolean shouldRefreshUserToken() {
		long expirationTime = mConfiguration.getUserExpirationDate();
		boolean userSessionValid = ((mConfiguration.getUserAccessToken() != null)
				&& (expirationTime != -1) && ((expirationTime == 0) || (expirationTime > System
				.currentTimeMillis())));
		return (!userSessionValid || expirationTime
				- System.currentTimeMillis() < (5 * 60L * 1000L));
	}

	/**
	 * Check if the user is premium using the Dailymotion API. If the user has
	 * changed subscription, a toast is shown to him.
	 */
	private void checkUserMembership() {
		setupUpdatingViews(true);
		new MembershipTask().execute(mConfiguration.getUserAccessToken());
	}

	private void refreshUserToken() {
		setupUpdatingViews(true);

		Bundle params = new Bundle();
		params.putString("grant_type", "refresh_token");
		params.putString("refresh_token", mConfiguration.getUserRefreshToken());
		final DailymotionRequestor requestor = new DailymotionRequestor(
				mDailymotion);
		requestor.requestToken(params, new DailymotionRequestListener() {

			@Override
			public void onSuccess(JSONObject response) {
				final String accessToken = response.optString("access_token");
				final String refreshToken = response.optString("refresh_token");
				final long expiresIn = response.optLong("expires_in");
				mConfiguration.storeUserAccessToken(accessToken, refreshToken,
						expiresIn);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// launch asynctask to check membership now that we have
						// the required info
						checkUserMembership();
					}
				});
			}

			@Override
			public void onFailure(DailyException e) {
				int message = -1;
				switch (e.getErrorCode()) {
				case DailyException.NO_TOKEN:
				case DailyException.EXPIRED_TOKEN:
				case DailyException.ACCESS_DENIED:
					message = R.string.homescreen_token_error;
					mConfiguration.storeUserAccessToken(null, null, -1);
					mConfiguration.setHasSubscribed(false);
					message = R.string.homescreen_access_denied;
					break;
				case DailyException.INVALID_PARAMETER:
				case DailyException.INVALID_RESPONSE:
					message = R.string.homescreen_server_error;
					break;
				case DailyException.IO_EXCEPTION:
					message = R.string.homescreen_io_error;
					break;
				default:
					message = R.string.homescreen_unknown_error;
					break;
				}
				doneUpdating(message);
			}
		});
	}

	/**
	 * Setup the various views involved in showing up and triggering the update
	 * process.
	 * 
	 * @param updating
	 *            Boolean indicating if the local cache is currently being
	 *            updated or not.
	 */
	@SuppressLint("NewApi")
	private void setupUpdatingViews(boolean updating) {
		mUpdating = updating;
		invalidateOptionsMenu();
	}

	/**
	 * Listener used by the Dailymotion SDK to give the result of the
	 * authentication back to us. Typically, we would store the various tokens
	 * locally in case of success, and notify the user if an error occurs.
	 * 
	 * If the access token is fetched successfully, an attempt to update the
	 * local content will be performed.
	 * 
	 * It implements the {@link AuthorizationListener} interface defined in the
	 * Dailymotion SDK.
	 * 
	 * @author Jean-Francois Moy
	 * 
	 */
	private class DailymotionAuthorizationListener implements
			AuthorizationListener {

		@Override
		public void onSuccess(Bundle result) {
			Log.i("Dailymotion Kids", "Authentication succeeded");
			mConfiguration.storeAccessToken(result.getString("access_token"),
					result.getString("refresh_token"),
					result.getLong("expires_in"));

			// Important to run it on UI thread to be able to update UI if
			// needed.
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					updateLocalCache(false);
				}

			});
		}

		@Override
		public void onFailure(DailyException e) {
			// TODO Show a message to the user if required.
			Log.e("Dailymotion Kids", "Authentication failed");
		}

	}

	/**
	 * This class is used as a callback for the local cache update operation. It
	 * is mainly used to display the cause of failure in case the operation
	 * failed, as well as updating the activity indicator and menu items
	 * accordingly.
	 */
	private class LocalCacheUpdateCallback implements Callback<Void> {

		@Override
		public void onSuccess(Void result) {
			doneUpdating(R.string.homescreen_update_success);
		}

		@Override
		public void onFailure(Exception e) {
			int message = -1;
			if (e instanceof DailyException) {
				DailyException error = (DailyException) e;
				switch (error.getErrorCode()) {
				case DailyException.NO_TOKEN:
				case DailyException.EXPIRED_TOKEN:
				case DailyException.ACCESS_DENIED:
					if (!refreshTokenIfNecessary()) {
						message = R.string.homescreen_auth_error;
					}
					break;
				case DailyException.INVALID_PARAMETER:
				case DailyException.INVALID_RESPONSE:
					message = R.string.homescreen_server_error;
					break;
				case DailyException.IO_EXCEPTION:
					message = R.string.homescreen_io_error;
					break;
				default:
					message = R.string.homescreen_unknown_error;
					break;
				}
			} else {
				message = R.string.homescreen_unknown_error;
			}
			doneUpdating(message);
		}
	}

	private class MembershipTask extends MembershipCheckingTask {
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			KidsLogger.v(LOG_TAG,
					"User was subscribed: " + mConfiguration.hasSubscribed());
			KidsLogger.v(LOG_TAG, "User subscribed: " + result);
			// force status when subscribed on google play
			if (mConfiguration.hasSubscribedOnGPlay()) {
				mConfiguration.setHasSubscribed(true);
			} else {
				if (mConfiguration.hasSubscribed() != result) {
					mConfiguration.setHasSubscribed(result);

					// force status when subscribed on google play
					if (mConfiguration.hasSubscribedOnGPlay()) {
						mConfiguration.setHasSubscribed(true);
					}

					int message = -1;
					if (mConfiguration.hasSubscribed()) {
						message = R.string.homescreen_new_member;
					} else {
						message = R.string.homescreen_no_longer_member;
					}
					doneUpdating(message);
				}
			}
			// todo: case of failure
			doneUpdating(-1);
		}
	}

	/**
	 * Display provided message in main thread using a toast.
	 */
	private void doneUpdating(final int message) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				setupUpdatingViews(false);
				if (message != -1) {
					Toast.makeText(HeroesActivity.this, getString(message),
							Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	public String getCurrentUser() {
		return currentUser;
	}

	/**
	 * Sets current logged in user
	 * 
	 * @param currentUser
	 *            current user to set
	 */
	public void setCurrentUser(final String currentUser) {
		this.currentUser = currentUser;
	}
}
