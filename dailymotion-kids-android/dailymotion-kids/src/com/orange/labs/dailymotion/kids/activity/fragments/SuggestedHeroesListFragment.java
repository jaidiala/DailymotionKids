package com.orange.labs.dailymotion.kids.activity.fragments;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.orange.labs.dailymotion.kids.activity.VideoPlayerActivity;
import com.orange.labs.dailymotion.kids.activity.adapters.HeroListAdapter;
import com.orange.labs.dailymotion.kids.user.User;
import com.orange.labs.dailymotion.kids.user.UserContract;
import com.orange.labs.dailymotion.kids.user.UsersProvider;
import com.orange.labs.dailymotion.kids.utils.KidsLogger;

public class SuggestedHeroesListFragment extends SherlockListFragment implements
		LoaderCallbacks<Cursor>, UpdatableFragment {

	private static final int USER_LOADER = 0;

	private CursorAdapter mListAdapter;

	/**
	 * If the user selects a suggested hero, we need to store a reference as we need to provision
	 * its id when reloading the video player.
	 */
	private Bundle mUpdatedArgs;
	private OnHeroSelected mListener;

	private boolean mNeedsReloading = true;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnHeroSelected) activity;
		} catch (ClassCastException e) {
			// no listener
			mListener = null;
		}
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mListAdapter = new HeroListAdapter(getActivity(), null,
				CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
		setListAdapter(mListAdapter);
		setListShown(false);
		reloadIfNeededAndPossible();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		reloadIfNeededAndPossible();
	}

	private void reloadIfNeededAndPossible() {
		if (mNeedsReloading && isAdded())
			reloadFragment();
	}
	
	private void reloadFragment() {
		Bundle args = (mUpdatedArgs != null) ? mUpdatedArgs : getArguments();
		KidsLogger.v(
				"Suggested Heroes Fragment",
				"Processing updated arguments. Loading description of :"
						+ args.getString(VideoPlayerActivity.USER_ID));
		if (args != null && args.getString(VideoPlayerActivity.USER_ID) != null) {
			getLoaderManager().restartLoader(USER_LOADER, args, this);
		}
		mNeedsReloading = false;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		switch (id) {
		case USER_LOADER:
			return new CursorLoader(getSherlockActivity(), Uri.parse(UsersProvider.SUGGESTION_URI
					+ "/" + bundle.getString(VideoPlayerActivity.USER_ID)), null, null, null,
					UserContract.SCREEN_NAME + " ASC");
		}

		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		switch (loader.getId()) {
		case USER_LOADER:
			mListAdapter.swapCursor(cursor);
			if (isResumed()) {
				setListShown(true);
			} else {
				setListShownNoAnimation(true);
			}
			setSelection(0);
			break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (loader.getId() == USER_LOADER) {
			mListAdapter.swapCursor(null);
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		KidsLogger.v(getTag(), "List Item Click : " + position);
		User selectedHero = User.fromCursor((Cursor) mListAdapter.getItem(position));
		if (selectedHero != null) {
			if (mListener != null)
				mListener.onHeroSelected(selectedHero);
			
			Bundle bundle = new Bundle();
			bundle.putString(VideoPlayerActivity.USER_ID, selectedHero.getDailymotionId());
			updateFragment(bundle);
		}
	}

	@Override
	public void updateFragment(Bundle args) {
		mUpdatedArgs = args;
		mNeedsReloading  = true;
		reloadIfNeededAndPossible();
	}
	
	public interface OnHeroSelected {
		void onHeroSelected(User hero);
	}
}
