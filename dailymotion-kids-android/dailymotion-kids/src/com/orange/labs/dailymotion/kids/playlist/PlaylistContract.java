package com.orange.labs.dailymotion.kids.playlist;

import com.orange.labs.dailymotion.kids.db.DatabaseColumn;
import com.orange.labs.dailymotion.kids.user.UserContract;
import com.orange.labs.dailymotion.kids.user.UsersProvider;

public final class PlaylistContract {

	public static final String ID = "_id";
	public static final String DAILYMOTION_ID = "dailymotion_id";
	public static final String NAME = "name";
	public static final String OWNER = "owner";
	public static final String THUMBNAIL_URL = "thumbnail_url";
	public static final String CATEGORY = "category";
	public static final String SEASON = "season";
	public static final String LAST_UPDATE = "last_update";
	public static final String TABLE_CONSTRAINTS = String.format(
			"FOREIGN KEY(%s) REFERENCES %s(%s) ON DELETE CASCADE", OWNER, UsersProvider.TABLE_NAME,
			UserContract.DAILYMOTION_ID);
	
	/**
	 * Enumeration that defines the table columns to store a {@link Playlist} instance. 
	 * 
	 * <p>
	 * It associates the name of the column to its SQL type and the version of the database it has been
	 * introduced at.
	 */
	public enum PlaylistColumns implements DatabaseColumn {
		ID(PlaylistContract.ID, "INTEGER PRIMARY KEY AUTOINCREMENT", 1),
		DAILYMOTION_ID(PlaylistContract.DAILYMOTION_ID, "TEXT NOT NULL UNIQUE", 1),
		NAME(PlaylistContract.NAME, "TEXT NOT NULL", 1),
		OWNER(PlaylistContract.OWNER, "TEXT NOT NULL", 1),
		THUMBNAIL_URL(PlaylistContract.THUMBNAIL_URL, "TEXT", 1),
		CATEGORY(PlaylistContract.CATEGORY, "TEXT", 1),
		SEASON(PlaylistContract.SEASON, "INTEGER", 1),
		LAST_UPDATE(PlaylistContract.LAST_UPDATE, "INTEGER", 1);

		private final String mName;
		private final String mSqlType;
		private final int mSinceVersion;

		private PlaylistColumns(String name, String sqlType, int sinceVersion) {
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
