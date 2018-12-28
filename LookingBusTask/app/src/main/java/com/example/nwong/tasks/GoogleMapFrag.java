package com.example.nwong.tasks;

import android.Manifest;
import android.app.Dialog;
import android.app.Fragment;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GoogleMapFrag extends Fragment implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "The Map Activity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private boolean mLocationPermissionGranted = false;
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final float DEFAULT_ZOOM =15f;
    private static final String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json";
    private static final String API_KEY = "AIzaSyCx8XJD1bSjLMFkMPVx1ILvwz810X-vUCc";


    private GoogleMap mMap;
    private FusedLocationProviderClient mFuseLocationProviderClient;
    private PlaceAutoCompleteAdapter mPlaceAutoCompleteAdapter;
    //private GoogleApiClient mGoogleApiClient;
    protected GeoDataClient mGeoDataClient;
    private String destinationLatlng;
    private String startLatlng;

    //widgets
    private AutoCompleteTextView mDestination;
    private AutoCompleteTextView mStart;
    private GoogleDirectionsResponse response = null;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.map, container, false);
    }

    private void getPermmissionLocation(){
        String[] permission = {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){

            if(ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED){
                mLocationPermissionGranted = true;
            }
            else{
                ActivityCompat.requestPermissions(getActivity(), permission, LOCATION_PERMISSION_REQUEST_CODE);

            }
        }
        else{
            ActivityCompat.requestPermissions(getActivity(), permission, LOCATION_PERMISSION_REQUEST_CODE);

        }
    }

    public boolean isServiceOK(){
        Log.d(TAG, "isServiceOK: checking service version");
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getActivity());

        if(available == ConnectionResult.SUCCESS){
            Log.d(TAG, "isServiceOK: Google Play Service is working");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            Log.d(TAG,"isServiceOK: error has occurred, but can be fixed");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(getActivity(),available,ERROR_DIALOG_REQUEST);
            dialog.show();
        }
        else{
            Toast.makeText(getActivity(), "You can't make map request", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    public void initMap(){
        FragmentManager manager = getFragmentManager();
        SupportMapFragment mapFragment = (SupportMapFragment) manager.findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
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

        Geocoder geocoder = new Geocoder(getActivity());
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
        mFuseLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());
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

}
