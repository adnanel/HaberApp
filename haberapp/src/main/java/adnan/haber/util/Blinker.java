package adnan.haber.util;

import adnan.haber.HaberActivity;
import adnan.haber.views.TabView;

/**
 * Created by Adnan on 14.2.2015..
 */
public class Blinker extends Thread {
    TabView tabView;
    boolean interrupted = false;

    public Blinker(TabView tabView) {
        this.tabView = tabView;
    }

    @Override
    public void interrupt() {
        interrupted = true;
    }

    @Override
    public void run() {
        while (!interrupted) {
            tabView.setPostState(HaberActivity.TabState.Normal);

            try {
                Thread.sleep(200);
            } catch (Exception er) {
                            /* probably interrupted */
                break;
            }
            if ( interrupted ) break;

            tabView.setPostState(HaberActivity.TabState.Marked);

            try {
                Thread.sleep(500);
            } catch (Exception er) {
                            /* probably interrupted */
                break;
            }
            if ( interrupted ) break;
        }

        tabView.postInvalidate();
    }
}
