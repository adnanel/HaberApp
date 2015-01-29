package adnan.haber.views;


import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import adnan.haber.R;
import adnan.haber.util.Debug;

/**
 * Created by Adnan on 17.9.2014.
 */
public class TextScroll extends FrameLayout {
    Context context;
    private OnClickListener clickListener;
    private int delay = 2250;

    private TextView textView;
    private List<String> values;
    private int cVal = 0;

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public void setValues(CharSequence[] values) {
        this.values = new ArrayList<String>();
        for ( CharSequence s : values ) {
            this.values.add(s.toString());
        }
    }

    public TextScroll(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public TextScroll(Context context, AttributeSet attrs) {
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

    public TextScroll(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;

        init();
    }


    @Override
    public void setOnClickListener(OnClickListener listener ) {
        this.clickListener = listener;

    }


    void init() {
        textView = (TextView)LayoutInflater.from(context).inflate(R.layout.text_scroll, this).findViewById(R.id.tvTextScroll);
        textView.setText("");

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
                            final Animation in = AnimationUtils.loadAnimation(context, R.anim.abc_slide_in_bottom);
                            Animation out = AnimationUtils.loadAnimation(context, R.anim.abc_slide_out_top);
                            out.setFillAfter(true);
                            out.setAnimationListener(new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {}

                                @Override
                                public void onAnimationEnd(Animation animation) {
                                    post(new Runnable() {
                                        @Override
                                        public void run() {
                                            textView.setText(values.get(cVal));
                                            cVal ++;
                                            if ( cVal == values.size() )
                                                cVal = 0;

                                            textView.startAnimation(in);
                                        }
                                    });
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {}
                            });

                            textView.startAnimation(out);


                        }
                    });
                }
            }
        }.start();
    }

}