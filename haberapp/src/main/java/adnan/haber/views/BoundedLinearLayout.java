package adnan.haber.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Display;
import android.view.WindowManager;
import android.widget.LinearLayout;

import adnan.haber.R;

/**
 * Created by Adnan on 14.2.2015..
 */
public class BoundedLinearLayout extends LinearLayout {
    private static int BoundedWidth = 0;

    public static void setWidth(int w) {
        BoundedWidth = w;
    }

    public static int getBoundWidth() {
        return BoundedWidth;
    }

    private int mBoundedHeight;

    public BoundedLinearLayout(Context context) {
        super(context);
        mBoundedHeight = 0;

    }

    public BoundedLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BoundedView);
        mBoundedHeight = a.getDimensionPixelSize(R.styleable.BoundedView_bounded_height, 0);
        a.recycle();

    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Adjust width as necessary
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        if(BoundedWidth > 0 && BoundedWidth < measuredWidth) {
            int measureMode = MeasureSpec.getMode(widthMeasureSpec);
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(BoundedWidth, measureMode);
        }
        // Adjust height as necessary
        int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);
        if(mBoundedHeight > 0 && mBoundedHeight < measuredHeight) {
            int measureMode = MeasureSpec.getMode(heightMeasureSpec);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(mBoundedHeight, measureMode);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}