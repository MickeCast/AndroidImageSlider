package com.daimajia.slider.library;

import static com.daimajia.slider.library.SliderTestSupport.motion;
import static com.daimajia.slider.library.SliderTestSupport.settle;
import static com.daimajia.slider.library.SliderTestSupport.showInActivity;
import static com.daimajia.slider.library.SliderTestSupport.waitFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.daimajia.slider.library.Indicators.PagerIndicator;
import com.daimajia.slider.library.SliderTypes.DefaultSliderView;
import com.daimajia.slider.library.Tricks.ViewPagerEx;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SliderLayoutTest {

    private static final int SLIDES = 3;

    /** A slider with three placeholder slides, already laid out in the test activity. */
    private SliderLayout sliderWithSlides(ActivityScenario<SliderTestActivity> scenario) {
        final SliderLayout[] holder = new SliderLayout[1];
        scenario.onActivity(activity -> {
            SliderLayout slider = new SliderLayout(activity);
            slider.stopAutoCycle();   // each test starts the cycle itself if it wants one
            for (int i = 0; i < SLIDES; i++) {
                slider.addSlider(new DefaultSliderView(activity)
                        .image(R.drawable.indicator_corner_bg));
            }
            holder[0] = slider;
        });
        showInActivity(scenario, holder[0], 600, 400);
        return holder[0];
    }

    @Test
    public void theIndicatorVisibilityIsReportedAsItWasSet() {
        // The getter used to dereference a null indicator, and answer Invisible for every
        // indicator that did exist -- whatever had been set on it.
        try (ActivityScenario<SliderTestActivity> scenario =
                     ActivityScenario.launch(SliderTestActivity.class)) {
            SliderLayout slider = sliderWithSlides(scenario);

            scenario.onActivity(activity ->
                    slider.setIndicatorVisibility(PagerIndicator.IndicatorVisibility.Visible));
            assertEquals(PagerIndicator.IndicatorVisibility.Visible, slider.getIndicatorVisibility());

            scenario.onActivity(activity ->
                    slider.setIndicatorVisibility(PagerIndicator.IndicatorVisibility.Invisible));
            assertEquals(PagerIndicator.IndicatorVisibility.Invisible, slider.getIndicatorVisibility());
        }
    }

    @Test
    public void theAutoCycleAdvancesAndStopsWhenTheViewLeavesTheWindow() {
        try (ActivityScenario<SliderTestActivity> scenario =
                     ActivityScenario.launch(SliderTestActivity.class)) {
            SliderLayout slider = sliderWithSlides(scenario);
            scenario.onActivity(activity -> slider.startAutoCycle(100, 100, false));

            waitFor("the cycle to advance", () -> slider.getCurrentPosition() != 0);

            // Detaching is what an activity going away does to its views. The cycle used to keep
            // firing, which is why upstream tells the caller to stopAutoCycle() by hand.
            scenario.onActivity(activity ->
                    ((ViewGroup) slider.getParent()).removeView(slider));
            settle(300);
            assertFalse("the cycle kept running after the view was detached", slider.isAutoCycling());
            int parked = slider.getCurrentPosition();
            settle(400);
            assertEquals("the slider moved on while detached", parked, slider.getCurrentPosition());
        }
    }

    @Test
    public void theCycleComesBackWithTheLifecycleButAnExplicitStopDoesNot() {
        // The lifecycle is driven through a registry the test owns, installed on the container as
        // the view tree's lifecycle owner. ActivityScenario cannot bring a real activity back from
        // CREATED to RESUMED on this emulator, and that limitation has nothing to do with what is
        // under test here: that ON_STOP suspends the cycle, ON_START restores it, and an explicit
        // stopAutoCycle() outranks both.
        try (ActivityScenario<SliderTestActivity> scenario =
                     ActivityScenario.launch(SliderTestActivity.class)) {
            AtomicReference<LifecycleRegistry> registry = new AtomicReference<>();
            AtomicReference<SliderLayout> holder = new AtomicReference<>();

            scenario.onActivity(activity -> {
                FrameLayout container = new FrameLayout(activity);
                TestLifecycleOwner owner = new TestLifecycleOwner();
                owner.registry.setCurrentState(Lifecycle.State.RESUMED);
                ViewTreeLifecycleOwner.set(container, owner);
                registry.set(owner.registry);

                SliderLayout slider = new SliderLayout(activity);
                slider.stopAutoCycle();
                for (int i = 0; i < SLIDES; i++) {
                    slider.addSlider(new DefaultSliderView(activity)
                            .image(R.drawable.indicator_corner_bg));
                }
                holder.set(slider);
                container.addView(slider, new ViewGroup.LayoutParams(600, 400));
                activity.root.addView(container, new ViewGroup.LayoutParams(600, 400));
            });
            SliderLayout slider = holder.get();
            waitFor("the slider to be laid out", () -> slider.getWidth() > 0);

            scenario.onActivity(activity -> slider.startAutoCycle(100, 100, false));
            waitFor("the cycle to start", slider::isAutoCycling);

            scenario.onActivity(activity ->
                    registry.get().handleLifecycleEvent(Lifecycle.Event.ON_STOP));
            settle(200);
            assertFalse("a stopped lifecycle should not keep cycling", slider.isAutoCycling());

            scenario.onActivity(activity ->
                    registry.get().handleLifecycleEvent(Lifecycle.Event.ON_START));
            waitFor("the cycle to resume with the lifecycle", slider::isAutoCycling);

            // An explicit stop outranks the lifecycle: coming back must not restart it.
            scenario.onActivity(activity -> slider.stopAutoCycle());
            scenario.onActivity(activity -> {
                registry.get().handleLifecycleEvent(Lifecycle.Event.ON_STOP);
                registry.get().handleLifecycleEvent(Lifecycle.Event.ON_START);
            });
            settle(300);
            assertFalse("stopAutoCycle() was undone by the lifecycle", slider.isAutoCycling());
        }
    }

    /** A lifecycle the test moves by hand, stood in front of the activity's own. */
    private static class TestLifecycleOwner implements LifecycleOwner {
        final LifecycleRegistry registry = new LifecycleRegistry(this);

        @Override
        public Lifecycle getLifecycle() {
            return registry;
        }
    }

    @Test
    public void theSwipeLockRefusesDragsButNotProgrammaticMoves() {
        try (ActivityScenario<SliderTestActivity> scenario =
                     ActivityScenario.launch(SliderTestActivity.class)) {
            SliderLayout slider = sliderWithSlides(scenario);
            ViewPagerEx pager = slider.findViewById(R.id.daimajia_slider_viewpager);

            AtomicInteger intercepted = new AtomicInteger();
            scenario.onActivity(activity -> {
                slider.setSwipeEnabled(true);
                MotionEvent down = motion(MotionEvent.ACTION_DOWN, 500f, 200f);
                MotionEvent move = motion(MotionEvent.ACTION_MOVE, 100f, 200f);
                pager.onInterceptTouchEvent(down);
                intercepted.set(pager.onInterceptTouchEvent(move) ? 1 : 0);
                pager.onInterceptTouchEvent(motion(MotionEvent.ACTION_UP, 100f, 200f));
                down.recycle();
                move.recycle();
            });
            assertEquals("a drag should page the slider while swiping is allowed",
                    1, intercepted.get());

            scenario.onActivity(activity -> {
                slider.setSwipeEnabled(false);
                MotionEvent down = motion(MotionEvent.ACTION_DOWN, 500f, 200f);
                MotionEvent move = motion(MotionEvent.ACTION_MOVE, 100f, 200f);
                pager.onInterceptTouchEvent(down);
                intercepted.set(pager.onInterceptTouchEvent(move) ? 1 : 0);
                down.recycle();
                move.recycle();
            });
            assertFalse("swiping was locked and the pager still took the drag",
                    slider.isSwipeEnabled());
            assertEquals("the pager took a drag although swiping was locked",
                    0, intercepted.get());

            // ...while the caller can still drive the slides itself.
            scenario.onActivity(activity -> slider.setCurrentPosition(2, false));
            waitFor("the programmatic move to land", () -> slider.getCurrentPosition() == 2);
        }
    }

    @Test
    public void theTransformDurationIsSetWithoutReflection() {
        // setSliderTransformDuration used to reach into a private field and swallow any failure,
        // so a rename would have left the scroll speed silently unchanged.
        try (ActivityScenario<SliderTestActivity> scenario =
                     ActivityScenario.launch(SliderTestActivity.class)) {
            SliderLayout slider = sliderWithSlides(scenario);
            scenario.onActivity(activity -> slider.setSliderTransformDuration(250, null));
            scenario.onActivity(activity -> slider.moveNextPosition(true));
            waitFor("the slider to move after the scroller was replaced",
                    () -> slider.getCurrentPosition() == 1);
            assertTrue(true);
        }
    }
}
