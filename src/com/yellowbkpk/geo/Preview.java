package com.yellowbkpk.geo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

class Preview extends SurfaceView implements SurfaceHolder.Callback, LocationListener, SensorEventListener {
    private SurfaceHolder mHolder;
    private DrawOnTop mStatus;
    private Camera mCamera;
    private LocationManager lm;
    private SensorManager sm;
    private Sensor orientSensor;
    private volatile float[] orientation;

    Preview(Context context, DrawOnTop draw) {
        super(context);

        mStatus = draw;
        
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.i("PictureDemo", "Starting up GPS location provider...");
            startGPS();
        }
        
        sm = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        if (!sm.getSensorList(Sensor.TYPE_ORIENTATION).isEmpty()) {
            startCompass();
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        mCamera = Camera.open();
        try {
           mCamera.setPreviewDisplay(holder);
           
           Parameters parameters = mCamera.getParameters();
           List<Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
           Size size = supportedPictureSizes.get(0);
           parameters.setPictureSize(size.width, size.height);
           mCamera.setParameters(parameters);
           
        } catch (IOException exception) {
            mCamera.release();
            mCamera = null;
            // TODO: add more exception handling logic here
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        stopGPS();
        stopCompass();
        
        // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused.
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }
    
    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();

        List<Size> sizes = parameters.getSupportedPreviewSizes();
        Size optimalSize = getOptimalPreviewSize(sizes, w, h);
        parameters.setPreviewSize(optimalSize.width, optimalSize.height);

        mCamera.setParameters(parameters);
        mCamera.startPreview();
    }

    void takePicture() {
        mCamera.takePicture(null, null, photoCallback);
    }
    
    Camera.PictureCallback photoCallback=new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            new SavePhotoTask().execute(data);
            camera.startPreview();
        }
    };

    class SavePhotoTask extends AsyncTask<byte[], String, String> {
        @Override
        protected String doInBackground(byte[]... jpeg) {
            File photo=new File(getContext().getExternalFilesDir("photos"),
//                                                    "photo.jpg");
                                                    (System.currentTimeMillis() / 1000L) + ".jpg");

            if (photo.exists()) {
                photo.delete();
            }

            try {
                FileOutputStream fos=new FileOutputStream(photo.getPath());

                fos.write(jpeg[0]);
                fos.close();
            }
            catch (java.io.IOException e) {
                Log.e("PictureDemo", "Exception in photoCallback", e);
            }
            
            Log.i("PictureDemo", "Saved to " + photo.getAbsolutePath());
            
            try {
                ExifInterface iff = new ExifInterface(photo.getAbsolutePath());
                iff.setAttribute("Orientation", Float.toString(orientation[0]));
                iff.saveAttributes();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return(null);
        }
    }
    
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if(LocationProvider.AVAILABLE == status) {
            Log.i("PictureDemo", "Satellite fix.");
            Toast
                .makeText(getContext(), "GPS Fix", Toast.LENGTH_LONG)
                .show();
        }
    }
    public void onProviderEnabled(String provider) {
        Log.d("PictureDemo", "Provider enabled.");
    }
    public void onProviderDisabled(String provider) {
        Log.d("PictureDemo", "Provider disabled.");
    }
    public void onLocationChanged(Location location) {
        Log.i("PictureDemo", "Location changed to " + location.getLatitude() + "," + location.getLongitude());
        
        mStatus.gpsLocation = location;
        mStatus.invalidate();
        
        Parameters parameters = mCamera.getParameters();
        parameters.setGpsLatitude(location.getLatitude());
        parameters.setGpsLongitude(location.getLongitude());
        parameters.setGpsAltitude(location.getAltitude());
        parameters.setGpsTimestamp(location.getTime());
        mCamera.setParameters(parameters);
    }
    
    void startCompass() {
        orientSensor = sm.getSensorList(Sensor.TYPE_ORIENTATION).get(0);
        sm.registerListener(this, orientSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    void startGPS() {
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000, 10,
                this);
    }

    void stopGPS() {
        lm.removeUpdates(this);
    }

    void stopCompass() {
        sm.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        orientation = event.values;
        mStatus.heading = event.values[0];
        mStatus.invalidate();
    }

}