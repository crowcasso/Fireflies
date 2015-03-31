package edu.elon.cs.fireflies;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.TextView;

public class Field extends Activity {

	private final long MIN_TIME = 100;  // 100ms
	private final float MIN_DISTANCE = 1;  // 1m

	private FieldView fv;
	private SensorManager sensorManager;
	private LocationManager locationManager;
	private FrameLayout frame;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// connect TextView to FieldView
		//fv = new FieldView(getBaseContext());
		fv = new FieldView(getApplication());
		//TextView tv = (TextView) findViewById(R.id.textview);
		//TextView tv = new TextView(getBaseContext());
		TextView tv = new TextView(getApplication());
		tv.setTextColor(Color.BLACK);
		fv.setTextView(tv);
		frame = (FrameLayout)findViewById(R.id.frame);
		frame.addView(fv);
		frame.addView(tv);

		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	}

	@Override
	protected void onResume() {
		super.onResume();

	
		// listen to orientation sensor
		sensorManager.registerListener(fv,
				sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_FASTEST);    	
		// listen to the accelerometers
		sensorManager.registerListener(fv,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);
		// listen to GPS updates
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				MIN_TIME, MIN_DISTANCE, 
				fv);
	
	}

	@Override
	protected void onPause() {
		super.onPause();

		
		sensorManager.unregisterListener(fv);
		locationManager.removeUpdates(fv);
		
	}
}