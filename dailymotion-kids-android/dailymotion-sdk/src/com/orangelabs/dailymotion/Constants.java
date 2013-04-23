package com.orangelabs.dailymotion;

public final class Constants {

	/**
	 * URI used for authenticating the user against Dailymotion.
	 */
	static final String DAILYMOTION_URI = "https://api.dailymotion.com/";

	/**
	 * Oauth Endpoint
	 */
	static final String OAUTH_ENDPOINT = "oauth/";

	/**
	 * End point used to authenticate.
	 */
	static final String AUTH_ENDPOINT = OAUTH_ENDPOINT + "authorize";

	/**
	 * End point used to refresh an existing token.
	 */
	static final String TOKEN_ENDPOINT = OAUTH_ENDPOINT + "token";

	/**
	 * Parameter used to specify the callback URI once the authentication has been performed.
	 */
	static final String REDIRECT_URI = "none://fake-callback";

	/**
	 * Threshold that defines how far from the validity date a token becomes invalid. (default is 5
	 * minutes).
	 */
	static final long REFRESH_TOKEN_THRESHOLD = 5L * 60L * 1000L;

	/**
	 * Size of the cache used for HTTP response caching. Default is 10Mb.
	 */
	static final int CACHE_SIZE = 10 * 1024 * 1024;
	
}
