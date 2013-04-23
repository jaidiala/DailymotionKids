package com.orange.labs.dailymotion.kids.activity.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

import com.orange.labs.dailymotion.kids.activity.fragments.UpdatableFragment;

/**
 * Allows to use a {@link TabHost} in combination with a {@link ViewPager}. Each components updates
 * the other when a changes occur. A new tab can be added using the addTab method, providing a
 * {@link TabSpec}. Tabs are displayed in order of insertion.
 */
public class PagerTabManager implements TabHost.OnTabChangeListener, OnPageChangeListener {

	private final FragmentActivity mActivity;
	private final TabHost mTabHost;
	private final HashMap<String, TabInfo> mTabs = new HashMap<String, TabInfo>();
	private final ViewPager mViewPager;
	private final PagerAdapter mViewPagerAdapter;

	static final class TabInfo {
		private final String tag;
		private final Class<?> clss;

		TabInfo(String _tag, Class<?> _class) {
			tag = _tag;
			clss = _class;
		}
	}

	static class DummyTabFactory implements TabHost.TabContentFactory {
		private final Context mContext;

		public DummyTabFactory(Context context) {
			mContext = context;
		}

		@Override
		public View createTabContent(String tag) {
			View v = new View(mContext);
			v.setMinimumWidth(0);
			v.setMinimumHeight(0);
			return v;
		}
	}

	/**
	 * Simple Pager Adapter that contains a {@link List} of {@link Fragment}.
	 */
	static class PagerAdapter extends FragmentPagerAdapter {

		private final ArrayList<Fragment> mFragments = new ArrayList<Fragment>();

		public PagerAdapter(FragmentManager manager) {
			super(manager);
		}

		@Override
		public Fragment getItem(int position) {
			return mFragments.get(position);
		}

		@Override
		public int getCount() {
			return mFragments.size();
		}

		public void addFragment(final Fragment fragment) {
			mFragments.add(fragment);
		}
	}

	public PagerTabManager(FragmentActivity activity, TabHost tabHost, ViewPager viewPager) {
		mActivity = activity;
		mTabHost = tabHost;
		mViewPagerAdapter = new PagerAdapter(activity.getSupportFragmentManager());
		mViewPager = viewPager;
		mViewPager.setOnPageChangeListener(this);
		mViewPager.setAdapter(mViewPagerAdapter);
		mTabHost.setOnTabChangedListener(this);
	}

	public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {
		tabSpec.setContent(new DummyTabFactory(mActivity));
		String tag = tabSpec.getTag();
		TabInfo info = new TabInfo(tag, clss);

		mViewPagerAdapter.addFragment(Fragment.instantiate(mActivity, info.clss.getName(), args));
		mTabs.put(info.tag, info);
		mTabHost.addTab(tabSpec);
	}

	@Override
	public void onTabChanged(String tabId) {
		int position = mTabHost.getCurrentTab();
		mViewPager.setCurrentItem(position);
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
	}

	@Override
	public void onPageSelected(int position) {
		mTabHost.setCurrentTab(position);
	}
	
	public Fragment getFragmentAt(int position) {
		return mViewPagerAdapter.getItem(position);
	}

	public void updateTabs(Bundle newArgs) {
		for (int i = 0; i < mViewPagerAdapter.getCount(); i++) {
			try {
				((UpdatableFragment)mViewPagerAdapter.getItem(i)).updateFragment(newArgs);
			} catch (ClassCastException e) {
				// Ignoring that fragment, not updatable.
			}
		}
	}
}
