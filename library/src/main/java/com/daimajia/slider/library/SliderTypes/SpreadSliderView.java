package com.daimajia.slider.library.SliderTypes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.daimajia.slider.library.R;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.io.File;

/**
 * Two whole pages side by side, like an open book.
 * <p>
 * The companion to {@link SplitPageSliderView}, and the better answer on a screen wide and tall
 * enough to hold both pages at a readable size: a two-page piece then needs no page turn at all,
 * which is worth more to someone playing an instrument than the larger notes a split page gives.
 * Where both do not fit, split the pages instead.
 * <p>
 * Both pages load at their own size ({@link ScaleType#Natural}) for the same reason as the split
 * page: a view-sized decode shows as blur once the page is drawn large.
 */
public class SpreadSliderView extends BaseSliderView {

    private String secondUrl;
    private File secondFile;
    private int secondRes;

    public SpreadSliderView(Context context) {
        super(context);
        setScaleType(ScaleType.Natural);
    }

    /** The right-hand page. The left-hand one is the ordinary {@code image(...)}. */
    public SpreadSliderView second(String url) {
        secondUrl = url;
        return this;
    }

    public SpreadSliderView second(File file) {
        secondFile = file;
        return this;
    }

    public SpreadSliderView second(int res) {
        secondRes = res;
        return this;
    }

    @Override
    public View getView() {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.render_type_spread, null);
        ImageView left = v.findViewById(R.id.daimajia_slider_image);
        ImageView right = v.findViewById(R.id.daimajia_slider_image_second);
        bindEventAndShow(v, left);
        loadSecondPage(right);
        return v;
    }

    /**
     * The base class loads one picture; the second page is loaded here, through the same Picasso
     * instance so both pages share one cache and one set of settings.
     */
    private void loadSecondPage(ImageView target) {
        if (target == null) {
            return;
        }
        Picasso picasso = getPicasso() != null ? getPicasso() : Picasso.get();
        RequestCreator request;
        if (secondUrl != null) {
            request = picasso.load(secondUrl);
        } else if (secondFile != null) {
            request = picasso.load(secondFile);
        } else if (secondRes != 0) {
            request = picasso.load(secondRes);
        } else {
            // Only one page: it keeps the left half and the right stays empty, rather than
            // stretching one page across a spread meant for two.
            target.setVisibility(View.GONE);
            return;
        }
        if (getEmpty() != 0) {
            request.placeholder(getEmpty());
        }
        if (getError() != 0) {
            request.error(getError());
        }
        request.into(target);
    }
}
