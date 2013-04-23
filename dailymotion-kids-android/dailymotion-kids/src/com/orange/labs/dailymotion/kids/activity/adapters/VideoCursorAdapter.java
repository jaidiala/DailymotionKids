package com.orange.labs.dailymotion.kids.activity.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.dailymotion.kids.R;
import com.orange.labs.dailymotion.kids.db.DatabaseUtils;
import com.orange.labs.dailymotion.kids.utils.KidsLogger;
import com.orange.labs.dailymotion.kids.video.VideoContract.VideoColumns;

public class VideoCursorAdapter extends CursorAdapter {
	
	private static String LOG_TAG = "VideoCursorAdapter";

	private LayoutInflater mInflater;
	private ImageLoader mImageLoader = ImageLoader.getInstance();

	public VideoCursorAdapter(Context context) {
		super(context, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		if (holder == null) {
			holder = new ViewHolder();
			holder.thumbnail = (ImageView) view.findViewById(R.id.video_list_item_iv);
			holder.title = (TextView) view.findViewById(R.id.video_list_item_tv);
			view.setTag(holder);
		}

		holder.title.setText(DatabaseUtils.getString(mCursor, VideoColumns.TITLE));
		mImageLoader.displayImage(DatabaseUtils.getString(mCursor, VideoColumns.THUMBNAIL_URL),
				holder.thumbnail);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
		return mInflater.inflate(R.layout.video_list_item, null);
	}

	static class ViewHolder {
		ImageView thumbnail;
		TextView title;
	}
}
