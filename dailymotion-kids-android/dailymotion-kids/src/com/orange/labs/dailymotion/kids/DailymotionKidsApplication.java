package com.orange.labs.dailymotion.kids;

import android.app.Application;

import com.dailymotion.kids.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.orange.labs.dailymotion.kids.dependency.DependencyResolverImpl;
import com.orange.labs.dailymotion.kids.utils.MaskedDisplayer;

/**
 * Extension of the default Android {@link Application} class to allow the initialization of the
 * Dependency Resolver before the rest of the application is set up.
 * 
 * @author Jean-Francois Moy
 * 
 */
public class DailymotionKidsApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		// Simple initialization of the Dependency Resolver.
		DependencyResolverImpl.initialize(getApplicationContext());

		// Configure ImageLoader once and for all.
		setupImageLoader();
	}

	private void setupImageLoader() {
		DisplayImageOptions imageOptions = new DisplayImageOptions.Builder()
				.showStubImage(R.drawable.thumbnail_default)
				.showImageForEmptyUri(R.drawable.thumbnail_default).cacheInMemory().cacheInMemory()
				.displayer(new MaskedDisplayer(getApplicationContext())).build();
		// Initialization of the ImageLoader using app context.
		ImageLoaderConfiguration loaderConfig = new ImageLoaderConfiguration.Builder(
				getApplicationContext()).defaultDisplayImageOptions(imageOptions).build();
		ImageLoader imageLoader = ImageLoader.getInstance();
		imageLoader.init(loaderConfig);
	}
}
