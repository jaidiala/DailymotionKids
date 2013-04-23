package com.orange.labs.dailymotion.kids.db;

import android.content.ContentProvider;

public abstract class BaseContentProvider extends ContentProvider {

	protected DatabaseHelper mDbHelper;
	
	@Override
	public boolean onCreate() {
		mDbHelper = DatabaseHelper.getInstance(getContext());
		return (mDbHelper != null);
	}

}
