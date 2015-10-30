package wangshiyuan.impatient;


import android.app.Application;

import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseUser;


/**
 * Created by wangshiyuan on 9/24/15.
 */

public class ImPatientApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Required - Initialize the Parse SDK
        Parse.enableLocalDatastore(this);
        Parse.initialize(this);

        ParseInstallation.getCurrentInstallation().saveInBackground();
        ParseObject.registerSubclass(ParseUser.class);
        ParseUser.enableRevocableSessionInBackground();
        Parse.setLogLevel(Parse.LOG_LEVEL_DEBUG);

    }
}
