package com.orangelabs.dailymotion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

public final class DailymotionUtils {

	private static final String CONTENT_TYPE = "Content-Type";
	private static final String FORM_URL_ENCODED_TYPE = "application/x-www-form-urlencoded";

	/**
	 * Encode parameters contained in the provided {@link Bundle} and return the built value.
	 * 
	 * @param parameters
	 *            Parameters that should be encoded.
	 * @return Encoded parameters as a String.
	 */
	public static String encodeUrl(Bundle parameters) {
		if (parameters == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String key : parameters.keySet()) {
			Object parameter = parameters.get(key);
			if (!(parameter instanceof String)) {
				continue;
			}

			if (first)
				first = false;
			else
				sb.append("&");
			sb.append(URLEncoder.encode(key) + "=" + URLEncoder.encode(parameters.getString(key)));
		}
		return sb.toString();
	}

	/**
	 * Decode parameters contained in the provided String and generate a {@link Bundle} associating
	 * parameters name and values.
	 * 
	 * @param url
	 *            Url that we decode parameters from.
	 * @return Bundle containing decoded parameters.
	 */
	public static Bundle decodeUrl(String url) {
		Bundle bundle = new Bundle();
		if (!TextUtils.isEmpty(url) && url.indexOf("?") != -1) {
			String urlParameters = url.substring(url.indexOf("?") + 1);
			String[] parameters = urlParameters.split("&");
			for (String parameter : parameters) {
				String[] keyValue = parameter.split("=");
				if (keyValue.length == 2) {
					bundle.putString(URLDecoder.decode(keyValue[0]), URLDecoder.decode(keyValue[1]));
				}
			}
		}

		return bundle;
	}

	/**
	 * Execute a request to the specified URL, using the provided Bundle as parameters. A HTTP
	 * method should be provided as well, but will be defaulted to GET.
	 * 
	 * @param url
	 *            Url to request
	 * @param parameters
	 *            Parameters to include in the body
	 * @param method
	 *            HTTP method to use to request the remote server.
	 * @return Response from the server as a String.
	 */
	public static String request(String url, Bundle parameters, String method) throws IOException,
			MalformedURLException {
		if (TextUtils.isEmpty(method)) {
			method = "GET";
		}

		URL requestUrl;
		if (method.equalsIgnoreCase("GET")) {
			requestUrl = new URL(url + "?" + encodeUrl(parameters));
		} else {
			requestUrl = new URL(url);
		}

		DailymotionLogger.v("Dailymotion Utils", "Requesting : " + requestUrl);

		String response;
		HttpURLConnection conn;
		if (requestUrl.getProtocol().equals("https")) {
			conn = (HttpsURLConnection) requestUrl.openConnection();
		} else {
			conn = (HttpURLConnection) requestUrl.openConnection();
		}
		try {
			if (method.equalsIgnoreCase("POST")) {
				conn.setRequestMethod("POST");
				conn.setRequestProperty(CONTENT_TYPE, FORM_URL_ENCODED_TYPE);
				conn.setDoInput(true);
				conn.setDoOutput(true);
				writeStream(encodeUrl(parameters), conn.getOutputStream());
			}

			response = readStream(conn.getInputStream());
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		return response;
	}

	/**
	 * Read the stream and returns the String it contains.
	 */
	private static String readStream(InputStream in) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, "utf-8"));
		StringBuilder builder = new StringBuilder();
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			DailymotionLogger.v("Line", line);
			builder.append(line);
		}
		reader.close();
		return builder.toString();
	}

	/**
	 * Write the provided String, line by line, in the provided OutputStream.
	 */
	private static void writeStream(String toWrite, OutputStream out) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
		BufferedReader reader = new BufferedReader(new StringReader(toWrite));
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			writer.write(line);
		}
		reader.close();
		writer.close();
	}

	/**
	 * Simple method that takes a String as an argument (typically a response from Dailymotion) and
	 * parse it using {@link JSONObject}.
	 */
	public static JSONObject parseJson(String value) throws DailyException {
		JSONObject json = null;
		try {
			json = new JSONObject(value);

			// An error has occurred while requesting. Throw an exception.
			if (json.has("error")) {
				JSONObject error = json.optJSONObject("error");
				if (error != null) {
					String type = error.getString("type");
					String message = error.has("message") ? error.getString("message") : "";
					throw DailyException.getException(type, message);
				} else {
					throw DailyException.getException(json.getString("error"),
							json.getString("error_description"));
				}
			}
		} catch (JSONException e) {
			throw new DailyException(DailyException.INVALID_RESPONSE,
					"Impossible to parse the response as JSON.");
		}

		return json;
	}

	/**
	 * Format the permissions by concatenating them using white space delimiter. If no permission
	 * has been provided, return an empty string.
	 * 
	 * @param permissions
	 *            Permissions to format
	 * @return String contained formatted permissions.
	 */
	public static String formatPermissions(String[] permissions) {
		StringBuilder permsBuilder = new StringBuilder();
		if (permissions != null && permissions.length > 0) {
			for (String permission : permissions) {
				if (permsBuilder.length() > 0) {
					permsBuilder.append(" ");
				}
				permsBuilder.append(permission);
			}
		}
		return permsBuilder.toString();
	}

	/**
	 * A bug in Android OS prior to Froyo forbids the usage of http.keepAlive as closing a
	 * connection input stream would poison the connection pool.
	 */
	static void disableConnectionReuseIfNecessary() {
		// HTTP connection reuse which was buggy pre-froyo
		if (Integer.parseInt(Build.VERSION.SDK) < 8) {
			System.setProperty("http.keepAlive", "false");
		}
	}

	/**
	 * Enable HTTP response cache, available from Android 4.0 onward, using reflection.
	 */
	static void enableHttpResponseCache(Context context, long cacheSize) {
		try {
			File httpCacheDir = new File(context.getCacheDir(), "http");
			Class.forName("android.net.http.HttpResponseCache")
					.getMethod("install", File.class, long.class)
					.invoke(null, httpCacheDir, cacheSize);
		} catch (Exception httpResponseCacheNotAvailable) {
			// Do nothing, user is using a version prior to 4.0
		}
	}

}
