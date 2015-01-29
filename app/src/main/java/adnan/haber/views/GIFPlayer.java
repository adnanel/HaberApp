package adnan.haber.views;


import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import adnan.haber.R;
import adnan.haber.util.Debug;

/**
 * Created by Adnan on 17.9.2014.
 *
 * !Ne koriste se vise!
 */
@Deprecated
public class GIFPlayer extends FrameLayout {
    Context context;
    private OnClickListener clickListener;
    private int delay = 100;

    private ImageView imageView;
    private List<Integer> values;
    private int cVal = 0;

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public void setValues(List<Integer> values) {
        this.values = values;
    }


    public GIFPlayer(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public GIFPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.MenuItem,
                0, 0);

        /*try {
            align = a.getTextArray(R.styleable.TextScroll_, 0);
        } finally {
            a.recycle();
        }*/

        init();
    }

    public GIFPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;

        init();
    }


    @Override
    public void setOnClickListener(OnClickListener listener ) {
        this.clickListener = listener;

    }

    public void setScaleType(ImageView.ScaleType scaleType) {
        imageView.setScaleType(scaleType);
    }

    void init() {
        imageView = (ImageView)LayoutInflater.from(context).inflate(R.layout.gifplayer, this).findViewById(R.id.ivGIF);

        new Thread() {
            @Override
            public void run() {
                while ( !this.isInterrupted() ) {
                    try { Thread.sleep(delay); } catch (Exception e ) { Debug.log(e); }

                    if ( values == null ) continue;
                    if ( values.size() == 0 ) continue;

                    post(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageResource(values.get(cVal));

                            cVal ++;
                            if ( cVal == values.size() )
                                cVal = 0;
                        }
                    });
                }
            }
        }.start();
    }

}