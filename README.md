# Android Image Slider

> ### This is a maintained fork
>
> Upstream ([daimajia/AndroidImageSlider](https://github.com/daimajia/AndroidImageSlider)) stopped
> in 2020, and this fork carries it forward for the apps that still use it. It is consumed through
> JitPack; there is nothing to publish by hand.
>
> ```groovy
> repositories { maven { url 'https://jitpack.io' } }
>
> dependencies {
>     implementation 'com.github.MickeCast:AndroidImageSlider:1.3.0'
> }
> ```
>
> **Building it:** AGP 8.13.2 / Gradle 8.14.3 / **JDK 17**, `compileSdk` 36, `minSdk` 24. The JDK is
> what `jitpack.yml` pins -- JitPack still defaults to Java 8, which cannot run AGP 8. The version
> deliberately trails the apps that consume it: an AAR is version-independent of its consumers, and
> JitPack's builders are the ceiling here.
>
> **Tests:** `./gradlew :library:connectedDebugAndroidTest` with a device attached. They are
> instrumented because a slider without a window, a Looper and a real layout pass is not the thing
> worth testing.
>
> ### What this fork changed
>
> **Fixed**
> - `SliderLayout.getIndicatorVisibility()` dereferenced a null indicator, and reported `Invisible`
>   for every indicator that existed, whatever had been set on it.
> - `PagerIndicator.setIndicatorVisibility()` never stored the value, so the getter answered
>   whatever the XML said at inflation.
> - A successful image load told nobody: `ImageLoadListener.onEnd(true, ...)` was never called.
> - `ScaleType.FitCenterCrop` was in the enum but missing from the switch, so asking for it did
>   nothing at all.
> - The auto cycle ran on a `Timer` thread per cycle and hopped back through a `Handler` built with
>   no `Looper` (deprecated since API 30). It is one main-thread `Handler` now.
> - `setSliderTransformDuration()` set the scroller by reflection on a private field, with an empty
>   catch; `ViewPagerEx` exposes it properly now.
> - The library manifest forced `WRITE_EXTERNAL_STORAGE` on every app that depended on it. Nothing
>   here writes to storage.
> - jcenter is gone from the build, the `maven` plugin is replaced by `maven-publish`, and the demo
>   no longer needs two artifacts that died with jcenter.
>
> **Added**
> - `SliderLayout.setSwipeEnabled(boolean)` (and the `swipe_enabled` XML attribute): forbid paging
>   by finger while still moving the slides from code.
> - The auto cycle stops when the view leaves the window or its lifecycle owner stops, and comes
>   back with it. An explicit `stopAutoCycle()` still wins.
> - `ZoomableSliderView`: pinch-to-zoom slides, with PhotoView as a `compileOnly` dependency, so
>   only the apps that use that class need to declare it.
> - `ScaleType.FitWidth` and `ScaleType.FitHeight`, for full-screen pictures on tall screens where
>   `CenterInside` leaves broad empty bands.
> - `SliderLayout.isAutoCycling()`.
>
> ### 1.4.0 -- tall pages
>
> For a portrait page in a landscape view -- sheet music, a scan, a document. Fitted whole it is a
> narrow column in the middle of a wide screen, unreadable until the reader zooms in by hand every
> single time.
>
> - **`FillWidthFromTop`** (and `ZoomableSliderView.fillWidthFromTop(true)`) opens such a page
>   zoomed to the full width, showing its top. It is deliberately **inert** for a picture that
>   already fills the width, so it is safe on a slider carrying pictures of mixed shapes. The zoom
>   is a starting point, not a cage: pinch, drag and double-tap still work, it re-applies on
>   rotation, and it never overrides a zoom the reader chose.
> - **`ScaleType.Natural`** loads the picture at its own size instead of the view's. Every other
>   scale type decodes to the view, which is right for a picture shown whole and exactly wrong for
>   one that will be magnified -- zooming then enlarges a view-sized copy, and the detail that was
>   thrown away never comes back. `ZoomableSliderView` uses it by default.
>
> Two things worth knowing if you are debugging around this:
>
> - A view that is not attached to a window yet hands out a **throwaway `ViewTreeObserver`**, and
>   anything registered on it is dropped when the real one arrives. A slide is built detached, so
>   the pre-draw listener has to be registered on attach.
> - `PhotoView.setScale(scale)` zooms about the **centre** of the view. Showing the top needs the
>   focal-point form, `setScale(scale, x, 0, false)`.
>
> ### 1.5.0 -- a whole page at once, and two pager bugs
>
> For a musician reading from the screen with both hands busy. A portrait page of sheet music on a
> landscape device is either too small to read or too big to see at once; cut in half and set side
> by side it fits whole, at about twice the size.
>
> - **`SplitPageImageView` / `SplitPageSliderView`** draw one tall page as two columns. **The cut is
>   found, not assumed**: it looks for the band of blank paper nearest the halfway point -- the
>   gutter between two systems, 30 to 45 px on a typical hymn page -- so each column starts at the
>   top of a system instead of through the middle of a stave. A page with no such band is cut at the
>   middle, which is no worse than not splitting it.
>
> **Two bugs this turned up in `InfinitePagerAdapter`, both of which affect any caller that
> replaces its slides:**
>
> - **`getItemPosition` was never overridden**, so the wrapper inherited `POSITION_UNCHANGED` while
>   the adapter it wraps answers `POSITION_NONE`. The ViewPager only asks the adapter it was given,
>   so replacing the slides and calling `notifyDataSetChanged` left the page already on screen
>   exactly as it was, until you paged away and back.
> - **`destroyItem` returned early when the adapter was empty** -- which is precisely the state
>   `removeAllSliders()` leaves it in. The page was never taken off the pager, so every rebuild
>   leaked its views and the new slide was merely laid on top of the old one. That hid the first
>   bug, which is why neither was ever noticed.

---

[![Gitter](https://badges.gitter.im/Join Chat.svg)](https://gitter.im/daimajia/AndroidImageSlider?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
 
This is an amazing image slider for the Android platform. I decided to open source this because there is really not an attractive, convenient slider widget in Android.
 
You can easily load images from an internet URL, drawable, or file. And there are many kinds of amazing animations you can choose. :-D
 
## Demo
 
![](http://ww3.sinaimg.cn/mw690/610dc034jw1egzor66ojdg20950fknpe.gif)

[Download Apk](https://github.com/daimajia/AndroidImageSlider/releases/download/v1.0.8/demo-1.0.8.apk)
 
## Usage

### Step 1

#### Gradle

```groovy
dependencies {
    	compile "com.android.support:support-v4:+"
    	compile 'com.squareup.picasso:picasso:2.3.2'
    	compile 'com.nineoldandroids:library:2.4.0'
    	compile 'com.daimajia.slider:library:1.1.5@aar'
}
```


#### Maven

```xml
<dependency>
    <groupId>com.squareup.picasso</groupId>
    <artifactId>picasso</artifactId>
    <version>2.3.2</version>
</dependency>
<dependency>
    <groupId>com.nineoldandroids</groupId>
    <artifactId>library</artifactId>
    <version>2.4.0</version>
</dependency>
<dependency>
    <groupId>com.daimajia.slider</groupId>
    <artifactId>library</artifactId>
    <version>1.1.2</version>
    <type>apklib</type>
</dependency>
```

#### Eclipse

For Eclipse users, I provided a sample project which orgnized as Eclipse way. You can download it from [here](https://github.com/daimajia/AndroidImageSlider/releases/download/v1.0.9/AndroidImageSlider-Eclipse.zip), and make some changes to fit your project.

Notice: It's the version of 1.0.9, it may not update any more. You can update manually by yourself.

### Step 2

Add permissions (if necessary) to your `AndroidManifest.xml`

```xml
<!-- if you want to load images from the internet -->
<uses-permission android:name="android.permission.INTERNET" /> 

<!-- if you want to load images from a file OR from the internet -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

**Note:** If you want to load images from the internet, you need both the `INTERNET` and `READ_EXTERNAL_STORAGE` permissions to allow files from the internet to be cached into local storage.

If you want to load images from drawable, then no additional permissions are necessary.

### Step 3

Add the Slider to your layout:
 
```java
<com.daimajia.slider.library.SliderLayout
        android:id="@+id/slider"
        android:layout_width="match_parent"
        android:layout_height="200dp"
/>
```        
 
There are some default indicators. If you want to use a provided indicator:
 
```java
<com.daimajia.slider.library.Indicators.PagerIndicator
        android:id="@+id/custom_indicator"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:gravity="center"
        />
```

[Code example](https://github.com/daimajia/AndroidImageSlider/blob/master/demo%2Fsrc%2Fmain%2Fjava%2Fcom%2Fdaimajia%2Fslider%2Fdemo%2FMainActivity.java)
 
====
 
## Advanced usage

Please visit [Wiki](https://github.com/daimajia/AndroidImageSlider/wiki)
 
## Thanks

- [Picasso](https://github.com/square/picasso)
- [NineOldAndroids](https://github.com/JakeWharton/NineOldAndroids)
- [ViewPagerTransforms](https://github.com/ToxicBakery/ViewPagerTransforms)

##About me
 
I am a student in mainland China. I love Google, love Android, love everything that is interesting. If you get any problems when using this library or you have an internship opportunity, please feel free to [email me](mailto:daimajia@gmail.com). :smiley:
