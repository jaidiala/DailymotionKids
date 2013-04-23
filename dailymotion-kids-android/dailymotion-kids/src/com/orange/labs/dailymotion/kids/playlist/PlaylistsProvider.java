package com.orange.labs.dailymotion.kids.playlist;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.orange.labs.dailymotion.kids.db.BaseContentProvider;
import com.orange.labs.dailymotion.kids.user.UserContract.UserColumns;
import com.orange.labs.dailymotion.kids.utils.KidsLogger;

public class PlaylistsProvider extends BaseContentProvider {

	public static final String TABLE_NAME = "playlists";
	
	private static final int PLAYLISTS = 10;
	private static final int PLAYLIST_ID = 20;

	static final String AUTHORITY = "com.orange.labs.dailymotion.kids.playlistsprovider";

	private static final String BASE_PATH = "playlists";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + AUTHORITY + ".playlists";
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + AUTHORITY + ".playlist";

	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		sURIMatcher.addURI(AUTHORITY, BASE_PATH, PLAYLISTS);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", PLAYLIST_ID);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		int rowsDeleted = 0;
		switch (uriType) {
		case PLAYLISTS:
			rowsDeleted = db.delete(TABLE_NAME, selection, selectionArgs);
			break;
		case PLAYLIST_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = db
						.delete(TABLE_NAME, PlaylistContract.ID + "=" + id, null);
			} else {
				rowsDeleted = db.delete(TABLE_NAME, PlaylistContract.ID + "=" + id
						+ " and " + selection, selectionArgs);
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
		case PLAYLISTS:
			type = CONTENT_TYPE;
			break;
		case PLAYLIST_ID:
			type = CONTENT_ITEM_TYPE;
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
		case PLAYLISTS:
			id = db.insertWithOnConflict(TABLE_NAME, null, values,SQLiteDatabase.CONFLICT_REPLACE);
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
		case PLAYLISTS:
			break;
		case PLAYLIST_ID:
			// Adding the ID to the original query
			queryBuilder.appendWhere(UserColumns.ID.getName() + "=" + uri.getLastPathSegment());
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		KidsLogger.v("Playlist Provider", selection);
		KidsLogger.v("Playlist Provider", selectionArgs.toString());
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
		case PLAYLISTS:
			rowsUpdated = sqlDB.update(TABLE_NAME, values, selection, selectionArgs);
			break;
		case PLAYLIST_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = sqlDB.update(TABLE_NAME, values, PlaylistContract.ID + "="
						+ id, null);
			} else {
				rowsUpdated = sqlDB.update(TABLE_NAME, values, PlaylistContract.ID + "="
						+ id + " and " + selection, selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		return rowsUpdated;
	}

}
