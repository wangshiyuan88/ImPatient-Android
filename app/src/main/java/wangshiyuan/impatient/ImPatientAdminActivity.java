package wangshiyuan.impatient;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.baoyz.widget.PullRefreshLayout;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;
import wangshiyuan.impatient.object.CheckIn;
import wangshiyuan.impatient.object.ImPatientResponse;
import wangshiyuan.impatient.util.ImPatientRestfulService;
import wangshiyuan.impatient.util.ImPatientUtil;

/**
 * Created by wangshiyuan on 11/15/15.
 */
public class ImPatientAdminActivity extends AppCompatActivity{

    private PullRefreshLayout checkInDashboard;
    private SwipeMenuListView checkInListView;
    private CheckInAdapter checkInAdapter;
    ImPatientRestfulService imPatientRestfulService;
    private List<CheckIn> checkIns = new ArrayList<CheckIn>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.admin_main);
        initRetrofit();
        initCheckInDashBoard();
        initListView();

    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getResources().getString(R.string.test_url)).addConverterFactory(GsonConverterFactory.create())
                .build();
        imPatientRestfulService = retrofit.create(ImPatientRestfulService.class);
    }

    private void initCheckInDashBoard() {
        checkInDashboard = (PullRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        checkInDashboard.setRefreshStyle(PullRefreshLayout.STYLE_RING);
        checkInDashboard.setOnRefreshListener(new PullRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                checkInDashboard.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkInDashboard.setRefreshing(true);
                        final Call<List<CheckIn>> checkInCall = imPatientRestfulService.getAllCheckIns();
                        checkInCall.enqueue(new Callback<List<CheckIn>>() {

                            @Override
                            public void onResponse(Response<List<CheckIn>> response, Retrofit retrofit) {
                                checkIns = response.body();
                                checkInAdapter.refresh();
                                checkInDashboard.setRefreshing(false);
                            }

                            @Override
                            public void onFailure(Throwable t) {

                            }
                        });
                    }
                }, 3000);
            }
        });
    }



    private void initListView(){

        checkInListView = (SwipeMenuListView) findViewById(R.id.listView);

        checkInAdapter = new CheckInAdapter();
        checkInListView.setAdapter(checkInAdapter);

        SwipeMenuCreator creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {
                // create "up" item
                SwipeMenuItem up = new SwipeMenuItem(
                        getApplicationContext());
                up.setBackground(new ColorDrawable(Color.rgb(0xC9, 0xC9,
                        0xCE)));
                up.setWidth(dp2px(90));
                up.setIcon(R.drawable.icon_up);
                menu.addMenuItem(up);

                SwipeMenuItem down = new SwipeMenuItem(
                        getApplicationContext());
                down.setBackground(new ColorDrawable(Color.rgb(0xC9, 0xC9,
                        0xCE)));
                down.setWidth(dp2px(90));
                down.setIcon(R.drawable.icon_down);
                menu.addMenuItem(down);
            }
        };
        // set creator
        checkInListView.setMenuCreator(creator);

        checkInListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int position, long id) {
                deleteDialog(position);
                return true;
            }
        });

        checkInListView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                CheckIn checkIn = checkIns.get(position);
                String direction = null;
                switch (index) {
                    case 0:
                        // up
                        if(position!=0){
                           direction = "UP";
                        }
                        break;
                    case 1:
                        //down
                        if(position<checkIns.size()-1){
                            direction = "DOWN";
                        }
                        break;
                }
                if(direction==null) {
                    checkInAdapter.refresh();
                    return true;
                }
                else {
                    swapCheckIn(checkIn, direction);
                    checkInAdapter.refresh();
                }
                Call<ImPatientResponse> adjustCall = imPatientRestfulService.adjustOrder(checkIn.getObjectId(), direction);
                adjustCall.enqueue(new Callback<ImPatientResponse>() {
                    @Override
                    public void onResponse(Response<ImPatientResponse> response, Retrofit retrofit) {
                        ImPatientResponse body = response.body();
                        if(body.getStatus()){
                            ImPatientUtil.makeToast(getApplicationContext(), "Swap Order Success", Toast.LENGTH_LONG);
                        }else{
                            ImPatientUtil.makeToast(getApplicationContext(), body.getMsg(), Toast.LENGTH_LONG);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        ImPatientUtil.makeToast(getApplicationContext(), "Swap Order Fail", Toast.LENGTH_LONG);
                    }
                });
                return true;
            }
        });
    }

    private void swapCheckIn(CheckIn checkIn, String direction){
        int index = checkIns.indexOf(checkIn);
        if(direction.equals("UP")){
            CheckIn prev = checkIns.get(index-1);
            int temp = prev.getCheckInTime();
            prev.setCheckInTime(checkIn.getCheckInTime());
            checkIn.setCheckInTime(temp);
            checkIns.set(index, prev);
            checkIns.set(index-1, checkIn);
        }else{
            CheckIn next = checkIns.get(index+1);
            int temp = next.getCheckInTime();
            next.setCheckInTime(checkIn.getCheckInTime());
            checkIn.setCheckInTime(temp);
            checkIns.set(index, next);
            checkIns.set(index+1, checkIn);
        }
    }

    private void deleteDialog(final int position) {
        final CheckIn checkInToDelete = checkIns.get(position);

        AlertDialog.Builder confirm = new AlertDialog.Builder(this);
        confirm.setTitle(R.string.confirm_delete_checkin).setIcon(R.drawable.icon_delete);
        confirm.setMessage("You are going to cancel " + checkInToDelete.getFirstName() + " " + checkInToDelete.getLastName() + "'s appointment");
        confirm.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Call<List<CheckIn>> deleteCall = imPatientRestfulService.deleteCheckin(checkInToDelete.getObjectId());
                checkInDashboard.setRefreshing(true);
                deleteCall.enqueue(new Callback<List<CheckIn>>() {
                    @Override
                    public void onResponse(Response<List<CheckIn>> response, Retrofit retrofit) {
                        List<CheckIn> body = response.body();
                        for (CheckIn checkIn : body) {
                            if (checkIn.getObjectId().equals(checkInToDelete.getObjectId()))
                                ImPatientUtil.makeToast(ImPatientAdminActivity.this, getString(R.string.fail_to_delete_checkin), Toast.LENGTH_LONG);
                        }
                        checkIns = body;
                        checkInAdapter.refresh();
                        checkInDashboard.setRefreshing(false);
                    }

                    @Override
                    public void onFailure(Throwable t) {

                    }
                });
            }
        });

        confirm.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        confirm.show();
    }


    public void logout(MenuItem item){
        ParseUser.logOut();
        Intent logoutIntent = new Intent(this, ImPatientGateKeeperActivity.class);
        startActivity(logoutIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_admin_main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    public class CheckInAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return checkIns.size();
        }

        @Override
        public Object getItem(int position) {
            return checkIns.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(getApplicationContext(),
                        R.layout.checkin_item_list, null);
                new ViewHolder(convertView);
            }
            ViewHolder holder = (ViewHolder) convertView.getTag();
            holder.name.setText(checkIns.get(position).getFirstName() + " " + checkIns.get(position).getLastName());
            holder.checkInTime.setText(ImPatientUtil.convertTimeString(checkIns.get(position).getCheckInTime()/60+"")+":"+
            ImPatientUtil.convertTimeString(checkIns.get(position).getCheckInTime()%60+""));
            return convertView;
        }

        class ViewHolder {
            TextView name;
            TextView checkInTime;

            public ViewHolder(View view) {
                name = (TextView) view.findViewById(R.id.patient_name);
                checkInTime = (TextView) view.findViewById(R.id.checkin_time);
                view.setTag(this);
            }
        }

        public void refresh(){
            notifyDataSetChanged();
        }
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }


}
