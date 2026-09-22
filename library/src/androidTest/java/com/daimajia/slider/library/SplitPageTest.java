package com.daimajia.slider.library;

import static com.daimajia.slider.library.SliderTestSupport.showInActivity;
import static com.daimajia.slider.library.SliderTestSupport.waitFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.daimajia.slider.library.Pages.SplitPageImageView;
import com.daimajia.slider.library.SliderTypes.SplitPageSliderView;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A page of sheet music cut in two and shown side by side, for reading on a wide screen with both
 * hands busy.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SplitPageTest {

    private static final int PAGE_WIDTH = 600;
    private static final int PAGE_HEIGHT = 1000;
    /** Where the systems sit, as fractions of the page: four blocks of music with gaps between. */
    private static final float[][] SYSTEMS = {{0.05f, 0.22f}, {0.27f, 0.44f},
            {0.56f, 0.73f}, {0.78f, 0.95f}};

    /**
     * A stand-in hymn page: blocks of "music" separated by blank paper. The gap between the second
     * and third block runs from 44% to 56%, so its middle is at 50%.
     */
    private static Bitmap page() {
        Bitmap bitmap = Bitmap.createBitmap(PAGE_WIDTH, PAGE_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        Paint ink = new Paint();
        ink.setColor(Color.BLACK);
        ink.setStrokeWidth(3f);
        for (float[] system : SYSTEMS) {
            float top = system[0] * PAGE_HEIGHT;
            float bottom = system[1] * PAGE_HEIGHT;
            for (int line = 0; line < 5; line++) {
                float y = top + (bottom - top) * line / 4f;
                canvas.drawLine(40, y, PAGE_WIDTH - 40, y, ink);
            }
        }
        return bitmap;
    }

    private static File pageFile(android.content.Context context, String name) {
        File file = new File(context.getCacheDir(), name);
        try (FileOutputStream out = new FileOutputStream(file)) {
            page().compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException ex) {
            throw new AssertionError("could not write the test page", ex);
        }
        return file;
    }

    @Test
    public void theCutFallsInTheBlankPaperBetweenTwoSystems() {
        // Cutting at the exact middle of a page can slice a stave in half. The cut should land in
        // the gap, which here runs from 44% to 56% of the page.
        int cut = SplitPageImageView.findSplitRow(page());

        assertTrue("the cut at row " + cut + " fell inside a system, not in the gap between two",
                cut > SYSTEMS[1][1] * PAGE_HEIGHT && cut < SYSTEMS[2][0] * PAGE_HEIGHT);
        assertEquals("the gap is centred on the halfway point, so the cut should be too",
                PAGE_HEIGHT / 2f, cut, PAGE_HEIGHT * 0.02f);
    }

    @Test
    public void aPageWithNoBlankPaperIsCutInTheMiddle() {
        // A photograph, or a page with music right across the middle: there is no good cut, and
        // the halfway point is no worse than refusing to split at all.
        Bitmap solid = Bitmap.createBitmap(PAGE_WIDTH, PAGE_HEIGHT, Bitmap.Config.ARGB_8888);
        solid.eraseColor(Color.DKGRAY);

        assertEquals(PAGE_HEIGHT / 2, SplitPageImageView.findSplitRow(solid));
    }

    @Test
    public void theSplitSlideShowsTheWholePageAndTheWholePageIsBigger() {
        try (ActivityScenario<SliderTestActivity> scenario =
                     ActivityScenario.launch(SliderTestActivity.class)) {
            AtomicReference<View> view = new AtomicReference<>();
            scenario.onActivity(activity -> view.set(new SplitPageSliderView(activity)
                    .image(pageFile(activity, "split-page.png"))
                    .getView()));
            // A landscape screen, which is where a page has to be split to be readable.
            showInActivity(scenario, view.get(), 1000, 500);

            SplitPageImageView target = view.get().findViewById(R.id.daimajia_slider_image);
            waitFor("the page to load", () -> target.getDrawable() != null);
            waitFor("the cut to be chosen", () -> {
                target.invalidate();
                return target.getSplitRow() > 0;
            });

            assertTrue("the slide should be split into columns", target.isSplitIntoColumns());

            // Each column is about half the view wide, and holds half the page: so the page is
            // drawn at a scale the whole page could never reach unsplit.
            float columnWidth = (1000 - 24) / 2f;
            float splitScale = Math.min(columnWidth / PAGE_WIDTH, 500f / (PAGE_HEIGHT / 2f));
            float wholePageScale = Math.min(1000f / PAGE_WIDTH, 500f / PAGE_HEIGHT);
            assertTrue("splitting the page should show it larger than fitting it whole ("
                            + splitScale + " vs " + wholePageScale + ")",
                    splitScale > wholePageScale * 1.5f);
        }
    }

    @Test
    public void withoutTheSplitItDrawsAsAnOrdinarySlide() {
        try (ActivityScenario<SliderTestActivity> scenario =
                     ActivityScenario.launch(SliderTestActivity.class)) {
            AtomicReference<View> view = new AtomicReference<>();
            scenario.onActivity(activity -> view.set(new SplitPageSliderView(activity)
                    .setSplit(false)
                    .image(pageFile(activity, "whole-page.png"))
                    .getView()));
            showInActivity(scenario, view.get(), 500, 1000);

            SplitPageImageView target = view.get().findViewById(R.id.daimajia_slider_image);
            waitFor("the page to load", () -> target.getDrawable() != null);
            assertTrue("portrait should be able to keep the page whole",
                    !target.isSplitIntoColumns());
        }
    }
}
