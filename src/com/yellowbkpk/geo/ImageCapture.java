package com.yellowbkpk.geo;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;

public class ImageCapture extends Activity {
    
    private Preview preview=null;
    private DrawOnTop draw;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        draw = new DrawOnTop(this);
        preview = new Preview(this, draw);
        setContentView(preview);
        addContentView(draw, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
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