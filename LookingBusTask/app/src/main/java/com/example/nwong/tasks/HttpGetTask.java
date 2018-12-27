package com.example.nwong.tasks;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

//Aysnc Task<Parameters, Progress, Results>
public class HttpGetTask extends AsyncTask<String, Integer, String> {
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

    //Automatically gets called after the return.
    @Override
    protected void onPostExecute(String s) {
        MainActivity.getInstance().parseMessage(s);
    }
}