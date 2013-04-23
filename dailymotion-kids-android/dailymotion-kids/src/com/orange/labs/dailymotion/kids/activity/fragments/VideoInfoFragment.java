package com.orange.labs.dailymotion.kids.activity.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.dailymotion.kids.R;
import com.orange.labs.dailymotion.kids.activity.VideoPlayerActivity;
import com.orange.labs.dailymotion.kids.utils.KidsLogger;
import com.orange.labs.dailymotion.kids.video.Video;
import com.orange.labs.dailymotion.kids.video.VideoContract;
import com.orange.labs.dailymotion.kids.video.VideosProvider;

/**
 * Fragment that displays information about an episode. This information includes the title and the
 * description. VideoPlayerActivity.VIDEO_ID should be used to provide the remote ID of the episode
 * to show the description of.
 */
public class VideoInfoFragment extends SherlockFragment implements LoaderCallbacks<Cursor>,
		UpdatableFragment {

	private static final int VIDEO_LOADER_ID = 0;

	private TextView mTitleTv;
	private TextView mDescriptionTv;
	private ProgressBar mLoadingBar;
	private Bundle mUpdatedArgs;

	private Video mCurrentEpisode;

	private boolean mNeedsReloading = true;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View parent = inflater.inflate(R.layout.video_info_fragment, container, false);
		ScrollView scrollView = (ScrollView) parent.findViewById(R.id.videoplayer_info_sv);
		scrollView.setSmoothScrollingEnabled(true);
		mTitleTv = (TextView) parent.findViewById(R.id.videoplayer_videotitle_tv);
		mDescriptionTv = (TextView) parent.findViewById(R.id.videoplayer_description_tv);
		mLoadingBar = (ProgressBar) parent.findViewById(R.id.videoplayer_info_pb);
		return parent;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		KidsLogger.v("Video Info Fragment", "onActivityCreated");
		reloadIfNeededAndPossible();
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
		if (args != null && !TextUtils.isEmpty(args.getString(VideoPlayerActivity.VIDEO_ID))) {
			KidsLogger.v(
					"Video Info Fragment",
					"Processing updated arguments. Loading description of :"
							+ args.getString(VideoPlayerActivity.VIDEO_ID));
			getLoaderManager().restartLoader(VIDEO_LOADER_ID, args, this);
		} else {
			mLoadingBar.setVisibility(View.VISIBLE);
			mTitleTv.setVisibility(View.GONE);
			mDescriptionTv.setVisibility(View.GONE);
		}
		mNeedsReloading = false;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		if (id == VIDEO_LOADER_ID) {
			return new CursorLoader(getActivity(), VideosProvider.CONTENT_URI, null,
					VideoContract.DAILYMOTION_ID + "=?",
					new String[] { bundle.getString(VideoPlayerActivity.VIDEO_ID) }, null);
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		switch (loader.getId()) {
		case VIDEO_LOADER_ID:
			if (cursor.moveToFirst()) {
				mCurrentEpisode = Video.fromCursor(cursor);
				mTitleTv.setText(mCurrentEpisode.getTitle());
				mDescriptionTv.setText(Html.fromHtml(mCurrentEpisode.getDescription()));
			} else {
				mTitleTv.setText(getString(R.string.video_info_no_title));
				mDescriptionTv.setText(getString(R.string.video_info_no_desc));
			}
			mLoadingBar.setVisibility(View.GONE);
			mTitleTv.setVisibility(View.VISIBLE);
			mDescriptionTv.setVisibility(View.VISIBLE);
			break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	public Video getCurrentEpisode() {
		return mCurrentEpisode;
	}

	@Override
	public void updateFragment(Bundle args) {
		mUpdatedArgs = args;
		mNeedsReloading  = true;
		reloadIfNeededAndPossible();
	}

}
