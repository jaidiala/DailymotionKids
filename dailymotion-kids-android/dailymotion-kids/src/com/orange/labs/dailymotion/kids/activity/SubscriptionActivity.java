package com.orange.labs.dailymotion.kids.activity;

import java.util.HashMap;
import java.util.Map;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.amazon.inapp.purchasing.PurchasingManager;
import com.dailymotion.kids.R;
import com.orange.labs.dailymotion.kids.billing.amazon.ButtonClickerObserver;
import com.orange.labs.dailymotion.kids.billing.amazon.DialogCommandWrapper;
import com.orange.labs.dailymotion.kids.config.ConfigurationManager;
import com.orange.labs.dailymotion.kids.dependency.DependencyResolver;
import com.orange.labs.dailymotion.kids.dependency.DependencyResolverImpl;
import com.orange.labs.dailymotion.kids.utils.KidsLogger;

public class SubscriptionActivity extends SherlockActivity {

	private static final String LOG_TAG = "Subscription Activity";

	private DependencyResolver mResolver;

	private Button mPurchaseButton;
	private Button mPurchaseButton_amazon;
	private TextView mPurchaseStatus;
	private Button mRestoreButton;
	private TextView mRestoreStatus;
	private Button mSeeSubscOnMarket;
	public static final String HAS_SUBSCRIPTION = "hasSubscription";
	private ConfigurationManager mConfiguration;
	public Map<String, String> requestIds;
	private boolean mRestoreAsked;
	private String currentUser;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.subscription_activity);

		final ActionBar actionBar = getSherlock().getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		mResolver = DependencyResolverImpl.getInstance();
		mConfiguration = mResolver.getConfiguration();
		requestIds = new HashMap<String, String>();
		mRestoreAsked = false;


		mPurchaseStatus = (TextView) findViewById(R.id.subsc_purchase_status_tv);

		mPurchaseButton_amazon = (Button) findViewById(R.id.subsc_purchase_bt_amazon);
		mPurchaseButton_amazon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i("DaylimotionKids", "purchase with amazon");
				if (mConfiguration.hasSubscribed()) {
					Toast.makeText(getApplicationContext(),
							getString(R.string.sub_purchased),
							Toast.LENGTH_LONG).show();
				} else {
					generateSubscribeDialog();
				}

			}
		});

		
	/*	 mRestoreStatus = (TextView) findViewById(R.id.subsc_market_tv);
		 mRestoreStatus.setVisibility(View.INVISIBLE);
		mSeeSubscOnMarket = (Button) findViewById(R.id.subsc_market_bt);
		mSeeSubscOnMarket.setVisibility(View.INVISIBLE);*/
		/*mSeeSubscOnMarket.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO open the google play market
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("market://details?id="
						+ getApplicationContext().getPackageName()));
				KidsLogger.d(LOG_TAG,
						"Can't purchase or restore on this device");
				startActivity(intent);
			}
		});
*/
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	protected void onStart() {	
		super.onStart();
		// ici on enregistre l'observateur amazon sur l'activity
		ButtonClickerObserver buttonClickerObserver = new ButtonClickerObserver(
				this);
		PurchasingManager.registerObserver(buttonClickerObserver);
	}




	// ******************************

	private void generateSubscribeDialog() {
		DialogCommandWrapper.createConfirmationDialog(this,
				getString(R.string.sub_message),
				"Subscribe", "No Thanks", new Runnable() {
					@Override
					public void run() {
		
						
						PurchasingManager
								.initiatePurchaseRequest(getResources()
										.getString(
												R.string.child_subscription_sku_monthly));
						Log.i("generateSubscribeDialog", "fin run");
					}
				}).show();
	}

	// *********************************

	@Override
	protected void onPause() {
		KidsLogger.d(LOG_TAG, "onPause())");
		super.onPause();
	}

	/**
	 * Responsible of getting the billing service running status
	 */
	private boolean isBillingServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
	
		}
		return false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		PurchasingManager.initiateGetUserIdRequest();
	};

	@Override
	protected void onStop() {
	
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	public String getCurrentUser() {
		return currentUser;
	}

	/**
	 * Sets current logged in user
	 * 
	 * @param currentUser
	 *            current user to set
	 */
	public void setCurrentUser(final String currentUser) {
		this.currentUser = currentUser;
	}
}
