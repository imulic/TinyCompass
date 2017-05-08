package com.snipsystems.irfanmulic.tinycompass;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.snipsystems.irfanmulic.model.Person;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener {


    static final float ALPHA = 0.25f;

    static final double ANGLE_DETECTION_DIFFERENCE = 10d;
    public static final String NA = "N/A";
    public static final String FIXED = "FIXED";
    // location min time
    private static final int LOCATION_MIN_TIME = 30 * 1000;
    // location min distance
    private static final int LOCATION_MIN_DISTANCE = 10;
    // Gravity for accelerometer data
    private float[] gravity = new float[3];
    // magnetic data
    private float[] geomagnetic = new float[3];
    // Rotation data
    private float[] rotation = new float[9];
    // orientation (azimuth, pitch, roll)
    private float[] orientation = new float[3];
    // smoothed values
    private float[] smoothed = new float[3];
    // sensor manager
    private SensorManager sensorManager;
    // sensor gravity
    private Sensor sensorGravity;
    private Sensor sensorMagnetic;
    private LocationManager locationManager;
    private Location currentLocation;
    private GeomagneticField geomagneticField;
    private double bearing = 0;
    private TextView textDirection, textLat, textLong;
    private Compass compassView;
    Vibrator v;
    private FirebaseAuth.AuthStateListener authListener;
    private FirebaseAuth auth;

    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    long[] pattern = {0, 500, 500, 300, 500, 300, 500, 300, 500}; // vibration pattern

    Ringtone r;

    ArrayList<Person> person_locations = new ArrayList<Person>();
    Person myLocation = new Person("irfan", 32.65702666004866d, -116.9703197479248d, "San Diego");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textLat = (TextView) findViewById(R.id.latitude);
        textLong = (TextView) findViewById(R.id.longitude);
        textDirection = (TextView) findViewById(R.id.text);
        compassView = (Compass) findViewById(R.id.compass);

        //get firebase auth instance
        auth = FirebaseAuth.getInstance();

        //get current user
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();

        mFirebaseRemoteConfig.setConfigSettings(configSettings);

        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);

        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    // user auth state is changed - user is null
                    // launch login activity
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }
            }
        };

        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        person_locations.add(new Person("Mama", Double.valueOf(32.850495943334d), Double.valueOf(-112.938938948d), "Bariloche"));

        // keep screen light on (wake lock light)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_people:
                Toast.makeText(this, "My People selected", Toast.LENGTH_SHORT)
                        .show();

                startActivity(new Intent(MainActivity.this, Persons.class));
                break;
            // action with ID action_settings was selected
            case R.id.action_logout:

                auth.signOut();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));

                break;
            case R.id.action_settings:

                startActivity(new Intent(MainActivity.this, SettingsActivity.class));

                break;
            case R.id.action_profile:

                startActivity(new Intent(MainActivity.this, ProfileActivity.class));

                break;
            default:
                break;
        }

        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorGravity = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // listen to these sensors
        sensorManager.registerListener(this, sensorGravity,
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorMagnetic,
                SensorManager.SENSOR_DELAY_NORMAL);

        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        // I forgot to get location manager from system service ... Ooops <img draggable="false" class="emoji" alt="😀" src="https://s.w.org/images/core/emoji/2.2.1/svg/1f600.svg">
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // request location data
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                LOCATION_MIN_TIME, LOCATION_MIN_DISTANCE, this);

        // get last known position
        Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);


        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
             Log.i("COMPASS"," User Logged in :" + auth.getCurrentUser().getDisplayName());
        }

        if (gpsLocation != null) {
            currentLocation = gpsLocation;
        } else {
            // try with network provider
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
               // return;
            }
            Location networkLocation = locationManager
                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (networkLocation != null) {
                currentLocation = networkLocation;
            } else {
                // Fix a position
                currentLocation = new Location(FIXED);
                currentLocation.setAltitude(1);
                currentLocation.setLatitude(43.296482);
                currentLocation.setLongitude(5.36978);
            }

            // set current location
            onLocationChanged(currentLocation);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // remove listeners
        sensorManager.unregisterListener(this, sensorGravity);
        sensorManager.unregisterListener(this, sensorMagnetic);
        locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        // used to update location info on screen
        updateLocation(location);
        geomagneticField = new GeomagneticField(
                (float) currentLocation.getLatitude(),
                (float) currentLocation.getLongitude(),
                (float) currentLocation.getAltitude(),
                System.currentTimeMillis());
    }

    private void updateLocation(Location location) {
        if (FIXED.equals(location.getProvider())) {
            textLat.setText(NA);
            textLong.setText(NA);
        }

        myLocation.setLat(location.getLatitude());
        myLocation.setLon(location.getLongitude());

        Log.i("IRFAN:","location found ");
        Log.i("Latitude:",String.format("%4.8f",location.getLatitude()));
        Log.i("Longitude:",String.format("%4.8f",location.getLongitude()));

        // better => make this creation outside method
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setDecimalSeparator('.');
        NumberFormat formatter = new DecimalFormat("#0.00", dfs);
        textLat.setText("Lat : " + formatter.format(location.getLatitude()));
        textLong.setText("Long : " + formatter.format(location.getLongitude()));
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

    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        boolean accelOrMagnetic = false;

        // get accelerometer data
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // we need to use a low pass filter to make data smoothed
            smoothed = lowPass(event.values, gravity);
            gravity[0] = smoothed[0];
            gravity[1] = smoothed[1];
            gravity[2] = smoothed[2];
            accelOrMagnetic = true;

        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            smoothed = lowPass(event.values, geomagnetic);
            geomagnetic[0] = smoothed[0];
            geomagnetic[1] = smoothed[1];
            geomagnetic[2] = smoothed[2];
            accelOrMagnetic = true;

        }

        // get rotation matrix to get gravity and magnetic data
        SensorManager.getRotationMatrix(rotation, null, gravity, geomagnetic);
        // get bearing to target
        SensorManager.getOrientation(rotation, orientation);
        // east degrees of true North
        bearing = orientation[0];
        // convert from radians to degrees
        bearing = Math.toDegrees(bearing);

        // fix difference between true North and magnetical North
        if (geomagneticField != null) {
            bearing += geomagneticField.getDeclination();
        }

        // bearing must be in 0-360
        if (bearing < 0) {
            bearing += 360;
        }

        // update compass view
        compassView.setBearing((float) bearing);

        if (accelOrMagnetic) {
            compassView.postInvalidate();
        }

        updateScreenInfoForBearing(bearing); // display text direction on screen
    }

    protected static double getBearing(double lat1, double lon1, double lat2, double lon2) {
        double longitude1 = lon1;
        double longitude2 = lon2;
        double latitude1 = Math.toRadians(lat1);
        double latitude2 = Math.toRadians(lat2);
        double longDiff = Math.toRadians(longitude2 - longitude1);
        double y = Math.sin(longDiff) * Math.cos(latitude2);
        double x = Math.cos(latitude1) * Math.sin(latitude2) - Math.sin(latitude1) * Math.cos(latitude2) * Math.cos(longDiff);

        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }


    private void updateScreenInfoForBearing(double bearing) {
        int range = (int) (bearing / (360f / 16f));
        String dirTxt = "";

        if (range == 15 || range == 0)
            dirTxt = "N";
        if (range == 1 || range == 2)
            dirTxt = "NE";
        if (range == 3 || range == 4)
            dirTxt = "E";
        if (range == 5 || range == 6)
            dirTxt = "SE";
        if (range == 7 || range == 8)
            dirTxt = "S";
        if (range == 9 || range == 10)
            dirTxt = "SW";
        if (range == 11 || range == 12)
            dirTxt = "W";
        if (range == 13 || range == 14)
            dirTxt = "NW";

        double bearingDiff = getBearing(myLocation.getLat(), myLocation.getLon(), person_locations.get(0).getLat(), person_locations.get(0).getLon());

        textDirection.setText("" + ((int) bearing) + ((char) 176) + " "
                + dirTxt+ "("+String.format("%8.2f",bearingDiff)+")"); // char 176 ) = degrees ...

        float angle_difference = (float)Math.abs(bearing - bearingDiff);

        compassView.setAngleDifference(angle_difference);

        //Log.i("IRFAN :",String.format("%8.2f",angle_difference));
        if (angle_difference < ANGLE_DETECTION_DIFFERENCE){
          //  v.vibrate(2000);
            v.vibrate(pattern,-1);
        //    playSound();
        } else {
            v.cancel();
            if (r != null && r.isPlaying()) r.stop();
        }


    }

    private void playSound(){
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
        r.play();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD
                && accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            // manage fact that compass data are unreliable ...
            // toast ? display on screen ?
        }
    }

}
