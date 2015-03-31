package edu.elon.cs.fireflies;

import edu.elon.cs.fireflies.ImageSet;
import android.content.Context;
import android.location.Location;

public class Firefly {
	
	private final boolean DEBUG_MODE = false;

	private Location location;
	
	public float ffx, ffy;
	
	//50 meters = .00045 degree
	private final double MAX_DISTANCE = .000090;
	private  double LAT_RANDOM; // = Math.random() * 2 - 1;
	private  double LONG_RANDOM; // = Math.random() * 2 - 1;
	
	private final double ALT_MIN = -1;
	private final double ALT_MAX = 3;
	
	private final double X_RANGE = .000045; 
	private final double Y_RANGE = .000045; 
	private final double LARGEST_RANGE = Y_RANGE;
	private final float ONE_CURVE_MAX_TIME = 20000.0f;	// 8s
	private final int SUBDIVISIONS = 8;   // used in distance formula
	private float completionTime;
	private float movementTime;
	private Location A;
	private Location B;
	private Location C;
	private Location D;
	
	// Light Animation
	private final float LIT_TIME_MIN = 2000.0f;
	private final float LIT_TIME_MAX = 4000.0f;
	private final float UNLIT_TIME_MIN = 1000.0f;
	private final float UNLIT_TIME_MAX = 3000.0f;
	private final float LIT_RAMP_PERC = 0.25f;
	private float litTime;
	private float unlitTime;
	private float stayLitTime;
	private float stayUnlitTime;
	private boolean isLit;
	private int lit_ramp_stages;
	
	// Wing Animation
	private final float WING_CYCLE_TIME = 75.0f;
	private float wingTime;
	private int wing_cycle_num;
	private float wing_cycle_segment;
	
	// The Firefly
	public int light;
	public int wing;
	
	private ImageSet imageSet;
	
	// Speed Hacks
	//private Location distanceEnd, distanceStart;
	
	public Firefly(Context context, Location pLocation, ImageSet imageSet) {
		//super(baseContext);
		//location = pLocation;
		
		if (DEBUG_MODE) {
			// fixed location -- South
			location = new Location("byhand");
			location.setLatitude(pLocation.getLatitude() - .0000225);
			location.setLongitude(pLocation.getLongitude());
			location.setAltitude(0);
		} else {
			LAT_RANDOM = Math.random() * 2 - 1;
			LONG_RANDOM = Math.random() * 2 - 1;
			
			// random location
			location = new Location("random");
			location.setLatitude(pLocation.getLatitude() - LAT_RANDOM * MAX_DISTANCE);
			location.setLongitude(pLocation.getLongitude() - LONG_RANDOM * MAX_DISTANCE);
			location.setAltitude(Math.random() * (ALT_MAX - ALT_MIN) + ALT_MIN);
		}
		
		this.imageSet = imageSet;
		lit_ramp_stages = imageSet.firefly[0].length - 1;
		
		// initialize wing and light variables
		wing_cycle_num = (imageSet.firefly.length - 1) * 2;
		wing_cycle_segment = WING_CYCLE_TIME/wing_cycle_num;


		ffx = ffy = -500;
		
		// starting curve
		A = location;
		B = generatePoint2(A);
		//C = generatePoint(A);
		//D = generatePoint(A);
		
		wing = 0;
		light = 0;
		isLit = false;
		stayLitTime = LIT_TIME_MIN + ((float)Math.random() * (LIT_TIME_MAX - LIT_TIME_MIN));
		stayUnlitTime = UNLIT_TIME_MIN + ((float)Math.random() * (UNLIT_TIME_MAX - UNLIT_TIME_MIN));

		
		// initialize movement
		//completionTime = (float) (ONE_CURVE_MAX_TIME * (distance() / (LARGEST_RANGE)));
		completionTime = ONE_CURVE_MAX_TIME;
		movementTime = 0.0f;
	}

	public Location getLocation() {
		return location;
	}
	
	public float getAltitude() {
		return (float)location.getAltitude();
	}
	
	/**
	 * General update of location, angle, wing, and light.
	 * 
	 * @param elapsed Time elapsed since last cycle.
	 */
	public void update(double elapsed) {
		updateLocation(elapsed);
		//updateAngle();
		updateWing(elapsed);
		updateLight(elapsed);
	}
	
	/**
	 * Determine the wing stage.
	 * 
	 * @param elapsed Time elapsed since last cycle.
	 */
	private void updateWing(double elapsed) {
		// accumulate time in milliseconds
		wingTime += elapsed;

		// reset the accumulator
		if (wingTime > WING_CYCLE_TIME) 
			wingTime = 0;

		// figure out which wing stage FF should be in
		float segment = wing_cycle_segment;
		wing = 0;
		
		while (wingTime > segment) {
			if (segment < (WING_CYCLE_TIME/2.0f) + wing_cycle_segment) 
				wing++;
			else
				wing--;
			segment += wing_cycle_segment;
		}
	}
	

	public void updateLocation(double elapsed) {
		//accumulate milliseconds
		movementTime += elapsed;

		// how far along the curve?
		double percentageTime = movementTime / completionTime;

		// finished the curve?
		if (percentageTime >= 1.0) { 
			movementTime = 0.0f; 
			percentageTime = 0.0f;

			// generate a new curve starting from your last know location
			A.set(B);
			B = generatePoint2(A);  
		}

		double zDistance = Math.abs(A.getAltitude() - B.getAltitude());
		// when the movement along this curve should stop
		completionTime = (float) (ONE_CURVE_MAX_TIME * (zDistance / (ALT_MAX - ALT_MIN)));
		//completionTime = (float) (ONE_CURVE_MAX_TIME * (cartesianDistance(A,B) / (LARGEST_RANGE)));

		location = interpolation(A, B, percentageTime);
	}
	
	/*
	public void updateLocation(double elapsed) {
		// accumulate milliseconds
		movementTime += elapsed;

		// how far along the curve?
		float percentageTime = movementTime / completionTime;

		// finished the curve?
		if (percentageTime >= 1.0) { 
			movementTime = 0.0f; 
			percentageTime = 0.0f;

			// generate a new curve starting from your last know location
			A.setLatitude(D.getLatitude());
			A.setLongitude(D.getLongitude()); 
			A.setAltitude(B.getAltitude());
			B = generatePoint(A); 
			C = generatePoint(A); 
			D = generatePoint(A); 
		}

		// when the movement along this curve should stop
		completionTime = (float) (ONE_CURVE_MAX_TIME * (distance() / (LARGEST_RANGE)));

		bezier(percentageTime, ffLocation);	

	}
	*/

	/**
	 * Determine the light stage.
	 * 
	 * @param elapsed Time elapsed since last cycle.
	 */
	private void updateLight(double elapsed) {

		if (isLit) {   // show the light
			litTime += elapsed;

			float percentageTime = litTime / stayLitTime;
			if (percentageTime > 1.0) {   // turn off the light
				litTime = 0;
				isLit = false;
			}

			if (percentageTime < LIT_RAMP_PERC) {   // ramp up
				light = 1;
				float stepSize = LIT_RAMP_PERC / (lit_ramp_stages - 1);
				while (percentageTime > (light * stepSize))
					light++;
			} else if (percentageTime >= LIT_RAMP_PERC && percentageTime < (1.0f - LIT_RAMP_PERC)) {   // top
				light = lit_ramp_stages;
			} else {   // ramp down
				light = 1;
				float stepSize = LIT_RAMP_PERC / (lit_ramp_stages - 1);
				while (percentageTime > (1.0f - LIT_RAMP_PERC) + (light * stepSize))
					light++;

				light = lit_ramp_stages - light;
			}

		} else {   // no light
			unlitTime += elapsed;
			if (unlitTime > stayUnlitTime) {    // light it up
				unlitTime = 0;
				isLit = true;
			}
			light = 0;	
		}
	}
	

		/**
		 * Generate a new random point based on a given 
		 * point within the field.
		 * 
		 * @param current Current point.
		 * @return The new point.
		 */
		private Location generatePoint(Location current) {    
			double x = (X_RANGE * Math.random() - (X_RANGE/2) + current.getLatitude()); 
			double y = (Y_RANGE * Math.random() - (Y_RANGE/2) + current.getLongitude()); 
			//double z = (ALT_RANDOM * Math.random() - (ALT_RANDOM/2) + current.getAltitude());

			Location newLoc = new Location("byhand"); 
			newLoc.setLatitude(x);
			newLoc.setLongitude(y);
			//newLoc.setAltitude(z);
			return newLoc;
		} 
		
		private Location generatePoint2(Location current) {    
			
			double z = Math.random() * (ALT_MAX - ALT_MIN) + ALT_MIN;
			double lat = 0.0;
			double lng = 0.0;
			
			/*
			double which1 = Math.random();
			double which2 = Math.random();
			
			if (which1 < .15) {
				lat = .000008;
			} else if (which1 < .3) {
				lat = -.00008;
			} else if (which2 < .15) {
				lng = .000008;
			} else if (which2 < .3) {
				lng = -.000008;
			}
			*/

			Location newLoc = new Location("byhand"); 
			newLoc.setLatitude(current.getLatitude() + lat);
			newLoc.setLongitude(current.getLongitude() + lng);
			//newLoc.setAltitude(current.getAltitude());
			newLoc.setAltitude(z);
			return newLoc;
		} 

		/**
		 * Compute an interpolation between two points.
		 * 
		 * @param one A point.
		 * @param two A point.
		 * @param u A percentage.
		 * @return Newly interpolated point.
		 */
		private Location interpolation(Location one, Location two, double u) {

			Location p = new Location("byhand");
			//double lat = ((1 - u) * one.getLatitude() + u * two.getLatitude());
			//double lngtd = ((1 - u) * one.getLongitude() + u * two.getLongitude());
			double alt = ((1 - u) * one.getAltitude() + u * two.getAltitude());

			//p.setLatitude(lat);
			//p.setLongitude(lngtd);
			p.setLatitude(one.getLatitude());
			p.setLongitude(one.getLongitude());
			p.setAltitude(alt);
			return p;
		}

		/**
		 * Find a point along the Bezier curve defined
		 * by points A, B, C, D.
		 * 
		 * @param u Percentage along the Bezier curve.
		 * @return Computed point on the bezier curve.
		 */
		private void bezier(float u, Location dest) {
			// interpolation AB
			double latAB = ((1 - u) * A.getLatitude() + u * B.getLatitude());
			double lngtdAB = ((1 - u) * A.getLongitude() + u * B.getLongitude());
			double altAB = ((1 - u) * A.getAltitude() + u * B.getAltitude());
			
			// interpolation BC
			double latBC = ((1 - u) * B.getLatitude() + u * C.getLatitude());
			double lngtdBC = ((1 - u) * B.getLongitude() + u * C.getLongitude());
			double altBC = ((1 - u) * B.getAltitude() + u * C.getAltitude());
			
			// interpolation CD
			double latCD = ((1 - u) * C.getLatitude() + u * D.getLatitude());
			double lngtdCD = ((1 - u) * C.getLongitude() + u * D.getLongitude());
			double altCD = ((1 - u) * C.getAltitude() + u * D.getAltitude());
			
			// interpolation ABBC
			double latABBC = ((1 - u) * latAB + u * latBC);
			double lngtdABBC = ((1 - u) * lngtdAB + u * lngtdBC);
			double altABBC = ((1 - u) * altAB + u * altBC);
			
			// interpolation BCCD
			double latBCCD = ((1 - u) * latBC + u * latCD);
			double lngtdBCCD = ((1 - u) * lngtdBC + u * lngtdCD);
			double altBCCD = ((1 - u) * altBC + u * altCD);
			
			//Location dest = new Location("");
			dest.setLatitude(((1 - u) * latABBC + u * latBCCD));
			dest.setLongitude(((1 - u) * lngtdABBC + u * lngtdBCCD));
			dest.setAltitude(((1 - u) * altABBC + u * altBCCD));

			//return dest;
		}
		
		/**
		 * Cartesian distance between two points.
		 * 
		 * @param one A point.
		 * @param two A point.
		 * @return Distance between give two Points.
		 */
		private float cartesianDistance(Location one, Location two) {
			return (float)Math.sqrt(Math.pow(one.getLatitude() - two.getLatitude(), 2) 
					+ Math.pow(one.getLongitude() - two.getLongitude(), 2));
		}

		// This helps with GC issues
		Location distanceStart = new Location("distanceStart");
		Location distanceEnd = new Location("distanceEnd");
		
		/**
		 * Approximate distance along a Bezier curve defined
		 * by Points A, B, C, and D.
		 * 
		 * @return Approximate distance along a Bezier curve.
		 */
		private float distance() {
			float dis = 0.0f;
			
			distanceStart.set(A);

			float segment = 1.0f/SUBDIVISIONS;
			for (float u = segment; u < (1.0f + segment/2); u+=segment) {
				bezier(u, distanceEnd);
				dis += cartesianDistance(distanceStart, distanceEnd);
				distanceStart.set(distanceEnd);
			}

			return dis;
		}
		


}
