package adnan.haber.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import adnan.haber.HaberActivity;
import adnan.haber.R;
import adnan.haber.fragments.AdvancedPreferences;
import adnan.haber.util.Blinker;

/**
 * Created by prg01 on 12.2.2015.
 */
public class TabView extends FrameLayout {
    Context context;


    private View tabBackground;
    private View rlMsgCounter;
    private TextView tvMsgCounter;
    private TextView tvTitle;

    public String getTitle() {
        return tvTitle.getText().toString();
    }

    public void setTitle(String title) {
        tvTitle.setText(title);
    }

    public TabView(Context context) {
        super(context);
        this.context = context;
        init(null);
    }

    public TabView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.TabView,
                0, 0);
        try {
            //align = a.getInteger(R.styleable.MenuItem_picturePosition, 0);
        } finally {
            a.recycle();
        }

        init(attrs);
    }

    public TabView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.TabView,
                0, 0);
        try {
            //align = a.getInteger(R.styleable.MenuItem_picturePosition, 0);
        } finally {
            a.recycle();
        }

        init(attrs);
    }


    Thread blinker = null;
    HaberActivity.TabState currentState = HaberActivity.TabState.Normal;


    @Override
    protected void onDraw(Canvas canvas) {
        if ( currentState == HaberActivity.TabState.Active ) {
            tabBackground.setBackgroundResource(R.drawable.tab_background_active);
        } else if ( currentState == HaberActivity.TabState.Normal ) {
            tabBackground.setBackgroundResource(R.drawable.tab_background);
        } else if ( currentState == HaberActivity.TabState.Marked ) {
            tabBackground.setBackgroundResource(R.drawable.tab_background_selected);
        }

        super.onDraw(canvas);
    }

    public void setPostState(HaberActivity.TabState state) {
        currentState = state;
        postInvalidate();
    }

    public void setState(HaberActivity.TabState state) {
        currentState = state;
        if ( blinker != null ) {
            blinker.interrupt();
        }

        if ( state == HaberActivity.TabState.Active ) {
            rlMsgCounter.setVisibility(View.INVISIBLE);
            tvMsgCounter.setText("0");
        } else if ( state == HaberActivity.TabState.Normal ) {
            rlMsgCounter.setVisibility(View.INVISIBLE);
            tvMsgCounter.setText("0");
        } else if ( state == HaberActivity.TabState.Marked ) {
            if (!AdvancedPreferences.ShouldBlink(context) ) {
                currentState = HaberActivity.TabState.Marked;
            } else {
                blinker = new Blinker(this);
                blinker.start();
            }

            rlMsgCounter.setVisibility(View.VISIBLE);
        }

        invalidate();
    }

    public int getUnreadMessagesCount() {
        return Integer.parseInt(tvMsgCounter.getText().toString());
    }


    public void setUnreadMessagesCount(int count) {
        tvMsgCounter.setText(count + "");
    }

    void init(AttributeSet attrs) {
        this.setWillNotDraw(false);

        if ( attrs != null ) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.TabView,
                    0, 0);

            try {

            } finally {
                a.recycle();
            }
        }

        LayoutInflater.from(context).inflate(R.layout.single_tab, this);

        rlMsgCounter    = this.findViewById(R.id.rlMessageCounter);
        tvMsgCounter    = (TextView)this.findViewById(R.id.tvMessageCounter);
        tabBackground   = this.findViewById(R.id.rlTabBackground);
        tvTitle         = (TextView)this.findViewById(R.id.tvUser);
    }

}
