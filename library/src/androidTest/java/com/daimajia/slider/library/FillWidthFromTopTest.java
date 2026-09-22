package com.daimajia.slider.library;

import static com.daimajia.slider.library.SliderTestSupport.showInActivity;
import static com.daimajia.slider.library.SliderTestSupport.waitFor;
import static com.daimajia.slider.library.SliderTestSupport.writeTestImage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.RectF;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.daimajia.slider.library.SliderTypes.ZoomableSliderView;
import com.github.chrisbanes.photoview.PhotoView;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A portrait page in a landscape view: fitted whole it is a narrow column in the middle of a wide
 * screen, which is unreadable without zooming in by hand every time.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class FillWidthFromTopTest {

    /** A wide view, as a landscape phone would give. */
    private static final int VIEW_WIDTH = 1000;
    private static final int VIEW_HEIGHT = 500;

    private PhotoView showPage(ActivityScenario<SliderTestActivity> scenario,
                               String name, int imageWidth, int imageHeight, boolean fillWidth) {
        AtomicReference<View> view = new AtomicReference<>();
        scenario.onActivity(activity -> {
            File image = writeTestImage(activity, name, imageWidth, imageHeight);
            view.set(new ZoomableSliderView(activity)
                    .fillWidthFromTop(fillWidth)
                    .image(image)
                    .getView());
        });
        showInActivity(scenario, view.get(), VIEW_WIDTH, VIEW_HEIGHT);
        PhotoView photo = view.get().findViewById(R.id.daimajia_slider_image);
        waitFor("the page to load", () -> photo.getDrawable() != null);
        return photo;
    }

    @Test
    public void aTallPageInAWideViewOpensFilledToTheWidthAndShowingItsTop() {
        try (ActivityScenario<SliderTestActivity> scenario =
                     ActivityScenario.launch(SliderTestActivity.class)) {
            // 400x800: twice as tall as it is wide, like a sheet of music.
            PhotoView photo = showPage(scenario, "tall-page.png", 400, 800, true);

            waitFor("the page to be zoomed to the width",
                    () -> photo.getScale() > photo.getMinimumScale() * 1.01f);

            RectF shown = photo.getDisplayRect();
            assertEquals("the page should span the view's width",
                    VIEW_WIDTH, shown.width(), 2f);
            assertEquals("the top of the page should be at the top of the view",
                    0f, shown.top, 2f);
            assertTrue("the page should overflow the view, leaving something to scroll to",
                    shown.height() > VIEW_HEIGHT);
        }
    }

    @Test
    public void theSamePageIsLeftAloneWhenItAlreadyFillsTheWidth() {
        // A landscape picture in the same landscape view: fitting it already uses the full width,
        // so there is nothing to do and the whole picture stays visible.
        try (ActivityScenario<SliderTestActivity> scenario =
                     ActivityScenario.launch(SliderTestActivity.class)) {
            PhotoView photo = showPage(scenario, "wide-page.png", 800, 400, true);
            SliderTestSupport.settle(400);

            assertEquals("a wide picture should not be zoomed at all",
                    photo.getMinimumScale(), photo.getScale(), 0.01f);
            RectF shown = photo.getDisplayRect();
            assertTrue("the whole picture should still be visible",
                    shown.height() <= VIEW_HEIGHT + 2f);
        }
    }

    @Test
    public void withoutTheOptionATallPageStillOpensFittedWhole() {
        try (ActivityScenario<SliderTestActivity> scenario =
                     ActivityScenario.launch(SliderTestActivity.class)) {
            PhotoView photo = showPage(scenario, "tall-page-plain.png", 400, 800, false);
            SliderTestSupport.settle(400);

            assertEquals("the default must not change: the page opens fitted",
                    photo.getMinimumScale(), photo.getScale(), 0.01f);
        }
    }

    @Test
    public void aPageTheReaderHasZoomedIsLeftWhereTheyPutIt() {
        // The zoom is a starting point, not a cage: re-applying must not yank the page back from
        // wherever the reader has scrolled and zoomed it to.
        try (ActivityScenario<SliderTestActivity> scenario =
                     ActivityScenario.launch(SliderTestActivity.class)) {
            PhotoView photo = showPage(scenario, "tall-page-zoomed.png", 400, 800, true);
            waitFor("the initial zoom", () -> photo.getScale() > photo.getMinimumScale() * 1.01f);

            scenario.onActivity(activity -> photo.setScale(photo.getMaximumScale(), false));
            float readersScale = photo.getScale();

            scenario.onActivity(activity -> com.daimajia.slider.library.Zoom.FillWidthFromTop.apply(photo));
            SliderTestSupport.settle(200);

            assertEquals("the reader's own zoom was overridden",
                    readersScale, photo.getScale(), 0.01f);
        }
    }
}
