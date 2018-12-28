package com.example.nwong.tasks;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GoogleDirectionsResponse {
    private String response;
    private static final String TAG = "GoogleDirectionsRespons";

    private JSONArray routes;
    private String[] polylines = null;

    public GoogleDirectionsResponse(String response){
        this.response = response;
    }

    public void parseRoutes(){
        try {
            JSONObject jsonObject = new JSONObject(this.response);
            routes = jsonObject.getJSONArray("routes");
            if (routes.length() < 1) {
                // no routes
                return;
            }

            polylines = new String[routes.length()];
            String oRoute = "";

            for(int i = 0; i < routes.length();i++){
                JSONObject route = routes.getJSONObject(i);
                oRoute = route.getJSONObject("overview_polyline").getString("points");
                polylines[i] = oRoute;
                Log.d(TAG,"Overview Poly: " + oRoute);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getFirstPolyline() {
        if (polylines != null && polylines.length > 0) {
            return polylines[0];
        }
        return "";
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
