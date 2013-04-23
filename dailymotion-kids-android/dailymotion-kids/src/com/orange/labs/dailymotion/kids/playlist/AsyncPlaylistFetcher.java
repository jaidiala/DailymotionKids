package com.orange.labs.dailymotion.kids.playlist;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
import com.orange.labs.dailymotion.kids.user.User;
import com.orange.labs.dailymotion.kids.utils.KidsLogger;
import com.orangelabs.dailymotion.DailyException;
import com.orangelabs.dailymotion.Dailymotion;
import com.orangelabs.dailymotion.DailymotionRequestor;
import com.orangelabs.dailymotion.DailymotionRequestor.DailymotionRequestListener;

/**
 * Object responsible of fetching playlists. An instance of this class should be instantiated for
 * each fetching operation. The class will refuse to operate if you try to fetch several objects on
 * the same instance.
 * 
 * @author Jean-Francois Moy
 */
public class AsyncPlaylistFetcher {

	private static final String USER_PLAYLISTS_ENDPOINT = "user/%s/playlists";

	private static final String FIELDS = "id,name,owner,thumbnail_url";

	private static final String LOG_TAG = "Playlists Fetcher";

	private final Dailymotion mDailymotion;
	private final AtomicBoolean mUsed = new AtomicBoolean(false);

	private final List<Playlist> mFetchedPlaylists = new ArrayList<Playlist>();
	private Map<String, Playlist> mLocalPlaylists;

	private String mEndPoint;
	private ContentResolver mResolver;

	/**
	 * Callback used for giving the result back to the caller.
	 */
	private Callback<List<Playlist>> mCallback;

	public AsyncPlaylistFetcher(Dailymotion dailymotion, Context context) {
		mDailymotion = dailymotion;
		mResolver = context.getContentResolver();
	}

	public void fetch(final User user, final Callback<List<Playlist>> callback) {
		if (user == null || !user.hasDailymotionId()) {
			throw new IllegalArgumentException("Invalid user");
		} else if (mUsed.getAndSet(true)) {
			throw new IllegalStateException("AsyncPlaylistFetcher can only be used once.");
		}

		mCallback = callback;
		mEndPoint = String.format(USER_PLAYLISTS_ENDPOINT, user.getDailymotionId());
		mLocalPlaylists = getLocalPlaylists(user);
		fetchPage(1);
	}

	/**
	 * Get user's playlist already inserted in the local database. It allows us later on to do a
	 * comparison with the fetched one, and insert only the new and updated ones. The loss in memory
	 * is small compared to the gain in Database access.
	 */
	private Map<String, Playlist> getLocalPlaylists(final User owner) {
		Map<String, Playlist> playlists = new HashMap<String, Playlist>();
		Cursor cursor = mResolver.query(PlaylistsProvider.CONTENT_URI, null, PlaylistContract.OWNER
				+ "=?", new String[] { owner.getDailymotionId() }, null);
		while (cursor.moveToNext()) {
			Playlist playlist = Playlist.fromCursor(cursor);
			playlists.put(playlist.getDailymotionId(), playlist);
		}
		cursor.close();
		return playlists;
	}

	/**
	 * Fetch the provided page of playlists for the Dailymotion end point generated based on the
	 * object we need to fetch the playlists for.
	 * 
	 * @param page
	 *            Page to fetch as an int.
	 */
	private void fetchPage(int page) {
		KidsLogger.d(LOG_TAG, String.format("Fetching playlists at %s,page : %d", mEndPoint, page));

		Bundle bundle = new Bundle();
		bundle.putString("page", String.valueOf(page));
		bundle.putString("fields", FIELDS);
		bundle.putString("limit", String.valueOf(Constants.PAGE_LIMIT));

		DailymotionRequestor requestor = new DailymotionRequestor(mDailymotion);
		requestor.anonymousRequest(mEndPoint, bundle, new PlaylistsFetchingRequestListener());
	}

	/**
	 * Factory method used to create an instance of playlist based on a Json Object.
	 * 
	 * @param object
	 *            JSON Object to parse.
	 * @return Corresponding playlist object.
	 */
	public static Playlist playlistFromJson(JSONObject object) {
		String name = object.optString("name");
		Playlist playlist = new Playlist();
		playlist.setDailymotionId(object.optString("id"));
		playlist.setName(name);
		playlist.setOwner(object.optString("owner"));
		playlist.setThumbnailUrl(object.optString("thumbnail_url"));
		if (name != null) {
			String seasonNumber = name.toLowerCase().replace("saison ", "").replace("season ", "");
			try {
				int season = Integer.parseInt(seasonNumber);
				playlist.setSeason(season);
			} catch (NumberFormatException e) { /* Do nothing we just don't use it. */ }
		}
		return playlist;
	}

	private class PlaylistsFetchingRequestListener implements DailymotionRequestListener {

		@Override
		public void onSuccess(JSONObject response) {
			JSONArray array = response.optJSONArray("list");

			List<Playlist> playlistsToInsert = parsePlaylists(array);
			if (!insertNewPlaylists(playlistsToInsert)) {
				mCallback.onFailure(new IOException("Impossible to insert playlists in db."));
				return;
			}

			if (response.optBoolean("has_more") && response.optInt("page") != 0) {
				fetchPage(response.optInt("page") + 1);
			} else {
				deleteOutdatedPlaylists(); // TODO: Should we consider a failure critical?
				mCallback.onSuccess(mFetchedPlaylists);
			}
		}

		private List<Playlist> parsePlaylists(JSONArray array) {
			List<Playlist> playlistsToInsert = new ArrayList<Playlist>();
			if (array != null) {
				for (int i = 0; i < array.length(); ++i) {
					JSONObject object = array.optJSONObject(i);
					if (object != null) {
						Playlist remotePlaylist = AsyncPlaylistFetcher.playlistFromJson(object);
						mFetchedPlaylists.add(remotePlaylist);

						String remoteId = remotePlaylist.getDailymotionId();
						if (!mLocalPlaylists.containsKey(remoteId)) {
							playlistsToInsert.add(remotePlaylist);
						} else {
							if (!remotePlaylist.equals(mLocalPlaylists.get(remoteId))) {
								playlistsToInsert.add(remotePlaylist);
							}
							mLocalPlaylists.remove(remoteId);
						}
					}
				}
			}
			return playlistsToInsert;
		}

		/**
		 * Insert the provided playlists in the local database. A boolean is returned to indicate if
		 * the insertion went correctly.
		 */
		private boolean insertNewPlaylists(List<Playlist> playlists) {
			final int count = playlists.size();
			if (count > 0) {
				KidsLogger.d(LOG_TAG, String.format("Inserting %d playlists", count));
				ContentValues[] values = new ContentValues[count];
				for (int i = 0; i < count; ++i) {
					values[i] = playlists.get(i).toContentValues();
				}

				return (mResolver.bulkInsert(PlaylistsProvider.CONTENT_URI, values) == count);
			}

			return true;
		}

		/**
		 * Delete the users that are present in the local content providers, but are not returned
		 * anymore by Dailymotion. It means that these users are outdated and should not be proposed
		 * to the user.
		 * 
		 * @return A boolean indicating if the deletion successfully occurred.
		 */
		private boolean deleteOutdatedPlaylists() {
			if (mLocalPlaylists.size() > 0) {
				int count = mLocalPlaylists.size();
				KidsLogger.i(LOG_TAG, String.format("Removing %d playlists from db...", count));
				ArrayList<ContentProviderOperation> op = new ArrayList<ContentProviderOperation>(
						count);
				for (String key : mLocalPlaylists.keySet()) {
					ContentProviderOperation operation = ContentProviderOperation
							.newDelete(PlaylistsProvider.CONTENT_URI)
							.withSelection(PlaylistContract.DAILYMOTION_ID + "=?",
									new String[] { key }).build();
					op.add(operation);
				}
				try {
					return mResolver.applyBatch(PlaylistsProvider.AUTHORITY, op).length == count;
				} catch (RemoteException e) {
					// Should not happen, the Content Provider is local.
					KidsLogger.w(LOG_TAG,
							"An exception occurred while communicating with provider.");
					return false;
				} catch (OperationApplicationException e) {
					KidsLogger.w(LOG_TAG, "An operation has failed when deleting local playlists.");
					return false;
				}
			}

			return true;
		}

		@Override
		public void onFailure(DailyException e) {
			mCallback.onFailure(e);
		}
	}
}
