package com.orange.labs.dailymotion.kids.user;

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
import com.orange.labs.dailymotion.kids.video.VideoContract;
import com.orange.labs.dailymotion.kids.video.VideosProvider;

public class UsersProvider extends BaseContentProvider {

	public static final String TABLE_NAME = "users";

	private static final int USERS = 10;
	private static final int USER_ID = 20;
	private static final int SUGGESTION_ID = 30;
	private static final String BASE_PATH = "users";
	private static final String SUGGESTION_PATH = "suggestions";

	static final String AUTHORITY = "com.orange.labs.dailymotion.kids.usersprovider";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
	public static final Uri SUGGESTION_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH
			+ "/" + SUGGESTION_PATH);

	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + AUTHORITY
			+ ".users";
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ AUTHORITY + ".user";

	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		sURIMatcher.addURI(AUTHORITY, BASE_PATH, USERS);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", USER_ID);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/" + SUGGESTION_PATH + "/*", SUGGESTION_ID);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		int rowsDeleted = 0;
		switch (uriType) {
		case USERS:
			rowsDeleted = db.delete(TABLE_NAME, selection, selectionArgs);
			break;
		case USER_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = db.delete(TABLE_NAME, UserContract.ID + "=" + id, null);
			} else {
				rowsDeleted = db.delete(TABLE_NAME, UserContract.ID + "=" + id + " and "
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
		case USERS:
			type = CONTENT_TYPE;
			break;
		case USER_ID:
			type = CONTENT_ITEM_TYPE;
			break;
		case SUGGESTION_ID:
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
		case USERS:
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
		case USERS:
			break;
		case USER_ID:
			// Adding the ID to the original query
			queryBuilder.appendWhere(UserContract.ID + "=" + uri.getLastPathSegment());
			break;
		case SUGGESTION_ID:
			queryBuilder.appendWhere(UserContract.CATEGORY + "= (SELECT " + UserContract.CATEGORY
					+ " FROM " + UsersProvider.TABLE_NAME + " WHERE " + UserContract.DAILYMOTION_ID
					+ "='" + uri.getLastPathSegment() + "')");
			queryBuilder.appendWhere(" AND " + UserContract.DAILYMOTION_ID + " != '"
					+ uri.getLastPathSegment() + "'");
			queryBuilder.appendWhere("AND (SELECT COUNT(*) FROM " + VideosProvider.TABLE_NAME
					+ " v, " + PlaylistsProvider.TABLE_NAME + " p WHERE p."
					+ PlaylistContract.OWNER + "='" + uri.getLastPathSegment() + "' AND v."
					+ VideoContract.PLAYLIST + "=p." + PlaylistContract.DAILYMOTION_ID + ") > 0");
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
		case USERS:
			rowsUpdated = sqlDB.update(TABLE_NAME, values, selection, selectionArgs);
			break;
		case USER_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = sqlDB.update(TABLE_NAME, values, UserContract.ID + "=" + id, null);
			} else {
				rowsUpdated = sqlDB.update(TABLE_NAME, values, UserContract.ID + "=" + id + " and "
						+ selection, selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsUpdated;
	}

}
