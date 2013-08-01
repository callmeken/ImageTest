package com.example.imagetest;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.lang.Math;

public class MainActivity extends Activity implements OnTouchListener {

	private ImageView imgView;
	private MyDrawableView myDView;

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

	private int numScans = 1;
	private int numScansPending;

	/*
	 * We'll need to save the initial scaling factor in order to account for the
	 * pixel positioning of the localization program
	 */
	private static float initScaleX;
	private static float initScaleY;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Create a GLSurfaceView instance and set it
		// as the ContentView for this Activity
		// mGLView = new MyGLSurfaceView(this);
		// setContentView(mGLView);
		setContentView(R.layout.activity_two);

		imgView = (ImageView) findViewById(R.id.imageView1);

		myDView = (MyDrawableView) findViewById(R.id.circleView1);
		myDView.setVisibility(View.INVISIBLE);

		imgView.setOnTouchListener(this);

		calcInitScale(imgView);
		plotPoint(365, 261);
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

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			showSeek();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void showSeek() {
		final TextView tvBetVal;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = this.getLayoutInflater();
		View v = inflater.inflate(R.layout.dialog, null);
		builder
				.setView(v)
				.setTitle(
						"Enter the sampling speed (Note: Higher speeds may reduce accuracy)")
				.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// confirmValues();
						// SAVE THE PROGRESS BAR VALUE SOMEWHERE
						numScans = (numScansPending == 0) ? (1) : (numScansPending);
						dialog.dismiss();
					}
				}).setNeutralButton("Cancel", null).show();
		SeekBar sbBetVal = (SeekBar) v.findViewById(R.id.sbBetVal);
		tvBetVal = (TextView) v.findViewById(R.id.tvBetVal);
		sbBetVal.setMax(10);
		sbBetVal.setProgress(numScans);
		sbBetVal.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				tvBetVal.setText(String.valueOf(progress));
				numScansPending = progress;
			}
		});
	}

	public boolean onTouch(View v, MotionEvent event) {
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		// handle touch events here
		ImageView view = (ImageView) v;
		Drawable drawable = getResources().getDrawable(R.drawable.cc_1);

		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			savedMatrix.set(matrix);
			start.set(event.getX(), event.getY());
			mode = DRAG;
			// lastEvent = null;

			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			System.out.println("Subsequent presses");
			oldDist = spacing(event);
			if (oldDist > 10f) {
				savedMatrix.set(matrix);
				midPoint(mid, event);
				mode = ZOOM;
			}
			
			lastEvent = new float[4];
			lastEvent[0] = event.getX(0);
			lastEvent[1] = event.getX(1);
			lastEvent[2] = event.getY(0);
			lastEvent[3] = event.getY(1);
			d = rotation(event);
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			switch (mode) {
			case DRAG:
				view.getImageMatrix().getValues(eventMatrix);

				if (view.getWidth() < drawable.getIntrinsicWidth()) {
					if (eventMatrix[Matrix.MTRANS_X] > 0) {
						eventMatrix[Matrix.MTRANS_X] = 0;
					}
					else if (eventMatrix[Matrix.MTRANS_X] < view.getWidth()
							- drawable.getIntrinsicWidth()) {
						eventMatrix[Matrix.MTRANS_X] = view.getWidth()
								- drawable.getIntrinsicWidth();
					}
				}
				else
					eventMatrix[Matrix.MTRANS_X] = 0;

				if (view.getHeight() < drawable.getIntrinsicHeight()) {
					if (eventMatrix[Matrix.MTRANS_Y] > 0) {
						eventMatrix[Matrix.MTRANS_Y] = 0;
					}
					else if (eventMatrix[Matrix.MTRANS_Y] < view.getHeight()
							- drawable.getIntrinsicHeight()) {
						eventMatrix[Matrix.MTRANS_Y] = view.getHeight()
								- drawable.getIntrinsicHeight();
					}
				}
				else
					eventMatrix[Matrix.MTRANS_Y] = 0;

				resMatrix.setValues(eventMatrix);
				matrix = resMatrix;
				break;
			case ZOOM:
				// Ensure minimum and maximum scales
				view.getImageMatrix().getValues(eventMatrix);
				if (eventMatrix[0] < 1) {
					eventMatrix[0] = (float) 1.0;
					eventMatrix[2] = 0;
				}
				if (eventMatrix[4] < 1) {
					eventMatrix[4] = (float) 1.0;
					eventMatrix[5] = 0;
				}
				resMatrix.setValues(eventMatrix);
				matrix = resMatrix;
				System.out.println("AFTER: " + view.getImageMatrix().toString());
				break;
			}

			mode = NONE;
			break;
		case MotionEvent.ACTION_MOVE:
			if (mode == DRAG) {
				matrix.set(savedMatrix);
				float dx = event.getX() - start.x;
				float dy = event.getY() - start.y;

				/*
				 * WE MIGHT WANT TO RESTRICT MOVEMENT IN CERTAIN DIRECTIONS BASED ON
				 * SCALE: if (view.getHeight() >= drawable.getIntrinsicHeight()) { dy =
				 * 0; } if (view.getWidth() >= drawable.getIntrinsicWidth()) { dx = 0; }
				 */
				matrix.postTranslate(dx, dy);
			}
			else if (mode == ZOOM) {
				float newDist = spacing(event);
				if (newDist > 10f) {
					matrix.set(savedMatrix);
					float scale = (newDist / oldDist);

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

		matrix.getValues(eventMatrix);

		/* The point specified will be given by the localization function */
		plotPoint(365, 261);
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

	private void calcInitScale(View v) {
		Drawable drawable = getResources().getDrawable(R.drawable.cc_1);

		/* The initial size of the image will have to predefined somewhere */
		initScaleX = (float) drawable.getIntrinsicWidth() / 1293;
		initScaleY = (float) drawable.getIntrinsicHeight() / 755;
		
	  // IMAGE is 1293x755 NOTE ALL SHOULD BE THE SAME SIZE
		// NOTE: The xy spreadsheet is 1291 by 754 for some reason
		return;
	}

	/*
	 * Function that will plot the point correctly regardless of scale or position
	 */
	private void plotPoint(float x, float y) {
		// TODO adjust for rotation

		myDView.setVisibility(View.INVISIBLE);
		myDView.setX((x * initScaleX * eventMatrix[Matrix.MSCALE_X]) - 25
				+ eventMatrix[Matrix.MTRANS_X]);
		myDView.setY((y * initScaleY * eventMatrix[Matrix.MSCALE_Y]) - 25
				+ eventMatrix[Matrix.MTRANS_Y]);
		myDView.setVisibility(View.VISIBLE);
		return;
	}

	// FUNCTION NOT USED
	private boolean hasGLES20() {
		ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		ConfigurationInfo info = am.getDeviceConfigurationInfo();
		return info.reqGlEsVersion >= 0x20000;
	}
}

/*
 * Tracking circle
 */
class MyDrawableView extends View {
	private ShapeDrawable mDrawable;

	public MyDrawableView(Context context) {
		super(context);

		int x = 10;
		int y = 10;
		int width = 300;
		int height = 50;

		mDrawable = new ShapeDrawable(new OvalShape());
		mDrawable.getPaint().setColor(0xff74AC23);
		mDrawable.setBounds(x, y, x + width, y + height);
	}

	public MyDrawableView(Context context, AttributeSet attrs) {
		super(context, attrs);

		int x = 0;
		int y = 0;
		int width = 50;
		int height = 50;

		mDrawable = new ShapeDrawable(new OvalShape());
		mDrawable.getPaint().setColor(0xff74AC23);
		mDrawable.setBounds(x, y, x + width, y + height);
	}

	protected void onDraw(Canvas canvas) {
		mDrawable.draw(canvas);
	}
}

/**********************************************************
 * EVERYTHING BELOW THIS COMMENT IS NO LONGER BEING USED *
 * 
 * @author Ken * *
 **********************************************************/
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