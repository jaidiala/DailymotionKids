package com.orange.labs.dailymotion.kids.callback;

/**
 * Callback that can be used for asynchronous operations. Depending of the success of the operation,
 * the onSuccess or onFailure method is invoked.
 */
public interface Callback<T> {

	/**
	 * Executed when the operation has ended successfully, providing the result.
	 */
	public void onSuccess(final T result);

	/**
	 * Executed when the operation has failed, providing the responsible {@link Exception} if known.
	 */
	public void onFailure(final Exception e);

}
