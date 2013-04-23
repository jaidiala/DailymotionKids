package com.orange.labs.dailymotion.kids.callback;

/**
 * Factory Method for commonly used callbacks.
 * 
 * @author Jean-Francois Moy
 */
public final class Callbacks {

	/**
	 * The single global instance of the empty callback.
	 * <p>
	 * This is without type parameter, because we want to re-use the same single instance for any
	 * type of operation.
	 */
	// Safe, can be used with any type.
	@SuppressWarnings("rawtypes")
	private static final Callback EMPTY_CALLBACK = new Callback() {
		@Override
		public void onFailure(Exception error) {
			// Ignored.
		}

		@Override	
		public void onSuccess(Object result) {
			// Ignored.
		}

	};

	/**
	 * Return an empty callback that can be ignored by the caller. Internally, it always uses the
	 * same {@link Callback} instance, EMPTY_CALLBACK.
	 */
	// Safe, can be used with any type.
	@SuppressWarnings("unchecked")
	public static <T> Callback<T> emptyCallback() {
		return EMPTY_CALLBACK;
	}
}
