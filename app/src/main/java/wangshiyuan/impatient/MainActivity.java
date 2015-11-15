package wangshiyuan.impatient;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.romainpiel.shimmer.ShimmerTextView;
import com.yalantis.contextmenu.lib.ContextMenuDialogFragment;
import com.yalantis.contextmenu.lib.MenuObject;
import com.yalantis.contextmenu.lib.MenuParams;
import com.yalantis.contextmenu.lib.interfaces.OnMenuItemClickListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;
import wangshiyuan.impatient.object.ImPatientObject;
import wangshiyuan.impatient.object.ImPatientResponse;
import wangshiyuan.impatient.util.ImPatientRestfulService;
import wangshiyuan.impatient.util.ImPatientUtil;


public class MainActivity extends AppCompatActivity implements OnMenuItemClickListener,
        android.app.FragmentManager.OnBackStackChangedListener, CalendarDatePickerDialogFragment.OnDateSetListener, RadialTimePickerDialogFragment.OnTimeSetListener {

    public enum PatientStatus{
        NO_APPOINTMENT, NOT_CHECK_IN, CHECK_IN, READY_FOR_TREATMENT, IN_TREATMENT, FINISH_TREATMENT, UNKNOWN
    }

    private final String TAG = "ImPatient.MainActivity";
    Retrofit retrofit;
    public static ImPatientRestfulService imPatientRestfulService;
    private FragmentManager fragmentManager;
    private ContextMenuDialogFragment mMenuDialogFragment;
    private ParseObject appointment_to_schedule;
    private int alarmControlCode = 0;
    private final int timeCheckerFreq = 60 * 1000;

    private final static int scheduleApp = 1;
    private final static int checkIn = 2;
    private final static int delay = 3;
    private final static int readyForTreatment = 4;
    private final static int logout = 5;
    private static final String FRAG_TAG_DATE_PICKER = "fragment_date_picker_name";
    private static final String FRAG_TAG_TIME_PICKER = "fragment_time_picker_name";
    private boolean mCheckIning = false;
    private PatientStatus status = PatientStatus.UNKNOWN;

    public final static int RESULT_RECEIVER_CHECK_WAITING_TIME_CODE = 1;
    public final static int RESULT_RECEIVER_CHECK_IN_READY_FOR_TREATEMENT_CODE = 2;
    public final static int RESULT_RECEIVER_CHECK_IN_TREATMENT_CODE = 3;
    public final static int RESULT_RECEIVER_CHECK_FINISH_TREATEMNT_CODE = 4;

    /**
     * A handler object, used for deferring UI operations.
     */
    private Handler mHandler = new Handler();

    public static final String KEY_RESULT_RECEIVER = "0";
    public static final String KEY_STATUS = "1";
    private final long oneMin = 60*1000;
    private TimeChecker timeChecker;

    private ResultReceiver timeCheckerResultReceiver;

    public static final String APP_STATE_NOT_START = "nostart";
    public static final String APP_STATE_CHECK_IN = "checkin";
    public static final String APP_STAET_IN_TRETMENT = "intreatment";
    public static final String APP_STAET_READY_FOR_TRETMENT = "readyfortreatment";
    public static final String APP_STATE_FINISH = "finish";

    public static final String APP_STATE_KEY = "state";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        fragmentManager = getFragmentManager();

        if(savedInstanceState==null) {
            addFragment(new ImPatientTimeCheckFragment(), true, R.id.container);
        }else{
            mCheckIning = (getFragmentManager().getBackStackEntryCount() > 0);
        }

        retrofit = new Retrofit.Builder()
                .baseUrl(getResources().getString(R.string.test_url)).addConverterFactory(GsonConverterFactory.create())
                .build();
        imPatientRestfulService = retrofit.create(ImPatientRestfulService.class);
        getFragmentManager().addOnBackStackChangedListener(this);

        timeCheckerResultReceiver = new ResultReceiver(mHandler){
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if(resultCode==MainActivity.RESULT_RECEIVER_CHECK_WAITING_TIME_CODE){
                    final String msg = resultData.getString("msg");
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(msg!=null) {
                                if(msg.equals("00:00")){
                                    setStatus(PatientStatus.READY_FOR_TREATMENT);
                                } else
                                    ((ShimmerTextView) findViewById(R.id.status)).setText("Waiting Time:\n" + msg);
                            }
                        }
                    });
                }else if(resultCode==MainActivity.RESULT_RECEIVER_CHECK_IN_TREATMENT_CODE){
                    MainActivity.this.setStatus(PatientStatus.IN_TREATMENT);
                    Log.d(TAG, "In Treatment Now...");
                }else if(resultCode==MainActivity.RESULT_RECEIVER_CHECK_IN_READY_FOR_TREATEMENT_CODE){
                    MainActivity.this.setStatus(PatientStatus.READY_FOR_TREATMENT);
                    Log.d(TAG, "Ready for Treatment Now...");
                }else if(resultCode==MainActivity.RESULT_RECEIVER_CHECK_FINISH_TREATEMNT_CODE){
                    MainActivity.this.setStatus(PatientStatus.FINISH_TREATMENT);
                    Log.d(TAG, "Finish Treatment Now...");
                }
            }
        };
    }

    @Override
    protected void onStart(){
        super.onStart();
        findPatientStatus();

    }

    private void initMainMenuFragment() {
        MenuParams menuParams = new MenuParams();
        menuParams.setActionBarSize((int) getResources().getDimension(R.dimen.tool_bar_height));
        menuParams.setMenuObjects(getMenuObjects());
        menuParams.setClosableOutside(false);
        mMenuDialogFragment = ContextMenuDialogFragment.newInstance(menuParams);
    }

    private List<MenuObject> getMenuObjects() {
        List<MenuObject> menuObjects = new ArrayList<>();

        MenuObject close = new MenuObject();
        close.setResource(R.drawable.icon_close);

        MenuObject schduleAppointment = new MenuObject("Schdule Appointment");
        schduleAppointment.setResource(R.drawable.icon_schedule_appointment);

        MenuObject checkIn = new MenuObject("Check In");
        checkIn.setResource(R.drawable.icon_checkin);

        MenuObject delay = new MenuObject("Delay");
        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.icon_coffee);
        delay.setBitmap(b);

        MenuObject readyForTreatment = new MenuObject("Ready for treatment");
        BitmapDrawable bd = new BitmapDrawable(getResources(),
                BitmapFactory.decodeResource(getResources(), R.drawable.icon_therapy));
        readyForTreatment.setDrawable(bd);

        // Better to have these two features, may be implement in the furture.
//        MenuObject cancelAppointment = new MenuObject("Cancel appointment");
//        cancelAppointment.setResource(R.drawable.icon_cancel_appointment);
//
//        MenuObject remindMe = new MenuObject("Remind me");
//        remindMe.setResource(R.drawable.icon_remainder);

        MenuObject logout = new MenuObject("Logout");
        BitmapDrawable logout_icon = new BitmapDrawable(getResources(),
                BitmapFactory.decodeResource(getResources(), R.drawable.icon_logout));
        logout.setDrawable(logout_icon);

        menuObjects.add(close);
        menuObjects.add(schduleAppointment);
        menuObjects.add(checkIn);
        //menuObjects.add(remindMe);
        menuObjects.add(delay);
        menuObjects.add(readyForTreatment);
        //menuObjects.add(cancelAppointment);
        menuObjects.add(logout);


        return menuObjects;
    }

    protected void addFragment(Fragment fragment, boolean addToBackStack, int containerId) {
        invalidateOptionsMenu();
        String backStackName = fragment.getClass().getName();
        boolean fragmentPopped = fragmentManager.popBackStackImmediate(backStackName, 0);
        if (!fragmentPopped) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(containerId, fragment, backStackName)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            if (addToBackStack)
                transaction.addToBackStack(backStackName);
            transaction.commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        if(!mCheckIning) {
            initMainMenuFragment();
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.menu_main, menu);
        }else{

        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.context_menu:
                if (fragmentManager.findFragmentByTag(ContextMenuDialogFragment.TAG) == null) {
                    mMenuDialogFragment.show(fragmentManager, ContextMenuDialogFragment.TAG);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mMenuDialogFragment != null && mMenuDialogFragment.isAdded()) {
            mMenuDialogFragment.dismiss();
        } else{
            finish();
        }
    }

    @Override
    public void onMenuItemClick(View clickedView, int position) {
        switch (position) {
            case checkIn:{
                if(status==PatientStatus.NOT_CHECK_IN)
                    checkIn();
                else if(status==PatientStatus.CHECK_IN)
                    startTimeChecker();
                break;
            }
            case scheduleApp:{
                appointment_to_schedule = new ParseObject(ImPatientObject.appointment_name);
                appointment_to_schedule.put("patient", ParseUser.getCurrentUser().getObjectId());
                appointment_to_schedule.put("state", MainActivity.APP_STATE_NOT_START);
                scheduleAppointment();
                break;
            }
            case delay:{
                delay();
                break;
            }
            case readyForTreatment:{
                readyForTreatment();
                break;
            }
            case logout : {
                stopTimeChecker();
                ParseUser.logOut();
                Intent logoutIntent = new Intent(this, ImPatientGateKeeperActivity.class);
                startActivity(logoutIntent);
                break;
            }
        }
    }

    private void readyForTreatment() {
        Call<ImPatientResponse> call = imPatientRestfulService.treatment(ParseUser.getCurrentUser().getObjectId());
        call.enqueue(new Callback<ImPatientResponse>() {
            @Override
            public void onResponse(Response<ImPatientResponse> response, Retrofit retrofit) {
                if(response.body().getStatus())
                    MainActivity.this.setStatus(PatientStatus.IN_TREATMENT);
            }

            @Override
            public void onFailure(Throwable t) {

            }
        });
    }

    @Override
    public void onBackStackChanged() {

    }

    private void scheduleAppointment(){
        Calendar now = Calendar.getInstance();
        CalendarDatePickerDialogFragment calendarDatePickerDialogFragment = CalendarDatePickerDialogFragment
                .newInstance(MainActivity.this, now.get(Calendar.YEAR), now.get(Calendar.MONTH),
                        now.get(Calendar.DAY_OF_MONTH));
        calendarDatePickerDialogFragment.show(getSupportFragmentManager(), FRAG_TAG_DATE_PICKER);

    }

    private void checkIn(){
        checkIn(new PatientCheckInFragment());
    }

    private void checkIn(Fragment fragment){
        mCheckIning = true;

        getFragmentManager()
                .beginTransaction()

                        // Replace the default fragment animations with animator resources representing
                        // rotations when switching to the back of the card, as well as animator
                        // resources representing rotations when flipping back to the front (e.g. when
                        // the system Back button is pressed).
                .setCustomAnimations(
                        R.animator.card_flip_right_in, R.animator.card_flip_right_out,
                        R.animator.card_flip_left_in, R.animator.card_flip_left_out)

                        // Replace any fragments currently in the container view with a fragment
                        // representing the next page (indicated by the just-incremented currentPage
                        // variable).
                .replace(R.id.container, fragment)

                        // Add this transaction to the back stack, allowing users to press Back
                        // to get to the front of the card.
                .addToBackStack(null)

                        // Commit the transaction.
                .commit();

        // Defer an invalidation of the options menu (on modern devices, the action bar). This
        // can't be done immediately because the transaction may not yet be committed. Commits
        // are asynchronous in that they are posted to the main thread's message loop.
        invalidateMenu();
    }

    public void quitCheckIn(){
        mCheckIning = false;
        getFragmentManager().popBackStack();
        invalidateMenu();
        return;
    }

    public void invalidateMenu(){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                invalidateOptionsMenu();
            }
        });
    }

    @Override
    public void onDateSet(CalendarDatePickerDialogFragment dialog, int year, int monthOfYear, int dayOfMonth) {
        showTimePicker();
        appointment_to_schedule.put("date", ImPatientUtil.convertTimeString(String.valueOf(monthOfYear + 1)) + "-" + ImPatientUtil.convertTimeString(String.valueOf(dayOfMonth)) + "-" + year);
    }

    @Override
    public void onTimeSet(RadialTimePickerDialogFragment dialog, int hourOfDay, int minute) {
        appointment_to_schedule.put("timestamp", hourOfDay * 60 + minute);
        appointment_to_schedule.put("hour", hourOfDay-1);
        appointment_to_schedule.put("minute", minute);
        confirmAppointment();

    }

    public void showTimePicker(){
        RadialTimePickerDialogFragment timePickerDialog = RadialTimePickerDialogFragment
                .newInstance(MainActivity.this, 12, 0,
                        DateFormat.is24HourFormat(MainActivity.this));
        timePickerDialog.show(getSupportFragmentManager(), FRAG_TAG_TIME_PICKER);

    }

    public void confirmAppointment(){
        AlertDialog.Builder confirm = new AlertDialog.Builder(this);
        confirm.setTitle(R.string.confirm_string).setIcon(R.drawable.icon_schedule_appointment);
        confirm.setMessage("You schedule an appointment at " + ImPatientUtil.getTimeString(appointment_to_schedule));
        confirm.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                appointment_to_schedule.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e == null) {
                            String text = "Your appointment at "+ ImPatientUtil.getTimeString(appointment_to_schedule)+" is confirmed!";
                            pushNotification(getString(R.string.schedule_appointment_success_title), text, R.drawable.icon_confirm);
                        }else{
                            String text = "Your appointment at "+ ImPatientUtil.getTimeString(appointment_to_schedule)+ " is not scheduled successfully, " +
                                    "please come back and schduel later";
                            pushNotification(getString(R.string.schedule_appointment_fail_title), text, R.drawable.icon_fail);
                            setStatus(PatientStatus.NOT_CHECK_IN);
                        }
                        appointment_to_schedule = null;
                    }
                });
            }
        });

        confirm.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                appointment_to_schedule = null;
            }
        });

        confirm.show();
    }

    private void pushNotification(String title, String text, int icon_id){
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(icon_id)
                        .setContentTitle(title)
                        .setContentText(text);
        int mNotificationId = 0;
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

    public ImPatientRestfulService getImPatientRestfulService(){
        return imPatientRestfulService;
    }

    public Handler getHandler(){
        return mHandler;
    }

    private void delay(){
        Call<String> call = imPatientRestfulService.delay(ParseUser.getCurrentUser().getObjectId());
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Response<String> response, Retrofit retrofit) {
                int statusCode = response.code();
                if (statusCode == 200) {
                    String msg = response.body();
                    ImPatientUtil.makeToast(MainActivity.this, msg, Toast.LENGTH_LONG);
                    findPatientStatus();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
                ImPatientUtil.makeToast(MainActivity.this, "Something wrong with server, please try later", Toast.LENGTH_LONG);
            }
        });
    }


    private void startTimeChecker(){
        if(timeChecker==null)
            timeChecker = new TimeChecker();
        registerReceiver(timeChecker, new IntentFilter(("wangshiyuan.impatient.timechecker")));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, alarmControlCode, new Intent("wangshiyuan.impatient.timechecker"), PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager)(this.getSystemService(Context.ALARM_SERVICE));
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, 0, timeCheckerFreq, pendingIntent);
    }

    public class TimeChecker extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            intent = new Intent(context, TimeCheckService.class);
            intent.putExtra(KEY_RESULT_RECEIVER, timeCheckerResultReceiver);
            intent.putExtra(KEY_STATUS, ((MainActivity) context).getStatus().toString());
            context.startService(intent);
        }
    }

    public void stopTimeChecker(){
        timeChecker = null;
        PendingIntent senderstop = PendingIntent.getBroadcast(this,
                alarmControlCode, new Intent("wangshiyuan.impatient.timechecker"), PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManagerstop = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManagerstop.cancel(senderstop);
    }

    public void setStatus(PatientStatus status){
        this.status = status;
        nextAction();
    }


    private void nextAction() {
        switch (status){
            case NO_APPOINTMENT:
                ImPatientUtil.displayNextAppointment(((ShimmerTextView) findViewById(R.id.status)));
                break;
            case NOT_CHECK_IN:
                ImPatientUtil.displayNextAppointment(((ShimmerTextView) findViewById(R.id.status)));
                break;
            case CHECK_IN:{
                startTimeChecker();
                break;
            }
            case READY_FOR_TREATMENT: {
                startTimeChecker();
                ((ShimmerTextView) findViewById(R.id.status)).setText("Your turn for Treatment.\nPlease press ready for treatment in menu");
                break;
            }
            case IN_TREATMENT: {
                startTimeChecker();
                ((ShimmerTextView) findViewById(R.id.status)).setText("In treatment now...");
                break;
            }
            case FINISH_TREATMENT: {
                pushNotification("Treatment Done", "You have finished your treatment today", R.drawable.icon_confirm);
                stopTimeChecker();
                ImPatientUtil.displayNextAppointment(((ShimmerTextView) findViewById(R.id.status)), MainActivity.APP_STATE_NOT_START);
                break;
            }
            case UNKNOWN: {
                findPatientStatus();
                break;
            }
        }
    }

    public void findPatientStatus(){
        ParseQuery<ParseObject> query = ParseQuery.getQuery(ImPatientObject.appointment_name);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> objects, ParseException e) {
                final ParseObject nextAppointment = ImPatientUtil.getNextAppointment(objects, e, null);
                if(nextAppointment==null)
                    setStatus(PatientStatus.NO_APPOINTMENT);
                else{
                    String state = nextAppointment.getString(MainActivity.APP_STATE_KEY);
                    if(state.equals(APP_STATE_NOT_START)){
                        setStatus(PatientStatus.NOT_CHECK_IN);
                    }else if(state.equals(APP_STATE_CHECK_IN)){
                        setStatus(PatientStatus.CHECK_IN);
                    }else if(state.equals(APP_STAET_READY_FOR_TRETMENT)){
                        setStatus(PatientStatus.READY_FOR_TREATMENT);
                    }else if(state.equals(APP_STAET_IN_TRETMENT)){
                        setStatus(PatientStatus.IN_TREATMENT);
                    }else if(state.equals(APP_STATE_FINISH)){
                        setStatus(PatientStatus.FINISH_TREATMENT);
                    }
                }
            }
        });
    }

    public PatientStatus getStatus(){
        return status;
    }
}

