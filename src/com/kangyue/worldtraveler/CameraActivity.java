package com.kangyue.worldtraveler;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TableRow.LayoutParams;
import android.widget.Toast;

import com.Kangyue.worldtraveler.R;
import com.kangyue.worldtraveler.filter.ar.ImageDetectionFilter;


//Use the deprecated Camera class.

public class CameraActivity extends Activity  implements CvCameraViewListener2 {

	// FlagDraw
		private boolean flagDraw =false;
	
	// A tag for log output.
	private static String tag = CameraActivity.class.getSimpleName();

	// A key for storing the index of the active camera.
	private static final String STATE_CAMERA_INDEX = "cameraIndex";

	// A key for storing the index of the active image size.
	private static final String STATE_IMAGE_SIZE_INDEX = "imageSizeIndex";

	// Keys for storing the indices of the active filters.
	private static final String STATE_IMAGE_DETECTION_FILTER_INDEX = "imageDetectionFilterIndex";

	// The filters.
	private ImageDetectionFilter[] mImageDetectionFilters;
	
	// The indices of the active filters.
	private int mImageDetectionFilterIndex;

	// The index of the active camera.
	private int mCameraIndex;

	// The index of the active image size.
	private int mImgSizeIndex;

	// Whather the active camera is front-facing.
	// If so, the camera view should be mirroed.
	private boolean mIsCameraFrontFacing;


	// The image sizes supported by the active camera.
	private List<Size> mSupportedImageSizes;

	// The camera view.
	private CameraBridgeViewBase mCameraView;
	
	private int mDesiredCameraPreviewWidth = 320;




	// The OpenCV loader callback.
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:
				Log.d(tag, "OpenCV loaded successfully");
				mCameraView.enableView();
				mCameraView.enableFpsMeter();
				
				
				
				
				

				final ImageDetectionFilter starryNight;
				try {
					starryNight = new ImageDetectionFilter(CameraActivity.this,
							R.drawable.starry_night);
				} catch (IOException e) {
					Log.e(tag, "Failed to load drawable: " + "starry_night");
					e.printStackTrace();
					break;
				}

				final ImageDetectionFilter akbarHunting;
				try {
					akbarHunting = new ImageDetectionFilter(
							CameraActivity.this,
							R.drawable.akbar_hunting_with_cheetahs);
				} catch (IOException e) {
					Log.e(tag, "Failed to load drawable: "
							+ "akbar_hunting_with_cheetahs");
					e.printStackTrace();
					break;
				}

				mImageDetectionFilters = new ImageDetectionFilter[] {starryNight, akbarHunting };
				
				break;

			default:
				super.onManagerConnected(status);
				break;
			}
		}
	};

	// Support back incompatibility errors because we provide
	// backward-compatible fallbacks.
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		if (savedInstanceState != null) {
			mCameraIndex = savedInstanceState.getInt(STATE_CAMERA_INDEX, 0);
			mImageDetectionFilterIndex = savedInstanceState.getInt(
					STATE_IMAGE_DETECTION_FILTER_INDEX, 0);
			mImgSizeIndex = savedInstanceState
					.getInt(STATE_IMAGE_SIZE_INDEX, 0);
			
		} else {
			mCameraIndex = 0;
			mImgSizeIndex = 0;
			mImageDetectionFilterIndex = 0;
			
		}

		final Camera camera;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			CameraInfo cameraInfo = new CameraInfo();
			Camera.getCameraInfo(mCameraIndex, cameraInfo);

			mIsCameraFrontFacing = false;
			
			camera = Camera.open(mCameraIndex);
		} else { // pre-Gingerbread
					// Assume that there is only 1 camera and it is rear-facing.
			mIsCameraFrontFacing = false;
			
			camera = Camera.open();

		}

		final Parameters parameters = camera.getParameters();
		camera.release();
		mSupportedImageSizes = parameters.getSupportedPreviewSizes();

		int currentWidth = 0;
		int currentHeight = 0;
		boolean foundDesiredWidth = false;
		
		for (Camera.Size s : mSupportedImageSizes) {
			if (s.width == mDesiredCameraPreviewWidth) {
				currentWidth = s.width;
				currentHeight = s.height;
				foundDesiredWidth = true;
				break;
			}
		}
		if (foundDesiredWidth) {
			parameters.setPreviewSize(currentWidth, currentHeight);
		}

//		DisplayMetrics deafultDm = new DisplayMetrics();
//		DisplayMetrics dm = new DisplayMetrics();
//		getWindowManager().getDefaultDisplay().getMetrics(deafultDm);
//		dm.widthPixels = currentWidth;
//		dm.heightPixels = currentHeight;
//	
//		deafultDm.setTo(dm);
		
		
		
		
		mCameraView = new JavaCameraView(this, mCameraIndex);
		mCameraView.setMaxFrameSize(currentWidth,currentHeight);
		mCameraView.setCvCameraViewListener(this);
		
	    setContentView(mCameraView);
		
		

	}

	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
		// Save the current camera index.
		savedInstanceState.putInt(STATE_CAMERA_INDEX, mCameraIndex);

		// Save the current image size index.
		savedInstanceState.putInt(STATE_IMAGE_SIZE_INDEX, mImgSizeIndex);

		// Save the current filter indices.
		savedInstanceState.putInt(STATE_IMAGE_DETECTION_FILTER_INDEX,
				mImageDetectionFilterIndex);
		

		super.onSaveInstanceState(savedInstanceState);
	}

	// Suppress backward incompatibility errors because we provide
	// backward-compatible fallbacks.

	public void recreate() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			super.recreate();
		} else {
			finish();
			startActivity(getIntent());
		}
	};

	public void onPause() {
		if (mCameraView != null) {
			mCameraView.disableView();
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!OpenCVLoader.initDebug()) {
            Log.d(tag, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(tag, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
		
	}

	@Override
	protected void onDestroy() {
		if (mCameraView != null) {
			mCameraView.disableView();
		}
		super.onDestroy();
	}
	
	@Override
	public void onCameraViewStarted(int width, int height) {

	}

	@Override
	public void onCameraViewStopped() {
		// TODO Auto-generated method stub

	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		final Mat rgba = inputFrame.rgba();

		// Apply the active filters.
		
			mImageDetectionFilters[0].apply(rgba, rgba);
		
			
			flagDraw = mImageDetectionFilters[0].getFlagDraw();
			Log.d(tag, "flagDraw : "+flagDraw);
			

		
		return rgba;
	}
	
	
}
