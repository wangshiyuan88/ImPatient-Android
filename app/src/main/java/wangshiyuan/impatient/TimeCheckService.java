package wangshiyuan.impatient;

import android.app.IntentService;
import android.content.Intent;
import android.os.ResultReceiver;

import com.parse.ParseUser;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;


public class TimeCheckService extends IntentService {

    public static String KEY_RESULT_RECEIVER = "0";

    public TimeCheckService() {
        super("TimeCheckingService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ResultReceiver receiver = intent.getParcelableExtra(KEY_RESULT_RECEIVER);

        //Todo: send retrofit api to check remaining time
        Call<String> time = MainActivity.imPatientRestfulService.checkStatus(ParseUser.getCurrentUser().getObjectId());
        time.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Response<String> response, Retrofit retrofit) {
                int statusCode = response.code();
                String msg = response.body();
                if (statusCode == 200) {

                }
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        });
    }

}
