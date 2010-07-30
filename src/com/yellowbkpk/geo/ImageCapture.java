package com.yellowbkpk.geo;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class ImageCapture extends Activity implements LocationListener {
    
    private SurfaceView preview=null;
    private SurfaceHolder previewHolder=null;
    private Camera camera=null;
    private LocationManager lm;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            Log.i("PictureDemo", "Starting up GPS location provider...");
            startGPS();
        }
        
        preview=(SurfaceView)findViewById(R.id.preview);
        previewHolder=preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private void startGPS() {
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                2000, 10,
                this);
    }
    
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if(LocationProvider.AVAILABLE == status) {
            Log.i("PictureDemo", "Satellite fix.");
            Toast
                .makeText(ImageCapture.this, "GPS Fix", Toast.LENGTH_LONG)
                .show();
        }
    }
    public void onProviderEnabled(String provider) {
        Log.i("PictureDemo", "Provider enabled.");
    }
    public void onProviderDisabled(String provider) {
        Log.i("PictureDemo", "Provider disabled.");
    }
    public void onLocationChanged(Location location) {
        Log.i("PictureDemo", "Location changed to " + location.getLatitude() + "," + location.getLongitude());
    }
    
    @Override
    protected void onPause() {
        stopGPS();
        
        super.onPause();
    }

    private void stopGPS() {
        lm.removeUpdates(this);
    }

    @Override
    protected void onResume() {
        startGPS();
        
        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode==KeyEvent.KEYCODE_VOLUME_DOWN) {
            takePicture();

            return(true);
        }

        return(super.onKeyDown(keyCode, event));
    }

    private void takePicture() {
        camera.takePicture(null, null, photoCallback);
    }

    SurfaceHolder.Callback surfaceCallback=new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i("PictureDemo", "Opening camera");
            camera=Camera.open();

            try {
                Parameters parameters = camera.getParameters();
//                List<Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
//                for (Size size : supportedPreviewSizes) {
//                    Log.i("PictureDemo", "Supported preview: " + size.width + "x" + size.height);
//                }
//                Log.i("PictureDemo", "Preview window: " + holder.getSurfaceFrame().width() + "x" + holder.getSurfaceFrame().height());
//                parameters.setPreviewSize(480, 320);
                parameters.setRotation(270);
                parameters.setPictureFormat(PixelFormat.JPEG);
                camera.setParameters(parameters);
                camera.setPreviewDisplay(previewHolder);
            }
            catch (Throwable t) {
                Log.e("PictureDemo-surfaceCallback",
                            "Exception in setPreviewDisplay()", t);
                Toast
                    .makeText(ImageCapture.this, t.getMessage(), Toast.LENGTH_LONG)
                    .show();
            }
        }

        public void surfaceChanged(SurfaceHolder holder,
                                     int format, int width,
                                     int height) {
            Log.i("PictureDemo", "Surface changed: " + width + "x" + height);
            camera.startPreview();
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            camera.stopPreview();
            camera.release();
            camera=null;
        }
    };

    Camera.PictureCallback photoCallback=new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            new SavePhotoTask().execute(data);
            camera.startPreview();
        }
    };

    class SavePhotoTask extends AsyncTask<byte[], String, String> {
        @Override
        protected String doInBackground(byte[]... jpeg) {
            File photo=new File(getExternalFilesDir("photos"),
                                                    "photo.jpg");
//                                                    System.currentTimeMillis() + ".jpg");

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

            return(null);
        }
    }
}