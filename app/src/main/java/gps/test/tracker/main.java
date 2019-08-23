package gps.test.tracker;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

/**
 * Created by user_0 on 9/2/2017.
 */

public class main extends AppCompatActivity implements OnMapReadyCallback {
    private TextView start_service_button, stop_service_button,show_result_but,filter_results_but;
    private EditText dist,interv,start_date,end_date,start_time,end_time;
    private Intent location_tracker_service_intent;
    private boolean app_exited;
    private Database_io db_io;
    private Thread keep_ui_updated_with_background_processes_thread;
    private View permission_warning;
    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST=0;
    private LocationManager locationManager;
    private RelativeLayout main_menu;
    private LinearLayout filter_options_menu;

    private RecyclerView.LayoutManager layoutManager;
    private RecyclerView recyclerView;
    private fileadapter adap;
    private MapView mapView;
    private GoogleMap googleMap;

    public static class Date_and_Time_PickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {
        public static final int Type_Date=1,Type_Time=0;
        public int type=0;
        private EditText editText;
        public Date_and_Time_PickerFragment(int type,EditText editText_to_be_filled){
            this.type=type;
            this.editText=editText_to_be_filled;
        }
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            if(type==Date_and_Time_PickerFragment.Type_Time) {
                final Calendar c = Calendar.getInstance();
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int minute = c.get(Calendar.MINUTE);
                return new TimePickerDialog(getActivity(), this, hour, minute,
                        DateFormat.is24HourFormat(getActivity()));
            }
            if(type==Date_and_Time_PickerFragment.Type_Date){
                final Calendar c = Calendar.getInstance();
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);
                return new DatePickerDialog(getActivity(), this, year, month, day);
            }
            return null;
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            Calendar c=Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY,hourOfDay);
            c.set(Calendar.MINUTE,minute);
            editText.setText(new SimpleDateFormat("HH:mm").format(c.getTime()));
        }
        public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
            Calendar c=Calendar.getInstance();
            c.set(i,i1,i2);
            editText.setText(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
        }
    }
    public static class location_info {
        public double lat, longt, alt;
        public long timestamp;

        public location_info setLat(double lat) {
            this.lat = lat;
            return this;
        }

        public location_info setLongt(double longt) {
            this.longt = longt;
            return this;
        }

        public location_info setAlt(double alt) {
            this.alt = alt;
            return this;
        }

        public location_info setTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public location_info setDate(String date) {
            this.date = date;
            return this;
        }

        public int position_in_db;
        private String date;
    }
    //below is a data_Array holder class which loads only the datas that are about to be seen by user and nulls unnecessary pointers;
    // we have alllllot of locations to show after all, so memory management is a must;
    private class recycler_data_set {
        private ArrayList<location_info> data_array = new ArrayList<>(),temp;
        static final int PRELOAD_SIZE=150;//how many data_Array be loaded before and after the current RecyclerView position;
        public int current_view_position,all_rows_number=(int)db_io.get_all_rows_count();//this value is updated as recyclerview is scrolled; so new_data_offset ('offset' value in the query sent
        // to database) would be determined from this;
        private int current_data_offset;//obvious!
        public recycler_data_set(){
            new Thread(new Runnable(){
                @Override
                public void run() {
                    int new_data_offset;
                    while (!app_exited){
                        temp=new ArrayList<>();
                        if(current_view_position<recycler_data_set.PRELOAD_SIZE)
                            new_data_offset=0;
                        else
                            new_data_offset=current_view_position-recycler_data_set.PRELOAD_SIZE;
                        Cursor cursor=db_io.get_all_locations_within(new_data_offset,recycler_data_set.PRELOAD_SIZE*2);
                        while (cursor.moveToNext()&&!app_exited) {
                            location_info l_i = new location_info().setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(Database_io.Database_info.recorded_locations_column_timestamp)))
                                    .setLat(cursor.getDouble(cursor.getColumnIndexOrThrow(Database_io.Database_info.recorded_locations_column_latitude)))
                                    .setLongt(cursor.getDouble(cursor.getColumnIndexOrThrow(Database_io.Database_info.recorded_locations_column_longtitude)))
                                    .setAlt(cursor.getDouble(cursor.getColumnIndexOrThrow(Database_io.Database_info.recorded_locations_column_altitude)));
                            Calendar calendar = Calendar.getInstance();
                            TimeZone tz = TimeZone.getDefault();
                            calendar.setTimeInMillis(l_i.timestamp);
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            Date currenTimeZone = (Date) calendar.getTime();
                            l_i.date=sdf.format(currenTimeZone);
                            temp.add(l_i);
                        }
                        cursor.close();
                        data_array=(ArrayList<location_info>)temp.clone();
                        all_rows_number=(int)db_io.get_all_rows_count();
                        current_data_offset=new_data_offset;
                        temp=null;
                    }
                }
            }).start();
        }
        public location_info get(int position){
            if(position-current_data_offset<data_array.size()){
                return data_array.get(position-current_data_offset);
            }
            return null;//apologies; data is being loaded!
        }
        public int total_size(){//how many rows in total?
            return all_rows_number;
        }
        public void destroy_current_data(){
            all_rows_number=(int)db_io.get_all_rows_count();
            current_data_offset=0;
            current_view_position=0;
            data_array.removeAll(data_array);
        }
    }
    private recycler_data_set dataset;

    @TargetApi(23)
    private void handle_permissions(){
        if ((ContextCompat.checkSelfPermission(main.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)|ContextCompat.checkSelfPermission(main.this,
                Manifest.permission.ACCESS_FINE_LOCATION))
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(main.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)||ActivityCompat.shouldShowRequestPermissionRationale(main.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                //it was just denied, let the user decide
                ActivityCompat.requestPermissions(main.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_FINE_LOCATION},
                        WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST);
            } else {
                ActivityCompat.requestPermissions(main.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_FINE_LOCATION},
                        WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST);
                //it was denied permanently; either user made this decision or the OS policies prevented it from being granted
            }
        } else {
            init_activity();
        }
    }
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        try {
            MapsInitializer.initialize(getApplicationContext());
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
        }
        if(Build.VERSION.SDK_INT>22) {
            permission_warning = findViewById(R.id.permission_warrning);
            findViewById(R.id.ask_permissions).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handle_permissions();
                }
            });
            handle_permissions();
        }else{
            init_activity();
        }
    }
    private void apply_smaller_attributes_to_textview(TextView textView){
        RelativeLayout.LayoutParams small_layout_params=(RelativeLayout.LayoutParams) textView.getLayoutParams();
        int small_margin=(int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                5,
                getApplicationContext().getResources().getDisplayMetrics()
        );
        if(Build.VERSION.SDK_INT>=17){
            small_layout_params.setMarginEnd(small_margin);
            small_layout_params.setMarginStart(small_margin);
        }
        small_layout_params.setMargins(small_margin,small_margin,small_margin,small_margin);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,getResources().getDimension(R.dimen.text_size_small));
        textView.setLayoutParams(small_layout_params);
    }
    private Dialog get_tracking_options_dialog,get_filter_options_dialog;
    private void init_activity(){
        //permission 'granit' :)

        permission_warning.setVisibility(View.GONE);

        db_io=new Database_io(getApplicationContext());

        final RelativeLayout main=findViewById(R.id.main);
        TransitionManager.beginDelayedTransition(main,new androidx.transition.Slide());

        main_menu =main.findViewById(R.id.main_menu);
        locationManager=(LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        start_service_button =(TextView) findViewById(R.id.start_service_id);
        stop_service_button =(TextView) findViewById(R.id.stop_service_id);
        show_result_but=(TextView) findViewById(R.id.show_result_but_id);
        filter_results_but=(TextView)findViewById(R.id.filter_results_but);
        recyclerView = (RecyclerView) findViewById(R.id.recycy);
        layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        filter_options_menu=(LinearLayout) getLayoutInflater().inflate(R.layout.filter_results_layout,null);

        RelativeLayout.LayoutParams params=(RelativeLayout.LayoutParams)recyclerView.getLayoutParams();
        params.height=(int)(getResources().getConfiguration().screenHeightDp*getResources().getDisplayMetrics().density/3);
        recyclerView.setLayoutParams(params);

        db_io=new Database_io(getApplicationContext());

        location_tracker_service_intent =new Intent(getApplicationContext(),back_ground_tracking.class);

        get_tracking_options_dialog =new Dialog(main.this);
        get_tracking_options_dialog.setContentView(R.layout.options);
        get_tracking_options_dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        dist=(EditText) get_tracking_options_dialog.findViewById(R.id.distance_id);interv=(EditText) get_tracking_options_dialog.findViewById(R.id.interval_id);
        get_tracking_options_dialog.findViewById(R.id.done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                    Toast.makeText(getApplicationContext(),"GPS sensor is turned off",Toast.LENGTH_LONG).show();
                    return;
                }
                if(dist.getText().toString().length()==0){
                    Toast.makeText(getApplicationContext(),"Minimum distance change must be at least zero",Toast.LENGTH_LONG).show();
                    return;
                }
                if(interv.getText().toString().length()==0){
                    Toast.makeText(getApplicationContext(),"Minimum time interval must be at least zero",Toast.LENGTH_LONG).show();
                    return;
                }
                location_tracker_service_intent.putExtra("distance",back_ground_tracking.Stringnumtoint(dist.getText().toString()));
                location_tracker_service_intent.putExtra("interval",back_ground_tracking.Stringnumtoint(interv.getText().toString())*60000);
                //from android O on sole background services are limited and will be killed if app gets idle; so we must start as a  foreground service
                // in order to keep it running
                if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
                    location_tracker_service_intent.putExtra("command",back_ground_tracking.COMMAND_START_RESTART_TRACKING);
                    startForegroundService(location_tracker_service_intent);
                }else {
                    startService(location_tracker_service_intent);
                }
                start_service_button.setText("Restart tracking with new configuration");
                stop_service_button.setVisibility(View.VISIBLE);
                get_tracking_options_dialog.dismiss();
            }
        });
        get_filter_options_dialog=new Dialog(main.this);
        get_filter_options_dialog.setContentView(R.layout.filter_results_layout);
        get_filter_options_dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        start_date=(EditText) get_filter_options_dialog.findViewById(R.id.after);
        end_date=(EditText)get_filter_options_dialog.findViewById(R.id.before);
        start_time=(EditText)get_filter_options_dialog.findViewById(R.id.after_time);
        end_time=(EditText)get_filter_options_dialog.findViewById(R.id.before_time);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            start_date.setShowSoftInputOnFocus(false);
            end_date.setShowSoftInputOnFocus(false);
            start_time.setShowSoftInputOnFocus(false);
            end_time.setShowSoftInputOnFocus(false);
        } else {
            start_date.setTextIsSelectable(true);
            end_date.setTextIsSelectable(true);
            start_time.setTextIsSelectable(true);
            end_time.setTextIsSelectable(true);
        }
        start_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment a=new Date_and_Time_PickerFragment(Date_and_Time_PickerFragment.Type_Date,start_date);
                a.show(getSupportFragmentManager(),"d");
            }
        });
        end_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment a=new Date_and_Time_PickerFragment(Date_and_Time_PickerFragment.Type_Date,end_date);
                a.show(getSupportFragmentManager(),"d");
            }
        });
        start_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment a=new Date_and_Time_PickerFragment(Date_and_Time_PickerFragment.Type_Time,start_time);
                a.show(getSupportFragmentManager(),"d");
            }
        });
        end_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment a=new Date_and_Time_PickerFragment(Date_and_Time_PickerFragment.Type_Time,end_time);
                a.show(getSupportFragmentManager(),"d");
            }
        });
        get_filter_options_dialog.findViewById(R.id.apply_filter_but_id).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if(start_date.getText().toString().length()==0){
                        Toast.makeText(getApplicationContext(),"Set start date",Toast.LENGTH_LONG).show();
                        return;
                    }
                    if(start_time.getText().toString().length()==0){
                        Toast.makeText(getApplicationContext(),"Set start time",Toast.LENGTH_LONG).show();
                        return;
                    }
                    if(end_date.getText().toString().length()==0){
                        Toast.makeText(getApplicationContext(),"Set end date",Toast.LENGTH_LONG).show();
                        return;
                    }
                    if(end_time.getText().toString().length()==0){
                        Toast.makeText(getApplicationContext(),"Set end time",Toast.LENGTH_LONG).show();
                        return;
                    }
                    db_io.filters.start_date_timestamp=new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(start_date.getText().toString()+" "+
                            start_time.getText().toString()).getTime();
                    db_io.filters.end_date_timestamp=new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(end_date.getText().toString()+" "+
                            end_time.getText().toString()).getTime();
                    dataset.destroy_current_data();
                    adap.notifyDataSetChanged();
                    get_filter_options_dialog.dismiss();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });
        start_service_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                get_tracking_options_dialog.show();
            }
        });
        stop_service_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start_service_button.setText("Start tracking location");
                stopService(location_tracker_service_intent);
                stop_service_button.setVisibility(View.GONE);
            }
        });
        show_result_but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(db_io.tracked_locations_exists()){
                    int i=0;
                    while(i<main_menu.getChildCount()){
                        apply_smaller_attributes_to_textview((TextView) main_menu.getChildAt(i));
                        i++;
                    }
                    main_menu.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT));
                    main_menu.setBackgroundColor(Color.TRANSPARENT);
                    load_locations();
                    show_result_but.setVisibility(View.GONE);
                    filter_results_but.setVisibility(View.VISIBLE);
                } else{
                    Toast.makeText(getApplicationContext(),"No location recorded" ,Toast.LENGTH_SHORT).show();
                }
            }
        });
        filter_results_but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                get_filter_options_dialog.show();
            }
        });
        //check if for any reason(including user pressing the stop button in the notification on api levels>=26) the
        //service is destroyed;
        keep_ui_updated_with_background_processes_thread =new Thread(new Runnable() {
            @Override
            public void run() {
                while(!app_exited){
                    main.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //obvious!
                            if(back_ground_tracking.Service_is_running&&!start_service_button.getText().equals("Restart tracking with new configuration")){
                                start_service_button.setText("Restart tracking with new configuration");
                                stop_service_button.setVisibility(View.VISIBLE);
                            }
                            if(!back_ground_tracking.Service_is_running&& start_service_button.getText().equals("Restart tracking with new configuration")){
                                start_service_button.setText("Start tracking location");
                                stop_service_button.setVisibility(View.GONE);
                            }
                            if(show_result_but.getVisibility()==View.GONE){//we're showing results;
                                adap.notifyDataSetChanged();
                            }
                        }
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        keep_ui_updated_with_background_processes_thread.start();
    }
    public void load_locations() {
        dataset=new recycler_data_set();
        adap = new fileadapter();
        recyclerView.setAdapter(adap);
        adap.notifyDataSetChanged();
    }
    public class fileadapter extends RecyclerView.Adapter<fileadapter.fileholder> {

        private Marker marker;
        public fileadapter() {
        }

        public class fileholder extends RecyclerView.ViewHolder {
            public TextView lat, longt, time, alt;
            LinearLayout rec_lo;

            public fileholder(View view) {
                super(view);
                rec_lo = (LinearLayout) view.findViewById(R.id.rec_layout);
                lat = (TextView) view.findViewById(R.id.lat_id);
                longt = (TextView) view.findViewById(R.id.longt_id);
                time = (TextView) view.findViewById(R.id.time_stamp_id);
                alt = (TextView) view.findViewById(R.id.altit_id);
            }
        }
        public fileadapter.fileholder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemview = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.rec, parent, false);
            return new fileadapter.fileholder(itemview);
        }
        @Override
        public void onBindViewHolder(final fileadapter.fileholder holder, final int position) {
            dataset.current_view_position=position;
            final location_info l_i=dataset.get(position);
            if(l_i!=null){
                holder.lat.setText(String.valueOf(l_i.lat));
                holder.longt.setText(String.valueOf(l_i.longt));
                holder.alt.setText(String.valueOf(l_i.alt));
                holder.time.setText(String.valueOf(l_i.timestamp));
                holder.rec_lo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(marker!=null)marker.remove();
                        marker = googleMap.addMarker(new MarkerOptions()
                                .position(new LatLng(l_i.lat,l_i.longt))
                                .title(l_i.date));
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 1000));
                    }
                });
            }else{
                holder.lat.setText("loading...");
                holder.longt.setText("loading...");
                holder.alt.setText("loading...");
                holder.time.setText("loading...");
            }
        }
        public int getItemCount() {
            return dataset.total_size();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST){
            /*if both permissions are granted start the activity, else display an explaination about why the
            permissions are necessary;
            * */
            //we need all the permissions; so we just loop through all the permissions;
            int i=0;
            while(i<grantResults.length){
                if(grantResults[i]!=PackageManager.PERMISSION_GRANTED)break;
                i++;
            }
            if(grantResults.length==i){//this condition means all the permissions were granted; otherwise the loop would have broken sooner that variable 'i' reaches 'grantResults.length';
                init_activity();
                return;
            }
            if(permission_warning.getVisibility()==View.GONE)permission_warning.setVisibility(View.VISIBLE);
        }
    }
    public void onMapReady(GoogleMap map) {
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        googleMap = map;
    }
    protected void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
        app_exited=true;
        if(db_io!=null)db_io.close();
    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
        super.onLowMemory();
    }
}