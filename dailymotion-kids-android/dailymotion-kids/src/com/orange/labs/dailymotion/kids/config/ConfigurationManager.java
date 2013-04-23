package com.orange.labs.dailymotion.kids.config;

import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class ConfigurationManager {

	private static final String DAILYMOTION_KIDS_PREFS = "dailymotion_kids_prefs";

	private static final String ACCESS_TOKEN_KEY = "access_token";

	private static final String REFRESH_TOKEN_KEY = "refresh_token";

	private static final String TOKEN_EXPIRATION_DATE_KEY = "token_expiration_date";
	
	private static final String USER_ACCESS_TOKEN_KEY = "user_access_token";

	private static final String USER_REFRESH_TOKEN_KEY = "user_refresh_token";

	private static final String USER_TOKEN_EXPIRATION_DATE_KEY = "user_token_expiration_date";

	private static final String LAST_UPDATE_SUCCESS_KEY = "last_update_success";

	private static final String LAST_UPDATE_TIME_KEY = "last_update_time";
	
	private static final String HAS_SUBSCRIBED_KEY = "has_subscribed";
	
	private static final String HAS_SUBSCRIBED_ON_GPLAY_KEY = "has_subscribed_on_gplay";

	private static final String HAS_SUBSCRIBED_ON_AMAZON_KEY = "has_subscribed_on_amazon";
	
	private SharedPreferences mPreferences;

	public ConfigurationManager(Context context) {
		mPreferences = context.getSharedPreferences(DAILYMOTION_KIDS_PREFS, Context.MODE_PRIVATE);
	}

	/**
	 * Store the Dailymotion access token information, including the refresh token and expiration
	 * date.
	 */
	public void storeAccessToken(String accessToken, String refreshToken, long expirationDate) {
		Editor editor = mPreferences.edit();
		editor.putString(ACCESS_TOKEN_KEY, accessToken);
		editor.putString(REFRESH_TOKEN_KEY, refreshToken);
		editor.putLong(TOKEN_EXPIRATION_DATE_KEY, expirationDate);
		editor.commit();
	}

	/**
	 * Store the Dailymotion Kids user access token information, including the refresh token used to
	 * refresh it in the future, as well as its expiration time.
	 */
	public void storeUserAccessToken(String accessToken, String refreshToken, long expirationDate) {
		Editor editor = mPreferences.edit();
		editor.putString(USER_ACCESS_TOKEN_KEY, accessToken);
		editor.putString(USER_REFRESH_TOKEN_KEY, refreshToken);
		editor.putLong(USER_TOKEN_EXPIRATION_DATE_KEY, expirationDate);
		editor.commit();
	}
	
	public boolean hasSubscribed() {
		return mPreferences.getBoolean(HAS_SUBSCRIBED_KEY, false);
	}
	
	public void setHasSubscribed(boolean subscribed) {
		Editor  editor = mPreferences.edit();
		editor.putBoolean(HAS_SUBSCRIBED_KEY, subscribed);
		editor.commit();
	}
	
	public boolean hasSubscribedOnGPlay() {
		return mPreferences.getBoolean(HAS_SUBSCRIBED_ON_GPLAY_KEY, false);
	}
	
	public void setHasSubscribedOnGPlay(boolean subscribed) {
		Editor  editor = mPreferences.edit();
		editor.putBoolean(HAS_SUBSCRIBED_ON_GPLAY_KEY, subscribed);
		editor.commit();
	}
	
	public boolean hasSubscribedOnAmazon() {
		return mPreferences.getBoolean(HAS_SUBSCRIBED_ON_AMAZON_KEY, false);
	}
	
	public void setHasSubscribedOnAmazon(boolean subscribed) {
		Editor  editor = mPreferences.edit();
		editor.putBoolean(HAS_SUBSCRIBED_ON_AMAZON_KEY, subscribed);
		editor.commit();
	}

	/**
	 * Returns the stored access token for the Dailymotion API.
	 */
	public String getAccessToken() {
		return mPreferences.getString(ACCESS_TOKEN_KEY, "");
	}
	
	public String getUserAccessToken() {
		return mPreferences.getString(USER_ACCESS_TOKEN_KEY, "");
	}

	/**
	 * Returns the stored access token for the Dailymotion API.
	 */
	public String getRefreshToken() {
		return mPreferences.getString(REFRESH_TOKEN_KEY, "");

	}
	
	public String getUserRefreshToken() {
		return mPreferences.getString(REFRESH_TOKEN_KEY, "");
	}

	/**
	 * Returns the stored access token for the Dailymotion API.
	 */
	public long getExpirationDate() {
		return mPreferences.getLong(TOKEN_EXPIRATION_DATE_KEY, -1);
	}
	
	public long getUserExpirationDate() {
		return mPreferences.getLong(USER_TOKEN_EXPIRATION_DATE_KEY, -1);
	}

	/**
	 * Returns if the last update has succeeded or failed.
	 */
	public boolean hasLastUpdateSucceeded() {
		return mPreferences.getBoolean(LAST_UPDATE_SUCCESS_KEY, false);
	}

	/**
	 * Returns the {@link Date} the last update of the local cache has occurred. If no update has
	 * occured yet, returns the 1st January 1970.
	 */
	public Date getLastUpdate() {
		return new Date(mPreferences.getLong(LAST_UPDATE_TIME_KEY, 0));
	}

	/**
	 * Should be executed after each update to indicate if it has succeeded or not. The update time
	 * is updated at the same time using the current time.
	 */
	public void setLastUpdateSucceeded(boolean success) {
		Editor editor = mPreferences.edit();
		editor.putBoolean(LAST_UPDATE_SUCCESS_KEY, success);
		editor.putLong(LAST_UPDATE_TIME_KEY, new Date().getTime());
		editor.commit();
	}

}
