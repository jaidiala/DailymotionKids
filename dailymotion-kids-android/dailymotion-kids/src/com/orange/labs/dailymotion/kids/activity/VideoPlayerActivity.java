package com.orange.labs.dailymotion.kids.activity;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.VideoView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.dailymotion.kids.R;
import com.orange.labs.dailymotion.kids.activity.fragments.PlaylistVideosFragment;
import com.orange.labs.dailymotion.kids.activity.fragments.PlaylistVideosFragment.OnVideoSelectedListener;
import com.orange.labs.dailymotion.kids.activity.fragments.SuggestedHeroesListFragment;
import com.orange.labs.dailymotion.kids.activity.fragments.SuggestedHeroesListFragment.OnHeroSelected;
import com.orange.labs.dailymotion.kids.activity.fragments.VideoInfoFragment;
import com.orange.labs.dailymotion.kids.activity.managers.PagerTabManager;
import com.orange.labs.dailymotion.kids.activity.views.SubscriptionDialogFragment;
import com.orange.labs.dailymotion.kids.db.DatabaseUtils;
import com.orange.labs.dailymotion.kids.dependency.DependencyResolverImpl;
import com.orange.labs.dailymotion.kids.user.User;
import com.orange.labs.dailymotion.kids.user.UserContract;
import com.orange.labs.dailymotion.kids.user.UsersProvider;
import com.orange.labs.dailymotion.kids.utils.KidsLogger;
import com.orange.labs.dailymotion.kids.video.Video;
import com.orange.labs.dailymotion.kids.video.VideoContract;
import com.orange.labs.dailymotion.kids.video.VideosProvider;
import com.orangelabs.dailymotion.DailyException;
import com.orangelabs.dailymotion.Dailymotion;
import com.orangelabs.dailymotion.DailymotionRequestor;
import com.orangelabs.dailymotion.DailymotionRequestor.DailymotionRequestListener;

public class VideoPlayerActivity extends SherlockFragmentActivity implements
		LoaderCallbacks<Cursor>, View.OnClickListener, OnTouchListener, OnVideoSelectedListener,
		OnHeroSelected {

	public static final String USER_ID = "user_id";
	public static final String VIDEO_ID = "video_id";
	public static final String PLAYLIST_ID = "playlist_id";
	public static final String VIDEO_LIST_POSITION = "video_list_position";
	public static final String VIDEO_POSITION = "video_position";

	public static final int VIDEO_PLAYER_RESULT = 0;

	private static final int USER_LOADER_ID = 0;
	private static final int VIDEO_LOADER_ID = 1;
	private static final int NEXT_VIDEO_LOADER_ID = 2;

	private static final String CURRENT_TAB_KEY = "current_tab";
	private static final String VIDEO_CURRENT_POS_KEY = "video_current_pos";

	private static final String LOG_TAG = "Video Player";

	private static final long CONTROLS_HIDE_DELAY = 3000; // 3 seconds before hiding controls
	private static final int NEXT_EPISODE_DELAY = 10; // 10 seconds before playing next episode.

	private static final int SUBSCRIPTION_CODE = 1000;

	private RelativeLayout mVideoPlayerLayout;
	private RelativeLayout mPlayerControlsLayout;
	private TextView mElapsedTimeTv;
	private TextView mTotalTimeTv;
	private TextView mStatusTv;
	private TextView mNextEpisodeTv;
	private ImageButton mPlayPauseButton;
	private ImageButton mStatusButton;
	private ImageButton mFullscreenButton;
	private ImageButton mNextEpisodeButton;
	private SeekBar mSeekbar;
	private ProgressBar mLoadingBar;
	private VideoView mVideoView;

	private TabHost mTabHost;
	private PagerTabManager mTabManager;
	private ViewPager mViewPager;

	private User mEpisodeHero;
	private Video mEpisode;
	private Video mNextEpisode;

	private Dailymotion mDailymotion;

	private String mStreamUrl;

	private int mVideoCurrentPosition;
	private int mVideoListPosition = 0;

	private boolean mUserIsPremium = false;
	private boolean mWasPlaying = false;

	private Thread mMonitoringThread;
	private Runnable mNextEpisodeRunnable;
	private AtomicBoolean mMonitoring = new AtomicBoolean(false);
	private final MediaPlayerMonitor mMediaPlayerListener = new MediaPlayerMonitor();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.videoplayer_activity);

		// Action Bar has a back button.
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		mDailymotion = DependencyResolverImpl.getInstance().getDailymotion();

		mElapsedTimeTv = (TextView) findViewById(R.id.videoplayer_controls_elapsed_tv);
		mTotalTimeTv = (TextView) findViewById(R.id.videoplayer_controls_total_tv);
		mStatusTv = (TextView) findViewById(R.id.videoplayer_status_tv);
		mSeekbar = (SeekBar) findViewById(R.id.videoplayer_controls_seekbar);
		mLoadingBar = (ProgressBar) findViewById(R.id.videoplayer_controls_loading_pb);
		mVideoPlayerLayout = (RelativeLayout) findViewById(R.id.videoplayer_rl);
		mPlayerControlsLayout = (RelativeLayout) findViewById(R.id.videoplayer_controls_rl);
		mStatusButton = (ImageButton) findViewById(R.id.videoplayer_status_iv);
		mFullscreenButton = (ImageButton) findViewById(R.id.videoplayer_controls_fullscreen_b);
		mPlayPauseButton = (ImageButton) findViewById(R.id.videoplayer_controls_play_ib);
		mNextEpisodeButton = (ImageButton) findViewById(R.id.videoplayer_nextepisode_ib);
		mNextEpisodeTv = (TextView) findViewById(R.id.videoplayer_nextepisode_tv);

		// Initialise surface view the video will be played on.
		mVideoView = (VideoView) findViewById(R.id.videoplayer_vv);
		mVideoView.setKeepScreenOn(true);
		mVideoView.setOnErrorListener(mMediaPlayerListener);
		mVideoView.setOnPreparedListener(mMediaPlayerListener);
		mVideoView.setOnCompletionListener(mMediaPlayerListener);

		mVideoView.setOnTouchListener(this);
		mFullscreenButton.setOnClickListener(this);
		mPlayPauseButton.setOnClickListener(this);
		mStatusButton.setOnClickListener(this);
		mNextEpisodeButton.setOnClickListener(this);

		mTabHost = (TabHost) findViewById(R.id.videoplayer_tabhost);
		mTabHost.setup();

		mViewPager = (ViewPager) findViewById(R.id.videoplayer_viewpager);
		mTabManager = new PagerTabManager(this, mTabHost, mViewPager);

		setupActivity(savedInstanceState);
	}

	private void setupActivity(Bundle savedInstanceState) {
		final Bundle extras = getIntent().getExtras();
		final LoaderManager loader = getSupportLoaderManager();

		String videoId = extras.getString(VIDEO_ID);
		if (!TextUtils.isEmpty(videoId)) {
			loader.initLoader(VIDEO_LOADER_ID, extras, this);
		}

		String heroId = extras.getString(USER_ID);
		if (!TextUtils.isEmpty(heroId)) {
			loader.initLoader(USER_LOADER_ID, extras, this);
		}

		mVideoListPosition = extras.getInt(VIDEO_LIST_POSITION, 0);

		mTabManager.addTab(
				mTabHost.newTabSpec("info").setIndicator(
						createTabView(this, getString(R.string.videoplayer_tab_info))),
				VideoInfoFragment.class, extras);
		mTabManager.addTab(
				mTabHost.newTabSpec("episodes").setIndicator(
						createTabView(this, getString(R.string.videoplayer_tab_episodes))),
				PlaylistVideosFragment.class, extras);
		mTabManager.addTab(
				mTabHost.newTabSpec("suggestions").setIndicator(
						createTabView(this, getString(R.string.videoplayer_tab_suggestion))),
				SuggestedHeroesListFragment.class, extras);

		if (savedInstanceState != null && savedInstanceState.getString(CURRENT_TAB_KEY) != null) {
			mTabHost.setCurrentTabByTag(savedInstanceState.getString(CURRENT_TAB_KEY));
		} else {
			mTabHost.setCurrentTabByTag("episodes");
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		mUserIsPremium = DependencyResolverImpl.getInstance().getConfiguration().hasSubscribed();
		setupViews(getResources().getConfiguration().orientation);
		toggleVideoControls(true);

		if (mWasPlaying) {
			mVideoView.seekTo(mVideoCurrentPosition);
			mVideoView.start();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mVideoView.isPlaying()) {
			mWasPlaying = true;
			mVideoCurrentPosition = mVideoView.getCurrentPosition();
			mVideoView.pause();
		} else {
			mWasPlaying = false;
		}
		stopMonitoringThread();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mVideoView.stopPlayback();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mTabHost != null)
			outState.putString(CURRENT_TAB_KEY, mTabHost.getCurrentTabTag());
		outState.putInt(VIDEO_CURRENT_POS_KEY, mVideoCurrentPosition);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setupViews(newConfig.orientation);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	private void updateActivityResult() {
		KidsLogger.v(LOG_TAG, "Updating activity result");
		Intent intent = new Intent();
		int resultCode = (mEpisodeHero == null || mEpisode == null) ? RESULT_CANCELED : RESULT_OK;
		if (mEpisodeHero != null) {
			KidsLogger.v(LOG_TAG, "Hero ID: " + mEpisodeHero.getDailymotionId());
			intent.putExtra(VideosListActivity.USER_ID, mEpisodeHero.getDailymotionId());
		}
		if (mEpisode != null) {
			KidsLogger.v(LOG_TAG, "Playlist ID: " + mEpisode.getPlaylist());
			intent.putExtra(VideosListActivity.PLAYLIST_ID, mEpisode.getPlaylist());
		}
		setResult(resultCode, intent);
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		switch (id) {
		case USER_LOADER_ID:
			return new CursorLoader(this, UsersProvider.CONTENT_URI, null,
					UserContract.DAILYMOTION_ID + "=?", new String[] { bundle.getString(USER_ID) },
					null);
		case VIDEO_LOADER_ID:
			return new CursorLoader(this, VideosProvider.CONTENT_URI, null,
					VideoContract.DAILYMOTION_ID + "=?",
					new String[] { bundle.getString(VIDEO_ID) }, null);
		case NEXT_VIDEO_LOADER_ID:
			if (mEpisode != null && mEpisode.hasId() && mEpisode.hasPlaylist()) {
				return new CursorLoader(this, VideosProvider.CONTENT_URI, null, VideoContract.ID
						+ "> ? AND " + VideoContract.PLAYLIST + "=?", new String[] {
						String.valueOf(mEpisode.getId()), mEpisode.getPlaylist() }, null);
			}
		}

		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (!cursor.moveToFirst())
			return;

		switch (loader.getId()) {
		case USER_LOADER_ID:
			mEpisodeHero = User.fromCursor(cursor);
			updateTitle();
			updateActivityResult();
			break;
		case VIDEO_LOADER_ID:
			Video episode = null;
			if ((episode = Video.fromCursor(cursor)) != null) {
				updateActivityResult();
				loadNewEpisode(episode, mVideoListPosition);
			}
			break;
		case NEXT_VIDEO_LOADER_ID:
			mNextEpisode = Video.fromCursor(cursor);
			break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursor) {
	}

	@Override
	public void onHeroSelected(User hero) {
		if (hero != null && (mEpisodeHero == null || !hero.getDailymotionId().equals(mEpisodeHero))) {
			// Update current hero by reloading the cursor loader.
			Bundle bundle = new Bundle();
			bundle.putString(USER_ID, hero.getDailymotionId());
			getSupportLoaderManager().restartLoader(USER_LOADER_ID, bundle, this);

			// Now check if the current user has some videos and load the first if possible
			Uri queryUri = Uri.withAppendedPath(VideosProvider.USER_URI, hero.getDailymotionId());
			Cursor cursor = getContentResolver().query(queryUri, null, null, null, null);
			if (cursor.moveToFirst()) {
				loadNewEpisode(Video.fromCursor(cursor), 0);
			} else {
				// TODO : show error.
			}
			DatabaseUtils.closeQuietly(cursor);

			// Hide episodes list as we are loading a new one.
			PlaylistVideosFragment fragment = (PlaylistVideosFragment) mTabManager.getFragmentAt(1);
			if (fragment != null) {
				fragment.setListShown(false);
			}

			// Switch to the episodes tab
			mTabHost.setCurrentTab(1);
		}
	}

	/**
	 * If the user picks a new video. We stop the playback for the current one and play this new
	 * video.
	 */
	@Override
	public void onVideoSelected(Video video, int position) {
		if (mEpisode == null || !video.getDailymotionId().equals(mEpisode.getDailymotionId())) {
			loadNewEpisode(video, position);
		}
	}

	private void updateTitle() {
		if (mEpisodeHero != null && mEpisode != null && mEpisodeHero.hasScreenName()
				&& mEpisode.hasTitle()) {
			setTitle(String.format("%s - %s", mEpisodeHero.getScreenName(), mEpisode.getTitle()));
		} else if (mEpisodeHero != null && mEpisodeHero.hasScreenName()) {
			setTitle(mEpisodeHero.getScreenName());
		} else if (mEpisode != null && mEpisode.hasTitle()) {
			setTitle(mEpisode.getTitle());
		}
	}

	private void setupViews(int orientation) {
		Window window = getWindow();
		Display defaultDisplay = getWindowManager().getDefaultDisplay();
		int width = defaultDisplay.getWidth();

		int height = (int) (width / 1.3);
		if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
			height = defaultDisplay.getHeight();
			RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

			mVideoPlayerLayout.setLayoutParams(layoutParams);
			mFullscreenButton.setBackgroundResource(R.drawable.collapse_button_selector);

			mTabHost.setVisibility(View.GONE);
			getSupportActionBar().hide();
			window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
					mVideoView.getLayoutParams());
			layoutParams.height = height;
			layoutParams.width = RelativeLayout.LayoutParams.FILL_PARENT;
			mVideoPlayerLayout.setLayoutParams(layoutParams);

			mFullscreenButton.setBackgroundResource(R.drawable.expand_button_selector);

			mTabHost.setVisibility(View.VISIBLE);
			getSupportActionBar().show();
			window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}

	/**
	 * Setup the seek bar, using the provided duration.
	 */
	private void setupSeekBar() {
		mSeekbar.setProgress(mVideoCurrentPosition);
		mSeekbar.setMax(mVideoView.getDuration());
		mSeekbar.setEnabled(true);
		mSeekbar.setOnSeekBarChangeListener(mSeekBarListener);
	}

	/**
	 * Start the monitoring thread responsible of updating the elapsed time, and show progress
	 * through the seekbar.
	 */
	private void startMonitoringThread() {
		if (mMonitoring.getAndSet(true)) {
			return;
		}
		mMonitoringThread = new MonitoringThread();
		mMonitoringThread.start();
	}

	/**
	 * Stop the monitoring thread responsible of updating the seekbar and the elapsed time.
	 */
	private void stopMonitoringThread() {
		if (mMonitoring.getAndSet(false)) {
			mMonitoringThread = null;
		}
	}

	private void onLoadingStream(boolean loading) {
		if (loading) {
			mLoadingBar.setVisibility(View.VISIBLE);
			mStatusTv.setText(R.string.videoplayer_buffering);
			mStatusTv.setVisibility(View.VISIBLE);
			mStatusButton.setVisibility(View.GONE);
		} else {
			mLoadingBar.setVisibility(View.GONE);
			mStatusTv.setVisibility(View.GONE);
		}
	}

	/**
	 * Display a critical error to the user using the provided message.
	 * 
	 * @param message
	 *            Message ressource id to display to the user.
	 */
	private void onStreamingError(int message) {
		mStatusTv.setText(message);
		mStatusTv.setVisibility(View.VISIBLE);
		mLoadingBar.setVisibility(View.GONE);
		mStatusButton.setVisibility(View.VISIBLE);
	}

	private void loadNewEpisode(Video video, int listPosition) {
		if (mVideoView.isPlaying())
			mVideoView.pause();

		KidsLogger.v(LOG_TAG, "Loading new episode : " + video.getTitle());
		mVideoListPosition = listPosition;

		// We need to update views that were configured for the previous episode.
		VideoInfoFragment videoFragment = (VideoInfoFragment) mTabManager.getFragmentAt(0);
		if (videoFragment != null) {
			Bundle bundle = new Bundle();
			bundle.putString(VIDEO_ID, video.getDailymotionId());
			videoFragment.updateFragment(bundle);
		}

		// If the playlist is not the same, we need to update the playlist fragment.
		KidsLogger.v(LOG_TAG, "Updating playlist fragment with playlist: " + video.getPlaylist());
		PlaylistVideosFragment playlistFragment = (PlaylistVideosFragment) mTabManager
				.getFragmentAt(1);
		if (playlistFragment != null) {
			Bundle bundle = new Bundle();
			bundle.putString(PLAYLIST_ID, video.getPlaylist());
			bundle.putInt(VIDEO_LIST_POSITION, mVideoListPosition);
			playlistFragment.updateFragment(bundle);
		}

		mEpisode = video;

		// Cancel timers that could be running.
		if (mNextEpisodeRunnable != null) {
			sVideoPlayerHandler.removeCallbacks(mNextEpisodeRunnable); // cancel timer if running.
			mNextEpisodeRunnable = null;
		}

		// reset views
		mVideoCurrentPosition = 0;
		mElapsedTimeTv.setText(R.string.video_initialtime);
		mTotalTimeTv.setText(R.string.video_initialtime);
		mSeekbar.setProgress(0);
		mNextEpisodeTv.setVisibility(View.GONE);
		mNextEpisodeButton.setVisibility(View.GONE);

		fetchStreamingUrl(mEpisode); // fetch if not fetched already.

		// Update activity title to show current episode title.
		updateTitle();
		updateActivityResult();

		// Fetch next episode information.
		getSupportLoaderManager().restartLoader(NEXT_VIDEO_LOADER_ID, new Bundle(), this);
	}

	/**
	 * Fetch the URL for the video stream from Dailymotion.
	 * 
	 * @param video
	 *            Video to fetch the streaming URL for.
	 */
	private void fetchStreamingUrl(final Video video) {
		onLoadingStream(true);
		DailymotionRequestor requestor = new DailymotionRequestor(mDailymotion);
		requestor.request(String.format("video/%s", video.getDailymotionId()), getStreamFields(),
				mStreamingRequestListener);
	}

	/**
	 * Setup the media player providing a data source, and launch an asynchronous preparation.
	 * 
	 * @return a boolean indicating if the player is currently preparing or if something failed.
	 */
	private boolean startStreamingEpisode() {
		if (!mVideoView.isPlaying()) {
			mVideoView.pause();
		}
		
		if (!TextUtils.isEmpty(mStreamUrl)) {
			KidsLogger.v(LOG_TAG, "Attempt to stream episode: " + mStreamUrl);
			mVideoView.setVideoPath(mStreamUrl);
			if (mVideoCurrentPosition > 0 && mVideoView.canSeekForward()) {
				mVideoView.seekTo(mVideoCurrentPosition);
			}
			mVideoView.start();
			return true;
		}
		KidsLogger.v(LOG_TAG, "No stream URL, cannot stream it.");
		return false;
	}

	/**
	 * Hide video controls after the duration specified by CONTROLS_HIDE_DELAY.
	 */
	private void toggleVideoControls(boolean show) {
		if (show) {
			mPlayerControlsLayout.setVisibility(View.VISIBLE);
		}

		Message msg = new Message();
		msg.what = HIDING_MSG;
		msg.obj = mPlayerControlsLayout;

		sVideoPlayerHandler.removeMessages(HIDING_MSG);
		sVideoPlayerHandler.sendMessageDelayed(msg, CONTROLS_HIDE_DELAY);
	}

	private class MediaPlayerMonitor implements MediaPlayer.OnPreparedListener,
			MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			int message = R.string.videoplayer_unknown_error;
			switch (what) {
			case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
				message = R.string.videoplayer_server_died;
				break;
			}
			onStreamingError(message);
			KidsLogger.d(LOG_TAG, "Error with the media: what: " + what + " extra: " + extra);
			return true;
		}

		@Override
		public void onCompletion(MediaPlayer mp) {
			toggleVideoControls(false);

			mStatusTv.setText(R.string.videoplayer_play_again);
			mStatusTv.setVisibility(View.VISIBLE);
			mStatusButton.setVisibility(View.VISIBLE);
			if (mUserIsPremium && mNextEpisode != null) {
				ImageLoader.getInstance().displayImage(mNextEpisode.getThumbnailUrl(),
						mNextEpisodeButton);
				mNextEpisodeTv.setText(getString(R.string.videoplayer_nextepisode_in,
						NEXT_EPISODE_DELAY));
				mNextEpisodeTv.setVisibility(View.VISIBLE);
				mNextEpisodeButton.setVisibility(View.VISIBLE);

				// Start timer to play next episode.
				mNextEpisodeRunnable = new NextEpisodeTimer(VideoPlayerActivity.this,
						NEXT_EPISODE_DELAY, mNextEpisodeTv, mNextEpisodeButton);
				sVideoPlayerHandler.postDelayed(mNextEpisodeRunnable, 1000);
			} else {
				// Show dialog to the user to propose him/her to subscribe.
				showSubscriptionDialog();
			}
		}

		@Override
		public void onPrepared(MediaPlayer mp) {
			formatTimeAndDisplay(mTotalTimeTv, mVideoView.getDuration());
			setupSeekBar();
			startMonitoringThread();
			onLoadingStream(false);
		}
	}

	/**
	 * The responsibility of the seek bar change listener is to ensure that the controls won't be
	 * hidden while the user modifies the seek bar position as well as allowing the user to seek to
	 * a new position within the video.
	 */
	private final SeekBar.OnSeekBarChangeListener mSeekBarListener = new SeekBar.OnSeekBarChangeListener() {

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			toggleVideoControls(false);
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			if (fromUser && mVideoView.canSeekBackward() && mVideoView.canSeekForward()) {
				mVideoView.seekTo(progress);
				mSeekbar.setProgress(progress);
			}
		}
	};

	@Override
	public void onClick(View target) {
		if (target == mFullscreenButton) {
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				target.setBackgroundResource(R.drawable.collapse_button_selector);
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			} else {
				target.setBackgroundResource(R.drawable.expand_button_selector);
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}
		} else if (target == mPlayPauseButton) {
			if (mVideoView.isPlaying()) {
				mSeekbar.setEnabled(false);
				mVideoView.pause();
				mPlayPauseButton.setBackgroundResource(R.drawable.play_button_selector);
			} else {
				mSeekbar.setEnabled(true);
				mVideoView.start();
				mPlayPauseButton.setBackgroundResource(R.drawable.pause_button_selector);
			}
		} else if (target == mStatusButton) {
			loadNewEpisode(mEpisode, mVideoListPosition);
		} else if (target == mNextEpisodeButton) {
			loadNewEpisode(mNextEpisode, ++mVideoListPosition);
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent ev) {
		if (v == mVideoView && ev.getAction() == MotionEvent.ACTION_DOWN) {
			toggleVideoControls(true);
			return true;
		}
		return false;
	}

	/**
	 * Thread used to monitor the current progress of the video being streamed. Its responsibility
	 * is notably to update the seek bar and the elapsed time text view, as well as keeping track of
	 * the current progress of the video. It does so every second.
	 */
	private class MonitoringThread extends Thread {

		private Runnable mProgressUpdateRunnable = new Runnable() {
			@Override
			public void run() {
				final int elapsed = mVideoView.getCurrentPosition();
				formatTimeAndDisplay(mElapsedTimeTv, elapsed);
				mSeekbar.setProgress(elapsed);
			}
		};

		@Override
		public void run() {
			super.run();
			while (mMonitoring.get()) {
				if (mVideoView.isPlaying()) {
					runOnUiThread(mProgressUpdateRunnable);
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	private class NextEpisodeTimer implements Runnable {

		private int secondsLeft;
		private final Context context;
		private final TextView nextEpisodeTv;
		private final ImageButton nextEpisodeIb;

		public NextEpisodeTimer(final Context context, final int timeLeft,
				final TextView nextEpisodeTv, final ImageButton nextEpisodeIb) {
			this.context = context;
			this.secondsLeft = timeLeft;
			this.nextEpisodeTv = nextEpisodeTv;
			this.nextEpisodeIb = nextEpisodeIb;
		}

		@Override
		public void run() {
			this.secondsLeft--;
			if (this.secondsLeft > 0) {
				this.nextEpisodeTv.setText(this.context.getString(
						R.string.videoplayer_nextepisode_in, this.secondsLeft));
				sVideoPlayerHandler.postDelayed(this, 1000);
			} else {
				this.nextEpisodeIb.performClick();
			}
		}

	}

	private static void formatTimeAndDisplay(TextView textView, int time) {
		if (time != 0) {
			int seconds = (time / 1000) % 60;
			int minutes = ((time / (1000 * 60)) % 60);
			int hours = ((time / (1000 * 60 * 60)) % 24);
			if (hours > 0) {
				textView.setText(String.format("%d:%02d:%02d", hours, minutes, seconds));
			} else {
				textView.setText(String.format("%02d:%02d", minutes, seconds));
			}
			textView.setVisibility(View.VISIBLE);
		}
	}

	private static View createTabView(final Context context, final String text) {
		View view = LayoutInflater.from(context).inflate(R.layout.tab_item, null);
		TextView tv = (TextView) view.findViewById(R.id.tab_text);
		tv.setText(text.toUpperCase(Locale.getDefault()));

		return view;
	}

	/**
	 * Message hiding the controls for controlling the video playback.
	 */
	private static final int HIDING_MSG = 0x1;

	private static Handler sVideoPlayerHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HIDING_MSG:
				((View) msg.obj).setVisibility(View.GONE);
				break;
			}
		}
	};

	private static Bundle getStreamFields() {
		Bundle bundle = new Bundle();
		bundle.putString("fields", "stream_premium_preview_mp4_url, stream_h264_ld_url");
		return bundle;
	}

	private DailymotionRequestListener mStreamingRequestListener = new DailymotionRequestListener() {

		@Override
		public void onSuccess(JSONObject response) {
			if (mUserIsPremium) {
				mStreamUrl = response.optString("stream_h264_ld_url");
			} else {
				mStreamUrl = response.optString("stream_premium_preview_mp4_url");
			}
			KidsLogger.d(LOG_TAG, mStreamUrl);
			fetchDone(true);
		}

		@Override
		public void onFailure(DailyException e) {
			KidsLogger.w(LOG_TAG, String.format("Could not fetch video stream : %s", e.toString()));
			fetchDone(false);
		}

		private void fetchDone(final boolean success) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (!success || !startStreamingEpisode()) {
						onStreamingError(R.string.videoplayer_no_stream_error);
					}
				}
			});
		}
	};

	private void showSubscriptionDialog() {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag("subscription_dialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);

		// Create and show the dialog.
		SubscriptionDialogFragment newFragment = SubscriptionDialogFragment.newInstance();
		newFragment.show(ft, "subscription_dialog");
	}

	public void subscribe() {
		// Launch subscription activity
		Intent intent = new Intent(this, AuthenticationActivity.class);
		startActivityForResult(intent, SUBSCRIPTION_CODE);
	}

	public void cancelDialog() {
		finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent extras) {
		if (requestCode == SUBSCRIPTION_CODE) {
			mUserIsPremium = DependencyResolverImpl.getInstance().getConfiguration()
					.hasSubscribed();
			loadNewEpisode(mEpisode, mVideoListPosition);
		}
	}
}