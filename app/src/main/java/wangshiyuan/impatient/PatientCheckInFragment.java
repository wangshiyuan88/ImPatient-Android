package wangshiyuan.impatient;


import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;
import wangshiyuan.impatient.object.ImPatientObject;
import wangshiyuan.impatient.util.ImPatientRestfulService;
import wangshiyuan.impatient.util.ImPatientUtil;

/**
 * Created by wangshiyuan on 10/12/15.
 */
public class PatientCheckInFragment extends Fragment {
    MainActivity parentActivity;
    private ParseObject currentAppointment;

    @Override
    public void onStart() {
        super.onStart();
        initButton();
        getCurrentAppointment();
    }

    private void getCurrentAppointment(){
        ParseQuery<ParseObject> query = ParseQuery.getQuery(ImPatientObject.appointment_name);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> objects, ParseException e) {
                currentAppointment = ImPatientUtil.getTodayAppointment(objects, e);
                if (currentAppointment == null) {
                    ImPatientUtil.makeToast(getActivity().getApplicationContext(), "You don't have an appointment today, please schedule.", Toast.LENGTH_LONG);
                    parentActivity.quitCheckIn();
                } else {
                    Handler handler = parentActivity.getHandler();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView)getView().findViewById(R.id.appintment_view)).setText("Next Appointment:\n" + ImPatientUtil.getTimeString(currentAppointment));
                        }
                    });
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                  Bundle savedInstanceState) {
        View v =  inflater.inflate(R.layout.checkin_form, container, false);
        parentActivity = (MainActivity)getActivity();
        return v;
    }

    public void initButton(){
        Button checkin_canel = (Button)getView().findViewById(R.id.checkin_cancel);
        checkin_canel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parentActivity.quitCheckIn();
            }
        });
        Button checkin_upload = (Button)getView().findViewById(R.id.checkin_upload);
        checkin_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentAppointment==null){
                    Context context = getActivity().getApplicationContext();
                    ImPatientUtil.makeToast(context, "Appointment Information is not ready yet.", Toast.LENGTH_LONG);
                    parentActivity.setStatus(MainActivity.PatientStatus.NO_APPOINTMENT);
                    parentActivity.quitCheckIn();
                }else if(((String)currentAppointment.get("state")).equals(MainActivity.APP_STATE_CHECK_IN)) {
                    parentActivity.setStatus(MainActivity.PatientStatus.CHECK_IN);
                    ImPatientUtil.makeToast(getActivity().getApplicationContext(), "Appoinment already got checked in.", Toast.LENGTH_LONG);
                    parentActivity.quitCheckIn();
                }else{
                    uploadCheckInFrom();
                    parentActivity.setStatus(MainActivity.PatientStatus.CHECK_IN);
                }
            }
        });
    }

    private void uploadCheckInFrom(){
        final String firstName = ((TextView)getView().findViewById(R.id.first_name)).getText().toString();
        final String lastName = ((TextView)getView().findViewById(R.id.last_name)).getText().toString();
        RadioGroup radioSexGroup = (RadioGroup)getView().findViewById(R.id.sex);
        int selectedId = radioSexGroup.getCheckedRadioButtonId();
        final String sex = (String)((RadioButton) getView().findViewById(selectedId)).getText();
        final String userID = ParseUser.getCurrentUser().getObjectId();
        final int checkInTime = Integer.parseInt(getCheckInTime((int) currentAppointment.get("timestamp")));
        final String appointmentID = currentAppointment.getObjectId();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, Object> requestBody = new HashMap<String, Object>();
                requestBody.put("firstName", firstName);
                requestBody.put("lastName", lastName);
                requestBody.put("sex", sex);
                requestBody.put("userID", userID);
                requestBody.put("checkInTime", checkInTime);
                requestBody.put("appointmentID", appointmentID);
                final ImPatientRestfulService imPatientRestfulService = parentActivity.getImPatientRestfulService();
                try {
                    Call<String> call = imPatientRestfulService.checkIn(requestBody);
                    call.enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Response<String> response, Retrofit retrofit) {
                            int statusCode = response.code();
                            String msg = response.body();
                            if (statusCode == 200) {
//                                currentAppointment.put("state", MainActivity.APP_STATE_CHECK_IN);
//                                currentAppointment.saveInBackground();
                                if (Looper.myLooper() == null) {
                                    Looper.prepare();
                                }
                                ImPatientUtil.makeToast(parentActivity, msg, Toast.LENGTH_LONG);
                                parentActivity.setStatus(MainActivity.PatientStatus.CHECK_IN);
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            t.printStackTrace();
                            if (Looper.myLooper() == null)
                            {
                                Looper.prepare();
                            }
                            ImPatientUtil.makeToast(parentActivity, "Check In failed, please try later", Toast.LENGTH_LONG);
                        }
                    });

                }catch (Exception e){
                    e.printStackTrace();
                    if (Looper.myLooper() == null)
                    {
                        Looper.prepare();
                    }
                    ImPatientUtil.makeToast(parentActivity, "Check In failed, please try later", Toast.LENGTH_LONG);
                }
            }
        }).start();
        parentActivity.quitCheckIn();
        return;
    }


    private String getCheckInTime(int appointmentTimeStamp){
        int ret = ImPatientUtil.getCurrentTimeStamp() > appointmentTimeStamp? ImPatientUtil.getCurrentTimeStamp(): appointmentTimeStamp;
        return String.valueOf(ret);
    }
}
