package com.daimajia.slider.library.Zoom;

import android.graphics.RectF;
import android.view.View;
import android.view.ViewTreeObserver;

import com.github.chrisbanes.photoview.PhotoView;

/**
 * Opens a picture that is taller than its view zoomed in to the full width, showing its top, so
 * the reader starts at the beginning of the page at a readable size instead of at a whole page
 * shrunk to a sliver.
 * <p>
 * The case this exists for: a portrait page -- sheet music, a scanned hymn, a document -- in a
 * landscape view. Fitted whole, such a page occupies a narrow column in the middle of a wide
 * screen and every reader has to zoom in by hand before they can read a note of it.
 * <p>
 * <b>It is deliberately inert for everything else.</b> The picture is only scaled when fitting it
 * leaves its width short of the view's, which is exactly the tall-picture-in-a-wide-view case; a
 * landscape slide, or the same page held in portrait, already fills the width and is left alone.
 * So it can be attached unconditionally to a slider carrying pictures of mixed shapes.
 * <p>
 * The zoom is where the reader starts, not a cage: pinch, drag and double-tap all still work, and
 * double-tapping back down to the minimum shows the whole page as before. It re-applies when the
 * view changes size -- a rotation -- unless the reader has meanwhile zoomed for themselves, whose
 * choice then stands.
 * <p>
 * PhotoView is a {@code compileOnly} dependency of this library, so an app using this class must
 * declare {@code com.github.chrisbanes:PhotoView} itself.
 */
public final class FillWidthFromTop {

    /** How much headroom to leave above the fill-width scale, so the reader can still zoom in. */
    private static final float ZOOM_HEADROOM = 1.5f;

    private FillWidthFromTop() {
    }

    /**
     * Applies the behaviour to a PhotoView: once the picture is there, and again whenever the view
     * changes size.
     * <p>
     * The waiting is the whole difficulty. The picture is loaded asynchronously, so at the layout
     * pass there is usually nothing to scale yet -- and when it does arrive the view is not laid
     * out again, because its size has not changed. So the trigger is a pre-draw listener, which
     * fires on the frame the picture is first drawn in, and removes itself as soon as the zoom has
     * been applied.
     */
    public static void attach(final PhotoView photoView) {
        if (photoView == null) {
            return;
        }
        final ViewTreeObserver.OnPreDrawListener[] waiting = new ViewTreeObserver.OnPreDrawListener[1];
        waiting[0] = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (apply(photoView)) {
                    photoView.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                return true;
            }
        };
        // ...and the listener has to be registered once the view is in a window. A view that is
        // not attached yet -- which is exactly how a slide is built, inflated and handed to the
        // pager -- hands out a throwaway ViewTreeObserver, and anything registered on it is
        // silently dropped when the real one arrives.
        if (photoView.isAttachedToWindow()) {
            photoView.getViewTreeObserver().addOnPreDrawListener(waiting[0]);
        }
        photoView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                ViewTreeObserver observer = photoView.getViewTreeObserver();
                observer.removeOnPreDrawListener(waiting[0]);
                observer.addOnPreDrawListener(waiting[0]);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                photoView.getViewTreeObserver().removeOnPreDrawListener(waiting[0]);
            }
        });

        photoView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                boolean sizeChanged = (right - left) != (oldRight - oldLeft)
                        || (bottom - top) != (oldBottom - oldTop);
                if (!sizeChanged) {
                    return;
                }
                // A rotation: the picture is fitted afresh to the new shape, so start waiting for
                // the next draw again rather than scaling against the old measurements.
                ViewTreeObserver observer = photoView.getViewTreeObserver();
                observer.removeOnPreDrawListener(waiting[0]);
                observer.addOnPreDrawListener(waiting[0]);
            }
        });
    }

    /**
     * Zooms the picture to the view's width, anchored at the top, once. Does nothing if the
     * picture is not yet loaded, if fitting it already fills the width, or if the reader has
     * zoomed away from the fitted size themselves.
     *
     * @return true if the picture was zoomed.
     */
    public static boolean apply(PhotoView photoView) {
        if (photoView == null || photoView.getDrawable() == null) {
            return false;
        }
        int viewWidth = photoView.getWidth() - photoView.getPaddingLeft() - photoView.getPaddingRight();
        if (viewWidth <= 0) {
            return false;
        }
        RectF displayed = photoView.getDisplayRect();
        if (displayed == null || displayed.width() <= 0) {
            return false;
        }
        float currentScale = photoView.getScale();
        if (currentScale > photoView.getMinimumScale() * 1.01f) {
            return false;   // the reader has zoomed; leave their view of the page alone
        }
        float fittedWidth = displayed.width() / currentScale;
        float target = viewWidth / fittedWidth;
        if (target <= 1.01f) {
            return false;   // already as wide as the view: a landscape picture, or a portrait view
        }
        // PhotoView refuses a scale above its maximum, and its levels must stay ordered.
        if (photoView.getMaximumScale() < target * ZOOM_HEADROOM) {
            photoView.setScaleLevels(photoView.getMinimumScale(), target, target * ZOOM_HEADROOM);
        }
        // The focal point is the top edge: what sits there stays there while everything grows
        // around it, which is what puts the top of the page on screen. setScale() on its own
        // zooms about the centre of the view, and shows the middle of the page instead.
        photoView.setScale(target, viewWidth / 2f, 0f, false);
        return true;
    }
}
