package edu.elon.cs.fireflies;

import java.util.ArrayList;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

/**
 * Augmented Reality view using the camera and a bunch of fireflies.
 * 
 * Controls the listeners for GPS and phone movement.
 * 
 * @author Amy Eubanks and Joel Hollingsworth
 */

public class FieldView extends SurfaceView implements SurfaceHolder.Callback, 
                                                      SensorEventListener,
                                                      LocationListener {
	
	private ArrayList<Firefly> fireflies;
	
	// low-pass filter rates
	private final float KFILTERINGFACTOR = 0.15f;  // azimuth
	private final float GPS_KFILTERINGFACTOR = 0.15f;  // GPS locations
	
	// viewing angles
	private final float XANGLEWIDTH = 29;
	private final float YANGLEWIDTH = 19;
	
	// GPS update rates
	private final long GPS_UPDATE_TIME = 0;
	private final float GPS_UPDATE_DISTANCE = 0.0f;
	
	// for debugging
	private static final boolean DEBUG_MODE = true;
	private TextView tv;
	
	// field of view
	private float direction = 0.0f;
	private float rollingZ = 0.0f;
	private float rollingX = 0.0f;
	private float inclination = 0.0f;
	
	// FIXME screen -- landscape mode
	private int screenWidth = 1280;
	private int screenHeight = 720;
	
	// location
	private LocationManager locmngr;
	private Location location;
	private final float NEEDED_ACCURACY = 6.0f;  // 8m
	private boolean accuracyAchieved;
	
	// animation loop and surface
	private SurfaceHolder surfaceHolder;
	private FieldViewThread thread;
	
	// animation images
	private ImageSet imageSet;
	
	// vibrate the phone
	private Vibrator vib;
	
	// how many fireflies?
	private final int NUM_FIREFLIES = 30;
	
	// access to R
	private Context context;
	
	/**
	 * Create a set of images, setup GPS, and create the animation thread.
	 * 
	 * @param context The context
	 */
	public FieldView(Context context) {
		super(context);
		this.context = context;
		
		// vibration
		vib = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		
		// need to be on top of the camera SurfaceView
		setZOrderMediaOverlay(true);
		
		// animation images
		imageSet = new ImageSet(context);
				
		// want to hear about changes to our surface
		surfaceHolder = getHolder();
		surfaceHolder.addCallback(this);
		
		// make it see-through
		surfaceHolder.setFormat(PixelFormat.TRANSPARENT);
		
		// location manager -- setup the updates 
		locmngr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	    locmngr.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_UPDATE_TIME, GPS_UPDATE_DISTANCE, this);
	    
	    // container for our FFs -- empty until we have a starting location
	    fireflies = new ArrayList<Firefly>();
	    
	    // phone's current location
	    location = null;
	    if (!DEBUG_MODE) {
	    	accuracyAchieved = false;
	    } else {
	    	accuracyAchieved = true;
	    	
	    	location = new Location("FIXED POSITION");
	    	location.setLatitude(0);
	    	location.setLongitude(0);
	    	location.setAltitude(0);
	    	
	    	// if no FFs, create some
			if (fireflies.isEmpty()) {
				for (int i = 0; i < NUM_FIREFLIES; i++) {
					Firefly firefly = new Firefly(context, location, imageSet);
					fireflies.add(firefly);
				}
			}
	    }
	   
	    // animation thread
	    thread = new FieldViewThread(new Handler() {
	    	public void handleMessage(Message m) {
	    		tv.setText(m.getData().getString("data"));
	    	}
	    });
	}
	
	/**
	 * Get access to a TextView for messages.
	 * 
	 * @param text A TextView
	 */
	public void setTextView(TextView text) {
		tv = text;
	}
	
	
	/*
	*********************
	** FieldViewThread **
	*********************
	*/
	private class FieldViewThread extends Thread {
		
		private Handler msgHandler;
		private boolean isRunning;
		
		// matrix used to scale a firefly
		// FIXME rotation matrix?
		private Matrix matrix;
		
		public FieldViewThread(Handler handler) {
			
			// for display messages
			msgHandler = handler;
			
			matrix = new Matrix();
			
			// keep looping?
			isRunning = false;
		}
		
		public void run() {
			
			// starting time
			long lastTime = System.currentTimeMillis();
			
			// main animation loop
			while (isRunning) {
				Canvas canvas = null;
				
				try {
					canvas = surfaceHolder.lockCanvas();
					synchronized(surfaceHolder) {
						long now = System.currentTimeMillis();
						double elapsed = now - lastTime;
						lastTime = now;
						 
						// needed to properly clear the screen
						canvas.drawColor(0, PorterDuff.Mode.CLEAR);
						
						// splash screen
						if (!accuracyAchieved) {
							Paint shade = new Paint();
							shade.setARGB(196, 154, 50, 205);
							
							canvas.drawRect(0, 0, screenWidth, screenHeight, shade);
							canvas.drawBitmap(imageSet.firefly[0][0], screenWidth/2 - imageSet.width/2, screenHeight/2 - imageSet.height/2,  null);
							continue; // skip the FFs
						}
						
						for (Firefly firefly: fireflies) {
							firefly.update(elapsed);

							// FIXME start debug message
							if (DEBUG_MODE) {
								/*
								Message msg = msgHandler.obtainMessage();
								Bundle b = new Bundle();
								//b.putString("data", firefly.debugString());
								float b2 = location.bearingTo(firefly.getLocation());
								if (b2 < 0.0) b2 += 360.0;
								b.putString("data", "distance=" + distanceTo(location, firefly.getLocation())
										+ "\n" + "scale = " + (((-96.0 / 29.0) * distanceTo(location, firefly.getLocation()) + (96.0 / 29.0) + 96)/96.0)
										+ "\n" + "z=" + firefly.getLocation().getAltitude()
										+ "\n" + "bearingTo=" + b2
										+ "\n" + "direction=" + direction
								);
								msg.setData(b);
								msgHandler.sendMessage(msg);
								*/
							}
							// FIXME end debug message
							
							// compute the FFs scale
							float scale = (float)(((-imageSet.FIREFLY_SIZE / 29.0) * distanceTo(location, firefly.getLocation()) + (imageSet.FIREFLY_SIZE / 29.0) + imageSet.FIREFLY_SIZE)/imageSet.FIREFLY_SIZE);
							if (scale > 1.0f) scale = 1.0f;  // chop at 1
							
							// only draw FF if its within the screen
							if (scale > 0.0f 
									&& (firefly.ffx + imageSet.FIREFLY_SIZE*scale) >= 0 
									&& firefly.ffx <= screenWidth 
									&& (firefly.ffy + imageSet.FIREFLY_SIZE*scale) >= 0 
									&& firefly.ffy <= screenHeight) {
								
								// resize the FF by scale
								matrix.reset();
								matrix.postScale(imageSet.FIREFLY_SIZE/imageSet.width, imageSet.FIREFLY_SIZE/imageSet.height);
								if (scale < 1.0f)
									matrix.postScale(scale, scale);
								
								// FF location
								matrix.postTranslate(firefly.ffx, firefly.ffy);	

								// draw the FF
								canvas.drawBitmap(imageSet.firefly[firefly.wing][firefly.light], matrix, null);
							}
						}
					}
				} finally {
					if (canvas != null) {
						surfaceHolder.unlockCanvasAndPost(canvas);
					}
				}
			}
		}
		
		public void setRunning(boolean b) {
			isRunning = b;
		}
	}
	
	
	/*
	****************************
	** SurfaceHolder.Callback **
	****************************
	*/
	public void surfaceCreated(SurfaceHolder holder) {
		thread.setRunning(true);
		thread.start();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
		thread.setRunning(false);
		while (retry) {
			try {
				thread.join();
				retry = false;
			} catch (InterruptedException ex) {
			}
		}
	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

	
	/*
	*************************
	** SensorEventListener **
	*************************
	*/
	public void onSensorChanged(SensorEvent event) {
		
		// handle direction changes
		if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
			float azimuth = event.values[0];
			// adjust for landscape orientation
			azimuth = azimuth - 270;
			if (azimuth < 0) azimuth += 360;
			if (azimuth < XANGLEWIDTH/2 || azimuth > (360 - XANGLEWIDTH/2)) {
				// bias towards the new direction
				direction = (float)((direction * KFILTERINGFACTOR) + azimuth * (1.0 - KFILTERINGFACTOR));
			} else {
				// bias towards the current direction
				direction = (float)((azimuth * KFILTERINGFACTOR) + direction * (1.0 - KFILTERINGFACTOR));
			}
		}
		
		// handle tilt changes
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			// low-pass filter
			rollingZ = (float)((event.values[2] * KFILTERINGFACTOR) + (rollingZ * (1.0 - KFILTERINGFACTOR)));
			rollingX = (float)((event.values[0] * KFILTERINGFACTOR) + (rollingX * (1.0 - KFILTERINGFACTOR)));
			
			if (rollingZ != 0.0f) {
				inclination = (float)Math.atan(rollingX / rollingZ);
			} else if (rollingX < 0) {
				inclination = (float)(Math.PI/2.0);
 			} else if (rollingX >= 0) {
 				inclination = (float)(3 * Math.PI/2.0);
 			}
			
			// convert to degrees
			inclination = (float)(inclination * (360/(2*Math.PI)));
			
			// flip
			if (inclination < 0) inclination += 90;
			else inclination -= 90;
			
			inclination += 10;
		}
		
		updateFFs();
	}
	
	private void updateFFs() {
		
		float leftArm = direction - (XANGLEWIDTH/2);
		if (leftArm < 0) leftArm += 360;
		float rightArm = direction + (XANGLEWIDTH/2);
		if (rightArm > 360) rightArm -= 360;
		
		float upperArm = inclination + (YANGLEWIDTH/2);
		float lowerArm = inclination - (YANGLEWIDTH/2);
		
		// FIXME temp code
		if (location != null) {
			int count = 0;
			for (Firefly firefly : fireflies) {
				float azi = location.bearingTo(firefly.getLocation());
				if (azi < 0) azi += 360;
				float inc = (float)Math.atan(firefly.getAltitude()/location.distanceTo(firefly.getLocation()));
				inc =  (float) (inc * 180/ Math.PI);
				firefly.ffx = computeX(leftArm, rightArm, azi);
				firefly.ffy = computeY(lowerArm, upperArm, inc);
				
				count++;
					
				}
			}
		}
	
	
	private float computeX(float leftArm, float rightArm, float azi) {
		float offset = azi - leftArm;
		if (leftArm > rightArm && azi <= rightArm)
			offset = 360 - leftArm  + azi;
		return (offset/XANGLEWIDTH) * screenWidth;
	}
	
	private float computeY(float lowerArm, float upperArm, float inc) {
		float offset = ((upperArm - YANGLEWIDTH) - inc) * -1;
		return screenHeight - ((offset/YANGLEWIDTH) * screenHeight);
	}
	
	private double distanceTo(Location one, Location two) {
		return Math.sqrt(Math.pow(one.distanceTo(two), 2) + Math.pow(one.getAltitude() - two.getAltitude(), 2));
	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}

	
	private int count = 0;
	
	/*
	**********************
	** LocationListener **
	**********************
	*/
	public void onLocationChanged(Location location) {

		count++;
		if (!accuracyAchieved && location.getAccuracy() > NEEDED_ACCURACY) {
			// not good enough
			//tv.setText(count + "" + "GPS accuracy = " + location.getAccuracy());
			tv.setText("GPS accuracy = " + location.getAccuracy());
			return;
		}
		
		//tv.setText(count + "" + "GPS accuracy = " + location.getAccuracy());
		
		// if no FFs, create some
		if (fireflies.isEmpty()) {
			for (int i = 0; i < NUM_FIREFLIES; i++) {
				Firefly firefly = new Firefly(context, location, imageSet);
				fireflies.add(firefly);
			}
		}
		
		/*
		if (DEBUG_MODE && !location.equals(this.location)) { // announce new locations
			vib.vibrate(1000);
		}
		*/
		if (DEBUG_MODE) {
			return;
		}
		
		if (this.location == null) {
			this.location = new Location(location);
			this.location.setAltitude(0.0);
		}

		if (!DEBUG_MODE) {
			// low-pass filter for latitude and longitude changes
			this.location.setLatitude(location.getLatitude() * GPS_KFILTERINGFACTOR + this.location.getLatitude() * (1.0 - GPS_KFILTERINGFACTOR));
			this.location.setLongitude(location.getLongitude() * GPS_KFILTERINGFACTOR + this.location.getLongitude() * (1.0 - GPS_KFILTERINGFACTOR));
			this.location.setAltitude(0.0);
		} else {
			// phone is at a fixed position
			this.location.setLatitude(0.0);
			this.location.setLongitude(0.0);
			this.location.setAltitude(0.0);
		}
		
		accuracyAchieved = true;
	}

	public void onProviderDisabled(String provider) {}
	public void onProviderEnabled(String provider) {}
	public void onStatusChanged(String provider, int status, Bundle extras) {}
}
