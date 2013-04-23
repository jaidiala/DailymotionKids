package com.orange.labs.dailymotion.kids.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

import com.dailymotion.kids.R;

public final class KidsUtils {

	public static Bitmap getMaskedBitmap(Context context, Bitmap input) {
		Bitmap mask = BitmapFactory.decodeResource(context.getResources(),
				R.drawable.hero_item_layout_background);
		Bitmap output = getMaskedBitmap(mask, input);
		return output;
	}

	public static Bitmap getMaskedBitmap(Bitmap mask, Bitmap input) {
		Bitmap scaledMask = Bitmap.createScaledBitmap(mask, input.getWidth(), input.getHeight(),
				true);
		Bitmap output = Bitmap.createBitmap(input.getWidth(), input.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		final Rect rect = new Rect(0, 0, input.getWidth(), input.getHeight());
		final Rect rectF = new Rect(rect);

		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(0xFFFFFFFF);

		canvas.drawBitmap(input, rect, rect, paint);
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
		canvas.drawBitmap(scaledMask, rectF, rectF, paint);

		return output;
	}

}
