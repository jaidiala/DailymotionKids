package com.orange.labs.dailymotion.kids.activity.asynctasks;

import java.util.concurrent.CountDownLatch;

import org.json.JSONArray;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;

import com.orange.labs.dailymotion.kids.dependency.DependencyResolverImpl;
import com.orangelabs.dailymotion.DailyException;
import com.orangelabs.dailymotion.Dailymotion;
import com.orangelabs.dailymotion.DailymotionRequestor;
import com.orangelabs.dailymotion.DailymotionRequestor.DailymotionRequestListener;

public class MembershipCheckingTask extends AsyncTask<String, Void, Boolean> {

	private static final String OFFER_ENDPOINT = "user/me/offers/kids-plus_fr";

	@Override
	protected Boolean doInBackground(String... params) {
		String userToken = params[0];
		if (TextUtils.isEmpty(userToken)) {
			return false;
		}

		final Bundle args = new Bundle();
		args.putString("access_token", userToken);

		final Dailymotion dailymotion = DependencyResolverImpl.getInstance().getDailymotion();
		final DailymotionRequestor requestor = new DailymotionRequestor(dailymotion);
		final SyncCallback callback = new SyncCallback();
		requestor.anonymousRequest(OFFER_ENDPOINT, args, callback);
		return callback.waitForResult();
	}

	private class SyncCallback implements DailymotionRequestListener {

		private boolean mRegistered = false;

		private final CountDownLatch mIsComplete = new CountDownLatch(1);

		@Override
		public void onSuccess(JSONObject response) {
			JSONArray list = response.optJSONArray("list");
			mRegistered = (list.length() > 0);
			mIsComplete.countDown();
		}

		@Override
		public void onFailure(DailyException e) {
			mIsComplete.countDown();
			//TODO: Add failure (in case of communication)
		}

		public boolean waitForResult() {
			try {
				mIsComplete.await();
			} catch (InterruptedException e) {
				// Restore interrupt status and fall through.
				Thread.currentThread().interrupt();
			}
			return mRegistered;
		}
	}

}
