package com.snipsystems.irfanmulic.tinycompass;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;

import com.snipsystems.irfanmulic.model.Person;

public class Compass extends View {
    private static final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int width = 0;
    private int height = 0;
    private Matrix matrix; // to manage rotation of the compass view
    private Bitmap bitmap;
    private float bearing; // rotation angle to North
    private float angle_difference; // angle difference from subject to north
    private Person personFound = null;

    public Compass(Context context) {
        super(context);
        initialize();
    }

    public Compass(Context context, AttributeSet attr) {
        super(context, attr);
        initialize();
    }

    private void initialize() {
        matrix = new Matrix();
        // create bitmap for compass icon
        bitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.location_north);
    }

    public void setBearing(float b) {
        bearing = b;
    }

    public void setAngleDifference(float b) { angle_difference = b;}

    public void setPersonFound(Person person) { this.personFound = person;}

    public void setCompassVisibility (int visibility){this.setVisibility(visibility);}

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();



        if (bitmapWidth > canvasWidth || bitmapHeight > canvasHeight) {
            // resize bitmap to fit in canvas
            bitmap = Bitmap.createScaledBitmap(bitmap,
                    (int) (bitmapWidth * 0.85), (int) (bitmapHeight * 0.85), true);
        }


        // center
        int bitmapX = bitmap.getWidth() / 2;
        int bitmapY = bitmap.getHeight() / 2;
        int parentX = width / 2;
        int parentY = height / 4;
        int centerX = parentX - bitmapX;
        int centerY = parentY - bitmapY;

        canvas.drawText( String.format("%4.2f",angle_difference), parentX,parentY-70,paint);
        canvas.drawText( personFound.toString(), 0,parentY-80,paint);

        // calculate rotation angle
        int rotation = (int) (360 - bearing);

        // reset matrix
        matrix.reset();
        matrix.setRotate(rotation, bitmapX, bitmapY);
        // center bitmap on canvas
        matrix.postTranslate(centerX, centerY);
        // draw bitmap
        canvas.drawBitmap(bitmap, matrix, paint);
    }
}
