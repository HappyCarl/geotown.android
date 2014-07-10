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

    private float circleMaxDiameter = 0;
    private float centerX, centerY;

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
        distance = attrs.getAttributeIntValue(R.styleable.WaypointDistanceView_distance, 0);

        circlePaint = new Paint(0);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setColor(0xff303030);
        circlePaint.setStrokeWidth(10f);

        circleHighlightPaint = new Paint(0);
        circleHighlightPaint.setStyle(Paint.Style.STROKE);
        circleHighlightPaint.setColor(0xffff0000);
        circleHighlightPaint.setStrokeWidth(10f);
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int dist) {
        distance = dist;
        invalidate();
        requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (int i = 200; i <= 600; i += 200) {
            Log.d("onDraw", "" + i + " : " + distance);
            Paint paint = circlePaint;
            if(distance > i)
                paint = circleHighlightPaint;
            canvas.drawCircle(centerX, centerY, (circleMaxDiameter / 2) * (i / 600), paint);
        }


    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d("OnSizeChanged", "size: " + w + ":" + h);
        float realWidth = w - (getPaddingLeft() + getPaddingRight());
        float realHeight = h - (getPaddingBottom() + getPaddingTop());

        circleMaxDiameter = Math.min(realWidth,realHeight);
        circleMaxDiameter -= (circleMaxDiameter / 10);

        centerX = getX() + realWidth / 2;
        centerY = getY() + realHeight / 2;
        Log.d("OnSizeChanged", "circle " + centerX + ":" + centerY + " : " + circleMaxDiameter);
    }


}
