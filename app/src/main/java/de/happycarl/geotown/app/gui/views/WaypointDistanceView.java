package de.happycarl.geotown.app.gui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import de.happycarl.geotown.app.R;

/**
 * TODO: document your custom view class.
 */
public class WaypointDistanceView extends View {

    private int distance;

    private Paint circlePaint;
    private Paint circleHighlightPaint;

    private Paint centerPaint;
    private Paint centerHighlightPaint;

    private Paint boxPaint;
    private Paint textPaint;

    private float circleMaxDiameter = 0;
    private float centerX, centerY, textOffX;

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

        circleHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circleHighlightPaint.setStyle(Paint.Style.STROKE);
        circleHighlightPaint.setColor(getResources().getColor(android.R.color.holo_red_light));
        circleHighlightPaint.setStrokeWidth(10f);

        centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        centerPaint.setColor(getResources().getColor(android.R.color.darker_gray));
        centerPaint.setStrokeWidth(10f);

        centerHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerHighlightPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        centerHighlightPaint.setColor(getResources().getColor(android.R.color.holo_red_light));
        centerHighlightPaint.setStrokeWidth(10f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(getResources().getColor(android.R.color.primary_text_light));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(80);

        boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boxPaint.setColor(getResources().getColor(android.R.color.background_light));
        boxPaint.setStyle(Paint.Style.FILL);
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int dist) {
        distance = dist;
        String distStr = dist + " m";
        textOffX = textPaint.getTextSize() * (distStr.length() / 2);
        invalidate();
        requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawCircle(centerX, centerY, circleMaxDiameter / 35, (distance < 200)? centerHighlightPaint : centerPaint);

        for (int i = 200; i <= 800; i += 200) {
            Log.d("onDraw", "" + i + " : " + distance + " : " + (circleMaxDiameter / 2) * ((float)i / 1000));
            Paint paint = circlePaint;
            if(distance >= i && distance < i + 200)
                paint = circleHighlightPaint;
            canvas.drawCircle(centerX, centerY, (circleMaxDiameter / 2) * ((float)i / 1000), paint);
        }

        canvas.drawCircle(centerX, centerY, circleMaxDiameter / 2, (distance >= 1000)? circleHighlightPaint : circlePaint);



        canvas.drawText(distance + " m", centerX, (float) (centerY * 1.3), textPaint );

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d("OnSizeChanged", "size: " + w + ":" + h + "; pos: " + getX() + ":" + getY());


        circleMaxDiameter = Math.min(w,h);
        circleMaxDiameter -= (circleMaxDiameter / 10);

        centerX = w / 2;
        centerY = h / 2;
        Log.d("OnSizeChanged", "circle " + centerX + ":" + centerY + " : " + circleMaxDiameter);
    }


}
