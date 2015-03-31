package edu.elon.cs.fireflies;

import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback {
	private SurfaceHolder previewHolder;
	private Camera camera;
  
    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        previewHolder = this.getHolder();
        previewHolder.addCallback(this);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }	
    	
    	public void surfaceDestroyed(SurfaceHolder holder) {
    		camera.stopPreview();
    		camera.release();
    		camera = null;
    		
    	}
    	public void surfaceCreated(SurfaceHolder holder) {
			camera = Camera.open();
			
			try {
				camera.setPreviewDisplay(previewHolder);
			}
			catch (Throwable t) {
				// error message
			}
			
		}
    	public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			Camera.Parameters parameters = camera.getParameters();
			List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
			
			Camera.Size previewSize = previewSizes.get(0);
			
			parameters.setPreviewSize(previewSize.width, previewSize.height);
			camera.setParameters(parameters);
			camera.startPreview();
			
		}   
    
}
