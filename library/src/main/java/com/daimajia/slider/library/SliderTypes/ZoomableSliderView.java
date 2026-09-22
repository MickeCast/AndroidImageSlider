package com.daimajia.slider.library.SliderTypes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.daimajia.slider.library.R;
import com.daimajia.slider.library.Zoom.FillWidthFromTop;
import com.github.chrisbanes.photoview.PhotoView;

/**
 * A slide the user can pinch to zoom and drag around, for pictures that carry detail worth a
 * closer look -- sheet music, lyric slides, a scanned page.
 * <p>
 * <b>PhotoView is a {@code compileOnly} dependency of this library</b>, so it is NOT pulled in by
 * depending on the slider. An app that uses this class must declare it itself:
 * <pre>
 * implementation 'com.github.chrisbanes:PhotoView:2.3.0'
 * </pre>
 * An app that never touches this class needs nothing: the class is only loaded when it is first
 * named, so its absence costs the rest of the library nothing.
 * <p>
 * Zooming and paging share the same gestures, and PhotoView resolves that itself: it asks its
 * parent not to intercept while the picture is zoomed in, and releases it again at the edges, so
 * a swipe still moves to the next slide once you are back to fit. {@link
 * com.daimajia.slider.library.SliderLayout#setSwipeEnabled(boolean)} turns paging off entirely if
 * you would rather drive the slides yourself.
 */
public class ZoomableSliderView extends BaseSliderView {

    private float mMaximumScale = 4.0f;
    private boolean mFillWidthFromTop;

    /**
     * Where a picture taller than the view starts: fitted whole (the default), or zoomed to the
     * view's width showing its top. See {@link FillWidthFromTop} -- it is inert for a picture that
     * already fills the width, so it is safe on a slider of mixed shapes.
     */
    public ZoomableSliderView fillWidthFromTop(boolean fill) {
        mFillWidthFromTop = fill;
        return this;
    }

    public ZoomableSliderView(Context context) {
        super(context);
        // A zoomable slide keeps the picture at its own size by default: the other scale types
        // decode it at the size of the view, and zooming into that only magnifies what is left.
        setScaleType(ScaleType.Natural);
    }

    /**
     * How far in the user may zoom, as a multiple of the fitted size. PhotoView's own default is
     * 3; this class asks for 4, which is enough to read small print on a phone.
     */
    public ZoomableSliderView setMaximumScale(float maximumScale) {
        mMaximumScale = maximumScale;
        return this;
    }

    @Override
    public View getView() {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.render_type_zoomable, null);
        PhotoView target = v.findViewById(R.id.daimajia_slider_image);
        target.setMaximumScale(mMaximumScale);
        bindEventAndShow(v, target);
        if (mFillWidthFromTop) {
            FillWidthFromTop.attach(target);
        }
        return v;
    }
}
