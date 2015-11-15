package wangshiyuan.impatient.util;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.Calendar;
import java.util.List;

import wangshiyuan.impatient.MainActivity;
import wangshiyuan.impatient.object.ImPatientObject;

/**
 * Created by wangshiyuan on 10/16/15.
 */
public class ImPatientUtil {
    private static final String TAG = ImPatientUtil.class.getSimpleName();
    public static ParseObject getNextAppointment(List<ParseObject> objects, ParseException e, String state){
        ParseObject nextApp = null;
        if(ParseUser.getCurrentUser()==null)
            return null;
        if (e == null) {
            for (ParseObject app : objects) {
                if (app.get("patient").equals(ParseUser.getCurrentUser().getObjectId()) && (((String) app.get("date")).compareTo(getCurrentDateString())) >= 0) {
                    if (state==null || app.getString(MainActivity.APP_STATE_KEY).equals(state)){
                        if (nextApp == null)
                            nextApp = app;
                        else {
                            nextApp = (getTimeCompareString(app).compareTo(getTimeCompareString(nextApp))) < 0 ? app : nextApp;
                        }
                    }
                }
            }
        }
        return nextApp;
    }

    public static ParseObject getTodayAppointment(List<ParseObject> objects, ParseException e){
        ParseObject todayApp = null;
        if(e!=null)
            return todayApp;

        for (ParseObject app : objects) {
            if (((String)app.get("patient")).equals(ParseUser.getCurrentUser().getObjectId()) && (((String) app.get("date")).equals(getCurrentDateString()))) {
                Log.d(TAG, "get today appointment at "+app.get("date"));
                todayApp =  app;
            }
        }

        return todayApp;
    }

    public static void displayNextAppointment(final TextView view){
        ParseQuery<ParseObject> query = ParseQuery.getQuery(ImPatientObject.appointment_name);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> objects, ParseException e) {
                final ParseObject nextApp = ImPatientUtil.getNextAppointment(objects, e, null);
                if(nextApp==null) {
                    //No Appointment is found
                    view.setText("No Appointment Found");
                }else{
                    view.setText("Next Appointment:\n" + getTimeString(nextApp));
                }
            }
        });
    }


    public static void displayNextAppointment(final TextView view, final String state){
        ParseQuery<ParseObject> query = ParseQuery.getQuery(ImPatientObject.appointment_name);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> objects, ParseException e) {
                final ParseObject nextApp = ImPatientUtil.getNextAppointment(objects, e, state);
                if(nextApp==null) {
                    //No Appointment is found
                    view.setText("No Appointment Found");
                }else{
                    view.setText("Next Appointment:\n" + getTimeString(nextApp));
                }
            }
        });
    }
    public static void makeToast(Context context, String messge, int duration){
        Toast toast = Toast.makeText(context, messge, duration);
        toast.show();
    }

    public static String getCurrentDateString(){
        Calendar now = Calendar.getInstance();
        String month = convertTimeString(String.valueOf(now.get(Calendar.MONTH) + 1));
        String day =  convertTimeString(String.valueOf(now.get(Calendar.DAY_OF_MONTH)));
        return month+"-"+day+"-"+now.get(Calendar.YEAR);
    }


    public static String convertTimeString(String raw){
        String ret = Integer.parseInt(raw)>=10? raw : "0"+raw;
        return ret;
    }

    public static int getCurrentTimeStamp(){
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        return 60*hour + minute;
    }


    public static String getTimeString(ParseObject appointment_to_schedule){
        String minute = convertTimeString(String.valueOf(appointment_to_schedule.get("minute")));
        String hour = convertTimeString(String.valueOf((int) (appointment_to_schedule.get("hour")) + 1));
        return hour + ":" + minute + ", " + appointment_to_schedule.get("date");
    }

    public static String getTimeCompareString(ParseObject appointment_to_schedule){
        String minute = convertTimeString(String.valueOf(appointment_to_schedule.get("minute")));
        String hour = convertTimeString(String.valueOf((int) (appointment_to_schedule.get("hour")) + 1));
        return appointment_to_schedule.get("date")+ ", "+convertTimeString(String.valueOf((int) (appointment_to_schedule.get("hour")) + 1)) +":"+convertTimeString(String.valueOf(appointment_to_schedule.get("minute")));
    }
}
