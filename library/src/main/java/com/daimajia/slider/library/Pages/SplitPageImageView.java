package com.daimajia.slider.library.Pages;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Shows one tall page as two columns side by side, so the whole of it is readable on a wide screen
 * without scrolling.
 * <p>
 * The case it exists for is a musician reading from the screen: a portrait page of sheet music on a
 * landscape device is either a narrow column in the middle, or -- zoomed to the width -- a third of
 * a page that has to be scrolled three times, with both hands busy. Cut in half and set side by
 * side, the same page fits whole and stays about twice the size the fitted page would be.
 * <p>
 * <b>The cut is found, not assumed.</b> Splitting at the exact middle can slice through a stave.
 * This looks for the band of blank paper nearest the halfway point -- the gutter between two
 * systems, which on a typical hymn page is 30 to 45 pixels tall -- and cuts there, so each column
 * begins at the top of a system. A page with no blank band anywhere near the middle (a photograph,
 * say) is cut at the middle, which is no worse than not splitting it at all.
 * <p>
 * Everything else about the view is an ordinary ImageView: with {@link #setSplitIntoColumns(boolean)
 * split} off it draws exactly as it did before.
 */
public class SplitPageImageView extends ImageView {

    /** Blank-paper search stays inside this band around the middle of the page. */
    private static final float SEARCH_FROM = 0.35f;
    private static final float SEARCH_TO = 0.65f;
    /** A row counts as blank when fewer than this fraction of the pixels sampled carry ink. */
    private static final float INK_ROW_THRESHOLD = 0.004f;
    /** Anything darker than this is ink. */
    private static final int INK_LEVEL = 160;
    /** Rows are sampled every few pixels in both directions; a stave line is far thicker. */
    private static final int ROW_STEP = 3;
    private static final int COLUMN_STEP = 4;

    private boolean splitIntoColumns;
    private int gutterPx;

    private Bitmap measuredBitmap;
    private int splitRow = -1;

    private final Rect source = new Rect();
    private final RectF destination = new RectF();

    public SplitPageImageView(Context context) {
        super(context);
    }

    public SplitPageImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SplitPageImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /** Draw the page as two columns rather than as one picture. */
    public void setSplitIntoColumns(boolean split) {
        if (splitIntoColumns != split) {
            splitIntoColumns = split;
            invalidate();
        }
    }

    public boolean isSplitIntoColumns() {
        return splitIntoColumns;
    }

    /** Blank space left between the two columns. */
    public void setGutter(int px) {
        gutterPx = Math.max(0, px);
        invalidate();
    }

    /**
     * The row of the picture the page is cut at, or -1 before one has been chosen. Exposed so a
     * test -- or a caller that wants to say where the cut fell -- can see the decision.
     */
    public int getSplitRow() {
        return splitRow;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Bitmap page = bitmapOf(getDrawable());
        if (!splitIntoColumns || page == null) {
            super.onDraw(canvas);
            return;
        }
        int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int viewHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        if (viewWidth <= 0 || viewHeight <= 0) {
            return;
        }
        if (page != measuredBitmap) {
            splitRow = findSplitRow(page);
            measuredBitmap = page;
        }

        int columnWidth = (viewWidth - gutterPx) / 2;
        drawSlice(canvas, page, 0, splitRow, getPaddingLeft(), columnWidth, viewHeight);
        drawSlice(canvas, page, splitRow, page.getHeight(),
                getPaddingLeft() + columnWidth + gutterPx, columnWidth, viewHeight);
    }

    /** Draws rows {@code top} to {@code bottom} of the page into one column, fitted and centred. */
    private void drawSlice(Canvas canvas, Bitmap page, int top, int bottom,
                           int columnLeft, int columnWidth, int columnHeight) {
        int sliceHeight = bottom - top;
        if (sliceHeight <= 0 || columnWidth <= 0) {
            return;
        }
        float scale = Math.min((float) columnWidth / page.getWidth(),
                (float) columnHeight / sliceHeight);
        float drawnWidth = page.getWidth() * scale;
        float drawnHeight = sliceHeight * scale;
        float left = columnLeft + (columnWidth - drawnWidth) / 2f;
        float topOffset = getPaddingTop() + (columnHeight - drawnHeight) / 2f;

        source.set(0, top, page.getWidth(), bottom);
        destination.set(left, topOffset, left + drawnWidth, topOffset + drawnHeight);
        canvas.drawBitmap(page, source, destination, null);
    }

    private static Bitmap bitmapOf(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                return bitmap;
            }
        }
        return null;
    }

    /**
     * The row to cut at: the middle of the band of blank paper nearest the halfway point, or the
     * halfway point itself when the page has no such band. Public so a caller can say where the
     * cut fell, and so it can be tested without a view.
     */
    public static int findSplitRow(Bitmap page) {
        int width = page.getWidth();
        int height = page.getHeight();
        int middle = height / 2;
        if (width <= 0 || height <= 0) {
            return middle;
        }
        int from = (int) (height * SEARCH_FROM);
        int to = (int) (height * SEARCH_TO);
        int sampledPerRow = (width + COLUMN_STEP - 1) / COLUMN_STEP;
        int inkAllowed = Math.max(1, (int) (sampledPerRow * INK_ROW_THRESHOLD));

        int[] row = new int[width];
        int bandStart = -1;
        int bestCut = -1;
        int bestDistance = Integer.MAX_VALUE;

        for (int y = from; y <= to; y += ROW_STEP) {
            boolean blank = y < to && isBlankRow(page, row, y, width, inkAllowed);
            if (blank) {
                if (bandStart < 0) {
                    bandStart = y;
                }
                continue;
            }
            if (bandStart < 0) {
                continue;
            }
            // A band of blank paper just ended: the cut goes through its middle, and the band
            // nearest the halfway point wins.
            int cut = (bandStart + y) / 2;
            int distance = Math.abs(cut - middle);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestCut = cut;
            }
            bandStart = -1;
        }
        return bestCut < 0 ? middle : bestCut;
    }

    private static boolean isBlankRow(Bitmap page, int[] row, int y, int width, int inkAllowed) {
        page.getPixels(row, 0, width, 0, y, width, 1);
        int ink = 0;
        for (int x = 0; x < width; x += COLUMN_STEP) {
            int pixel = row[x];
            // Luminance is close enough on a scan: these pages are black on white.
            int luminance = ((pixel >> 16 & 0xff) * 299 + (pixel >> 8 & 0xff) * 587
                    + (pixel & 0xff) * 114) / 1000;
            if (luminance < INK_LEVEL && (pixel >>> 24) > 32) {
                if (++ink > inkAllowed) {
                    return false;
                }
            }
        }
        return true;
    }
}
