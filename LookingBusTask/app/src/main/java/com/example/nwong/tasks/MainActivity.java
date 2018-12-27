package com.example.nwong.tasks;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "The Map Activity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private boolean mLocationPermissionGranted = false;
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final float DEFAULT_ZOOM =15f;
    private static final String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json";
    private static final String API_KEY = "AIzaSyCx8XJD1bSjLMFkMPVx1ILvwz810X-vUCc";

    //temporary bounds
   // private static final LatLngBounds latLongBounds = new LatLngBounds(new LatLng(-40, -168),new LatLng(71,136));

    //variables
    private GoogleMap mMap;
    private FusedLocationProviderClient mFuseLocationProviderClient;
    private PlaceAutoCompleteAdapter mPlaceAutoCompleteAdapter;
    //private GoogleApiClient mGoogleApiClient;
    protected GeoDataClient mGeoDataClient;
    private String destinationLatlng;
    private String startLatlng;

    private static MainActivity instance = null;
    public static  MainActivity getInstance() {
        return instance;
    }

    //widgets
    private AutoCompleteTextView mDestination;
    private AutoCompleteTextView mStart;
    private GoogleDirectionsResponse response = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        setContentView(R.layout.activity_main);
//        Log.d(TAG, "Testing parse message");
//        parseMessage(fileRead());
        mDestination = (AutoCompleteTextView) findViewById(R.id.input_search);
        mStart = (AutoCompleteTextView) findViewById(R.id.input_start);
        mGeoDataClient = Places.getGeoDataClient(this);
        getPermmissionLocation();

        if(isServiceOK()){
            initMap();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    private String fileRead(){
        StringBuilder buf=new StringBuilder();
        InputStream json= null;
        try {
            json = getAssets().open("route");
            BufferedReader in= new BufferedReader(new InputStreamReader(json, "UTF-8"));
            String str;

            while ((str=in.readLine()) != null) {
                buf.append(str);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return buf.toString();
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

    public void initMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(MainActivity.this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        if(mLocationPermissionGranted){
            getDeviceLocation();
        }
        init();
    }

    public void init(){

        //mPlaceAutoCompleteAdapter = new PlaceAutoCompleteAdapter(this,mGeoDataClient,latLongBounds,null);

        // setting up the listeners to the text fields
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
        String userInput = mDestination.getText().toString();

        if (userInput != null && !userInput.isEmpty()) {
            destinationLatlng = geoLocate(userInput, false);
        }

        if (startLatlng != null && !startLatlng.isEmpty() &&
                destinationLatlng != null && !destinationLatlng.isEmpty()) {
            // find bus route
            findRoutes();
        }
    }

    private void setStart() {
        String userInput = mStart.getText().toString();

        if (userInput != null && !userInput.isEmpty()) {
            // set origin
            startLatlng = geoLocate(userInput, false);
        }

        if (startLatlng != null && !startLatlng.isEmpty() &&
                destinationLatlng != null && !destinationLatlng.isEmpty()) {
            // find bus route
            findRoutes();
        }
    }

    private void findRoutes() {
        // safety checks
        if(startLatlng == null || startLatlng.isEmpty() ||
                destinationLatlng == null || destinationLatlng.isEmpty()||
                API_KEY == null || API_KEY.isEmpty()){
            Log.d(TAG, "Values are empty");
            return;
        }

        // Create Url with the user input
        String query = String.format("?origin=%s&destination=%s&mode=%s&alternatives=%s&key=%s",
                startLatlng, destinationLatlng, "transit", "true", API_KEY);
        String url = DIRECTIONS_URL + query;

        // Making the Async Web Request
        Log.d(TAG,"TRYING TO CONNECT to " + url);
        new HttpGetTask().execute(url);
    }

    private String geoLocate(String mSearchString, boolean shouldPosition){

        Geocoder geocoder = new Geocoder(MainActivity.this);
        List<Address> list = new ArrayList<>();

        try{
            list = geocoder.getFromLocationName(mSearchString,5);

            if(list == null || list.size() == 0){
                return null;
            }
            Address location = list.get(0);
            if (shouldPosition) {
                moveCamera(new LatLng(location.getLatitude(),location.getLongitude()), DEFAULT_ZOOM, location.getAddressLine(0));
            }
            String position = String.format("%.5f,%.5f", location.getLatitude(), location.getLongitude());
            return position;
        }catch(IOException e){
            Log.e(TAG, "GeoLocate: error:" + e.getMessage().toString() );
        }

        return null;
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


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    //Parsing the Json Response
    public void parseMessage(String result){
        if(result !=null && !result.isEmpty()){
            Log.d(TAG, "http result: " + result);
            response = new GoogleDirectionsResponse(result);
            response.parseRoutes();
            //populateListView();
            return;
        }
        //TODO Prompt message
        Log.d(TAG, "Response returned empty.");
    }

//    public void populateListView() {
//
//    }

}
