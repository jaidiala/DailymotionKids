package com.orange.labs.dailymotion.kids.billing;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.android.vending.billing.IMarketBillingService;
import com.orange.labs.dailymotion.kids.utils.KidsLogger;

public class BillingService extends Service implements ServiceConnection{
	
	private static final String LOG_TAG = "BillingService";
	
	/** The service connection to the remote MarketBillingService. */
	private IMarketBillingService mService;
	
	@Override
	public void onCreate() {
		super.onCreate();
		KidsLogger.i(LOG_TAG, "Service starting with onCreate");
		
		try {
			boolean bindResult = bindService(new Intent("com.android.vending.billing.MarketBillingService.BIND"), this, Context.BIND_AUTO_CREATE);
			if(bindResult){
				KidsLogger.i(LOG_TAG,"Market Billing Service Successfully Bound");
			} else {
				KidsLogger.e(LOG_TAG,"Market Billing Service could not be bound.");
				//TODO stop user continuing
			}
		} catch (SecurityException e){
			KidsLogger.e(LOG_TAG,"Market Billing Service could not be bound. SecurityException: "+e);
			//TODO stop user continuing
		}
	}
	
	public void setContext(Context context) {
        attachBaseContext(context);
    }
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		KidsLogger.i(LOG_TAG, "Market Billing Service Connected.");
		mService = IMarketBillingService.Stub.asInterface(service);
		BillingHelper.instantiateHelper(getBaseContext(), mService);
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		unbindService(this);
	}

}