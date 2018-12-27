package com.example.nwong.tasks;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GoogleDirectionsResponse {
    private String response;
    private static final String TAG = "GoogleDirectionsRespons";

    private JSONArray routes;

    public GoogleDirectionsResponse(String response){
        this.response = response;
    }

    public void parseRoutes(){
        try {
            JSONObject jsonObject = new JSONObject(this.response);
            routes = jsonObject.getJSONArray("routes");
            String oRoute = "";

            for(int i = 0; i < routes.length();i++){
                JSONObject route = routes.getJSONObject(i);
                oRoute = route.getJSONObject("overview_polyline").getString("points");
                Log.d(TAG,"Overview Poly: " + oRoute);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

//    public String[] getRouteNames() {
//        String[] somethin = new String[routes.length()];
//
//        somethin[0] = getRouteName(routes.getJSONObject(i))
//    }
//
//    private String getRouteName(JSONObject route) {
//
//    }
}
