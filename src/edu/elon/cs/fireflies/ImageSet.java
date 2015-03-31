package edu.elon.cs.fireflies;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Holds on full set of images: lantern and firefly images.
 * 
 * @author Amy Eubanks and Joel Hollingsworth
 */

public class ImageSet {
	
	// lantern constants
	public final int FRONT_TRANSPARENCY = 32;
	public final int LEFT = 70;
	public final int RIGHT = 360;
	public final int TOP = 221;
	public final int BOTTOM = 660;
	public final int STARTX = 222;
	public final int STARTY = 290;
	
	// firefly constants
	public final int FIREFLY_SIZE = 128;
	public final int FIREFLY_MIDDLE = FIREFLY_SIZE/2;

	// drawables
	private final int [][] FIREFLY = {
			{R.drawable.w0l0, R.drawable.w0l1, R.drawable.w0l2, R.drawable.w0l3},
			{R.drawable.w1l0, R.drawable.w1l1, R.drawable.w1l2, R.drawable.w1l3},
			{R.drawable.w2l0, R.drawable.w2l1, R.drawable.w2l2, R.drawable.w2l3}, 
			{R.drawable.w3l0, R.drawable.w3l1, R.drawable.w3l2, R.drawable.w3l3}
	};
	
	// instance variables
	public Bitmap [][] firefly;
	public Bitmap lantern;
	public float width;
	public float height;
	
	/**
	 * Build Bitmaps from the drawables.
	 * 
	 * @param context The Context.
	 */
	public ImageSet(Context context) {
		
		// build the BMPs of the FFs
		firefly = new Bitmap[FIREFLY.length][FIREFLY[0].length];
		for (int i = 0; i < FIREFLY.length; i++) {
			for (int j = 0; j < FIREFLY[0].length; j++) {
				firefly[i][j] = BitmapFactory.decodeResource(context.getResources(), FIREFLY[i][j]);
			}
		}
		
		width = firefly[0][0].getWidth();
		height = firefly[0][0].getHeight();
	}
}
