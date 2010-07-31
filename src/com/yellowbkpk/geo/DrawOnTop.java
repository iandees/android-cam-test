package com.yellowbkpk.geo;

import java.text.NumberFormat;

import android.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.view.View;

public class DrawOnTop extends View {

//    private Bitmap star_on;
//    private Bitmap star_off;
    private NumberFormat nf = NumberFormat.getInstance();
    
    public volatile Location gpsLocation;
    public volatile float heading;

    public DrawOnTop(Context context) {
        super(context);
        nf.setMaximumIntegerDigits(3);
        nf.setMaximumFractionDigits(3);
//        star_on = BitmapFactory.decodeResource(getResources(), R.drawable.btn_star_big_on);
//        star_off = BitmapFactory.decodeResource(getResources(), R.drawable.btn_star_big_off);
    }

    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setShadowLayer(2, 0, 0, Color.WHITE);
        paint.setTextSize(20);
        if (gpsLocation == null) {
            canvas.drawText("Loc: n/a", 24, 30, paint);
        } else {
            canvas.drawText("Loc: " + nf.format(gpsLocation.getLatitude()) + ", "
                    + nf.format(gpsLocation.getLongitude()), 24, 30, paint);
        }
        
        canvas.drawText("Hdg: " + nf.format(heading), 300, 30, paint);

        super.draw(canvas);
    }
    
}
