package com.orange.labs.dailymotion.kids.billing.amazon;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.amazon.inapp.purchasing.BasePurchasingObserver;
import com.amazon.inapp.purchasing.GetUserIdResponse;
import com.amazon.inapp.purchasing.GetUserIdResponse.GetUserIdRequestStatus;
import com.amazon.inapp.purchasing.Item;
import com.amazon.inapp.purchasing.ItemDataResponse;
import com.amazon.inapp.purchasing.Offset;
import com.amazon.inapp.purchasing.PurchaseUpdatesResponse;
import com.amazon.inapp.purchasing.PurchasingManager;
import com.amazon.inapp.purchasing.Receipt;
import com.amazon.inapp.purchasing.SubscriptionPeriod;
import com.orange.labs.dailymotion.kids.activity.HeroesActivity;
import com.orange.labs.dailymotion.kids.config.ConfigurationManager;
import com.orange.labs.dailymotion.kids.dependency.DependencyResolver;
import com.orange.labs.dailymotion.kids.dependency.DependencyResolverImpl;

public class UpdateObserver extends BasePurchasingObserver {

	private static final String OFFSET = "offset";
	private static final String START_DATE = "startDate";
	private static final String TAG = "Amazon-UpdateObserver";
	private final HeroesActivity baseActivity;
	private DependencyResolver mResolver;
	private ConfigurationManager mConfiguration;


	/**
	 * Creates new instance of the ButtonClickerObserver class.
	 * 
	 * @param buttonClickerActivity
	 *            Activity context
	 */
	public UpdateObserver(final HeroesActivity heroesActivity) {
		super(heroesActivity);
		mResolver = DependencyResolverImpl.getInstance();
		mConfiguration = mResolver.getConfiguration();
		this.baseActivity = heroesActivity;
		Log.i("ButtonClickerObserver", "created");
		
	}

	/**
	 * Invoked once the observer is registered with the Puchasing Manager If the
	 * boolean is false, the application is receiving responses from the SDK
	 * Tester. If the boolean is true, the application is live in production.
	 * 
	 * @param isSandboxMode
	 *            Boolean value that shows if the app is live or not.
	 */
	@Override
	public void onSdkAvailable(final boolean isSandboxMode) {
		Log.v(TAG, "onSdkAvailable recieved: Response -" + isSandboxMode);
		PurchasingManager.initiateGetUserIdRequest();
		Log.i("onSdkAvailable", "FINISHED");
	}

	/**
	 * Invoked once the call from initiateGetUserIdRequest is completed. On a
	 * successful response, a response object is passed which contains the
	 * request id, request status, and the userid generated for your
	 * application.
	 * 
	 * @param getUserIdResponse
	 *            Response object containing the UserID
	 */
	@Override
	public void onGetUserIdResponse(final GetUserIdResponse getUserIdResponse) {
		Log.v(TAG, "onGetUserIdResponse recieved: Response -"
				+ getUserIdResponse);
		Log.v(TAG, "RequestId:" + getUserIdResponse.getRequestId());
		Log.v(TAG,
				"IdRequestStatus:" + getUserIdResponse.getUserIdRequestStatus());
		Log.v(TAG, "current user: " + baseActivity.getCurrentUser());
		new GetUserIdAsyncTask().execute(getUserIdResponse);
	}

	/**
	 * Invoked once the call from initiateItemDataRequest is completed. On a
	 * successful response, a response object is passed which contains the
	 * request id, request status, and a set of item data for the requested
	 * skus. Items that have been suppressed or are unavailable will be returned
	 * in a set of unavailable skus.
	 * 
	 * @param itemDataResponse
	 *            Response object containing a set of
	 *            purchasable/non-purchasable items
	 */
	@Override
	public void onItemDataResponse(final ItemDataResponse itemDataResponse) {
		Log.v(TAG, "onItemDataResponse recieved");
		Log.v(TAG,
				"ItemDataRequestStatus"
						+ itemDataResponse.getItemDataRequestStatus());
		Log.v(TAG, "ItemDataRequestId" + itemDataResponse.getRequestId());
		new ItemDataAsyncTask().execute(itemDataResponse);
	}

	@Override
	public void onPurchaseUpdatesResponse(
			final PurchaseUpdatesResponse purchaseUpdatesResponse) {
		Log.v(TAG, "onPurchaseUpdatesRecived recieved: Response -"
				+ purchaseUpdatesResponse);
		Log.v(TAG,
				"PurchaseUpdatesRequestStatus:"
						+ purchaseUpdatesResponse
								.getPurchaseUpdatesRequestStatus());
		Log.v(TAG, "RequestID:" + purchaseUpdatesResponse.getRequestId());

		new PurchaseUpdatesAsyncTask().execute(purchaseUpdatesResponse);
	}

	/*
	 * Helper method to print out relevant receipt information to the log.
	 */
	private void printReceipt(final Receipt receipt) {
		Log.v(TAG, String.format(
				"Receipt: ItemType: %s Sku: %s SubscriptionPeriod: %s",
				receipt.getItemType(), receipt.getSku(),
				receipt.getSubscriptionPeriod()));
	}

	/*
	 * Helper method to retrieve the correct key to use with our shared
	 * preferences
	 */
	private String getKey(final String sku) {

		return "sub_amazon";
	}

	private SharedPreferences getSharedPreferencesForCurrentUser() {
		final SharedPreferences settings = baseActivity.getSharedPreferences(
				baseActivity.getCurrentUser(), Context.MODE_PRIVATE);
		return settings;
	}

	private SharedPreferences.Editor getSharedPreferencesEditor() {
		return getSharedPreferencesForCurrentUser().edit();
	}

	/*
	 * Started when the Observer receives a GetUserIdResponse. The Shared
	 * Preferences file for the returned user id is accessed.
	 */
	private class GetUserIdAsyncTask extends
			AsyncTask<GetUserIdResponse, Void, Boolean> {

		@Override
		protected Boolean doInBackground(final GetUserIdResponse... params) {
			GetUserIdResponse getUserIdResponse = params[0];
			if (getUserIdResponse.getUserIdRequestStatus() == GetUserIdRequestStatus.SUCCESSFUL) {
				final String userId = getUserIdResponse.getUserId();

				// Each UserID has their own shared preferences file, and we'll
				// load that file when a new user logs in.
				baseActivity.setCurrentUser(userId);
				/*
				 * PurchasingManager.initiatePurchaseUpdatesRequest(Offset.
				 * fromString(baseActivity.getApplicationContext()
				 * .getSharedPreferences(baseActivity.getCurrentUser(),
				 * Context.MODE_PRIVATE) .getString(OFFSET,
				 * Offset.BEGINNING.toString())));
				 */
				PurchasingManager
						.initiatePurchaseUpdatesRequest(Offset.BEGINNING);
				Log.i("GetUserIdAsyncTask",
						"getUserIdResponse : " + params[0].toString());
				Log.i("GetUserIdAsyncTask",
						"getCurrentUser : " + baseActivity.getCurrentUser());
				return true;
			} else {
				Log.v(TAG, "onGetUserIdResponse: Unable to get user ID.");
				Log.i("GetUserIdAsyncTask",
						"getUserIdResponse : " + params[0].toString());
				Log.i("GetUserIdAsyncTask",
						"getCurrentUser : " + baseActivity.getCurrentUser());
				return false;
			}
		}

		/*
		 * Call initiatePurchaseUpdatesRequest for the returned user to sync
		 * purchases that are not yet fulfilled.
		 */
		@Override
		protected void onPostExecute(final Boolean result) {
			super.onPostExecute(result);
			if (result) {

			}
		}
	}

	private class ItemDataAsyncTask extends
			AsyncTask<ItemDataResponse, Void, Void> {
		@Override
		protected Void doInBackground(final ItemDataResponse... params) {
			final ItemDataResponse itemDataResponse = params[0];

			switch (itemDataResponse.getItemDataRequestStatus()) {
			case SUCCESSFUL_WITH_UNAVAILABLE_SKUS:
				// Skus that you can not purchase will be here.
				for (final String s : itemDataResponse.getUnavailableSkus()) {
					Log.v(TAG, "Unavailable SKU:" + s);
				}
			case SUCCESSFUL:
				// Information you'll want to display about your IAP items is
				// here
				// In this example we'll simply log them.
				final Map<String, Item> items = itemDataResponse.getItemData();
				for (final String key : items.keySet()) {
					Item i = items.get(key);
					Log.v(TAG,
							String.format(
									"Item: %s\n Type: %s\n SKU: %s\n Price: %s\n Description: %s\n",
									i.getTitle(), i.getItemType(), i.getSku(),
									i.getPrice(), i.getDescription()));
				}
				break;
			case FAILED:
				// On failed responses will fail gracefully.
				break;

			}

			return null;
		}
	}

	private class PurchaseUpdatesAsyncTask extends
			AsyncTask<PurchaseUpdatesResponse, Void, Boolean> {

		@Override
		protected Boolean doInBackground(
				final PurchaseUpdatesResponse... params) {
		
		
			final PurchaseUpdatesResponse purchaseUpdatesResponse = params[0];
			final SharedPreferences.Editor editor = getSharedPreferencesEditor();
			final String userId = baseActivity.getCurrentUser();
			if (!purchaseUpdatesResponse.getUserId().equals(userId)) {
				Log.e("*HEROS*" + TAG, "not equal users");
				mConfiguration.setHasSubscribed(false);
				mConfiguration.setHasSubscribedOnAmazon(false);
				return false;
			}
			/*
			 * If the customer for some reason had items revoked, the skus for
			 * these items will be contained in the revoked skus set.
			 */
			for (final String sku : purchaseUpdatesResponse.getRevokedSkus()) {
				Log.v(TAG, "Revoked Sku:" + sku);
				final String key = getKey(sku);
				editor.putBoolean(key, false);
				editor.commit();
			}

			switch (purchaseUpdatesResponse.getPurchaseUpdatesRequestStatus()) {
			case SUCCESSFUL:
				Log.i("**" + TAG, "SUCCESSFUL");
				if (purchaseUpdatesResponse.getReceipts().isEmpty()) {
					Log.i("**" + TAG, "Receip Empty");
					mConfiguration.setHasSubscribed(false);
					mConfiguration.setHasSubscribedOnAmazon(false);
				} 
				  for (final Receipt receipt : purchaseUpdatesResponse.getReceipts()) {
					  if(receipt.getSubscriptionPeriod().getEndDate()!=null){
					
					Log.i("**" + TAG, "subscription end");
					mConfiguration.setHasSubscribed(false);
					mConfiguration.setHasSubscribedOnAmazon(false);
					
				}else{
					Log.i("**" + TAG, "subscription found");
					mConfiguration.setHasSubscribed(true);
					mConfiguration.setHasSubscribedOnAmazon(true);
				}
					
			
					  return true;
				
				  }
			

				return true;
			case FAILED:
				Log.i("**" + TAG, "Reception Failed");
				mConfiguration.setHasSubscribed(false);
				mConfiguration.setHasSubscribedOnAmazon(false);
				return false;
			}
			return false;
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			super.onPostExecute(success);
			if (success) {

			}
		}
	}
}
