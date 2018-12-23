package com.example.nwong.tasks;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Location;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.maps.GeoApiContext;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import java.net.*;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "The Map Activity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private boolean mLocationPermissionGranted = false;
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final float DEFAULT_ZOOM =15f;
    private static final LatLngBounds latLongBounds = new LatLngBounds(new LatLng(-40, -168),new LatLng(71,136));

    //variables
    private GoogleMap mMap;
    private FusedLocationProviderClient mFuseLocationProviderClient;
    private PlaceAutoCompleteAdapter mPlaceAutoCompleteAdapter;
    //private GoogleApiClient mGoogleApiClient;
    protected GeoDataClient mGeoDataClient;

    //widgets
    private AutoCompleteTextView mDestination;
    private AutoCompleteTextView mStart;

    private String destination;
    private String start;

    private String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json";
    private String API_KEY = "AIzaSyCCe9qpiUro4-M4Y6MzQ2bSFgCZ_IO7S9A";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDestination = (AutoCompleteTextView) findViewById(R.id.input_search);
        mStart = (AutoCompleteTextView) findViewById(R.id.input_start);
        mGeoDataClient = Places.getGeoDataClient(this);
        getPermmissionLocation();

        if(isServiceOK()){
            initMap();
        }
        String query = String.format("?origin=%s&destination=%s&key=%s&mode=%s&alternatives=%s",
                "37.78594,-122.47111", "37.77963,-122.47067", API_KEY, "transit", "true");
        String url = DIRECTIONS_URL + query;
        Log.d(TAG,"TRYING TO CONNECT");
        new HttpGetTask().execute(url);
    }
    public void init(){

        mPlaceAutoCompleteAdapter = new PlaceAutoCompleteAdapter(this,mGeoDataClient,latLongBounds,null);

        mDestination.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || event.getAction() == KeyEvent.ACTION_DOWN|| event.getAction() == KeyEvent.KEYCODE_ENTER){
                    //executes method for searching
                    setDestination();
                }
                return false;
            }
        });
        mStart.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || event.getAction() == KeyEvent.ACTION_DOWN|| event.getAction() == KeyEvent.KEYCODE_ENTER){
                    //executes method for searching
                    setStart();
                }
                return false;
            }
        });
    }

    private void setDestination() {
        destination = mDestination.getText().toString();

        if (start != "" && destination != "") {
            // find bus route
        }
    }

    private void setStart() {
        start = mStart.getText().toString();

        if (start!= "") {
            // set origin
//            geoLocate(start);
        }

        if (start != "" && destination != "") {
            // find bus route
        }
    }

    private void geoLocate(){
        String searchString = mDestination.getText().toString();

        Geocoder geocoder = new Geocoder(MainActivity.this);
        List<Address> list = new ArrayList<>();

        try{
            list = geocoder.getFromLocationName(searchString, 1);
        }catch(IOException e){
            Log.e(TAG, "GeoLocate IOException: " + e.getMessage());
        }
        if(list.size() > 0){
            Address address = list.get(0);

            Log.d(TAG, "GeoLocate Found: " + address.toString());
            moveCamera(new LatLng(address.getLatitude(),address.getLongitude()), DEFAULT_ZOOM, address.getAddressLine(0));
        }

    }
    private void getPermmissionLocation(){
        String[] permission = {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){

            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED){
                mLocationPermissionGranted = true;
            }
            else{
                ActivityCompat.requestPermissions(this, permission, LOCATION_PERMISSION_REQUEST_CODE);

            }
        }
        else{
            ActivityCompat.requestPermissions(this, permission, LOCATION_PERMISSION_REQUEST_CODE);

        }
    }
    private void getDeviceLocation(){
        mFuseLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try{
            if(mLocationPermissionGranted){
                Task location = mFuseLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                         if(task.isSuccessful()){
                             Location curLocation = (Location) task.getResult();
                             moveCamera(new LatLng(curLocation.getLatitude(),curLocation.getLongitude()),DEFAULT_ZOOM,"My Location");
                         }
                    }
                });
            }
        }catch(SecurityException e){
            Log.e(TAG, "getDeviceLocation SecurityException: " + e.getMessage());
        }
    }
    private void moveCamera(LatLng latLng, float zoom, String title){
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        MarkerOptions marker = new MarkerOptions().position(latLng).title(title);
        mMap.addMarker(marker);
    }
    public void initMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(MainActivity.this);
    }
    public boolean isServiceOK(){
        Log.d(TAG, "isServiceOK: checking service version");
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if(available == ConnectionResult.SUCCESS){
            Log.d(TAG, "isServiceOK: Google Play Service is working");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            Log.d(TAG,"isServiceOK: error has occurred, but can be fixed");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this,available,ERROR_DIALOG_REQUEST);
            dialog.show();
        }
        else{
            Toast.makeText(this, "You can't make map request", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        if(mLocationPermissionGranted){
            getDeviceLocation();
        }
        init();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    public void logMessage(String result){
        if(result !=null && !result.isEmpty()){
            Log.d(TAG, "http result: " + result);
            return;
        }
    }
    private class HttpGetTask extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... urls) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(urls[0]);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.connect();
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String content = "", line;
                while ((line = rd.readLine()) != null) {
                    content += line + "\n";
                }
                return content;
            } catch (Exception e) {
                Log.d( "Connection error", e.getMessage());
                return null;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(String s) {
            logMessage(s);
        }
    }



}
