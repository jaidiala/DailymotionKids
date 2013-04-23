package com.orange.labs.dailymotion.kids.activity.adapters;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.dailymotion.kids.R;
import com.orange.labs.dailymotion.kids.user.User;
import com.orange.labs.dailymotion.kids.user.User.State;
import com.orange.labs.dailymotion.kids.user.UsersProvider;
import com.orange.labs.dailymotion.kids.utils.KidsUtils;

/**
 * Adapter used to display heroes grid item in a GridView. It consists of displaying an image above
 * the hero name.
 * 
 * @author Jean-Francois Moy
 * 
 */
public class HeroGridAdapter extends CursorAdapter implements OnClickListener {

	protected ImageLoader mImageLoader = ImageLoader.getInstance();
	private final Context mContext;
	private final LayoutInflater mInflater;
	private Bitmap mNewHeroBitmap;
	private boolean mEditMode = false;

	public HeroGridAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
		mContext = context;
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		if (holder == null) {
			holder = new ViewHolder();
			holder.thumbnail = (ImageView) view.findViewById(R.id.hero_item_iv);
			holder.name = (TextView) view.findViewById(R.id.hero_name_tv);
			holder.newView = (ImageView) view.findViewById(R.id.hero_new_iv);
			holder.actionButton = (ImageView) view.findViewById(R.id.hero_item_hide_b);
			holder.favButton = (ImageView) view.findViewById(R.id.hero_item_fav_b);
			holder.actionButton.setOnClickListener(this);
			holder.favButton.setOnClickListener(this);
			view.setTag(holder);
		}

		User hero = User.fromCursor(cursor);
		holder.name.setText(hero.getScreenName());
		mImageLoader.displayImage(hero.getAvatarUrl(), holder.thumbnail);

		if (hero.isNew()) {
			if (mNewHeroBitmap == null) {
				Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),
						R.drawable.new_indicator);
				try {
					mNewHeroBitmap = KidsUtils.getMaskedBitmap(mContext, bitmap);
				} catch (OutOfMemoryError e) {
					mNewHeroBitmap = bitmap;
				}
			}
			holder.newView.setImageBitmap(mNewHeroBitmap);
			holder.newView.setVisibility(View.VISIBLE);
		} else {
			holder.newView.setVisibility(View.GONE);
		}

		holder.favButton.setTag(cursor.getPosition()); // access quickly the hero once clicked.
		holder.actionButton.setTag(cursor.getPosition());

		if (inEditMode()) {
			if (hero.getState() == State.HIDDEN) {
				holder.actionButton.setImageResource(R.drawable.show_button_selector);
				holder.favButton.setVisibility(View.GONE);
			} else {
				if (hero.getState() == State.FAVORITE) {
					holder.favButton.setImageResource(R.drawable.unfavorite_button_selector);
				} else {
					holder.favButton.setImageResource(R.drawable.favorite_button_selector);
				}
				holder.actionButton.setImageResource(R.drawable.hide_button_selector);
				holder.favButton.setVisibility(View.VISIBLE);
			}
			holder.actionButton.setVisibility(View.VISIBLE);
		} else {
			holder.favButton.setVisibility(View.GONE);
			holder.actionButton.setVisibility(View.GONE);
		}

	}

	public boolean inEditMode() {
		return mEditMode;
	}

	public void setEditMode(boolean editMode) {
		mEditMode = editMode;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return mInflater.inflate(R.layout.hero_grid_item, null);
	}

	static class ViewHolder {
		ImageView thumbnail;
		ImageView newView;
		ImageView actionButton;
		ImageView favButton;
		TextView name;
	}
	
	@Override
	public void onClick(View v) {
		Cursor cursor = getCursor();
		cursor.moveToPosition((Integer) v.getTag());

		User hero = User.fromCursor(cursor);
		if (!hero.hasState())
			return;

		State newState = State.STANDARD;
		switch (v.getId()) {
		case R.id.hero_item_fav_b:
			if (hero.getState() != State.FAVORITE)
				newState = State.FAVORITE;
			break;
		case R.id.hero_item_hide_b:
			if (hero.getState() != State.HIDDEN)
				newState = State.HIDDEN;
			break;
		}

		hero.setState(newState);
		mContext.getContentResolver().update(
				ContentUris.withAppendedId(UsersProvider.CONTENT_URI, hero.getId()),
				hero.toContentValues(), null, null);
		notifyDataSetChanged();
	}

}
