package com.orange.labs.dailymotion.kids.billing;

import com.orange.labs.dailymotion.kids.utils.KidsLogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static com.orange.labs.dailymotion.kids.billing.BillingConsts.*;

public class BillingReceiver extends BroadcastReceiver {

	private static final String LOG_TAG = "BillingReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		KidsLogger.i(LOG_TAG, "Received action: " + action);
        if (ACTION_PURCHASE_STATE_CHANGED.equals(action)) {
            String signedData = intent.getStringExtra(INAPP_SIGNED_DATA);
            String signature = intent.getStringExtra(INAPP_SIGNATURE);
            purchaseStateChanged(context, signedData, signature);
        } else if (ACTION_NOTIFY.equals(action)) {
            String notifyId = intent.getStringExtra(NOTIFICATION_ID);
            notify(context, notifyId);
        } else if (ACTION_RESPONSE_CODE.equals(action)) {
            long requestId = intent.getLongExtra(INAPP_REQUEST_ID, -1);
            int responseCodeIndex = intent.getIntExtra(INAPP_RESPONSE_CODE, BillingConsts.ResponseCode.RESULT_ERROR.ordinal());
            checkResponseCode(context, requestId, responseCodeIndex);
        } else {
           KidsLogger.e(LOG_TAG, "unexpected action: " + action);
        }
	}


	private void purchaseStateChanged(Context context, String signedData, String signature) {
		KidsLogger.i(LOG_TAG, "purchaseStateChanged got signedData: " + signedData);
		KidsLogger.i(LOG_TAG, "purchaseStateChanged got signature: " + signature);
		BillingHelper.verifyPurchase(signedData, signature);
	}
	
	private void notify(Context context, String notifyId) {
		KidsLogger.i(LOG_TAG, "notify got id: " + notifyId);
		String[] notifyIds = {notifyId};
		BillingHelper.getPurchaseInformation(notifyIds);
	}
	
	private void checkResponseCode(Context context, long requestId, int responseCodeIndex) {
		KidsLogger.i(LOG_TAG, "checkResponseCode got requestId: " + requestId);
		KidsLogger.i(LOG_TAG, "checkResponseCode got responseCode: " + BillingConsts.ResponseCode.valueOf(responseCodeIndex));
	}
}