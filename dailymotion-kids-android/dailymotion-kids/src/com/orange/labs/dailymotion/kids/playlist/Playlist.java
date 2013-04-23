package com.orange.labs.dailymotion.kids.playlist;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.orange.labs.dailymotion.kids.db.DatabaseUtils;
import com.orange.labs.dailymotion.kids.playlist.PlaylistContract.PlaylistColumns;

/**
 * Base class representing a playlist hosted on Dailymotion.
 * 
 * @author Olivier Briand
 * @author Jean-Francois Moy
 */
public class Playlist {

	private long mId = -1;

	/** Id of the playlist */
	private String mDailymotionId;

	/** Playlist Name */
	private String mName;

	/** Owner (creator or uploader) of the playlist */
	private String mOwner;

	/** Thumbnail Url */
	private String mThumbnailUrl;
	
	/** Season Number this playlist corresponds to */
	private int mSeason = -1;
	
	/** Last Update Time */
	private long mLastUpdate = -1;

	public Playlist(final String id, final String name, final String owner,
			final String thumbnailUrl, final int season, final long lastUpdate) {
		this();
		mDailymotionId = id;
		mName = name;
		mOwner = owner;
		mThumbnailUrl = thumbnailUrl;
		mSeason = season;
		mLastUpdate = lastUpdate;
	}

	/**
	 * Empty constructor used to build objects from scratch.
	 */
	public Playlist() {
	}

	public long getId() {
		return mId;
	}

	public String getDailymotionId() {
		return mDailymotionId;
	}
	
	public String getName() {
		return mName;
	}

	public String getOwner() {
		return mOwner;
	}

	public String getThumbnailUrl() {
		return mThumbnailUrl;
	}
	
	public int getSeason() {
		return mSeason;
	}
	
	public long getLastUpdate() {
		return mLastUpdate;
	}

	public void setId(long id) {
		mId = id;
	}

	public void setDailymotionId(String id) {
		mDailymotionId = id;
	}

	public void setName(String name) {
		mName = name;
	}

	public void setOwner(String owner) {
		mOwner = owner;
	}

	public void setThumbnailUrl(String url) {
		mThumbnailUrl = url;
	}
	
	public void setSeason(int season) {
		mSeason = season;
	}
	
	public void setLastUpdate(long lastUpdate) {
		mLastUpdate = lastUpdate;
	}

	public boolean hasId() {
		return (mId != -1);
	}

	public boolean hasDailymotionId() {
		return !TextUtils.isEmpty(mDailymotionId);
	}

	public boolean hasName() {
		return !TextUtils.isEmpty(mName);
	}

	public boolean hasOwner() {
		return !TextUtils.isEmpty(mOwner);
	}

	public boolean hasThumbnailUrl() {
		return !TextUtils.isEmpty(mThumbnailUrl);
	}
	
	public boolean hasSeason() {
		return (mSeason != -1);
	}
	
	public boolean hasLastUpdate() {
		return (mLastUpdate != -1);
	}
	
	public boolean equals(Object o) {
		if (o instanceof Playlist) {
			Playlist playlist = (Playlist) o;
			if (playlist == this || (playlist.getName().equals(getName())
					&& playlist.getOwner().equals(getOwner())
					&& playlist.getThumbnailUrl().equals(getThumbnailUrl()))) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Utility method used to generate a playlist object from a database record (provided as a
	 * {@link Cursor}).
	 * 
	 * @param cursor
	 *            Cursor containing a playlist record.
	 * @return Playlist object
	 */
	public static Playlist fromCursor(Cursor cursor) {
		Playlist playlist = new Playlist();
		playlist.setId(DatabaseUtils.getLong(cursor, PlaylistColumns.ID));
		playlist.setDailymotionId(DatabaseUtils.getString(cursor, PlaylistColumns.DAILYMOTION_ID));
		playlist.setName(DatabaseUtils.getString(cursor, PlaylistColumns.NAME));
		playlist.setOwner(DatabaseUtils.getString(cursor, PlaylistColumns.OWNER));
		playlist.setThumbnailUrl(DatabaseUtils.getString(cursor, PlaylistColumns.THUMBNAIL_URL));
		playlist.setSeason(DatabaseUtils.getInt(cursor, PlaylistColumns.SEASON));
		playlist.setLastUpdate(DatabaseUtils.getLong(cursor, PlaylistColumns.LAST_UPDATE));
		return playlist;
	}

	/**
	 * Generate a {@link ContentValues} using the attributes of the provided playlist. The result
	 * can be used to insert or update a record in the database.
	 * 
	 * @return {@link ContentValues} containing playlist attributes. Empty if the playlist is null.
	 */
	public ContentValues toContentValues() {
		ContentValues values = new ContentValues();
		if (hasId())
			values.put(PlaylistContract.ID, getId());
		if (hasDailymotionId())
			values.put(PlaylistContract.DAILYMOTION_ID, getDailymotionId());
		if (hasName())
			values.put(PlaylistContract.NAME, getName());
		if (hasOwner())
			values.put(PlaylistContract.OWNER, getOwner());
		if (hasThumbnailUrl())
			values.put(PlaylistContract.THUMBNAIL_URL, getThumbnailUrl());
		if (hasSeason())
			values.put(PlaylistContract.SEASON, getSeason());
		if (hasLastUpdate())
			values.put(PlaylistContract.LAST_UPDATE, getLastUpdate());
		return values;
	}

}
