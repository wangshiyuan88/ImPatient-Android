package wangshiyuan.impatient;

import com.parse.ui.ParseLoginDispatchActivity;

/**
 * Created by wangshiyuan on 9/24/15.
 */
public class ImPatientGateKeeperActivity extends ParseLoginDispatchActivity {
    @Override
    protected Class<?> getTargetClass() {
        return MainActivity.class;
    }

}
