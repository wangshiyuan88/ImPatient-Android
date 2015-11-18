package wangshiyuan.impatient;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;
import wangshiyuan.impatient.object.ImPatientObject;
import wangshiyuan.impatient.util.ImPatientUtil;


/**
 * This service can kick off background thread to check status based current station of patient
 * 1. If a patient is in waiting queue, service will send out request to Spring server to check estimate
 * waiting time
 * 2. If a patinet is in treatment, service will send to Parse server to check if treatment finishes by checking the
 * state value of Appointment object
 */
public class TimeCheckService extends IntentService {

    public TimeCheckService() {
        super("TimeCheckingService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ResultReceiver receiver = intent.getParcelableExtra(MainActivity.KEY_RESULT_RECEIVER);
        //String statusString = intent.getStringExtra(MainActivity.KEY_STATUS);
        handleAppointmentStatus(receiver);
    }

    private void handleAppointmentStatus(final ResultReceiver receiver) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(ImPatientObject.appointment_name);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> objects, ParseException e) {
                ParseObject appointmentInTreatment = ImPatientUtil.getTodayAppointment(objects, e);
                if(appointmentInTreatment!=null) {
                    String appState = appointmentInTreatment.getString(MainActivity.APP_STATE_KEY);
                    if(appState.equals(MainActivity.APP_STATE_CHECK_IN)){
                        getWaitngStatus(receiver);
                    }else if(appState.equals(MainActivity.APP_STAET_READY_FOR_TRETMENT)){
                        receiver.send(MainActivity.RESULT_RECEIVER_CHECK_IN_READY_FOR_TREATEMENT_CODE, null);
                    }else if(appState.equals(MainActivity.APP_STAET_IN_TRETMENT)){
                        receiver.send(MainActivity.RESULT_RECEIVER_CHECK_IN_TREATMENT_CODE, null);
                    }else if(appState.equals(MainActivity.APP_STATE_FINISH)){
                        receiver.send(MainActivity.RESULT_RECEIVER_CHECK_FINISH_TREATEMNT_CODE, null);
                    }
                }
            }
        });
    }


    private void getWaitngStatus(final ResultReceiver receiver){
        Call<String> time = MainActivity.imPatientRestfulService.checkStatus(ParseUser.getCurrentUser().getObjectId());
        time.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Response<String> response, Retrofit retrofit) {
                int statusCode = response.code();
                String msg = response.body();
                Bundle data = new Bundle();
                if (statusCode == 200) {
                    data.putString("msg", msg);
                }else{
                    data.putString("msg", null);
                }
                receiver.send(MainActivity.RESULT_RECEIVER_CHECK_WAITING_TIME_CODE, data);
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void getTreatmentStatus(final ResultReceiver receiver) {


    }

}
