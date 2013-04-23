package com.orangelabs.dailymotion;

import android.util.Log;

/**
 * Utility class used to log general and debugging information for the Dailymotion SDK. A flag
 * allows to disable/enable the additional debugging logs.
 * 
 * @author Jean-Francois Moy
 * 
 */
public final class DailymotionLogger {

	/**
	 * Application Wide Flag - Used to identify logs that belong to the library.
	 */
	private static final String APPLICATION_TAG = "Dailymotion SDK";

	/**
	 * Debugging Mode - Enables additional logging.
	 */
	private static final boolean DEBUGGING = false;

	public static void v(final String tag, final String msg) {
		if (DEBUGGING) {
			Log.v(APPLICATION_TAG, formattedMessage(tag, msg));
		}
	}

	public static void d(final String tag, final String msg) {
		if (DEBUGGING) {
			Log.d(APPLICATION_TAG, formattedMessage(tag, msg));
		}
	}

	public static void i(final String tag, final String msg) {
		Log.i(APPLICATION_TAG, formattedMessage(tag, msg));
	}

	public static void w(final String tag, final String msg) {
		Log.w(APPLICATION_TAG, formattedMessage(tag, msg));
	}

	public static void e(final String tag, final String msg) {
		Log.e(APPLICATION_TAG, formattedMessage(tag, msg));
	}

	private static String formattedMessage(String tag, String msg) {
		return new StringBuilder("[ ").append(tag).append(" ]").append(msg).toString();
	}

}
