package com.orange.labs.dailymotion.kids.utils;

import android.text.TextUtils;
import android.util.Log;

/**
 * Utility class used to log general and debugging information for the Dailymotion Kids application.
 * A flag allows to disable/enable the additional debugging logs.
 * 
 * @author Jean-Francois Moy
 *  
 */
public class KidsLogger {

	/**
	 * Application Wide Flag - Used to identify logs that belong to the library.
	 */
	private static final String APPLICATION_TAG = "Dailymotion Kids";

	/**
	 * Debugging Mode - Enables additional logging.
	 */
	private static final boolean DEBUGGING = false;

	public static void v(final String tag, final String msg) {
		if (DEBUGGING) {
			Log.v(APPLICATION_TAG, createMsg(tag, msg));
		}
	}

	public static void d(final String tag, final String msg) {
		if (DEBUGGING) {
			Log.d(APPLICATION_TAG, createMsg(tag, msg));
		}
	}

	public static void i(final String tag, final String msg) {
		Log.i(APPLICATION_TAG, createMsg(tag, msg));
	}

	public static void w(final String tag, final String msg) {
		Log.w(APPLICATION_TAG, createMsg(tag, msg));
	}

	public static void e(final String tag, final String msg) {
		Log.e(APPLICATION_TAG, createMsg(tag, msg));
	}

	private static String createMsg(final String tag, final String msg) {
		StringBuilder builder = new StringBuilder();
		if (!TextUtils.isEmpty(tag)) {
			builder.append("[ ").append(tag).append(" ] ");
		}
		if (!TextUtils.isEmpty(msg)) {
			builder.append(msg);
		}
		return builder.toString();
	}
	
}
