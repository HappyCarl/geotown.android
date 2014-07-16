package de.happycarl.geotown.app.gui.views;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ImageView;

/**
 * Created by jhbruhn on 20.06.14.
 */
public class FadingImageView extends ImageView {
    private FadeSide mFadeSide;

    private final Context c;

    public enum FadeSide {
        TOP_SIDE, BOTTOM_SIDE
    }

    public FadingImageView(Context c, AttributeSet attrs, int defStyle) {
        super(c, attrs, defStyle);

        this.c = c;

        init();
    }

    public FadingImageView(Context c, AttributeSet attrs) {
        super(c, attrs);

        this.c = c;

        init();
    }

    public FadingImageView(Context c) {
        super(c);

        this.c = c;

        init();
    }

    private void init() {
        // Enable vertical fading
        this.setVerticalFadingEdgeEnabled(true);
        // Apply default fading length
        this.setEdgeLength(28);
        // Apply default side
        this.setFadeDirection(FadeSide.TOP_SIDE);
    }

    public void setFadeDirection(FadeSide side) {
        this.mFadeSide = side;
    }

    public void setEdgeLength(int length) {
        this.setFadingEdgeLength(getPixels(length));
    }

    @Override
    protected float getTopFadingEdgeStrength() {
        return mFadeSide.equals(FadeSide.TOP_SIDE) ? 1.0f : 0.0f;
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        return mFadeSide.equals(FadeSide.BOTTOM_SIDE) ? 1.0f : 0.0f;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return true;
    }

    @Override
    public boolean onSetAlpha(int alpha) {
        return false;
    }

    private int getPixels(int dipValue) {
        Resources r = c.getResources();

        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dipValue, r.getDisplayMetrics());
    }
}
