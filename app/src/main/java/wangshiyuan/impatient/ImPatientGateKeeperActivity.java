package wangshiyuan.impatient;

import com.parse.ParseUser;
import com.parse.ui.ParseLoginDispatchActivity;

/**
 * Created by wangshiyuan on 9/24/15.
 */
public class ImPatientGateKeeperActivity extends ParseLoginDispatchActivity {
    public static final String ADMIN_USER = "Admin";
    public static final String PATIENT_USER = "Patient";

    @Override
    protected Class<?> getTargetClass() {
        if(ParseUser.getCurrentUser().get("Type").equals(PATIENT_USER))
            return MainActivity.class;
        else
            return ImPatientAdminActivity.class;
    }

}
