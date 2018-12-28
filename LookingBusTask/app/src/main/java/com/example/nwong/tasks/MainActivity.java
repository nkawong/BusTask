package com.example.nwong.tasks;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.AutoCompleteTextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Main Activity";

    private GoogleMapFrag mMapFrag;
    private FragmentTransaction fragTrans;
   // private static final LatLngBounds latLongBounds = new LatLngBounds(new LatLng(-40, -168),new LatLng(71,136));

    //variables


    private static MainActivity instance = null;
    public static  MainActivity getInstance() {
        return instance;
    }

    //widgets
    private AutoCompleteTextView mDestination;
    private AutoCompleteTextView mStart;
    private GoogleDirectionsResponse response = null;


    //ListRouteFragment
    ListRouteFragment mlistRouteFragment =  new ListRouteFragment();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        mMapFrag = new GoogleMapFrag();
        fragTrans = getFragmentManager().beginTransaction().add(R.id.fragment_container,mMapFrag);
        fragTrans.commit();


        setContentView(R.layout.activity_main);
//        Log.d(TAG, "Testing parse message");
//        parseMessage(fileRead());

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
