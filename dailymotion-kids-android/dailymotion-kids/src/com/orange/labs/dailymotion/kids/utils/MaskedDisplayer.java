package com.orange.labs.dailymotion.kids.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.display.BitmapDisplayer;
import com.dailymotion.kids.R;

public class MaskedDisplayer implements BitmapDisplayer {

	private final Bitmap mImageMask;

	public MaskedDisplayer(final Context _context) {
		mImageMask = BitmapFactory.decodeResource(_context.getResources(),
				R.drawable.hero_item_layout_background);
	}

	@Override
	public Bitmap display(Bitmap bitmap, ImageView imageView) {
		Bitmap maskedBitmap;
		try {
			maskedBitmap = KidsUtils.getMaskedBitmap(mImageMask, bitmap);
		} catch (OutOfMemoryError e) {
			maskedBitmap = bitmap;
		}
		imageView.setImageBitmap(maskedBitmap);
		return maskedBitmap;
	}
}