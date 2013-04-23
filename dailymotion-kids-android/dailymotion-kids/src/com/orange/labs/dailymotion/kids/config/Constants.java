package com.orange.labs.dailymotion.kids.config;

/**
 * Final class that contains all constants that are used throughout the application.
 * 
 * @author Jean-Francois Moy
 */
public final class Constants {

	/**
	 * Key identifying the Dailymotion Kids application when communicating with the Dailymotion API.
	 */
	public static final String DAILYMOTION_CLIENT_ID = "852256438bc3af3d3d70";

	/**
	 * Secret key used in combination with the Dailymotion Client ID key to communicate with the
	 * Dailymotion API.
	 */
	public static final String DAILYMOTION_CLIENT_SECRET = "d3c954635eb1a24aae57168b24b0935a5decae01";

	/**
	 * Frequency update of local cache should occur (in ms).
	 */
	public static final long UPDATE_PERIOD = 24L * 60L * 60L * 1000L;

	/**
	 * Number of API calls that can be performed simultaneously while updating the local cache.
	 */
	public static final int NB_SIMULTANEOUS_DOWNLOADS = 5;

	/**
	 * Page Size : When requesting Dailymotion, the API returns 10 items by default. This property
	 * is used to override that behaviour and retrieve more items at each request.
	 */
	public static final int PAGE_LIMIT = 25;

	/**
	 * Regular expressions applied to all episodes titles when they are retrieved, to eliminate
	 * uninteresting strings.
	 */
	public static final String TITLE_REGEXP = "\\s*+-?\\s*+S\\d\\d Ep\\d\\d";

	/**
	 * Regular expression applied to all episodes titles to remove owner's name from their title.
	 */
	public static final String OWNER_REGEXP = "^\\s*+%s\\s*+-?\\s*+";

	/**
	 * Flag for fetching all the remote content in one shot while updating the cache, or only
	 * heroes/playlists.
	 */
	public static final boolean UPDATE_INCLUDE_EPISODES = false;

}
