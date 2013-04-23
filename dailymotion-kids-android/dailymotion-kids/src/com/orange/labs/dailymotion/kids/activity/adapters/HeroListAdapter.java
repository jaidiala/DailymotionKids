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
import com.orange.labs.dailymotion.kids.user.UserContract.UserColumns;

/**
 * Adapter used to display heroes grid item in a GridView. It consists of displaying an image above
 * the hero name.
 * 
 * @author Jean-Francois Moy
 * 
 */
public class HeroListAdapter extends CursorAdapter {

	protected ImageLoader mImageLoader = ImageLoader.getInstance();
	private LayoutInflater mInflater;

	public HeroListAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		if (holder == null) {
			holder = new ViewHolder();
			holder.name = (TextView) view.findViewById(R.id.hero_list_item_tv);;
			holder.avatar = (ImageView) view.findViewById(R.id.hero_list_item_iv);
			view.setTag(holder);
		}

		holder.name.setText(DatabaseUtils.getString(mCursor, UserColumns.SCREEN_NAME));
		mImageLoader.displayImage(DatabaseUtils.getString(mCursor, UserColumns.AVATAR_URL), holder.avatar);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return mInflater.inflate(R.layout.hero_list_item, null);
	}

	static class ViewHolder {
		TextView name;
		ImageView avatar;
	}
	
}
