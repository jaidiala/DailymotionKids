package com.orange.labs.dailymotion.kids.utils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;

import com.orange.labs.dailymotion.kids.callback.Callback;
import com.orange.labs.dailymotion.kids.config.Constants;
import com.orange.labs.dailymotion.kids.config.ConfigurationManager;
import com.orange.labs.dailymotion.kids.dependency.DependencyResolver;
import com.orange.labs.dailymotion.kids.playlist.AsyncPlaylistFetcher;
import com.orange.labs.dailymotion.kids.playlist.Playlist;
import com.orange.labs.dailymotion.kids.user.AsyncUsersFetcher;
import com.orange.labs.dailymotion.kids.user.User;
import com.orange.labs.dailymotion.kids.video.AsyncVideosFetcher;
import com.orange.labs.dailymotion.kids.video.Video;

/**
 * Refresh the cached content from Dailymotion (including sUsers, playlists, and videos that belong
 * to those). A configuration parameter defines how often an update of the cache should be
 * performed.
 * 
 * @author Jean-Francois Moy
 * 
 */
public class HeroesCacheUpdater {

	private static final String LOG_TAG = "Cache Updater";

	private final ConfigurationManager mConfiguration;
	private final DependencyResolver mResolver; // Used to create the various fetchers.

	/**
	 * Pool with a fixed size that allows us to perform simultaneous calls to Dailymotion API.
	 */
	private ExecutorService mExecutorService;

	private Callback<Void> mListener;

	/**
	 * Used to store how many items still need to be fetched.
	 */
	private final AtomicInteger mActionsRemaining = new AtomicInteger(0);

	/**
	 * Used to know if an update has not already been performed using this object.
	 */
	private final AtomicBoolean mIsRunning = new AtomicBoolean(false);

	/**
	 * Used to know if the update has failed to avoid notifying the caller twice.
	 */
	private final AtomicBoolean mHasFailed = new AtomicBoolean(false);

	public HeroesCacheUpdater(final Context context, final DependencyResolver resolver,
			final ConfigurationManager configuration) {
		mConfiguration = configuration;
		mResolver = resolver;
	}

	/**
	 * Serves as an indication for external classes to know if the local content should be updated.
	 * An update should be done only if the previous fails, or if the period specified in the
	 * configuration has passed.
	 */
	public boolean shouldUpdate() {
		boolean shouldUpdate = true;
		if (mIsRunning.get()) {
			shouldUpdate = false;
		} else if (mConfiguration.hasLastUpdateSucceeded()) {
			Date updateTime = new Date(mConfiguration.getExpirationDate() + Constants.UPDATE_PERIOD);
			if (updateTime.after(new Date())) {
				shouldUpdate = false;
			}
		}

		return shouldUpdate;
	}

	public boolean isRunning() {
		return mIsRunning.get();
	}

	/**
	 * Fetch the updated content from the Dailymotion servers and update the local data. The update
	 * consists of a succession of requests using callbacks.
	 * 
	 * A callback has to be provided to be aware of when the operation is finished.
	 * 
	 * Return a boolean indicating if the update has been launched or not. The update won't be
	 * launched if an update is already in progress.
	 */
	public boolean update(final Callback<Void> listener) {
		if (mIsRunning.getAndSet(true)) {
			// An update is already running.
			KidsLogger.w(LOG_TAG, "An update is already running, should only be used once");
			return false;
		}

		KidsLogger.i(LOG_TAG, "Starting a new update");

		mExecutorService = Executors.newFixedThreadPool(Constants.NB_SIMULTANEOUS_DOWNLOADS);
		mListener = listener;
		fetchUsers(); // temporary, waiting for Dailymotion API.
		return true;
	}

	private void fetchUsers() {
		mActionsRemaining.incrementAndGet();
		mExecutorService.execute(new Runnable() {
			@Override
			public void run() {
				AsyncUsersFetcher userFetcher = mResolver.createUserFetcher();
				userFetcher.fetch(new CacheUpdaterCallback<List<User>>() {

					@Override
					public void onSuccess(List<User> result) {
						for (User user : result) {
							fetchUserPlaylist(user);
						}
						super.onSuccess(result);
					}
				});
			}
		});
	}

	private void fetchUserPlaylist(final User user) {
		mActionsRemaining.incrementAndGet(); // fetching playlists for one user = 1 action
		mExecutorService.execute(new Runnable() {

			@Override
			public void run() {
				// Start by fetching all the playlist of the user
				AsyncPlaylistFetcher playlistFetcher = mResolver.createPlaylistsFetcher();
				playlistFetcher.fetch(user, new CacheUpdaterCallback<List<Playlist>>() {

					@Override
					public void onSuccess(List<Playlist> playlists) {
						if (Constants.UPDATE_INCLUDE_EPISODES)
							fetchPlaylistsVideo(playlists);
						else {
							// Fetch only the first playlist
							fetchFirstPlaylistVideos(playlists);
						}

						// Fetching the playlist has succeeded.
						super.onSuccess(playlists);
					}
				});
			}
		});
	}

	private void fetchFirstPlaylistVideos(final List<Playlist> playlists) {
		for (final Playlist playlist : playlists) {
			if (playlist.hasSeason() && playlist.getSeason() == 1) {
				mActionsRemaining.incrementAndGet();
				mExecutorService.execute(new Runnable() {

					@Override
					public void run() {
						AsyncVideosFetcher videosFetcher = mResolver.createVideosFetcher();
						videosFetcher.fetch(playlist, new CacheUpdaterCallback<List<Video>>());
					}
				});
			}
		}
	}

	private void fetchPlaylistsVideo(final List<Playlist> playlists) {
		// Update the number of actions to be done
		mActionsRemaining.addAndGet(playlists.size());
		for (final Playlist playlist : playlists) {
			mExecutorService.execute(new Runnable() {

				@Override
				public void run() {
					AsyncVideosFetcher videosFetcher = mResolver.createVideosFetcher();
					videosFetcher.fetch(playlist, new CacheUpdaterCallback<List<Video>>());
				}
			});
		}
	}

	/**
	 * Restore the cache update in its original state so it can be executed again.
	 */
	private void resetCacheUpdater() {
		mExecutorService.shutdown();
		mIsRunning.set(false);
		mHasFailed.set(false);
	}

	/**
	 * Callback class that should be inherited by all callbacks used for updating the cache. It
	 * makes sure of storing the update status, as well as updating the caller using the provided
	 * callback of the success or failure.
	 * 
	 * @author Jean-Francois Moy
	 */
	private class CacheUpdaterCallback<T> implements Callback<T> {

		/**
		 * When no actions are remaining, we store that the operation has succeeded in the
		 * application configuration and notify the caller.
		 */
		public void onSuccess(T result) {
			if (mActionsRemaining.decrementAndGet() == 0) {
				if (!mHasFailed.get()) {
					KidsLogger.d(LOG_TAG, "Local data has been successfully updated.");
					mConfiguration.setLastUpdateSucceeded(true);
					mListener.onSuccess(null);
				}
				resetCacheUpdater();
			}
			KidsLogger.i(LOG_TAG,
					String.format("Update Succeeded, Left: %d", mActionsRemaining.get()));
		};

		/**
		 * The first time an operation fails, we notify the caller using the callback and store that
		 * it has failed in the application configuration.
		 */
		@Override
		public void onFailure(Exception e) {
			if (!mHasFailed.getAndSet(true)) {
				KidsLogger.w(LOG_TAG,
						String.format("Update has failed. Reason: %s", e.getMessage()));
				mConfiguration.setLastUpdateSucceeded(false); // store that it has failed.
				mListener.onFailure(e);
			}
			if (mActionsRemaining.decrementAndGet() == 0) {
				resetCacheUpdater();
			}
		}
	}
}
