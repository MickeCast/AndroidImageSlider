package com.daimajia.slider.library;

import static org.junit.Assert.fail;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Small helpers shared by the suite. */
final class SliderTestSupport {

    private SliderTestSupport() {
    }

    interface Condition {
        boolean isMet();
    }

    /** Polls on the instrumentation thread until the condition holds, or fails after 5 seconds. */
    static void waitFor(String what, Condition condition) {
        long deadline = SystemClock.uptimeMillis() + 5000;
        while (SystemClock.uptimeMillis() < deadline) {
            final AtomicBoolean met = new AtomicBoolean();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(
                    () -> met.set(condition.isMet()));
            if (met.get()) {
                return;
            }
            SystemClock.sleep(50);
        }
        fail("timed out waiting for " + what);
    }

    /** Lets the main thread run for a while, for the cases that assert nothing happens. */
    static void settle(long millis) {
        SystemClock.sleep(millis);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    /** A real PNG on disk, so Picasso has something to load without a network. */
    static File writeTestImage(Activity activity, String name, int width, int height) {
        File file = new File(activity.getCacheDir(), name);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.RED);
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException ex) {
            throw new AssertionError("could not write the test image", ex);
        }
        return file;
    }

    /** Adds a view to the test activity's root and waits until it has been laid out. */
    static void showInActivity(ActivityScenario<SliderTestActivity> scenario, View view,
                               int widthPx, int heightPx) {
        scenario.onActivity(activity ->
                activity.root.addView(view, new ViewGroup.LayoutParams(widthPx, heightPx)));
        waitFor("the view to be laid out", () -> view.getWidth() > 0 && view.getHeight() > 0);
    }

    static MotionEvent motion(int action, float x, float y) {
        long now = SystemClock.uptimeMillis();
        return MotionEvent.obtain(now, now, action, x, y, 0);
    }

    static <T> T onActivity(ActivityScenario<SliderTestActivity> scenario,
                            ValueProvider<T> provider) {
        AtomicReference<T> value = new AtomicReference<>();
        scenario.onActivity(activity -> value.set(provider.get(activity)));
        return value.get();
    }

    interface ValueProvider<T> {
        T get(SliderTestActivity activity);
    }
}
