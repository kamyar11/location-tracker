package gps.test.tracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

import static android.app.Notification.EXTRA_NOTIFICATION_ID;
import static android.app.Notification.INTENT_CATEGORY_NOTIFICATION_PREFERENCES;

/**
 * Created by user_0 on 9/2/2017.
 */

public class back_ground_tracking extends Service {
    private int distance=0;
    private int interval=0;
    private int counter=0;
    private Database_io db_io;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LocationCallback locationCallback;
    public static final String COMMAND_START_RESTART_TRACKING="ddd";
    private String chanel_id;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        db_io=new Database_io(getApplicationContext());
        locationManager=(LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        locationListener=new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
//                SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                simpleDateFormat.setTimeZone(TimeZone.getDefault());
//                write_data(String.valueOf(location.getLatitude()),String.valueOf(location.getLongitude()),String.valueOf(location.getAltitude()),simpleDateFormat.format(new Date()));
                if(location.getProvider().equals(LocationManager.GPS_PROVIDER))//we want the accurate stuff;
                    db_io.write_location_to_database(location);
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }
            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, distance, locationListener);

        }catch (SecurityException e){
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }

//        locationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(LocationResult locationResult) {
//                if (locationResult == null) {
//                    return;
//                }
//                for (Location location : locationResult.getLocations()) {
//                    db_io.write_location_to_database(location);
//                }
//            };
//        };

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            //make it a foreground service and show a notification about it;
            //from android O on we notifications must have a notification chanel;
            //check for stop button in the notification
            if(intent.getExtras()==null||intent.getExtras().getString("command")==null||!intent.getExtras().getString("command").equals(back_ground_tracking.COMMAND_START_RESTART_TRACKING)){
                stopForeground(true);
                stopSelf();
                return Service.START_NOT_STICKY;
            }
            chanel_id="some_unique_text_id";
            NotificationChannel channel = new NotificationChannel(chanel_id, "locationtracker_notif_channel", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("locationtracker app notification channel");
            // Register the channel with the system;
            getSystemService(NotificationManager.class).createNotificationChannel(channel);

            //add a stop button too, now that we're creating a notification;
            Intent snoozeIntent = new Intent(this, back_ground_tracking.class);

            PendingIntent pendingIntent = PendingIntent.getService(this, 0, snoozeIntent, 0);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, chanel_id)
                    .setSmallIcon(R.drawable.gps_test_tracker)
                    .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
                    .setContentTitle("You're being tracked!")
                    .setContentText("The title says it so why bother with extra explanation?")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .addAction(R.drawable.gps_test_tracker,"stop",pendingIntent);

            startForeground(5000,builder.build());
        }
        distance=intent.getExtras().getInt("distance");
        interval=intent.getExtras().getInt("interval");
        //lets try new these new stuff;
//        LocationRequest locationRequest = LocationRequest.create();
//        locationRequest.setInterval(5000);
//        locationRequest.setFastestInterval(500);
//        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//        FusedLocationProviderClient client=new FusedLocationProviderClient(getApplicationContext());
//        client.requestLocationUpdates(locationRequest,null);

        locationManager.removeUpdates(locationListener);
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, distance, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, interval, distance, locationListener);
        }catch (SecurityException e){
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
            Log.d("error__",e.getMessage());
        }
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(locationListener);
        db_io.close();
    }

    public static int Stringnumtoint(String x){
        if(x==null)return 0;
        byte[] b=x.getBytes();
        int i=0,num_in_int=0;
        while(i<b.length){
            b[i]=(byte)(b[i]-0x30);
            num_in_int=num_in_int*10+b[i];
            i++;
        }
        return num_in_int;
    }

}
