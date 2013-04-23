package com.orange.labs.dailymotion.kids.db;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.orange.labs.dailymotion.kids.playlist.PlaylistContract;
import com.orange.labs.dailymotion.kids.playlist.PlaylistContract.PlaylistColumns;
import com.orange.labs.dailymotion.kids.playlist.PlaylistsProvider;
import com.orange.labs.dailymotion.kids.user.UserContract.UserColumns;
import com.orange.labs.dailymotion.kids.user.UsersProvider;
import com.orange.labs.dailymotion.kids.video.VideoContract;
import com.orange.labs.dailymotion.kids.video.VideoContract.VideoColumns;
import com.orange.labs.dailymotion.kids.video.VideosProvider;

/**
 * Helper class to create a database helper for a set of columns.
 * <p>
 * This class simply wraps several {@link TableCreator} instances for each of the table of the
 * application. It also contains the global properties of the database such as its name, version,
 * etc.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String DB_NAME = "dailymotionkids.db";
	private static final int DB_VERSION = 1;

	/**
	 * Singleton instance : allows multithreading access of the database.
	 */
	private static DatabaseHelper sSingleton = null;

	private static final List<TableCreator> TABLE_CREATORS = new ArrayList<TableCreator>();

	static {
		TABLE_CREATORS.add(new TableCreator(VideosProvider.TABLE_NAME, VideoColumns.values(),
				VideoContract.TABLE_CONSTRAINTS));
		TABLE_CREATORS.add(new TableCreator(PlaylistsProvider.TABLE_NAME, PlaylistColumns.values(),
				PlaylistContract.TABLE_CONSTRAINTS));
		TABLE_CREATORS.add(new TableCreator(UsersProvider.TABLE_NAME, UserColumns.values()));
	}

	/** The current version of the database */
	private final int mVersion;

	public static synchronized DatabaseHelper getInstance(Context context) {
		if (sSingleton == null) {
			sSingleton = new DatabaseHelper(context, DB_NAME, DB_VERSION);
		}
		return sSingleton;
	}

	private DatabaseHelper(Context context, String dbName, int version) {
		super(context, dbName, null, version);
		mVersion = version;
	}

	@Override
	public void onOpen(SQLiteDatabase db) {
		if (!db.isReadOnly()) {
			// Enable foreign key constraints
			db.execSQL("PRAGMA foreign_keys=ON;");
		}
		super.onOpen(db);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		for (TableCreator tableCreator : TABLE_CREATORS) {
			db.execSQL(tableCreator.getCreateTableQuery(mVersion));
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		for (TableCreator tableCreator : TABLE_CREATORS) {
			db.execSQL(tableCreator.getUpgradeTableQuery(oldVersion, newVersion));
		}
	}
}
