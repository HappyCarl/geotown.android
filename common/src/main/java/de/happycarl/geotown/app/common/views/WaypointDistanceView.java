package de.happycarl.geotown.app.common.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.text.DecimalFormat;

/**
 * TODO: document your custom view class.
 */
public class WaypointDistanceView extends View {

    private int distance;

    float[] bearing = new float[50];
    float bearingValue;
    int bearingCounter = 0;
    boolean showCompass = true;

    private Paint circlePaint;
    private Paint circleHighlightPaint;

    private Paint compassPaint;

    private Paint centerPaint;
    private Paint centerHighlightPaint;

    private Paint textPaint;

    private float circleMaxDiameter = 0;
    private float centerX;
    private float centerY;

    DecimalFormat f = new DecimalFormat("##.00");
    Path compassTriangle = new Path();

    public WaypointDistanceView(Context context) {
        super(context);
        init(null, 0);
    }

    public WaypointDistanceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public WaypointDistanceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setColor(getResources().getColor(android.R.color.darker_gray));
        circlePaint.setStrokeWidth(10f);

        setColor(getResources().getColor(android.R.color.holo_red_light));

        centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        centerPaint.setColor(getResources().getColor(android.R.color.darker_gray));
        centerPaint.setStrokeWidth(10f);

        centerHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerHighlightPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        centerHighlightPaint.setColor(getResources().getColor(android.R.color.holo_green_light));
        centerHighlightPaint.setStrokeWidth(10f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(getResources().getColor(android.R.color.primary_text_light));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(80);

        Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boxPaint.setColor(getResources().getColor(android.R.color.background_light));
        boxPaint.setStyle(Paint.Style.FILL);
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int dist) {
        distance = dist;
        invalidate();
        requestLayout();
    }

    /*
    Sets the bearing that will be shown to the user
    The parameter has to be in radian, with 0 facing to device top and pi to bottom
     */
    public void setBearing(float bearing) {
        if(bearingCounter >= this.bearing.length) {
            System.arraycopy(this.bearing, 0, this.bearing, 1, this.bearing.length - 1);
            this.bearing[0] = bearing;
        } else {
            this.bearing[bearingCounter++] = (float) (bearing - Math.PI / 2);
        }

        this.bearingValue = 0;
        for(float b : this.bearing)
            bearingValue += b;
        bearingValue /= this.bearing.length;

        invalidate();
        requestLayout();
    }

    public boolean isShowCompass() {
        return showCompass;
    }

    public void setShowCompass(boolean show) {
        showCompass = show;
        invalidate();
        requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float compassRadius = 0;

        canvas.drawCircle(centerX, centerY, circleMaxDiameter / 35, (distance < 200) ? centerHighlightPaint : centerPaint);

        for (int i = 200; i <= 800; i += 200) {
            Paint paint = circlePaint;

            float radius = (circleMaxDiameter / 2) * ((float) i / 1000);
            if (distance >= i && distance < i + 200) {
                paint = circleHighlightPaint;

                compassRadius = radius;
            }
            canvas.drawCircle(centerX, centerY, radius, paint);
        }

        if(compassRadius == 0) {
            compassRadius = circleMaxDiameter/2;
        }

        canvas.drawCircle(centerX, centerY, circleMaxDiameter / 2, (distance >= 1000) ? circleHighlightPaint : circlePaint);

        if(showCompass) {
            //The following operations take place in a cartesian coordinate system assuming centerX,centerY as point of origin

            //Top of arrow
            int point1Dist = (int) (compassRadius + (circleMaxDiameter / 20));
            float point1Angle = bearingValue;

            //Bottom right of arrow
            int point2Dist = (int) compassRadius;
            float point2Angle = (float) (bearingValue + (Math.PI/40));

            //Bottom left of arrow
            int point3Dist = (int) compassRadius;
            float point3Angle = (float) (bearingValue - (Math.PI/40));

            compassTriangle.reset();
            compassTriangle.setFillType(Path.FillType.EVEN_ODD);
            //Top of arrow
            compassTriangle.moveTo((float) (point1Dist * Math.cos(point1Angle) + centerX), (float) (point1Dist * Math.sin(point1Angle) + centerY));
            //Down to bottom right
            compassTriangle.lineTo((float)( point2Dist * Math.cos(point2Angle) + centerX),(float) (point2Dist * Math.sin(point2Angle) + centerY));
            //To the left corner
            compassTriangle.lineTo((float)( point3Dist * Math.cos(point3Angle) + centerX),(float) (point3Dist * Math.sin(point3Angle) + centerY));
            compassTriangle.close();

            canvas.drawPath(compassTriangle, compassPaint);

        }

        if(distance < 1000) {
            canvas.drawText(distance + " m", centerX, (float) (centerY * 1.3), textPaint);
        } else {

            canvas.drawText(f.format((float)distance/1000) + " km", centerX, (float) (centerY * 1.3), textPaint);
        }


    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d("OnSizeChanged", "size: " + w + ":" + h + "; pos: " + getX() + ":" + getY());


        circleMaxDiameter = Math.min(w, h);
        circleMaxDiameter -= (circleMaxDiameter / 10);

        centerX = w / 2;
        centerY = h / 2;
        Log.d("OnSizeChanged", "circle " + centerX + ":" + centerY + " : " + circleMaxDiameter);
    }


    public void setColor(int rgb) {
        circleHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circleHighlightPaint.setStyle(Paint.Style.STROKE);
        circleHighlightPaint.setColor(rgb);
        circleHighlightPaint.setStrokeWidth(10f);

        compassPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        compassPaint.setStyle(Paint.Style.FILL);
        compassPaint.setColor(rgb);

        this.invalidate();
    }
}
