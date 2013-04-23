package com.orange.labs.dailymotion.kids.video;

import com.orange.labs.dailymotion.kids.db.DatabaseColumn;
import com.orange.labs.dailymotion.kids.playlist.PlaylistContract;
import com.orange.labs.dailymotion.kids.playlist.PlaylistsProvider;

public final class VideoContract {
	public static final String ID = "_id";
	public static final String DAILYMOTION_ID = "dailymotion_id";
	public static final String TITLE = "title";
	public static final String DESCRIPTION = "description";
	public static final String THUMBNAIL_URL = "thumbnail_url";
	public static final String OWNER = "owner";
	public static final String MODIFIED_TIME = "modified_time";
	public static final String DURATION = "duration";
	public static final String PLAYLIST = "playlist";

	public static final String TABLE_CONSTRAINTS = String.format(
			" UNIQUE (%s, %s) FOREIGN KEY(%s) REFERENCES %s(%s) ON DELETE CASCADE", DAILYMOTION_ID,
			PLAYLIST, PLAYLIST, PlaylistsProvider.TABLE_NAME, PlaylistContract.DAILYMOTION_ID);

	/**
	 * Enumeration that defines the table columns to store a {@link Video} instance.
	 * 
	 * <p>
	 * It associates the name of the column to its SQL type and the version of the database it has
	 * been introduced at.
	 */
	// TODO: Dailymotion ID is marked as unique, but the pair (playlist,dailymotion_id) should be
	// unique instead.
	public enum VideoColumns implements DatabaseColumn {
		ID(VideoContract.ID, "INTEGER PRIMARY KEY AUTOINCREMENT", 1), DAILYMOTION_ID(
				VideoContract.DAILYMOTION_ID, "TEXT NOT NULL", 1), TITLE(VideoContract.TITLE,
				"TEXT NOT NULL", 1), DESCRIPTION(VideoContract.DESCRIPTION, "TEXT", 1), THUMBNAIL_URL(VideoContract.THUMBNAIL_URL,
				"TEXT", 1), OWNER(VideoContract.OWNER, "TEXT NOT NULL", 1), MODIFIED_TIME(
				VideoContract.MODIFIED_TIME, "INTEGER", 1), DURATION(VideoContract.DURATION,
				"INTEGER", 1), PLAYLIST(VideoContract.PLAYLIST, "TEXT", 1);

		private final String mName;
		private final String mSqlType;
		private final int mSinceVersion;

		private VideoColumns(String name, String sqlType, int sinceVersion) {
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
