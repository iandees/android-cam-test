package com.yellowbkpk.geo;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;

public class ImageCapture extends Activity {
    
    private Preview preview=null;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        preview = new Preview(this);
        setContentView(preview);
        
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode==KeyEvent.KEYCODE_VOLUME_DOWN) {
            preview.takePicture();

            return(true);
        }

        return(super.onKeyDown(keyCode, event));
    }
    
    @Override
    protected void onPause() {
        preview.stopGPS();
        
        super.onPause();
    }

    @Override
    protected void onResume() {
        preview.startGPS();
        
        super.onResume();
    }
}