package com.orange.labs.dailymotion.kids.dependency;

import android.content.Context;

import com.orange.labs.dailymotion.kids.config.ConfigurationManager;
import com.orange.labs.dailymotion.kids.playlist.AsyncPlaylistFetcher;
import com.orange.labs.dailymotion.kids.user.AsyncUsersFetcher;
import com.orange.labs.dailymotion.kids.utils.HeroesCacheUpdater;
import com.orange.labs.dailymotion.kids.video.AsyncVideosFetcher;
import com.orangelabs.dailymotion.Dailymotion;
import com.orangelabs.dailymotion.DailymotionRequestor;

/**
 * Interface defining a Dependency Resolver. The Dependency Resolver is responsible of making
 * application-wide objects (typically singleton) available to the whole application. Such objects
 * can be accessed through the methods defined in this interface.
 * 
 * @author Jean-Francois Moy
 * 
 */
public interface DependencyResolver {

	/**
	 * Return the Application {@link Context} for the application.
	 */
	public Context getApplicationContext();

	/**
	 * Return the {@link Dailymotion} instance used by the application to manage the access to the
	 * Dailymotion API as well as to request it using a {@link DailymotionRequestor}.
	 */
	public Dailymotion getDailymotion();

	/**
	 * Retrive the cache updater that allows to download and update all the data stored locally.
	 */
	public HeroesCacheUpdater getCacheUpdater();
	
	/**
	 * Create a new videos fetcher using the current Dailymotion object and returns it.
	 */
	public AsyncVideosFetcher createVideosFetcher();
	
	/**
	 * Create a new playlists fetcher using the current Dailymotion object and returns it.
	 */
	public AsyncPlaylistFetcher createPlaylistsFetcher();
	
	/**
	 * Return the Configuraiton Manager used to store the application settings
	 */
	public ConfigurationManager getConfiguration();

	public AsyncUsersFetcher createUserFetcher();
}
