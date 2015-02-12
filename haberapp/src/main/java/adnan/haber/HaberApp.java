package adnan.haber;

import android.app.Application;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

/**
 * Created by Adnan on 12.2.2015..
 */

@ReportsCrashes(formKey = "", // will not be used
        mailTo = "adnanel94@gmail.com",
        mode = ReportingInteractionMode.DIALOG,
        resDialogText = R.string.crash_toast_text)
public class HaberApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }
}