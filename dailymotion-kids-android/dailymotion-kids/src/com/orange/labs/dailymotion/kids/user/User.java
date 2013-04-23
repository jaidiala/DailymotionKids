package com.orange.labs.dailymotion.kids.user;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.orange.labs.dailymotion.kids.db.DatabaseUtils;
import com.orange.labs.dailymotion.kids.user.UserContract.UserColumns;

/**
 * Base class representing a user hosted on Dailymotion.
 * 
 * @author Olivier Briand
 * @author Jean-Francois Moy
 */
public class User {

	private long mId = -1;
	private String mDailymotionId;
	private String mScreenName;
	private String mAvatarUrl;
	private String mCategory;
	private boolean mIsNew = false;
	private State mState = State.STANDARD;

	public User(final String id, final String screenname, final String avatarUrl,
			final String category, final boolean isNew, final State state) {
		mDailymotionId = id;
		mScreenName = screenname;
		mAvatarUrl = avatarUrl;
		mCategory = category;
		mIsNew = isNew;
		mState = state;
	}

	/**
	 * Empty constructor used to build objects from scratch.
	 */
	public User() {
	}

	public long getId() {
		return mId;
	}

	public String getDailymotionId() {
		return mDailymotionId;
	}

	public String getScreenName() {
		return mScreenName;
	}
	
	public String getAvatarUrl() {
		return mAvatarUrl;
	}

	public String getCategory() {
		return mCategory;
	}
	
	public boolean isNew() {
		return mIsNew;
	}
	
	public State getState() {
		return mState;
	}

	private void setId(long id) {
		mId = id;
	}

	/**
	 * Set the ID for the user.
	 */
	public void setDailymotionId(String id) {
		mDailymotionId = id;
	}

	/**
	 * Set the screenname for the user.
	 */
	public void setScreenName(String screenname) {
		mScreenName = screenname;
	}

	/**
	 * Set the avatar for the user.
	 */
	public void setAvatarUrl(String avatarUrl) {
		mAvatarUrl = avatarUrl;
	}

	public void setCategory(String category) {
		mCategory = category;
	}
	
	public void setNew(boolean isNew) {
		mIsNew = isNew;
	}
	
	public void setState(State state) {
		mState = state;
	}

	/**
	 * Return whether the user has an id or not.
	 */
	public boolean hasDailymotionId() {
		return !TextUtils.isEmpty(mDailymotionId);
	}

	public boolean hasId() {
		return (mId != -1);
	}

	/**
	 * Return whether the user has a name or not.
	 */
	public boolean hasScreenName() {
		return !TextUtils.isEmpty(mScreenName);
	}

	/**
	 * Return whether the user has an avatar or not.
	 */
	public boolean hasAvatarUrl() {
		return !TextUtils.isEmpty(mAvatarUrl);
	}

	public boolean hasCategory() {
		return !TextUtils.isEmpty(mCategory);
	}
	
	public boolean hasState() {
		return mState != null;
	}

	public boolean equals(Object o) {
		if (o instanceof User) {
			User user = (User) o;
			if (user == this
					|| (user.getScreenName().equals(getScreenName())
							&& user.getCategory().equals(getCategory())
							&& user.getAvatarUrl().equals(getAvatarUrl()))) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Utility method used to generate a user object from a database record (provided as a
	 * {@link Cursor}).
	 * 
	 * @param cursor
	 *            Cursor containing a user record.
	 * @return User object
	 */
	public static User fromCursor(Cursor cursor) {
		User user = new User();
		user.setId(DatabaseUtils.getLong(cursor, UserColumns.ID));
		user.setDailymotionId(DatabaseUtils.getString(cursor, UserColumns.DAILYMOTION_ID));
		user.setScreenName(DatabaseUtils.getString(cursor, UserColumns.SCREEN_NAME));
		user.setAvatarUrl(DatabaseUtils.getString(cursor, UserColumns.AVATAR_URL));
		user.setCategory(DatabaseUtils.getString(cursor, UserColumns.CATEGORY));
		user.setNew(DatabaseUtils.getInt(cursor, UserColumns.NEW) != 0);
		int stateOrd = DatabaseUtils.getInt(cursor, UserColumns.STATE);
		if (stateOrd > -1 && stateOrd < State.values().length) {
			user.setState(State.values()[stateOrd]);
		} else {
			user.setState(State.STANDARD);
		}
		
		return user;
	}

	/**
	 * Generate a {@link ContentValues} using the attributes of the provided user. The result can be
	 * used to insert or update a record in the database.
	 * 
	 * @return {@link ContentValues} containing user attributes. Empty if the user is null.
	 */
	public ContentValues toContentValues() {
		ContentValues values = new ContentValues();
		if (hasId())
			values.put(UserContract.ID, getId());
		if (hasDailymotionId())
			values.put(UserContract.DAILYMOTION_ID, getDailymotionId());
		if (hasScreenName())
			values.put(UserContract.SCREEN_NAME, getScreenName());
		if (hasAvatarUrl())
			values.put(UserContract.AVATAR_URL, getAvatarUrl());
		if (hasCategory())
			values.put(UserContract.CATEGORY, getCategory());
		if (hasState())
			values.put(UserContract.STATE, getState().ordinal());
		values.put(UserContract.NEW, isNew());
		return values;
	}
	
	/**
	 * Important to keep them in order as we use the ordinal to show the heroes in order.
	 */
	public enum State {
		FAVORITE, STANDARD, HIDDEN
	}
}
