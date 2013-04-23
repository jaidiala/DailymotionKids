package com.orange.labs.dailymotion.kids.video;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;

import com.orange.labs.dailymotion.kids.callback.Callback;
import com.orange.labs.dailymotion.kids.config.Constants;
import com.orange.labs.dailymotion.kids.db.DatabaseUtils;
import com.orange.labs.dailymotion.kids.playlist.Playlist;
import com.orange.labs.dailymotion.kids.playlist.PlaylistContract;
import com.orange.labs.dailymotion.kids.playlist.PlaylistsProvider;
import com.orange.labs.dailymotion.kids.user.User;
import com.orange.labs.dailymotion.kids.user.UserContract;
import com.orange.labs.dailymotion.kids.user.UsersProvider;
import com.orange.labs.dailymotion.kids.utils.KidsLogger;
import com.orangelabs.dailymotion.DailyException;
import com.orangelabs.dailymotion.Dailymotion;
import com.orangelabs.dailymotion.DailymotionRequestor;
import com.orangelabs.dailymotion.DailymotionRequestor.DailymotionRequestListener;

/**
 * Object responsible of fetching videos. An instance of this class should be instantiated for each
 * fetching operation. The class will refuse to operate if you try to fetch several objects on the
 * same instance.
 * 
 * @author Jean-Francois Moy
 */
public class AsyncVideosFetcher {

	private static final String LOG_TAG = "Video Fetcher";

	/**
	 * Fields the fetcher will request to the server as a String (comma separated).
	 */
	private static final String FIELDS = "id,title,owner.id,thumbnail_url,description,modified_time,duration";

	/**
	 * Endpoint when fetching videos that belong to a playlist.
	 */
	private static final String PLAYLIST_ENDPOINT = "playlist/%s/videos";

	/**
	 * Dailymotion object used to communicate with Dailymotion Graph API and download playlist
	 * content.
	 */
	private final Dailymotion mDailymotion;

	/**
	 * Flag that indicates if a fetch operation has been launched for that specific instance.
	 */
	private final AtomicBoolean mFetching = new AtomicBoolean(false);

	/**
	 * List containing all videos that have been fetched up to that point. As playlists somestimes
	 * store several pages of videos, we store them progressively before executing the callback and
	 * providing them back to the caller.
	 */
	private List<Video> mFetchedVideos = new ArrayList<Video>();

	/**
	 * Video set we compare the results we receive to. Typically, if we fetch videos for a specific
	 * user or playlist, a set of actual records that exist in the application database for that
	 * user or playlist should be fetched and compared against the result. As a result, only new and
	 * updated records would be written in the database.
	 * 
	 * This is done exclusively when the fetcher is not in replace all mode.
	 */
	private Map<String, Video> mLocalVideos;

	private String mEndPoint;
	private Playlist mPlaylist;

	/**
	 * Callback used for giving the result back to the caller.
	 */
	private Callback<List<Video>> mCallback;

	/**
	 * Content Resolver - Used to insert and update records in the database.
	 */
	private ContentResolver mContentResolver;

	private static final Pattern sTitlePattern = Pattern.compile(Constants.TITLE_REGEXP,
			Pattern.CASE_INSENSITIVE);
	/** Pattern used to remove the owner name from the episode title */
	private Pattern mOwnerPattern;

	public AsyncVideosFetcher(Dailymotion dailymotion, Context context) {
		mDailymotion = dailymotion;
		mContentResolver = context.getContentResolver();
	}

	/**
	 * Fetch the videos that belong to the provided playlist, and return the result back through the
	 * provided callback. The callback is called once per result page.
	 */
	public void fetch(final Playlist playlist, final Callback<List<Video>> callback) {
		if (playlist == null || !playlist.hasDailymotionId()) {
			throw new IllegalArgumentException("Invalid playlist");
		} else if (mFetching.getAndSet(true)) {
			throw new IllegalStateException("AsyncVideosFetcher can only be used once.");
		}

		mCallback = callback;
		mEndPoint = String.format(PLAYLIST_ENDPOINT, playlist.getDailymotionId());
		mPlaylist = playlist;
		mOwnerPattern = getOwnerRegularExpression(mPlaylist);
		mLocalVideos = getLocalVideos(playlist);
		fetchPage(1);
	}

	private Pattern getOwnerRegularExpression(final Playlist playlist) {
		Pattern pattern = null;
		Cursor cursor = mContentResolver.query(UsersProvider.CONTENT_URI,
				new String[] { UserContract.SCREEN_NAME }, UserContract.DAILYMOTION_ID + "=?",
				new String[] { playlist.getOwner() }, null);
		if (cursor.moveToFirst()) {
			User user = User.fromCursor(cursor);
			pattern = Pattern.compile(String.format(Constants.OWNER_REGEXP, user.getScreenName()),
					Pattern.CASE_INSENSITIVE);
		}
		DatabaseUtils.closeQuietly(cursor);
		return pattern;
	}

	/**
	 * Build a map associating the id of a video to the object itself and returns it.
	 */
	private Map<String, Video> getLocalVideos(final Playlist playlist) {
		Map<String, Video> videosMap = new HashMap<String, Video>();
		Cursor cursor = mContentResolver.query(VideosProvider.CONTENT_URI, null,
				VideoContract.PLAYLIST + "=?", new String[] { playlist.getDailymotionId() }, null);
		while (cursor.moveToNext()) {
			Video video = Video.fromCursor(cursor);
			videosMap.put(video.getDailymotionId(), video);
		}
		cursor.close();
		KidsLogger.i(LOG_TAG, videosMap.size() + " local videos");
		return videosMap;
	}

	/**
	 * Fetch the provided page of videos for the Dailymotion end point generated based on the object
	 * we need to fetch the videos for.
	 * 
	 * @param page
	 *            Page to fetch as an int.
	 */
	private void fetchPage(int page) {
		KidsLogger.d(LOG_TAG, String.format("Fetching videos from %s, page : %d", mEndPoint, page));

		Bundle bundle = new Bundle();
		bundle.putString("page", String.valueOf(page));
		bundle.putString("limit", String.valueOf(Constants.PAGE_LIMIT));
		bundle.putString("fields", FIELDS);

		DailymotionRequestor requestor = new DailymotionRequestor(mDailymotion);
		requestor.request(mEndPoint, bundle, new VideosFetchingRequestListener());
	}

	private void updatePlaylistUpdateDate(boolean success) {
		long date = (success) ? new Date().getTime() : -1;
		mPlaylist.setLastUpdate(date);
		mContentResolver.update(PlaylistsProvider.CONTENT_URI, mPlaylist.toContentValues(),
				PlaylistContract.DAILYMOTION_ID + "=?",
				new String[] { mPlaylist.getDailymotionId() });
	}

	private class VideosFetchingRequestListener implements DailymotionRequestListener {

		@Override
		public void onSuccess(JSONObject response) {
			JSONArray array = response.optJSONArray("list");

			List<Video> videosToInsert = parseVideos(array);
			if (!insertNewVideos(videosToInsert)) {
				mCallback.onFailure(new IOException("Impossible to insert videos in db."));
				return;
			}

			if (response.optBoolean("has_more") && response.optInt("page") != 0) {
				fetchPage(response.optInt("page") + 1);
			} else {
				updatePlaylistUpdateDate(true);
				deleteOutdatedVideos(); // TODO: Should we consider a failure critical?
				mCallback.onSuccess(mFetchedVideos);
			}
		}

		private boolean deleteOutdatedVideos() {
			if (mLocalVideos.size() > 0) {
				int count = mLocalVideos.size();
				KidsLogger.i(LOG_TAG, String.format("Removing %d videos from db...", count));
				ArrayList<ContentProviderOperation> op = new ArrayList<ContentProviderOperation>(
						count);
				for (String key : mLocalVideos.keySet()) {
					ContentProviderOperation operation = ContentProviderOperation
							.newDelete(VideosProvider.CONTENT_URI)
							.withSelection(VideoContract.DAILYMOTION_ID + "=?",
									new String[] { key }).build();
					op.add(operation);
				}
				try {
					return mContentResolver.applyBatch(VideosProvider.AUTHORITY, op).length == count;
				} catch (RemoteException e) {
					// Should not happen, the Content Provider is local.
					KidsLogger.w(LOG_TAG,
							"An exception occurred while communicating with provider.");
					return false;
				} catch (OperationApplicationException e) {
					KidsLogger.w(LOG_TAG, "An operation has failed when deleting local videos.");
					return false;
				}
			}

			return true;
		}

		private boolean insertNewVideos(List<Video> videos) {
			final int count = videos.size();
			if (count > 0) {
				KidsLogger.d(LOG_TAG, String.format("Inserting %d videos", count));
				ContentValues[] values = new ContentValues[count];
				for (int i = 0; i < count; ++i) {
					values[i] = videos.get(i).toContentValues();
				}

				return (mContentResolver.bulkInsert(VideosProvider.CONTENT_URI, values) == count);
			}

			return true;
		}

		private List<Video> parseVideos(JSONArray array) {
			List<Video> videosToInsert = new ArrayList<Video>();
			if (array != null) {
				for (int i = 0; i < array.length(); ++i) {
					JSONObject object = array.optJSONObject(i);
					if (object != null) {
						Video remoteVideo = AsyncVideosFetcher.videoFromJson(object, mPlaylist,
								mOwnerPattern);
						mFetchedVideos.add(remoteVideo);

						String remoteId = remoteVideo.getDailymotionId();
						if (!mLocalVideos.containsKey(remoteId)) {
							videosToInsert.add(remoteVideo);
						} else {
							Video localVideo = mLocalVideos.get(remoteId);
							if (remoteVideo.getModifiedTime() != localVideo.getModifiedTime()
									&& !remoteVideo.equals(localVideo)) {
								videosToInsert.add(remoteVideo);
							}
							mLocalVideos.remove(remoteId);
						}
					}
				}
			}
			return videosToInsert;
		}

		@Override
		public void onFailure(DailyException e) {
			updatePlaylistUpdateDate(false);
			mCallback.onFailure(e);
		}
	}

	/**
	 * Factory method used to create an instance of video based on a Json Object.
	 * 
	 * @param object
	 *            JSON Object to parse.
	 * @return Corresponding video object.
	 */
	private static Video videoFromJson(JSONObject object, Playlist playlist, Pattern ownerPattern) {
		// Remove garbage from titles.
		String title = sTitlePattern.matcher(object.optString("title", "")).replaceAll("");
		if (ownerPattern != null) {
			title = ownerPattern.matcher(title).replaceAll("");
		}

		Video video = new Video();
		video.setDailymotionId(object.optString("id"));
		video.setTitle(title);
		video.setDescription(object.optString("description"));
		video.setOwner(object.optString("owner.id"));
		video.setThumbnailUrl(object.optString("thumbnail_url"));
		video.setModifiedTime(object.optLong("modified_time", -1));
		video.setDuration(object.optInt("duration", -1));
		video.setPlaylist(playlist.getDailymotionId());
		return video;
	}

}
