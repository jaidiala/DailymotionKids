package com.orange.labs.dailymotion.kids.user;

import com.orange.labs.dailymotion.kids.db.DatabaseColumn;

public final class UserContract {
	
	public static final String ID = "_id";
	public static final String DAILYMOTION_ID = "dailymotion_id";
	public static final String SCREEN_NAME = "screen_name";
	public static final String AVATAR_URL = "avatar_large_url";
	public static final String CATEGORY = "category";
	public static final String NEW = "new";
	public static final String STATE = "state";
	
	/**
	 * Enumeration that defines the table columns to store a {@link User} instance. 
	 * 
	 * <p>
	 * It associates the name of the column to its SQL type and the version of the database it has been
	 * introduced at.
	 */
	public enum UserColumns implements DatabaseColumn {
		ID(UserContract.ID, "INTEGER PRIMARY KEY AUTOINCREMENT", 1),
		DAILYMOTION_ID(UserContract.DAILYMOTION_ID, "TEXT UNIQUE NOT NULL", 1),
		SCREEN_NAME(UserContract.SCREEN_NAME, "TEXT NOT NULL", 1), 
		AVATAR_URL(UserContract.AVATAR_URL, "TEXT" ,1),
		CATEGORY(UserContract.CATEGORY, "TEXT", 1),
		NEW(UserContract.NEW, "INTEGER", 1),
		STATE(UserContract.STATE, "INTEGER", 1);

		private final String mName;
		private final String mSqlType;
		private final int mSinceVersion;

		private UserColumns(String name, String sqlType, int sinceVersion) {
			mName = name;
			mSqlType = sqlType;
			mSinceVersion = sinceVersion;
		}

		@Override
		public String getName() {
			return mName;
		}

		@Override
		public String getType() {
			return mSqlType;
		}

		@Override
		public int getSinceVersion() {
			return mSinceVersion;
		}

	}

}
