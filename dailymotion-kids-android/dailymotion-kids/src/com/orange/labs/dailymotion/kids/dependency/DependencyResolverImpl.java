package com.orange.labs.dailymotion.kids.dependency;

import java.util.concurrent.Executors;

import android.content.Context;

import com.orange.labs.dailymotion.kids.config.ConfigurationManager;
import com.orange.labs.dailymotion.kids.config.Constants;
import com.orange.labs.dailymotion.kids.playlist.AsyncPlaylistFetcher;
import com.orange.labs.dailymotion.kids.user.AsyncUsersFetcher;
import com.orange.labs.dailymotion.kids.utils.HeroesCacheUpdater;
import com.orange.labs.dailymotion.kids.video.AsyncVideosFetcher;
import com.orangelabs.dailymotion.Dailymotion;

/**
 * Implementation of {@link DependencyResolver} giving access to singleton and utility objects
 * required by the rest of the application.
 * 
 * @author Jean-Francois Moy
 * 
 */
public class DependencyResolverImpl implements DependencyResolver {

	private static DependencyResolver sInstance;

	/** Application Context */
	private final Context mAppContext;

	/** Dailymotion object useful to interact with Dailymotion Graph API. */
	private Dailymotion mDailymotion;

	/** Configuration Manager */
	private ConfigurationManager mConfigManager;

	/** Cache Updater used to cache locally the playlists and videos */
	private HeroesCacheUpdater mCacheUpdater;

	public static void initialize(Context appContext) {
		if (sInstance == null) {
			sInstance = new DependencyResolverImpl(appContext);
		}
	}

	public static DependencyResolver getInstance() {
		if (sInstance == null) {
			throw new IllegalStateException(
					"Dependency resolver should be initialized before being accessed");
		}
		return sInstance;
	}

	private DependencyResolverImpl(Context appContext) {
		mAppContext = appContext;
	}

	@Override
	public Context getApplicationContext() {
		return mAppContext;
	}

	@Override
	public Dailymotion getDailymotion() {
		if (mDailymotion == null) {
			mDailymotion = new Dailymotion(getApplicationContext(),
					Executors.newCachedThreadPool(), Constants.DAILYMOTION_CLIENT_ID,
					Constants.DAILYMOTION_CLIENT_SECRET);
		}
		return mDailymotion;
	}

	@Override
	public AsyncVideosFetcher createVideosFetcher() {
		return new AsyncVideosFetcher(mDailymotion, getApplicationContext());
	}

	@Override
	public AsyncPlaylistFetcher createPlaylistsFetcher() {
		return new AsyncPlaylistFetcher(mDailymotion, getApplicationContext());
	}

	@Override
	public ConfigurationManager getConfiguration() {
		if (mConfigManager == null) {
			mConfigManager = new ConfigurationManager(getApplicationContext());
		}
		return mConfigManager;
	}

	@Override
	public HeroesCacheUpdater getCacheUpdater() {
		if (mCacheUpdater == null) {
			mCacheUpdater = new HeroesCacheUpdater(getApplicationContext(), this, getConfiguration());
		}
		return mCacheUpdater;
	}

	@Override
	public AsyncUsersFetcher createUserFetcher() {
		return new AsyncUsersFetcher(getDailymotion(), getApplicationContext());
	}

}
