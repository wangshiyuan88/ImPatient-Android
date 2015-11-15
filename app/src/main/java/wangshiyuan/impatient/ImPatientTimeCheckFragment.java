package wangshiyuan.impatient;


import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;

import wangshiyuan.impatient.util.ImPatientUtil;

public class ImPatientTimeCheckFragment extends Fragment {

    ShimmerTextView tv;
    Shimmer shimmer;
    Button refresh;
    final Handler handler = new Handler(Looper.getMainLooper());

    
    @Override
    public void onStart() {
        super.onStart();
        initViewElem();
        ImPatientUtil.displayNextAppointment(tv);
    }

    private void initViewElem() {
        refresh = (Button)getView().findViewById(R.id.refresh_dashboard);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity)getActivity()).findPatientStatus();
            }
        });
        tv = (ShimmerTextView) getView().findViewById(R.id.status);
        shimmer = new Shimmer();
        shimmer.start(tv);
    }

    public void displayNextAppointment(){
        ImPatientUtil.displayNextAppointment(tv);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_dashboard, container, false);
    }

}
