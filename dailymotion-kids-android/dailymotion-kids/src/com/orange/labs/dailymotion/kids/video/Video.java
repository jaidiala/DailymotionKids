package com.orange.labs.dailymotion.kids.video;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.orange.labs.dailymotion.kids.db.DatabaseUtils;
import com.orange.labs.dailymotion.kids.video.VideoContract.VideoColumns;

/**
 * Base class representing a video hosted on Dailymotion.
 * 
 * @author Jean-Francois Moy
 */
public class Video {

	/** Local Id */
	private long mId = -1;

	/** Id of the video */
	private String mDailymotionId;

	/** Video Title */
	private String mTitle;

	/** Video Description */
	private String mDescription;

	/** Thumbnail URL of the video */
	private String mThumbnailUrl;

	/** Owner (creator or uploader) of the video */
	private String mOwner;

	/** Date of the last modification, used to decide if we should update the offline record */
	private long mModifiedTime = -1;

	/** Duration of the video */
	private int mDuration = 0;

	/** Playlist the video belongs to (identified by its id). */
	private String mPlaylist;

	public Video(final String id, final String title, final String description,
			final String thumbnailUrl, final String owner, final long modifiedTime, int duration) {
		mDailymotionId = id;
		mTitle = title;
		mDescription = description;
		mOwner = owner;
		mModifiedTime = modifiedTime;
		mDuration = duration;
	}

	/**
	 * Empty constructor used to build objects from scratch.
	 */
	public Video() {
	}

	/**
	 * Return the local identifier.
	 */
	public long getId() {
		return mId;
	}

	/**
	 * Return the identifier of the video.
	 */
	public String getDailymotionId() {
		return mDailymotionId;
	}

	/**
	 * Return the title of the video.
	 */
	public String getTitle() {
		return mTitle;
	}

	/**
	 * Return the description of the video.
	 */
	public String getDescription() {
		return mDescription;
	}

	/**
	 * Return the thumbnail url of the video
	 */
	public String getThumbnailUrl() {
		return mThumbnailUrl;
	}

	/**
	 * Return the identifier of the video.
	 */
	public String getOwner() {
		return mOwner;
	}

	/**
	 * Return the last time the video has been modified remotely.
	 */
	public long getModifiedTime() {
		return mModifiedTime;
	}

	/**
	 * Return the duration of the video.
	 */
	public int getDuration() {
		return mDuration;
	}

	/**
	 * Return the playlist id this video belongs to.
	 */
	public String getPlaylist() {
		return mPlaylist;
	}

	/**
	 * Set the local ID
	 */
	public void setId(long id) {
		mId = id;
	}

	/**
	 * Set the ID for the video.
	 */
	public void setDailymotionId(String id) {
		mDailymotionId = id;
	}

	/**
	 * Set the title for the video.
	 */
	public void setTitle(String title) {
		mTitle = title;
	}

	/**
	 * Set the description for the video
	 */
	public void setDescription(String description) {
		mDescription = description;
	}

	/**
	 * Set the owner for the video.
	 */
	public void setOwner(String owner) {
		mOwner = owner;
	}

	/**
	 * Set the thumbnail URL for the video.
	 */
	public void setThumbnailUrl(String thumbnailUrl) {
		mThumbnailUrl = thumbnailUrl;
	}

	/**
	 * Set the time that the video has been last modified at.
	 */
	public void setModifiedTime(long modifiedTime) {
		mModifiedTime = modifiedTime;
	}

	/**
	 * Set the duration of the video.
	 */
	public void setDuration(int duration) {
		mDuration = duration;
	}

	/**
	 * Set the playlist the video belongs to.
	 */
	public void setPlaylist(String playlistId) {
		mPlaylist = playlistId;
	}

	public boolean hasId() {
		return (mId != -1);
	}

	/**
	 * Return whether the video has an id or not.
	 */
	public boolean hasDailymotionId() {
		return !TextUtils.isEmpty(mDailymotionId);
	}

	/**
	 * Return whether the video has a title or not.
	 */
	public boolean hasTitle() {
		return !TextUtils.isEmpty(mTitle);
	}

	/**
	 * Return whether the video has a description or not.
	 */
	public boolean hasDescription() {
		return !TextUtils.isEmpty(mDescription);
	}

	/**
	 * Return whether the video has an owner or not.
	 */
	public boolean hasOwner() {
		return !TextUtils.isEmpty(mOwner);
	}

	/**
	 * Return whether the video has a thumbnail url or not.
	 */
	public boolean hasThumbnailUrl() {
		return !TextUtils.isEmpty(mThumbnailUrl);
	}

	/**
	 * Return whether the video has a modified time.
	 */
	public boolean hasModifiedTime() {
		return mModifiedTime != -1;
	}

	/**
	 * Return whether the video has a duration
	 */
	public boolean hasDuration() {
		return mDuration != -1;
	}

	/**
	 * Return whether the video belongs to a playlist.
	 */
	public boolean hasPlaylist() {
		return !TextUtils.isEmpty(mPlaylist);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("Video: [\n");
		builder.append("ID : ").append(mId).append("\n");
		builder.append("Dailymotion ID : ").append(mDailymotionId).append("\n");
		builder.append("Title : ").append(mTitle).append("\n");
		builder.append("Description : ").append(mDescription).append("\n");
		builder.append("Thumbnail Url : ").append(mThumbnailUrl).append("\n");
		builder.append("Owner : ").append(mOwner).append("\n");
		builder.append("Modified : ").append(mModifiedTime).append("\n");
		builder.append("Duration : ").append(mDuration).append("\n");
		builder.append("Playlist : ").append(mPlaylist).append("\n");
		builder.append("]");
		return builder.toString();
	}

	/**
	 * Factory method used to generate a video object from a database record (provided as a
	 * {@link Cursor}).
	 * 
	 * @param cursor
	 *            Cursor containing a video record.
	 * @return Video object
	 */
	public static Video fromCursor(Cursor cursor) {
		Video video = new Video();
		video.setId(DatabaseUtils.getLong(cursor, VideoColumns.ID));
		video.setDailymotionId(DatabaseUtils.getString(cursor, VideoColumns.DAILYMOTION_ID));
		video.setTitle(DatabaseUtils.getString(cursor, VideoColumns.TITLE));
		video.setDescription(DatabaseUtils.getString(cursor, VideoColumns.DESCRIPTION));
		video.setOwner(DatabaseUtils.getString(cursor, VideoColumns.OWNER));
		video.setThumbnailUrl(DatabaseUtils.getString(cursor, VideoColumns.THUMBNAIL_URL));
		video.setModifiedTime(DatabaseUtils.getLong(cursor, VideoColumns.MODIFIED_TIME));
		video.setDuration(DatabaseUtils.getInt(cursor, VideoColumns.DURATION));
		video.setPlaylist(DatabaseUtils.getString(cursor, VideoColumns.PLAYLIST));
		return video;
	}

	public ContentValues toContentValues() {
		ContentValues values = new ContentValues();
		if (hasId())
			values.put(VideoContract.ID, getId());
		if (hasDailymotionId())
			values.put(VideoContract.DAILYMOTION_ID, getDailymotionId());
		if (hasTitle())
			values.put(VideoContract.TITLE, getTitle());
		if (hasDescription())
			values.put(VideoContract.DESCRIPTION, getDescription());
		if (hasOwner())
			values.put(VideoContract.OWNER, getOwner());
		if (hasThumbnailUrl())
			values.put(VideoContract.THUMBNAIL_URL, getThumbnailUrl());
		if (hasModifiedTime())
			values.put(VideoContract.MODIFIED_TIME, getModifiedTime());
		if (hasDuration())
			values.put(VideoContract.DURATION, getDuration());
		if (hasPlaylist())
			values.put(VideoContract.PLAYLIST, getPlaylist());

		return values;
	}
	
	public boolean equals(Object o) {
		if (o instanceof Video) {
			Video video = (Video) o;
			if (video == this || (video.getTitle().equals(getTitle())
					&& video.getOwner().equals(getOwner())
					&& video.getThumbnailUrl().equals(getThumbnailUrl())
					&& video.getDescription().equals(getDescription())
					&& video.getDuration() == getDuration())
					&& video.getPlaylist().equals(getPlaylist())
					&& video.getThumbnailUrl().equals(getThumbnailUrl())) {
				return true;
			}
		}

		return false;
	}

}
