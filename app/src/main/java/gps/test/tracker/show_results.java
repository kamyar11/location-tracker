package gps.test.tracker;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by user_0 on 9/2/2017.
 */

public class show_results extends Activity implements OnMapReadyCallback {
    private RecyclerView recyclerView;
    private ArrayList<String[]> data=new ArrayList<>();
    private fileadapter adap;
    private MapView mapView;
    private GoogleMap googleMap;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_result);

        recyclerView = (RecyclerView) findViewById(R.id.recycy);
        RecyclerView.LayoutManager layoutManager=new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        adap=new fileadapter();
        recyclerView.setAdapter(adap);
        load_locations();

        mapView=(MapView)findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        try{
            MapsInitializer.initialize(getApplicationContext());
        }catch (Exception e){
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }
    }

    public void onMapReady(GoogleMap map) {
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        googleMap=map;
    }

    public void load_locations(){
        try{
            File f=new File("sdcard/data_cap");
            InputStream is=new FileInputStream(f);
            String s="";
            byte b[]=new byte[1024];
            int c=is.read(b);
            while(c>0){
                s=s+new String(b,0,c);
                c=is.read(b);
            }
            is.close();
            byte reg[]=new byte[2];reg[0]='\n';reg[1]='\n';
            String datas[]=s.split(new String(reg));
            reg=new byte[1];reg[0]='\n';
            int i=0;
            while(i<datas.length){
                data.add(datas[i].split(new String(reg)));
                i++;
            }
            adap.notifyDataSetChanged();
        }catch (Exception e){
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }
    }

    public class fileadapter extends RecyclerView.Adapter<fileadapter.fileholder> {

        public fileadapter(){

        }

        public class fileholder extends RecyclerView.ViewHolder{
            public TextView lat,longt,time,alt;
            LinearLayout rec_lo;
            public fileholder(View view) {
                super(view);
                rec_lo=(LinearLayout)view.findViewById(R.id.rec_layout);
                lat=(TextView)view.findViewById(R.id.lat_id);
                longt=(TextView)view.findViewById(R.id.longt_id);
                time=(TextView)view.findViewById(R.id.time_stamp_id);
                alt=(TextView)view.findViewById(R.id.altit_id);
            }
        }
        public fileholder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemview= LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.rec, parent, false);
            return new fileholder(itemview);
        }

        @Override
        public void onBindViewHolder(final fileholder holder, final int position) {
            Calendar calendar=Calendar.getInstance(Locale.ENGLISH);
            calendar.setTimeInMillis(1000L*(long)(back_ground_tracking.Stringnumtoint(data.get(position)[2])));

            holder.lat.setText(data.get(position)[1]);
            holder.longt.setText(data.get(position)[0]);
            holder.alt.setText(data.get(position)[2]);
            holder.time.setText(data.get(position)[3]);

            holder.rec_lo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Marker marker=googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(Float.valueOf(data.get(position)[0]), Float.valueOf(data.get(position)[1])))
                            .title(data.get(position)[3]));
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(),1000));
                }
            });
        }
        public int getItemCount() {
            return data.size();
        }
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
    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
        super.onLowMemory();
    }
}
