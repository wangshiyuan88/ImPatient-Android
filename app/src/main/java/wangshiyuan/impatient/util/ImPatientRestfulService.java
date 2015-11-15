package wangshiyuan.impatient.util;

import java.util.Map;

import retrofit.Call;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import wangshiyuan.impatient.object.ImPatientResponse;

/**
 * Created by wangshiyuan on 10/20/15.
 */
public interface ImPatientRestfulService {

    @POST("patient/checkin")
    Call<String> checkIn(@Body Map<String, Object> body);

    @GET("patient/delay/{id}")
    Call<String> delay(@Path("id") String userId);

    @GET("patient/checkStatus/{id}")
    Call<String> checkStatus(@Path("id") String userId);

    @GET("patient/treatment/{id}")
    Call<ImPatientResponse> treatment(@Path("id") String userId);

    @GET("patient/cancelAppointment/{id}")
    Boolean cancelAppointment(@Path("id") String userId);


}
