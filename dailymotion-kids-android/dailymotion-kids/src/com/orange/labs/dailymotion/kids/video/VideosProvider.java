package com.orange.labs.dailymotion.kids.video;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.orange.labs.dailymotion.kids.db.BaseContentProvider;
import com.orange.labs.dailymotion.kids.playlist.PlaylistContract;
import com.orange.labs.dailymotion.kids.playlist.PlaylistsProvider;

public class VideosProvider extends BaseContentProvider {

	public static final String TABLE_NAME = "videos";

	private static final int VIDEOS = 10;
	private static final int VIDEO_ID = 20;
	private static final int USER_ID = 30;

	static final String AUTHORITY = "com.orange.labs.dailymotion.kids.videosprovider";

	private static final String BASE_PATH = "videos";
	private static final String USER_PATH = "user";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
	public static final Uri USER_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH + "/"
			+ USER_PATH);

	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + AUTHORITY
			+ ".videos";
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ AUTHORITY + ".video";

	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		sURIMatcher.addURI(AUTHORITY, BASE_PATH, VIDEOS);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", VIDEO_ID);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/" + USER_PATH + "/*", USER_ID);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		int rowsDeleted = 0;
		switch (uriType) {
		case VIDEOS:
			rowsDeleted = db.delete(TABLE_NAME, selection, selectionArgs);
			break;
		case VIDEO_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {

				rowsDeleted = db.delete(TABLE_NAME, VideoContract.ID + "=" + id, null);
			} else {
				rowsDeleted = db.delete(TABLE_NAME, VideoContract.ID + "=" + id + " and "
						+ selection, selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsDeleted;
	}

	@Override
	public String getType(Uri uri) {
		String type = null;
		int uriType = sURIMatcher.match(uri);

		switch (uriType) {
		case VIDEOS:
			type = CONTENT_TYPE;
			break;
		case VIDEO_ID:
			type = CONTENT_ITEM_TYPE;
			break;
		case USER_ID:
			type = CONTENT_TYPE;
			break;
		}

		return type;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase db = mDbHelper.getWritableDatabase();

		long id = -1;
		switch (uriType) {
		case VIDEOS:
			id = db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return Uri.parse(BASE_PATH + "/" + id);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {

		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(TABLE_NAME);

		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
		case VIDEOS:
			break;
		case VIDEO_ID:
			// Adding the ID to the original query
			queryBuilder.appendWhere(VideoContract.ID + "=" + uri.getLastPathSegment());
			break;
		case USER_ID:
			queryBuilder.appendWhere(VideoContract.PLAYLIST + "=(SELECT "
					+ PlaylistContract.DAILYMOTION_ID + " FROM " + PlaylistsProvider.TABLE_NAME
					+ " WHERE " + PlaylistContract.OWNER + "='" + uri.getLastPathSegment()
					+ "' ORDER BY " + PlaylistContract.SEASON + " ASC LIMIT 1)");
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null,
				sortOrder);
		// Make sure that potential listeners are getting notified
		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = mDbHelper.getWritableDatabase();
		int rowsUpdated = 0;
		switch (uriType) {
		case VIDEOS:
			rowsUpdated = sqlDB.update(TABLE_NAME, values, selection, selectionArgs);
			break;
		case VIDEO_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = sqlDB.update(TABLE_NAME, values, VideoContract.ID + "=" + id, null);
			} else {
				rowsUpdated = sqlDB.update(TABLE_NAME, values, VideoContract.ID + "=" + id
						+ " and " + selection, selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsUpdated;
	}

}
