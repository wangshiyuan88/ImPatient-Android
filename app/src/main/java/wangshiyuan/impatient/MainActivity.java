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
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;
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
import wangshiyuan.impatient.util.ImPatientRestfulService;
import wangshiyuan.impatient.util.ImPatientUtil;


public class MainActivity extends AppCompatActivity implements OnMenuItemClickListener,
        android.app.FragmentManager.OnBackStackChangedListener, CalendarDatePickerDialogFragment.OnDateSetListener, RadialTimePickerDialogFragment.OnTimeSetListener {


    Retrofit retrofit;
    public static ImPatientRestfulService imPatientRestfulService;
    private FragmentManager fragmentManager;
    private ContextMenuDialogFragment mMenuDialogFragment;
    private ParseObject appointment_to_schedule;


    private final static int scheduleApp = 1;
    private final static int checkIn = 2;
    private final static int delay = 4;
    private final static int logout = 7;
    private static final String FRAG_TAG_DATE_PICKER = "fragment_date_picker_name";
    private static final String FRAG_TAG_TIME_PICKER = "fragment_time_picker_name";
    private boolean mCheckIning = false;
    /**
     * A handler object, used for deferring UI operations.
     */
    private Handler mHandler = new Handler();

    private boolean ifCheckIn = false;

    private final long oneMin = 60*1000;
    private TimeChecker timeChecker;

    private ResultReceiver timeCheckerResultReceiver;

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

        MenuObject cancelAppointment = new MenuObject("Cancel appointment");
        cancelAppointment.setResource(R.drawable.icon_cancel_appointment);

        MenuObject remindMe = new MenuObject("Remind me");
        remindMe.setResource(R.drawable.icon_remainder);

        MenuObject logout = new MenuObject("Logout");
        BitmapDrawable logout_icon = new BitmapDrawable(getResources(),
                BitmapFactory.decodeResource(getResources(), R.drawable.icon_logout));
        logout.setDrawable(logout_icon);

        menuObjects.add(close);
        menuObjects.add(schduleAppointment);
        menuObjects.add(checkIn);
        menuObjects.add(remindMe);
        menuObjects.add(delay);
        menuObjects.add(readyForTreatment);
        menuObjects.add(cancelAppointment);
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
                checkIn();
                break;
            }
            case scheduleApp:{
                appointment_to_schedule = new ParseObject(ImPatientObject.appointment_name);
                appointment_to_schedule.put("patient", ParseUser.getCurrentUser().getObjectId());
                appointment_to_schedule.put("checkin", false);
                scheduleAppointment();
                break;
            }
            case delay:{
                delay();
                break;
            }
            case logout : {
                ParseUser.logOutInBackground();
                Intent logoutIntent = new Intent(this, ImPatientGateKeeperActivity.class);
                startActivity(logoutIntent);
                break;
            }
        }
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
        //Todo: Save the time
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
                if(statusCode==200){
                    String msg = response.body();
                    ImPatientUtil.makeToast(MainActivity.this, msg, Toast.LENGTH_LONG);
                }
            }
            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
                ImPatientUtil.makeToast(MainActivity.this, "Something wrong with server, please try later", Toast.LENGTH_LONG);
            }
        });
    }

    public void setCheckIn(boolean ifCheckIn){
        this.ifCheckIn = ifCheckIn;
    }

    private void startTimeChecker(){
        timeCheckerResultReceiver = new ResultReceiver(mHandler){
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                //Todo: result parsing
            }
        };

        timeChecker = new TimeChecker();
        registerReceiver(timeChecker, new IntentFilter(("wangshiyuan.impatient.timechecker")));

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent("wangshiyuan.impatient.timechecker"), PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager)(this.getSystemService(Context.ALARM_SERVICE));
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + oneMin, oneMin, pendingIntent);
    }

    public class TimeChecker extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            intent = new Intent(context, TimeCheckService.class);
            intent.putExtra(TimeCheckService.KEY_RESULT_RECEIVER, timeCheckerResultReceiver);
            context.startService(new Intent(context, TimeCheckService.class));
        }
    }
}
