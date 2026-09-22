package com.daimajia.slider.library.SliderTypes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.daimajia.slider.library.Pages.SplitPageImageView;
import com.daimajia.slider.library.R;

/**
 * One tall page shown as two columns, so the whole of it is readable on a wide screen with nothing
 * to scroll. See {@link SplitPageImageView} for how the cut between the columns is chosen.
 * <p>
 * Written for a musician reading from the screen with both hands busy: a hymn's sheet music is a
 * portrait page, and on a landscape device it is either too small to read or too big to see at
 * once. It loads at the picture's own size ({@link ScaleType#Natural}), because a page split in two
 * is drawn at roughly twice the size a fitted page would be, and a view-sized decode would show
 * that enlargement as blur.
 */
public class SplitPageSliderView extends BaseSliderView {

    private int gutterPx = 24;
    private boolean split = true;

    public SplitPageSliderView(Context context) {
        super(context);
        setScaleType(ScaleType.Natural);
    }

    /** Blank space between the two columns, in pixels. */
    public SplitPageSliderView setGutter(int px) {
        gutterPx = px;
        return this;
    }

    /**
     * False draws the page whole, as an ordinary slide. Useful for the same slider in portrait,
     * where a page already fits and splitting it would only make it smaller.
     */
    public SplitPageSliderView setSplit(boolean split) {
        this.split = split;
        return this;
    }

    @Override
    public View getView() {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.render_type_split_page, null);
        SplitPageImageView target = v.findViewById(R.id.daimajia_slider_image);
        target.setGutter(gutterPx);
        target.setSplitIntoColumns(split);
        bindEventAndShow(v, target);
        return v;
    }
}
