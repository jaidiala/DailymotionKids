package com.orange.labs.dailymotion.kids.activity;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.dailymotion.kids.R;

public class AboutActivity extends SherlockActivity {

	private ListView mAboutListview;
	private ProgressBar mProgressBar;
	private WebView mAboutWebview;
	private ArrayList<HashMap<String, String>> mItems;
	private HashMap<String, String> mItem;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.about_activity);
		
		getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);

		final ActionBar actionBar = getSherlock().getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		mAboutListview = (ListView) findViewById(R.id.about_list);		
		mAboutWebview = (WebView) findViewById(R.id.about_webview);
		mProgressBar = (ProgressBar) findViewById(R.id.about_loading_pb);

		mItems = new ArrayList<HashMap<String, String>>();

		mItem = new HashMap<String, String>();
		mItem.put("title", getString(R.string.about_about_title));
		mItem.put("content",getString(R.string.about_about_content));
		mItems.add(mItem);

		mItem = new HashMap<String, String>();
		mItem.put("title", getString(R.string.about_faq_title));
		mItem.put("content",getString(R.string.about_faq_content));
		mItems.add(mItem);

		mItem = new HashMap<String, String>();
		mItem.put("title", getString(R.string.about_terms_title));
		mItem.put("content", getString(R.string.about_terms_content));
		mItems.add(mItem);

		mItem = new HashMap<String, String>();
		mItem.put("title", getString(R.string.about_contact_title));
		mItem.put("content", getString(R.string.about_contact_content));
		mItems.add(mItem);

		mItem = new HashMap<String, String>();
		mItem.put("title", getString(R.string.about_version_title));
		try {
			mItem.put(
					"content",
					getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			mItem.put("content", "Not found");
			e.printStackTrace();
		}
		mItems.add(mItem);

		SimpleAdapter adapter = new SimpleAdapter(this, mItems,
				R.layout.about_list_item, new String[] { "title", "content" },
				new int[] { R.id.about_list_item_title_tv,
						R.id.about_list_item_content_tv });

		mAboutListview.setAdapter(adapter);

		mAboutListview
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> a, View v,
							int position, long id) {
						if (position == 3) {
							Intent intent = new Intent(Intent.ACTION_SEND);
							intent.setType("message/rfc822");
							intent.putExtra(Intent.EXTRA_EMAIL, new String[] {getString(R.string.about_contact_email)});
							intent.putExtra(
									Intent.EXTRA_SUBJECT, getString(R.string.about_contact_subject));
							String messageText = "";
							try {
								messageText = "[AppVer-"
										+ getPackageManager().getPackageInfo(
												getPackageName(), 0).versionName
										+ "]"
										+ "[Android]"
										+ "["
										+ android.os.Build.MODEL
										+ "]"
										+ "["
										+ android.os.Build.VERSION.RELEASE
										+ "] "
										+ getString(R.string.about_contact_message);
							} catch (NameNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (NotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							intent.putExtra(Intent.EXTRA_TEXT, messageText);
							try {
								startActivity(Intent.createChooser(intent,
										"Mail"));
							} catch (android.content.ActivityNotFoundException ex) {
								Toast.makeText(
										getApplicationContext(),
										getResources().getString(
												R.string.about_webview_error),
										Toast.LENGTH_SHORT).show();
							}
						} else {
							if (position!=4) {
								findViewById(R.id.about_list).setVisibility(
										View.GONE);
								mAboutWebview.getSettings().setJavaScriptEnabled(true);
								mAboutWebview.setWebChromeClient(new WebChromeClient() {
									   public void onProgressChanged(WebView view, int progress) {
									     mProgressBar.setVisibility(View.VISIBLE);
									     if (progress==100) {
									    	 mProgressBar.setVisibility(View.GONE);	
									    	 mAboutWebview.setVisibility(View.VISIBLE);
									     }
									   }
									 });
							}
							if (position == 0) {
								mAboutWebview.loadUrl(getString(R.string.about_about_url));
								setTitle(getString(R.string.about_about_title));
							}
							if (position == 1) {
								mAboutWebview.loadUrl(getString(R.string.about_faq_url));
								setTitle(getString(R.string.about_faq_title));
							}
							if (position == 2) {
								mAboutWebview.loadUrl(getString(R.string.about_terms_url));
								setTitle(getString(R.string.about_terms_title));
							}
						}
					}
				});
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			if (mAboutWebview.getVisibility() == View.VISIBLE) {
				mAboutWebview.setVisibility(View.GONE);
				mAboutListview.setVisibility(View.VISIBLE);
				setTitle(getString(R.string.about_title));
			} else {
				Intent intent = new Intent(this, HeroesActivity.class);
				startActivity(intent);
			}
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}
	
	@Override
	public void onBackPressed() {
		if (mAboutWebview.getVisibility() == View.VISIBLE) {
			mAboutWebview.setVisibility(View.GONE);
			mAboutListview.setVisibility(View.VISIBLE);
			setTitle(getString(R.string.about_title));
		} else {
			super.onBackPressed();
		}
	}

}
