package com.daimajia.slider.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.viewpager.widget.PagerAdapter;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.RelativeLayout;

import com.daimajia.slider.library.Animations.BaseAnimationInterface;
import com.daimajia.slider.library.Indicators.PagerIndicator;
import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.daimajia.slider.library.Transformers.AccordionTransformer;
import com.daimajia.slider.library.Transformers.BackgroundToForegroundTransformer;
import com.daimajia.slider.library.Transformers.BaseTransformer;
import com.daimajia.slider.library.Transformers.CubeInTransformer;
import com.daimajia.slider.library.Transformers.DefaultTransformer;
import com.daimajia.slider.library.Transformers.DepthPageTransformer;
import com.daimajia.slider.library.Transformers.FadeTransformer;
import com.daimajia.slider.library.Transformers.FlipHorizontalTransformer;
import com.daimajia.slider.library.Transformers.FlipPageViewTransformer;
import com.daimajia.slider.library.Transformers.ForegroundToBackgroundTransformer;
import com.daimajia.slider.library.Transformers.RotateDownTransformer;
import com.daimajia.slider.library.Transformers.RotateUpTransformer;
import com.daimajia.slider.library.Transformers.StackTransformer;
import com.daimajia.slider.library.Transformers.TabletTransformer;
import com.daimajia.slider.library.Transformers.ZoomInTransformer;
import com.daimajia.slider.library.Transformers.ZoomOutSlideTransformer;
import com.daimajia.slider.library.Transformers.ZoomOutTransformer;
import com.daimajia.slider.library.Tricks.FixedSpeedScroller;
import com.daimajia.slider.library.Tricks.InfinitePagerAdapter;
import com.daimajia.slider.library.Tricks.InfiniteViewPager;
import com.daimajia.slider.library.Tricks.ViewPagerEx;

/**
 * SliderLayout is compound layout. This is combined with {@link com.daimajia.slider.library.Indicators.PagerIndicator}
 * and {@link com.daimajia.slider.library.Tricks.ViewPagerEx} .
 *
 * There is some properties you can set in XML:
 *
 * indicator_visibility
 *      visible
 *      invisible
 *
 * indicator_shape
 *      oval
 *      rect
 *
 * indicator_selected_color
 *
 * indicator_unselected_color
 *
 * indicator_selected_drawable
 *
 * indicator_unselected_drawable
 *
 * pager_animation
 *      Default
 *      Accordion
 *      Background2Foreground
 *      CubeIn
 *      DepthPage
 *      Fade
 *      FlipHorizontal
 *      FlipPage
 *      Foreground2Background
 *      RotateDown
 *      RotateUp
 *      Stack
 *      Tablet
 *      ZoomIn
 *      ZoomOutSlide
 *      ZoomOut
 *
 * pager_animation_span
 *
 *
 */
public class SliderLayout extends RelativeLayout{

    private Context mContext;
    /**
     * InfiniteViewPager is extended from ViewPagerEx. As the name says, it can scroll without bounder.
     */
    private InfiniteViewPager mViewPager;

    /**
     * InfiniteViewPager adapter.
     */
    private SliderAdapter mSliderAdapter;

    /**
     * {@link com.daimajia.slider.library.Tricks.ViewPagerEx} indicator.
     */
    private PagerIndicator mIndicator;


    /**
     * The auto cycle runs on the main thread's Handler rather than on a {@link java.util.Timer}.
     * The Timer version started a thread per cycle, and every call to startAutoCycle() created
     * another one; it also hopped back to the main thread through a Handler built with no Looper,
     * which has been deprecated since API 30. Same behaviour, no threads.
     */
    private final Handler mCycleHandler = new Handler(Looper.getMainLooper());

    private final Runnable mCycleRunnable = new Runnable() {
        @Override
        public void run() {
            moveNextPosition(true);
            mCycleHandler.postDelayed(this, mSliderDuration);
        }
    };

    /**
     * For resuming the cycle, after user touch or click the {@link com.daimajia.slider.library.Tricks.ViewPagerEx}.
     */
    private final Runnable mResumingRunnable = new Runnable() {
        @Override
        public void run() {
            startAutoCycle();
        }
    };

    /**
     * Set while the cycle is suspended because the view left the window or its lifecycle owner
     * stopped, so that coming back can tell "paused by the system" from "stopped by the caller".
     */
    private boolean mPausedBySystem;

    private LifecycleOwner mObservedLifecycleOwner;

    /**
     * If {@link com.daimajia.slider.library.Tricks.ViewPagerEx} is Cycling
     */
    private boolean mCycling;

    /**
     * Determine if auto recover after user touch the {@link com.daimajia.slider.library.Tricks.ViewPagerEx}
     */
    private boolean mAutoRecover = true;

    private int mTransformerId;

    /**
     * {@link com.daimajia.slider.library.Tricks.ViewPagerEx} transformer time span.
     */
    private int mTransformerSpan = 1100;

    private boolean mAutoCycle;

    /**
     * the duration between animation.
     */
    private long mSliderDuration = 4000;

    /**
     * Visibility of {@link com.daimajia.slider.library.Indicators.PagerIndicator}
     */
    private PagerIndicator.IndicatorVisibility mIndicatorVisibility = PagerIndicator.IndicatorVisibility.Visible;

    /**
     * {@link com.daimajia.slider.library.Tricks.ViewPagerEx} 's transformer
     */
    private BaseTransformer mViewPagerTransformer;

    /**
     * @see com.daimajia.slider.library.Animations.BaseAnimationInterface
     */
    private BaseAnimationInterface mCustomAnimation;

    /**
     * {@link com.daimajia.slider.library.Indicators.PagerIndicator} shape, rect or oval.
     */

    public SliderLayout(Context context) {
        this(context,null);
    }

    public SliderLayout(Context context, AttributeSet attrs) {
        this(context,attrs,R.attr.SliderStyle);
    }

    public SliderLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.slider_layout, this, true);

        final TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs,R.styleable.SliderLayout,
                defStyle,0);

        mTransformerSpan = attributes.getInteger(R.styleable.SliderLayout_pager_animation_span, 1100);
        mTransformerId = attributes.getInt(R.styleable.SliderLayout_pager_animation, Transformer.Default.ordinal());
        mAutoCycle = attributes.getBoolean(R.styleable.SliderLayout_auto_cycle,true);
        boolean swipeEnabled = attributes.getBoolean(R.styleable.SliderLayout_swipe_enabled,true);
        int visibility = attributes.getInt(R.styleable.SliderLayout_indicator_visibility,0);
        for(PagerIndicator.IndicatorVisibility v: PagerIndicator.IndicatorVisibility.values()){
            if(v.ordinal() == visibility){
                mIndicatorVisibility = v;
                break;
            }
        }
        mSliderAdapter = new SliderAdapter(mContext);
        PagerAdapter wrappedAdapter = new InfinitePagerAdapter(mSliderAdapter);

        mViewPager = (InfiniteViewPager)findViewById(R.id.daimajia_slider_viewpager);
        mViewPager.setAdapter(wrappedAdapter);

        mViewPager.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                     case MotionEvent.ACTION_UP:
                        recoverCycle();
                        break;
                }
                return false;
            }
        });

        attributes.recycle();
        setPresetIndicator(PresetIndicators.Center_Bottom);
        setPresetTransformer(mTransformerId);
        setSliderTransformDuration(mTransformerSpan,null);
        setIndicatorVisibility(mIndicatorVisibility);
        setSwipeEnabled(swipeEnabled);
        if(mAutoCycle){
            startAutoCycle();
        }
    }

    public void addOnPageChangeListener(ViewPagerEx.OnPageChangeListener onPageChangeListener){
        if(onPageChangeListener != null){
            mViewPager.addOnPageChangeListener(onPageChangeListener);
        }
    }

    public void removeOnPageChangeListener(ViewPagerEx.OnPageChangeListener onPageChangeListener) {
        mViewPager.removeOnPageChangeListener(onPageChangeListener);
    }

    public void setCustomIndicator(PagerIndicator indicator){
        if(mIndicator != null){
            mIndicator.destroySelf();
        }
        mIndicator = indicator;
        mIndicator.setIndicatorVisibility(mIndicatorVisibility);
        mIndicator.setViewPager(mViewPager);
        mIndicator.redraw();
    }

    public <T extends BaseSliderView> void addSlider(T imageContent){
        mSliderAdapter.addSlider(imageContent);
    }

    public void startAutoCycle(){
        startAutoCycle(mSliderDuration, mSliderDuration, mAutoRecover);
    }

    /**
     * start auto cycle.
     * @param delay delay time
     * @param duration animation duration time.
     * @param autoRecover if recover after user touches the slider.
     */
    public void startAutoCycle(long delay,long duration,boolean autoRecover){
        mCycleHandler.removeCallbacks(mCycleRunnable);
        mCycleHandler.removeCallbacks(mResumingRunnable);
        mSliderDuration = duration;
        mAutoRecover = autoRecover;
        mCycleHandler.postDelayed(mCycleRunnable, delay);
        mCycling = true;
        mAutoCycle = true;
        mPausedBySystem = false;
    }

    /**
     * pause auto cycle.
     */
    private void pauseAutoCycle(){
        if(mCycling){
            mCycleHandler.removeCallbacks(mCycleRunnable);
            mCycling = false;
        }else{
            recoverCycle();
        }
    }

    /**
     * set the duration between two slider changes. the duration value must >= 500
     * @param duration
     */
    public void setDuration(long duration){
        if(duration >= 500){
            mSliderDuration = duration;
            if(mAutoCycle && mCycling){
                startAutoCycle();
            }
        }
    }

    /**
     * stop the auto circle
     */
    public void stopAutoCycle(){
        mCycleHandler.removeCallbacks(mCycleRunnable);
        mCycleHandler.removeCallbacks(mResumingRunnable);
        mAutoCycle = false;
        mCycling = false;
        // An explicit stop outranks the lifecycle: coming back to the screen must not restart a
        // cycle the caller switched off.
        mPausedBySystem = false;
    }

    /**
     * @return true while the slider is advancing by itself.
     */
    public boolean isAutoCycling(){
        return mCycling;
    }

    /**
     * when paused cycle, this method can weak it up.
     */
    private void recoverCycle(){
        if(!mAutoRecover || !mAutoCycle){
            return;
        }

        if(!mCycling){
            mCycleHandler.removeCallbacks(mResumingRunnable);
            mCycleHandler.postDelayed(mResumingRunnable, 6000);
        }
    }

    /**
     * Suspends the cycle without forgetting that the caller wanted one, so that
     * {@link #resumeAutoCycleAfterSystemPause()} can put it back.
     */
    private void pauseAutoCycleForSystem(){
        if(!mAutoCycle){
            return;
        }
        mCycleHandler.removeCallbacks(mCycleRunnable);
        mCycleHandler.removeCallbacks(mResumingRunnable);
        mCycling = false;
        mPausedBySystem = true;
    }

    private void resumeAutoCycleAfterSystemPause(){
        if(mPausedBySystem && mAutoCycle && !mCycling){
            startAutoCycle();
        }
    }

    /**
     * The auto cycle used to keep firing after the view left the window, which is why the caller
     * had to remember to call {@link #stopAutoCycle()} before its activity went away. It now stops
     * itself on detach, and on the owning lifecycle's ON_STOP when there is one
     * ({@link ViewTreeLifecycleOwner}), and starts again on the way back. An explicit
     * {@link #stopAutoCycle()} is still respected: only a cycle the system suspended is resumed.
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        observeLifecycle();
        resumeAutoCycleAfterSystemPause();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopObservingLifecycle();
        pauseAutoCycleForSystem();
        super.onDetachedFromWindow();
    }

    private final LifecycleEventObserver mLifecycleObserver = new LifecycleEventObserver() {
        @Override
        public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
            if (event == Lifecycle.Event.ON_STOP) {
                pauseAutoCycleForSystem();
            } else if (event == Lifecycle.Event.ON_START) {
                resumeAutoCycleAfterSystemPause();
            } else if (event == Lifecycle.Event.ON_DESTROY) {
                stopObservingLifecycle();
            }
        }
    };

    private void observeLifecycle() {
        LifecycleOwner owner = ViewTreeLifecycleOwner.get(this);
        if (owner == null || owner == mObservedLifecycleOwner) {
            return;
        }
        stopObservingLifecycle();
        mObservedLifecycleOwner = owner;
        owner.getLifecycle().addObserver(mLifecycleObserver);
    }

    private void stopObservingLifecycle() {
        if (mObservedLifecycleOwner != null) {
            mObservedLifecycleOwner.getLifecycle().removeObserver(mLifecycleObserver);
            mObservedLifecycleOwner = null;
        }
    }

    /**
     * Whether the user can page the slider with a finger. Programmatic moves
     * ({@link #moveNextPosition()}, {@link #setCurrentPosition(int)}) keep working either way, so
     * a caller that drives the slides itself -- from an audio track, say -- can forbid the stray
     * drag that would otherwise fight it.
     */
    public void setSwipeEnabled(boolean enabled){
        mViewPager.setSwipeEnabled(enabled);
    }

    public boolean isSwipeEnabled(){
        return mViewPager.isSwipeEnabled();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                pauseAutoCycle();
                break;
        }
        return false;
    }

    /**
     * set ViewPager transformer.
     * @param reverseDrawingOrder
     * @param transformer
     */
    public void setPagerTransformer(boolean reverseDrawingOrder,BaseTransformer transformer){
        mViewPagerTransformer = transformer;
        mViewPagerTransformer.setCustomAnimationInterface(mCustomAnimation);
        mViewPager.setPageTransformer(reverseDrawingOrder,mViewPagerTransformer);
    }



    /**
     * set the duration between two slider changes.
     * @param period
     * @param interpolator
     */
    public void setSliderTransformDuration(int period,Interpolator interpolator){
        // ViewPagerEx is this library's own class, so its scroller is set directly. It used to be
        // reached by reflection on a private field with an empty catch, which would have gone
        // silently dead the day the field was renamed.
        mViewPager.setScroller(new FixedSpeedScroller(mViewPager.getContext(), interpolator, period));
    }

    /**
     * preset transformers and their names
     */
    public enum Transformer{
        Default("Default"),
        Accordion("Accordion"),
        Background2Foreground("Background2Foreground"),
        CubeIn("CubeIn"),
        DepthPage("DepthPage"),
        Fade("Fade"),
        FlipHorizontal("FlipHorizontal"),
        FlipPage("FlipPage"),
        Foreground2Background("Foreground2Background"),
        RotateDown("RotateDown"),
        RotateUp("RotateUp"),
        Stack("Stack"),
        Tablet("Tablet"),
        ZoomIn("ZoomIn"),
        ZoomOutSlide("ZoomOutSlide"),
        ZoomOut("ZoomOut");

        private final String name;

        private Transformer(String s){
            name = s;
        }
        public String toString(){
            return name;
        }

        public boolean equals(String other){
            return (other == null)? false:name.equals(other);
        }
    };

    /**
     * set a preset viewpager transformer by id.
     * @param transformerId
     */
    public void setPresetTransformer(int transformerId){
        for(Transformer t : Transformer.values()){
            if(t.ordinal() == transformerId){
                setPresetTransformer(t);
                break;
            }
        }
    }

    /**
     * set preset PagerTransformer via the name of transforemer.
     * @param transformerName
     */
    public void setPresetTransformer(String transformerName){
        for(Transformer t : Transformer.values()){
            if(t.equals(transformerName)){
                setPresetTransformer(t);
                return;
            }
        }
    }

    /**
     * Inject your custom animation into PageTransformer, you can know more details in
     * {@link com.daimajia.slider.library.Animations.BaseAnimationInterface},
     * and you can see a example in {@link com.daimajia.slider.library.Animations.DescriptionAnimation}
     * @param animation
     */
    public void setCustomAnimation(BaseAnimationInterface animation){
        mCustomAnimation = animation;
        if(mViewPagerTransformer != null){
            mViewPagerTransformer.setCustomAnimationInterface(mCustomAnimation);
        }
    }

    /**
     * pretty much right? enjoy it. :-D
     *
     * @param ts
     */
    public void setPresetTransformer(Transformer ts){
        //
        // special thanks to https://github.com/ToxicBakery/ViewPagerTransforms
        //
        BaseTransformer t = null;
        switch (ts){
            case Default:
                t = new DefaultTransformer();
                break;
            case Accordion:
                t = new AccordionTransformer();
                break;
            case Background2Foreground:
                t = new BackgroundToForegroundTransformer();
                break;
            case CubeIn:
                t = new CubeInTransformer();
                break;
            case DepthPage:
                t = new DepthPageTransformer();
                break;
            case Fade:
                t = new FadeTransformer();
                break;
            case FlipHorizontal:
                t = new FlipHorizontalTransformer();
                break;
            case FlipPage:
                t = new FlipPageViewTransformer();
                break;
            case Foreground2Background:
                t = new ForegroundToBackgroundTransformer();
                break;
            case RotateDown:
                t = new RotateDownTransformer();
                break;
            case RotateUp:
                t = new RotateUpTransformer();
                break;
            case Stack:
                t = new StackTransformer();
                break;
            case Tablet:
                t = new TabletTransformer();
                break;
            case ZoomIn:
                t = new ZoomInTransformer();
                break;
            case ZoomOutSlide:
                t = new ZoomOutSlideTransformer();
                break;
            case ZoomOut:
                t = new ZoomOutTransformer();
                break;
        }
        setPagerTransformer(true,t);
    }



    /**
     * Set the visibility of the indicators.
     * @param visibility
     */
    public void setIndicatorVisibility(PagerIndicator.IndicatorVisibility visibility){
        if(mIndicator == null){
            return;
        }

        mIndicator.setIndicatorVisibility(visibility);
    }

    /**
     * This used to be inside out: it dereferenced the indicator when it was null, and answered
     * "Invisible" for every indicator that did exist, whatever had been set on it.
     */
    public PagerIndicator.IndicatorVisibility getIndicatorVisibility(){
        if(mIndicator == null){
            return PagerIndicator.IndicatorVisibility.Invisible;
        }
        return mIndicator.getIndicatorVisibility();
    }

    /**
     * get the {@link com.daimajia.slider.library.Indicators.PagerIndicator} instance.
     * You can manipulate the properties of the indicator.
     * @return
     */
    public PagerIndicator getPagerIndicator(){
        return mIndicator;
    }

    public enum PresetIndicators{
        Center_Bottom("Center_Bottom",R.id.default_center_bottom_indicator),
        Right_Bottom("Right_Bottom",R.id.default_bottom_right_indicator),
        Left_Bottom("Left_Bottom",R.id.default_bottom_left_indicator),
        Center_Top("Center_Top",R.id.default_center_top_indicator),
        Right_Top("Right_Top",R.id.default_center_top_right_indicator),
        Left_Top("Left_Top",R.id.default_center_top_left_indicator);

        private final String name;
        private final int id;
        private PresetIndicators(String name,int id){
            this.name = name;
            this.id = id;
        }

        public String toString(){
            return name;
        }

        public int getResourceId(){
            return id;
        }
    }
    public void setPresetIndicator(PresetIndicators presetIndicator){
        PagerIndicator pagerIndicator = (PagerIndicator)findViewById(presetIndicator.getResourceId());
        setCustomIndicator(pagerIndicator);
    }

    private InfinitePagerAdapter getWrapperAdapter(){
        PagerAdapter adapter = mViewPager.getAdapter();
        if(adapter!=null){
            return (InfinitePagerAdapter)adapter;
        }else{
            return null;
        }
    }

    private SliderAdapter getRealAdapter(){
        PagerAdapter adapter = mViewPager.getAdapter();
        if(adapter!=null){
            return ((InfinitePagerAdapter)adapter).getRealAdapter();
        }
        return null;
    }

    /**
     * get the current item position
     * @return
     */
    public int getCurrentPosition() {

        if (getRealAdapter() == null)
            throw new IllegalStateException("You did not set a slider adapter");
        try {
            return mViewPager.getCurrentItem() % getRealAdapter().getCount();
        } catch (Exception ex) {
            return 0;
        }
    }

    /**
     * get current slider.
     * @return
     */
    public BaseSliderView getCurrentSlider(){

        if(getRealAdapter() == null)
            throw new IllegalStateException("You did not set a slider adapter");

        int count = getRealAdapter().getCount();
        int realCount = 0;
        try {
            realCount = mViewPager.getCurrentItem() % count;
        } catch (Exception ex) {
            //preventing divide by zero
        }
        return getRealAdapter().getSliderView(realCount);
    }

    /**
     * remove  the slider at the position. Notice: It's a not perfect method, a very small bug still exists.
     */
    public void removeSliderAt(int position){
        if(getRealAdapter()!=null){
            getRealAdapter().removeSliderAt(position);
            mViewPager.setCurrentItem(mViewPager.getCurrentItem(),false);
        }
    }

    /**
     * remove all the sliders. Notice: It's a not perfect method, a very small bug still exists.
     */
    public void removeAllSliders(){
        if(getRealAdapter()!=null){
            int count = getRealAdapter().getCount();
            getRealAdapter().removeAllSliders();
            //a small bug, but fixed by this trick.
            //bug: when remove adapter's all the sliders.some caching slider still alive.
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() +  count,false);
        }
    }

    /**
     *set current slider
     * @param position
     */
    public void setCurrentPosition(int position, boolean smooth) {
        if (getRealAdapter() == null)
            throw new IllegalStateException("You did not set a slider adapter");
        if(position >= getRealAdapter().getCount()){
            throw new IllegalStateException("Item position is not exist");
        }
        int p = mViewPager.getCurrentItem() % getRealAdapter().getCount();
        int n = (position - p) + mViewPager.getCurrentItem();
        mViewPager.setCurrentItem(n, smooth);
    }

    public void setCurrentPosition(int position) {
        setCurrentPosition(position, true);
    }

    /**
     * move to prev slide.
     */
    public void movePrevPosition(boolean smooth) {

        if (getRealAdapter() == null)
            throw new IllegalStateException("You did not set a slider adapter");

        mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1, smooth);
    }

    public void movePrevPosition(){
        movePrevPosition(true);
    }

    /**
     * move to next slide.
     */
    public void moveNextPosition(boolean smooth) {

        if (getRealAdapter() == null)
            throw new IllegalStateException("You did not set a slider adapter");

        mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1, smooth);
    }

    public void moveNextPosition() {
        moveNextPosition(true);
    }
}
