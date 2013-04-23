package com.orange.labs.dailymotion.kids.activity;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.dailymotion.kids.R;
import com.orange.labs.dailymotion.kids.activity.fragments.PlaylistVideosFragment;
import com.orange.labs.dailymotion.kids.activity.fragments.PlaylistVideosFragment.OnVideoSelectedListener;
import com.orange.labs.dailymotion.kids.callback.Callback;
import com.orange.labs.dailymotion.kids.config.Constants;
import com.orange.labs.dailymotion.kids.db.DatabaseUtils;
import com.orange.labs.dailymotion.kids.dependency.DependencyResolver;
import com.orange.labs.dailymotion.kids.dependency.DependencyResolverImpl;
import com.orange.labs.dailymotion.kids.playlist.Playlist;
import com.orange.labs.dailymotion.kids.playlist.PlaylistContract;
import com.orange.labs.dailymotion.kids.playlist.PlaylistContract.PlaylistColumns;
import com.orange.labs.dailymotion.kids.playlist.PlaylistsProvider;
import com.orange.labs.dailymotion.kids.user.User;
import com.orange.labs.dailymotion.kids.user.UserContract;
import com.orange.labs.dailymotion.kids.user.UsersProvider;
import com.orange.labs.dailymotion.kids.utils.KidsLogger;
import com.orange.labs.dailymotion.kids.video.AsyncVideosFetcher;
import com.orange.labs.dailymotion.kids.video.Video;

public class VideosListActivity extends SherlockFragmentActivity implements TabListener,
		OnVideoSelectedListener, LoaderCallbacks<Cursor> {

	private static final String LOG_TAG = "Videos List Activity";

	/**
	 * Used to provide the user to display the playlists of.
	 */
	public static final String USER_ID = "user_id";

	/** Used to provide playlist ids to fragment composing the view */
	public static final String PLAYLIST_ID = "playlist_id";

	// Loader Ids - Used for loading cursors to retrieve user and video records.
	private static final int PLAYLISTS_LOADER_ID = 0;
	private static final int USER_LOADER_ID = 1;
	// Used for video player activity result.
	private static final int VIDEO_PLAYER_CODE = 0;

	private ViewPager mViewPager;
	private PlaylistPagerAdapter<PlaylistVideosFragment> mPlaylistAdapter;

	/**
	 * User we display the playlists and videos of.
	 */
	private User mUser;
	private DependencyResolver mResolver;

	private final AtomicInteger mFetchRemaining = new AtomicInteger();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.videoslist_activity);

		final ActionBar actionBar = getSherlock().getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		mResolver = DependencyResolverImpl.getInstance();
		mPlaylistAdapter = new PlaylistPagerAdapter<PlaylistVideosFragment>(
				getSupportFragmentManager(), PlaylistVideosFragment.class, null);

		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mPlaylistAdapter);
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);
			}
		});

		final LoaderManager loaderMgr = getSupportLoaderManager();
		loaderMgr.initLoader(USER_LOADER_ID, getIntent().getExtras(), this);
		loaderMgr.initLoader(PLAYLISTS_LOADER_ID, getIntent().getExtras(), this);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == VIDEO_PLAYER_CODE && resultCode == RESULT_OK) {
			Bundle extras = data.getExtras();
			if (mUser != null
					&& !mUser.getDailymotionId().equals(
							extras.getString(VideoPlayerActivity.USER_ID))) {
				final LoaderManager loaderMgr = getSupportLoaderManager();
				loaderMgr.restartLoader(USER_LOADER_ID, extras, this);
				loaderMgr.restartLoader(PLAYLISTS_LOADER_ID, extras, this);
			}
		}
	}

	@Override
	protected void onDestroy() {
		mPlaylistAdapter.swapCursor(null);
		super.onDestroy();
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.episodes_refresh_indicator).setVisible(mFetchRemaining.get() > 0);
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.episodes_activity, menu);
		return true;
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		switch (id) {
		case USER_LOADER_ID:
			return new CursorLoader(this, UsersProvider.CONTENT_URI, null,
					UserContract.DAILYMOTION_ID + "=?", new String[] { bundle.getString(USER_ID) },
					null);
		case PLAYLISTS_LOADER_ID:
			KidsLogger.v(LOG_TAG, bundle.getString(USER_ID));
			return new CursorLoader(this, PlaylistsProvider.CONTENT_URI, null,
					PlaylistContract.OWNER + "=?", new String[] { bundle.getString(USER_ID) },
					PlaylistContract.SEASON + " ASC");
		}

		// Unknown loader.
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (cursor.moveToFirst()) {
			switch (loader.getId()) {
			case USER_LOADER_ID:
				mUser = User.fromCursor(cursor);
				setTitle(mUser.getScreenName());
				break;
			case PLAYLISTS_LOADER_ID:
				final ActionBar actionBar = getSupportActionBar();
				actionBar.removeAllTabs();

				do {
					addPlaylistTab(actionBar, Playlist.fromCursor(cursor));
				} while (cursor.moveToNext());

				mPlaylistAdapter.swapCursor(cursor);

				if (cursor.getCount() > 1) {
					actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
				} else {
					actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
				}
				break;
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		switch (loader.getId()) {
		case PLAYLISTS_LOADER_ID:
			mPlaylistAdapter.swapCursor(null);
		}
	}

	/**
	 * Add a new tab in the action bar for the provided playlist.
	 */
	private void addPlaylistTab(final ActionBar actionBar, final Playlist playlist) {
		actionBar.addTab(actionBar.newTab().setText(playlist.getName()).setTabListener(this));
		fetchPlaylistIfNeeded(playlist);
	}

	private void fetchPlaylistIfNeeded(Playlist playlist) {
		if (shouldFetchPlaylistVideos(playlist)) {
			if (mFetchRemaining.incrementAndGet() == 1) {
				invalidateOptionsMenu();
			}

			AsyncVideosFetcher fetcher = mResolver.createVideosFetcher();
			fetcher.fetch(playlist, new PlaylistFetcherCallback(mFetchRemaining));
		}
	}

	/**
	 * Return a boolean indicating if the playlist videos should be fetched. It depends if the fetch
	 * all mode is enabled, as well as if the cache has not been updated for the past 24 hours.
	 */
	private boolean shouldFetchPlaylistVideos(Playlist playlist) {
		boolean shouldUpdate = true;

		if (Constants.UPDATE_INCLUDE_EPISODES) {
			shouldUpdate = false;
		} else if (playlist.hasLastUpdate()) {
			Date updateTime = new Date(playlist.getLastUpdate() + Constants.UPDATE_PERIOD);
			if (updateTime.after(new Date())) {
				shouldUpdate = false;
			}
		}

		return shouldUpdate;
	}

	/**
	 * Callback executed after a request to download videos that belong to a playlist has finished.
	 * The last update time is updated in the local record of the Playlist.
	 */
	private class PlaylistFetcherCallback implements Callback<List<Video>> {

		private final AtomicInteger remaining;

		public PlaylistFetcherCallback(final AtomicInteger fetchRemaining) {
			this.remaining = fetchRemaining;
		}

		@Override
		public void onSuccess(List<Video> result) {
			fetchDone();
		}

		@Override
		public void onFailure(Exception e) {
			fetchDone();
		}

		private void fetchDone() {
			if (this.remaining.decrementAndGet() == 0) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						invalidateOptionsMenu();
					}
				});
			}
		}
	}

	private class PlaylistPagerAdapter<F extends Fragment> extends FragmentStatePagerAdapter {

		private final Class<F> fragmentClass;

		private Cursor cursor;

		public PlaylistPagerAdapter(FragmentManager fm, Class<F> fragmentClass, Cursor cursor) {
			super(fm);
			this.fragmentClass = fragmentClass;
			this.cursor = cursor;
		}

		@Override
		public int getItemPosition(Object object) {
			return FragmentStatePagerAdapter.POSITION_NONE;
		}

		@Override
		public F getItem(int position) {
			if (cursor == null) // shouldn't happen
				return null;
			cursor.moveToPosition(position);
			F frag;
			try {
				frag = fragmentClass.newInstance();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
			Bundle args = new Bundle();
			args.putString(VideoPlayerActivity.PLAYLIST_ID,
					DatabaseUtils.getString(cursor, PlaylistColumns.DAILYMOTION_ID));
			args.putString(VideoPlayerActivity.USER_ID,
					DatabaseUtils.getString(cursor, PlaylistColumns.OWNER));
			args.putBoolean(PlaylistVideosFragment.UPDATE_SELECTION, false);
			frag.setArguments(args);
			return frag;
		}

		@Override
		public int getCount() {
			if (cursor == null)
				return 0;
			else
				return cursor.getCount();
		}

		public void swapCursor(Cursor c) {
			if (cursor == c)
				return;
			this.cursor = c;
			notifyDataSetChanged();
		}
	}

	@Override
	public void onVideoSelected(Video video, int position) {
		if (video == null)
			return;

		Intent playerIntent = new Intent(this, VideoPlayerActivity.class);
		playerIntent.putExtra(VideoPlayerActivity.USER_ID, mUser.getDailymotionId());
		playerIntent.putExtra(VideoPlayerActivity.PLAYLIST_ID, video.getPlaylist());
		playerIntent.putExtra(VideoPlayerActivity.VIDEO_ID, video.getDailymotionId());
		playerIntent.putExtra(VideoPlayerActivity.VIDEO_LIST_POSITION, position);
		startActivityForResult(playerIntent, VIDEO_PLAYER_CODE);
	}

}