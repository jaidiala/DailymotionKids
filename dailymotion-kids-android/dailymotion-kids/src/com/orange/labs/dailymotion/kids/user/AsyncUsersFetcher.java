package com.orange.labs.dailymotion.kids.user;

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
import com.orange.labs.dailymotion.kids.utils.KidsLogger;
import com.orangelabs.dailymotion.DailyException;
import com.orangelabs.dailymotion.Dailymotion;
import com.orangelabs.dailymotion.DailymotionRequestor;
import com.orangelabs.dailymotion.DailymotionRequestor.DailymotionRequestListener;

public class AsyncUsersFetcher {

	private static final String FIELDS = "owner.id,type,owner.username,owner.screenname,owner.avatar_large_url,owner.description";

	private static final String TILES_ENDPOINT = "tiles";

	private static final String LOG_TAG = "Users Fetcher";

	/**
	 * Dailymotion object used to communicate with Dailymotion Graph API and download playlist
	 * content.
	 */
	private final Dailymotion mDailymotion;

	/**
	 * Flag that indicates if a fetch operation has been launched for that specific instance.
	 */
	private final AtomicBoolean mFetching = new AtomicBoolean(false);

	private List<User> mFetchedUsers = new ArrayList<User>();

	/*
	 * List of users already inserted in the local database.
	 */
	private Map<String, User> mLocalUsers = new HashMap<String, User>();

	/**
	 * Dailymotion endpoint to request.
	 */
	private String mEndPoint;

	private ContentResolver mContentResolver;

	/**
	 * Callback used for giving the result back to the caller.
	 */
	private Callback<List<User>> mCallback;

	public AsyncUsersFetcher(Dailymotion dailymotion, Context context) {
		mDailymotion = dailymotion;
		mContentResolver = context.getContentResolver();
	}

	/**
	 * Fetch the videos taht belong to the provided user, and return the result back through the
	 * provided callback. The callback is called once per result page.
	 */
	public void fetch(final Callback<List<User>> callback) {
		if (mFetching.getAndSet(true)) {
			throw new IllegalStateException("AsyncUsersFetcher can only be used once.");
		}

		mCallback = callback;
		mEndPoint = TILES_ENDPOINT;
		mLocalUsers = getLocalUsers();
		fetchPage(1);
	}

	private Map<String, User> getLocalUsers() {
		Map<String, User> users = new HashMap<String, User>();
		Cursor cursor = mContentResolver.query(UsersProvider.CONTENT_URI, null, null, null, null);
		while (cursor.moveToNext()) {
			User user = User.fromCursor(cursor);
			users.put(user.getDailymotionId(), user);
		}
		cursor.close();
		return users;
	}

	/**
	 * Fetch the provided page of users for the Dailymotion end point generated based on the object
	 * we need to fetch the users for.
	 * 
	 * @param page
	 *            Page to fetch as an int.
	 */
	private void fetchPage(int page) {
		KidsLogger.d(LOG_TAG, String.format("Fetching users from %s, page : %d", mEndPoint, page));

		Bundle bundle = new Bundle();
		bundle.putString("fields", FIELDS);
		bundle.putString("tileset", "kidsplus");
		bundle.putString("localization", "fr");
		bundle.putString("page", String.valueOf(page));
		bundle.putString("limit", String.valueOf(Constants.PAGE_LIMIT));

		DailymotionRequestor requestor = new DailymotionRequestor(mDailymotion);
		requestor.anonymousRequest(mEndPoint, bundle, new UserFetchingRequestListener());
	}

	private class UserFetchingRequestListener implements DailymotionRequestListener {

		@Override
		public void onSuccess(JSONObject response) {
			JSONArray array = response.optJSONArray("list");
	
			List<User> usersToInsert = parseUsers(array);
			if (!insertNewUsers(usersToInsert)) {
				mCallback.onFailure(new IOException("Impossible to insert users in db."));
				return;
			}
			
			if (response.optBoolean("has_more") && response.optInt("page") != 0) {
				fetchPage(response.optInt("page") + 1);
			} else {
				deleteOutdatedUsers();
				mCallback.onSuccess(mFetchedUsers);
			}
		}
		
		@Override
		public void onFailure(DailyException e) {
			mCallback.onFailure(e);
		}
		
		private List<User> parseUsers(JSONArray array) {
			List<User> usersToInsert = new ArrayList<User>();
			if (array != null) {
				for (int i = 0; i < array.length(); ++i) {
					JSONObject object = array.optJSONObject(i);
					if (object != null) {
						User user = AsyncUsersFetcher.fromJson(object);
						String remoteId = user.getDailymotionId();
						if (!mLocalUsers.containsKey(remoteId)) {
							user.setNew(mLocalUsers.size() > 0); // considered new if existing vids.
							usersToInsert.add(user); // need insertion, not present locally.
						} else {
							if (!user.equals(mLocalUsers.get(remoteId))) {
								usersToInsert.add(user); // need insertion, been updated
							}
							mLocalUsers.remove(remoteId);
						}
						mFetchedUsers.add(user);
					}
				}
			}
			return usersToInsert;
		}

	}

	/**
	 * Insert the provided playlists in the local database. A boolean is returned to indicate if the
	 * insertion went correctly.
	 */
	private boolean insertNewUsers(List<User> users) {
		if (users.size() > 0) {
			KidsLogger.d(LOG_TAG, String.format("Inserting %d users", users.size()));
			ContentValues[] values = new ContentValues[users.size()];
			for (int i = 0; i < users.size(); ++i) {
				values[i] = users.get(i).toContentValues();
			}

			return (mContentResolver.bulkInsert(UsersProvider.CONTENT_URI, values) == users.size());
		}

		return true;
	}

	/**
	 * Delete the users that are present in the local content providers, but are not returned
	 * anymore by Dailymotion. It means that these users are outdated and should not be proposed to
	 * the user.
	 * 
	 * @return A boolean indicating if the deletion successfully occured.
	 */
	private boolean deleteOutdatedUsers() {
		if (mLocalUsers.size() > 0) {
			int count = mLocalUsers.size();
			KidsLogger.i(LOG_TAG, String.format("Removing %d users from db", count));
			ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(count);
			for (String key : mLocalUsers.keySet()) {
				ContentProviderOperation operation = ContentProviderOperation
						.newDelete(UsersProvider.CONTENT_URI)
						.withSelection(UserContract.DAILYMOTION_ID + "=?", new String[] { key })
						.build();
				ops.add(operation);
			}
			try {
				return mContentResolver.applyBatch(UsersProvider.AUTHORITY, ops).length == count;
			} catch (RemoteException e) {
				// Should not happen, the Content Provider is local.
				KidsLogger.w(LOG_TAG, "An exception occured while communicating with provider.");
				return false;
			} catch (OperationApplicationException e) {
				KidsLogger.w(LOG_TAG, "An operation has failed when deleting local users.");
				return false;
			}
		}

		return true;
	}

	/**
	 * Return a {@link User} instance with attributes corresponding to the received JSON.
	 * 
	 * @param object
	 *            {@link JSONObject} received from Dailymotion.
	 * @return Corresponding {@link User} instance.
	 */
	private static User fromJson(JSONObject object) {
		User user = new User();
		user.setDailymotionId(object.optString("owner.id"));
		user.setScreenName(object.optString("owner.screenname"));
		user.setAvatarUrl(object.optString("owner.avatar_large_url"));
		user.setCategory(object.optString("owner.description"));
		return user;
	}
}
