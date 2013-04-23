package com.orangelabs.dailymotion;

import android.text.TextUtils;

/**
 * Exception related to the Dailymotion SDK usage. This class integrates a factory to easily
 * generate the corresponding Exception to an error code. Alternatively, you can provide your own
 * using the constructor.
 * 
 * @author Jean-Francois Moy
 */
public class DailyException extends Exception {

	/**
	 * Unknown Error.
	 */
	public static final int UNKNOWN_ERROR = -1;

	/**
	 * Response could not be parsed.
	 */
	public static final int INVALID_RESPONSE = -2;

	/**
	 * Required permission has not been given for the request.
	 */
	public static final int INVALID_SCOPE = -3;
	
	/**
	 * Authentication has failed, because of an incorrect grant type or credentials.
	 */
	public static final int INVALID_GRANT_TYPE = -9;

	/**
	 * Acess Denied
	 */
	public static final int ACCESS_DENIED = -4;

	/**
	 * Parameter sent along the request was invalid.
	 */
	public static final int INVALID_PARAMETER = -5;

	/**
	 * Token has expired, refresh it or obtain a new one.
	 */
	public static final int EXPIRED_TOKEN = -6;
	
	/**
	 * Input/Ouptut Exception occured while communicating with the remote server.
	 */
	public static final int IO_EXCEPTION = -7;
	
	/**
	 * No token could be found (either access or refresh token).
	 */
	public static final int NO_TOKEN = -8;

	/**
	 * Factory method to obtain a {@link DailyException} instance based on the provided error
	 * code and description.
	 * 
	 * @param errorCode
	 *            Error Code returned by the Dailymotion API.
	 * @param description
	 *            Description of the error that will be returned when obtaining the message related
	 *            to the exception.
	 * @return {@link DailyException} based on provided values.
	 */
	public static DailyException getException(String errorCode, String description, Throwable cause) {
		int code = UNKNOWN_ERROR;
		if (errorCode.equals("invalid_scope")) {
			code = INVALID_SCOPE;
		} else if (errorCode.equals("access_denied")) {
			code = ACCESS_DENIED;
		} else if (errorCode.equals("invalid_parameter")) {
			code = INVALID_PARAMETER;
		} else if (errorCode.equals("invalid_grant")) {
			code = INVALID_GRANT_TYPE;
		}
		return new DailyException(code, description, cause);
	}
	
	public static DailyException getException(String errorCode, String description)
	{
		return DailyException.getException(errorCode, description, null);
	}
	
	/**
	 * Internal code for an error that occurred while using Dailymotion GRAPH API.
	 */
	private int mCode = UNKNOWN_ERROR;

	/**
	 * Verbose description of the error, usually reported by Dailymotion.
	 */
	private String mDescription;
	
	/**
	 * Throwable that caused the exception to be raised. (can be null)
	 */
	private final Throwable mCause;

	public DailyException(int errorCode, String errorDescription, Throwable cause) {
		mCode = errorCode;
		mDescription = errorDescription;
		mCause = cause;
	}

	public DailyException(int errorCode, String description) {
		this(errorCode, description, null);
	}
	
	public DailyException(int errorCode) {
		this(errorCode, "", null);
	}

	public int getErrorCode() {
		return mCode;
	}

	@Override
	public String getMessage() {
		return !TextUtils.isEmpty(mDescription) ? mDescription : "";
	}
	
	@Override
	public Throwable getCause() {
		return mCause;
	}

	/**
	 * Generated Serial ID
	 */
	private static final long serialVersionUID = -6468685538437367192L;
}