package com.daimajia.slider.library;

import static com.daimajia.slider.library.SliderTestSupport.showInActivity;
import static com.daimajia.slider.library.SliderTestSupport.waitFor;
import static com.daimajia.slider.library.SliderTestSupport.writeTestImage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.graphics.Matrix;
import android.view.View;
import android.widget.ImageView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.daimajia.slider.library.SliderTypes.DefaultSliderView;
import com.daimajia.slider.library.SliderTypes.ZoomableSliderView;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class BaseSliderViewTest {

    /** Builds a slide around a real PNG and shows it, returning the inflated view. */
    private View showSlide(ActivityScenario<SliderTestActivity> scenario,
                           BaseSliderView.ScaleType scaleType,
                           BaseSliderView.ImageLoadListener listener,
                           int imageWidth, int imageHeight) {
        AtomicReference<View> view = new AtomicReference<>();
        scenario.onActivity(activity -> {
            File image = writeTestImage(activity, "slide-" + scaleType + ".png",
                    imageWidth, imageHeight);
            BaseSliderView slide = new DefaultSliderView(activity)
                    .image(image)
                    .setScaleType(scaleType);
            if (listener != null) {
                slide.setOnImageLoadListener(listener);
            }
            view.set(slide.getView());
        });
        showInActivity(scenario, view.get(), 600, 400);
        return view.get();
    }

    @Test
    public void aSuccessfulLoadTellsTheListener() {
        // onEnd(false, ...) fired on failure and nothing fired on success, so no caller could
        // ever learn that a picture had actually arrived.
        try (ActivityScenario<SliderTestActivity> scenario =
                     ActivityScenario.launch(SliderTestActivity.class)) {
            AtomicBoolean started = new AtomicBoolean();
            AtomicBoolean succeeded = new AtomicBoolean();
            showSlide(scenario, BaseSliderView.ScaleType.Fit, new BaseSliderView.ImageLoadListener() {
                @Override
                public void onStart(BaseSliderView target) {
                    started.set(true);
                }

                @Override
                public void onEnd(boolean result, BaseSliderView target) {
                    succeeded.set(result);
                }
            }, 40, 20);

            waitFor("the load to be reported as a success", succeeded::get);
            assertTrue("onStart never fired", started.get());
        }
    }

    @Test
    public void theFitCenterCropScaleTypeReachesTheImageView() {
        // FitCenterCrop was in the enum from the start and missing from the switch, so asking for
        // it did nothing whatsoever.
        try (ActivityScenario<SliderTestActivity> scenario =
                     ActivityScenario.launch(SliderTestActivity.class)) {
            View slide = showSlide(scenario, BaseSliderView.ScaleType.FitCenterCrop, null, 40, 20);
            ImageView image = slide.findViewById(R.id.daimajia_slider_image);
            assertEquals(ImageView.ScaleType.CENTER_CROP, image.getScaleType());
        }
    }

    @Test
    public void theFitWidthScaleTypeMatchesTheViewWidthExactly() {
        try (ActivityScenario<SliderTestActivity> scenario =
                     ActivityScenario.launch(SliderTestActivity.class)) {
            View slide = showSlide(scenario, BaseSliderView.ScaleType.FitWidth, null, 40, 20);
            ImageView image = slide.findViewById(R.id.daimajia_slider_image);

            waitFor("the picture to be drawn", () -> image.getDrawable() != null);
            assertEquals(ImageView.ScaleType.MATRIX, image.getScaleType());

            float[] values = new float[9];
            image.getImageMatrix().getValues(values);
            float scaledWidth = image.getDrawable().getIntrinsicWidth() * values[Matrix.MSCALE_X];
            assertEquals("the picture should span the view's width",
                    image.getWidth(), scaledWidth, 1.0f);
        }
    }

    @Test
    public void theZoomableSlideInflatesAPhotoViewAndLoadsIntoIt() {
        // PhotoView is a compileOnly dependency of the library; the test APK declares it, which
        // is exactly what a consuming app has to do.
        try (ActivityScenario<SliderTestActivity> scenario =
                     ActivityScenario.launch(SliderTestActivity.class)) {
            AtomicReference<View> view = new AtomicReference<>();
            scenario.onActivity(activity -> {
                File image = writeTestImage(activity, "zoomable.png", 40, 20);
                view.set(new ZoomableSliderView(activity).image(image).getView());
            });
            showInActivity(scenario, view.get(), 600, 400);

            View target = view.get().findViewById(R.id.daimajia_slider_image);
            assertNotNull("the zoomable slide has no image view", target);
            assertTrue("the zoomable slide did not inflate a PhotoView",
                    target instanceof com.github.chrisbanes.photoview.PhotoView);
            waitFor("the picture to load into the PhotoView",
                    () -> ((ImageView) target).getDrawable() != null);
        }
    }
}
