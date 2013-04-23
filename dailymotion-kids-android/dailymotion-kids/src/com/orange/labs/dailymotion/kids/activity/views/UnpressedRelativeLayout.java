package com.orange.labs.dailymotion.kids.activity.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class UnpressedRelativeLayout extends RelativeLayout {
	
	public UnpressedRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public UnpressedRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public UnpressedRelativeLayout(Context context) {
		super(context);
	}

	@Override
	public void setPressed(boolean pressed) {
		// do nothing.
	}
}
