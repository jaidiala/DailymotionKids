package com.orange.labs.dailymotion.kids.activity.fragments;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.orange.labs.dailymotion.kids.activity.VideoPlayerActivity;
import com.orange.labs.dailymotion.kids.activity.VideosListActivity;
import com.orange.labs.dailymotion.kids.activity.adapters.VideoCursorAdapter;
import com.orange.labs.dailymotion.kids.utils.KidsLogger;
import com.orange.labs.dailymotion.kids.video.Video;
import com.orange.labs.dailymotion.kids.video.VideoContract;
import com.orange.labs.dailymotion.kids.video.VideosProvider;

public class PlaylistVideosFragment extends SherlockListFragment implements
		LoaderCallbacks<Cursor>, UpdatableFragment {

	/**
	 * Instantiate and return a new {@link PlaylistVideosFragment}, using the user id and playlist
	 * id as arguments.
	 */
	public static PlaylistVideosFragment newInstance(String userId, String playlistId) {
		PlaylistVideosFragment playlistFragment = new PlaylistVideosFragment();

		Bundle bundle = new Bundle();
		bundle.putString(VideosListActivity.PLAYLIST_ID, playlistId);
		bundle.putString(VideosListActivity.USER_ID, userId);
		playlistFragment.setArguments(bundle);

		return playlistFragment;
	}

	protected static final int VIDEOS_LOADER_ID = 0;

	public static final String UPDATE_SELECTION = "update_selection";

	private static final String LOG_TAG = "Playlist Fragment";

	protected VideoCursorAdapter mListAdapter;
	/** Hero that owns the playlist displayed by the fragment */
	private String mPlaylistId;
	private ListView mListView;
	/** Selected item in the list */
	private int mSelectedPosition = -1;
	private Bundle mUpdatedArgs;
	private OnVideoSelectedListener mListener;

	private boolean mNeedsReloading = true;
	private boolean mUpdateSelection = true;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnVideoSelectedListener) activity;
		} catch (ClassCastException e) {
			// no listener
			mListener = null;
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mListView = getListView();

		mListAdapter = new VideoCursorAdapter(getActivity());
		setListAdapter(mListAdapter);
		reloadIfNeededAndPossible();

		mUpdateSelection = getArguments().getBoolean(UPDATE_SELECTION, true);
		if (mUpdateSelection)
			mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
	}

	@Override
	public void onResume() {
		super.onResume();
		reloadIfNeededAndPossible();
	}

	private void reloadIfNeededAndPossible() {
		if (mNeedsReloading && isAdded())
			reloadFragment();
	}

	private void reloadFragment() {
		Bundle args = (mUpdatedArgs != null) ? mUpdatedArgs : getArguments();
		if (args != null) {
			if (mUpdateSelection) {
				mSelectedPosition = args.getInt(VideoPlayerActivity.VIDEO_LIST_POSITION);
				getListView().setItemChecked(mSelectedPosition, true);
				setSelection(mSelectedPosition);
			}

			String playlist = args.getString(VideoPlayerActivity.PLAYLIST_ID);

			boolean shouldReload = !TextUtils.isEmpty(playlist) && !playlist.equals(mPlaylistId);
			mPlaylistId = playlist;
			if (TextUtils.isEmpty(playlist) || shouldReload) {
				setListShown(false);
			}

			if (shouldReload) {
				KidsLogger.v(LOG_TAG, "Reloading with playlist: " + mPlaylistId);
				getLoaderManager().restartLoader(VIDEOS_LOADER_ID, null, this);
			}

			mNeedsReloading = false;
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		KidsLogger.v("Abstract List", "List Item Click: " + position);
		// Fetch needed information
		Cursor cursor = (Cursor) mListAdapter.getItem(position);
		if (cursor == null)
			return;

		Video video = Video.fromCursor(cursor);
		if (mListener != null) {
			mListener.onVideoSelected(video, position);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		KidsLogger.v(LOG_TAG, mPlaylistId);
		return new CursorLoader(getActivity(), VideosProvider.CONTENT_URI, null,
				VideoContract.PLAYLIST + "=?", new String[] { mPlaylistId }, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		KidsLogger.v("Playlist Fragment", "On Load Finished: " + cursor.getCount());
		if (cursor.getCount() > 0) {
			mListAdapter.swapCursor(cursor);
			
			if (mUpdateSelection) {
				getListView().setItemChecked(mSelectedPosition, true);
				if (mSelectedPosition < 1)
					setSelection(0);
				else
					setSelection(mSelectedPosition);
			}

			if (isResumed())
				setListShownNoAnimation(true);
			else
				setListShown(true);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mListAdapter.swapCursor(null);
	}

	@Override
	public void updateFragment(Bundle args) {
		mUpdatedArgs = args;
		mNeedsReloading = true;
		reloadIfNeededAndPossible();
	}

	public interface OnVideoSelectedListener {
		void onVideoSelected(Video video, int position);
	}
}
