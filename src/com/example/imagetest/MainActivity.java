package com.example.imagetest;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import java.lang.Math;

public class MainActivity extends Activity implements OnTouchListener {

	private ImageView imgView;

	/** Hold a reference to our GLSurfaceView */
	private GLSurfaceView mGLView;
	private GLSurfaceView mSurfaceView;

	// these matrices will be used to move and zoom image
	private Matrix matrix = new Matrix();
	private Matrix savedMatrix = new Matrix();
	private Matrix resMatrix = new Matrix();

	// we can be in one of these 3 states
	private static final int NONE = 0;
	private static final int DRAG = 1;
	private static final int ZOOM = 2;
	private int mode = NONE;
	private float[] eventMatrix = new float[9];
	private float[] lastEvent = null;
	private float oldDist = 1f;
	private float d = 0f;
	private float newRot = 0f;
	float mCurrentScale = 1.0f;
	private PointF start = new PointF();
	private PointF mid = new PointF();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Create a GLSurfaceView instance and set it
		// as the ContentView for this Activity
		// mGLView = new MyGLSurfaceView(this);
		// setContentView(mGLView);
		setContentView(R.layout.activity_two);

		// float rotation = 60;
		// float scaleX = 2;
		// float scaleY = 2;
		imgView = (ImageView) findViewById(R.id.imageView1);
		// imgView.setRotationX();
		// imgView.setRotationY();
		// imgView.setRotation(rotation);
		/*
		 * imgView.setPivotX(0); imgView.setPivotY(0); imgView.setScaleX(scaleX);
		 * imgView.setScaleY(scaleY);
		 */
		imgView.setOnTouchListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		// The following call pauses the rendering thread.
		// If your OpenGL application is memory intensive,
		// you should consider de-allocating objects that
		// consume significant memory here.
		// mGLView.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// The following call resumes a paused rendering thread.
		// If you de-allocated graphic objects for onPause()
		// this is a good place to re-allocate them.
		// mGLView.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public boolean onTouch(View v, MotionEvent event) {
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		// metrics.heightPixels;
		// metrics.widthPixels;
		// handle touch events here
		ImageView view = (ImageView) v;
		// MotionEvent.ACTION_MASK
		Drawable drawable = getResources().getDrawable(R.drawable.cc_1);

		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			System.out.println("First press down");
			savedMatrix.set(matrix);
			start.set(event.getX(), event.getY());
			// System.out.println("X is " + event.getX() + " Y is " + event.getY());
			mode = DRAG;
			// System.out.println("BEFORE: " + view.getImageMatrix().toString());
			// lastEvent = null;
			System.out.println("VIEW DIM: " + view.getBottom() + "x"
					+ view.getRight());

			System.out.println("IMG DIM: " + metrics.heightPixels + "x"
					+ metrics.widthPixels);

			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			System.out.println("Subsequent presses");
			oldDist = spacing(event);
			if (oldDist > 10f) {
				savedMatrix.set(matrix);
				midPoint(mid, event);
				mode = ZOOM;
			}
			// IMAGE is 1293x755 NOTE ALL SHOULD BE THE SAME SIZE
			lastEvent = new float[4];
			lastEvent[0] = event.getX(0);
			lastEvent[1] = event.getX(1);
			lastEvent[2] = event.getY(0);
			lastEvent[3] = event.getY(1);
			d = rotation(event);
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			System.out.println("PRESSES ENDED");
			switch (mode) {
			case DRAG:
				view.getImageMatrix().getValues(eventMatrix);
				System.out.println("AFTER: " + view.getImageMatrix().toString());
				if (eventMatrix[2] > 0) {
					eventMatrix[2] = 0;
					// view.setImageMatrix(resMatrix);
				}
				else if (eventMatrix[2] < view.getWidth()
						- drawable.getIntrinsicWidth()) {
					eventMatrix[2] = view.getWidth() - drawable.getIntrinsicWidth();
				}
				if (eventMatrix[5] > 0) {
					eventMatrix[5] = 0;
				}
				else if (eventMatrix[5] < view.getHeight()
						- drawable.getIntrinsicHeight()) {
					eventMatrix[5] = view.getHeight() - drawable.getIntrinsicHeight();
				}

				resMatrix.setValues(eventMatrix);
				matrix = resMatrix;
				break;
			case ZOOM:
				break;
			}

			mode = NONE;
			break;
		case MotionEvent.ACTION_MOVE:
			// System.out.println("ACTUALLY DOING ACTIONS - scroll, scale");

			if (mode == DRAG) {
				matrix.set(savedMatrix);
				float dx = event.getX() - start.x;
				float dy = event.getY() - start.y;
				matrix.getValues(eventMatrix);
				matrix.postTranslate(dx, dy);
			}
			else if (mode == ZOOM) {
				float newDist = spacing(event);
				if (newDist > 10f) {
					matrix.set(savedMatrix);
					float scale = (newDist / oldDist);

					// check scale is not too big or two small
					float newScale = scale * mCurrentScale;
					// if (newScale > 10) {
					// return false;
					// }
					// else if (newScale < 0.1) {
					// return false;
					// }
					mCurrentScale = newScale;
					matrix.postScale(scale, scale, mid.x, mid.y);
				}
				if (lastEvent != null && event.getPointerCount() == 3) {
					newRot = rotation(event);
					float r = newRot - d;
					float[] values = new float[9];
					matrix.getValues(values);
					float tx = values[2];
					float ty = values[5];
					float sx = values[0];
					float xc = (view.getWidth() / 2) * sx;
					float yc = (view.getHeight() / 2) * sx;
					matrix.postRotate(r, tx + xc, ty + yc);
				}
			}
			break;
		}

		view.setImageMatrix(matrix);
		return true;
	}

	private float spacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (float) Math.sqrt(x * x + y * y);
	}

	private void midPoint(PointF point, MotionEvent event) {
		float x = event.getX(0) + event.getX(1);
		float y = event.getY(0) + event.getY(1);
		point.set(x / 2, y / 2);
	}

	private float rotation(MotionEvent event) {
		double delta_x = (event.getX(0) - event.getX(1));
		double delta_y = (event.getY(0) - event.getY(1));
		double radians = Math.atan2(delta_y, delta_x);
		return (float) Math.toDegrees(radians);
	}

	private boolean hasGLES20() {
		ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		ConfigurationInfo info = am.getDeviceConfigurationInfo();
		return info.reqGlEsVersion >= 0x20000;
	}
}

class MyGLSurfaceView extends GLSurfaceView {

	private MyGLRenderer mRenderer;

	public MyGLSurfaceView(Context context) {
		super(context);
		init();
	}

	public MyGLSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
	private float mPreviousX;
	private float mPreviousY;

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		// MotionEvent reports input details from the touch screen
		// and other input controls. In this case, you are only
		// interested in events where the touch position changed.

		float x = e.getX();
		float y = e.getY();

		switch (e.getAction()) {
		case MotionEvent.ACTION_MOVE:

			float dx = x - mPreviousX;
			float dy = y - mPreviousY;

			// reverse direction of rotation above the mid-line
			if (y > getHeight() / 2) {
				dx = dx * -1;
			}

			// reverse direction of rotation to left of the mid-line
			if (x < getWidth() / 2) {
				dy = dy * -1;
			}

			mRenderer.mAngle += (dx + dy) * TOUCH_SCALE_FACTOR; // = 180.0f / 320
			requestRender();
		}

		mPreviousX = x;
		mPreviousY = y;
		return true;
	}

	public void init() {
		// Create an OpenGL ES 2.0 context.
		setEGLContextClientVersion(2);

		setPreserveEGLContextOnPause(true);
		// Set the Renderer for drawing on the GLSurfaceView
		mRenderer = new MyGLRenderer();
		setRenderer(mRenderer);

		// Render the view only when there is a change in the drawing data
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}
}