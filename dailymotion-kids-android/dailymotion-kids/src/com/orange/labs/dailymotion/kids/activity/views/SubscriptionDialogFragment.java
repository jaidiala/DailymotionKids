package com.orange.labs.dailymotion.kids.activity.views;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.dailymotion.kids.R;
import com.orange.labs.dailymotion.kids.activity.VideoPlayerActivity;

public class SubscriptionDialogFragment extends DialogFragment {

	public static SubscriptionDialogFragment newInstance() {
		return new SubscriptionDialogFragment();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new AlertDialog.Builder(getActivity())
				.setTitle(R.string.subscription_dialog_title)
				.setMessage(R.string.subscription_dialog_message)
				.setPositiveButton(R.string.subscription_dialog_subscribe, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						((VideoPlayerActivity) getActivity()).subscribe();
					}
				})
				.setNegativeButton(R.string.subscription_dialog_cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								((VideoPlayerActivity) getActivity()).cancelDialog();
							}
						}).create();
	}
}
