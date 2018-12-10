package com.example.nikola.zombieseminary.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.JsonReader;
import android.util.Log;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startMe();

    }

    void startMe(){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // stvaranje endpointa sa keyom
                    URL mojEndpoint = new URL("https://vision.googleapis.com/v1/images:annotate?key=AIzaSyBzNFlNI_G6ROs2L1lLaQYYbg0U1sDwE2w");
                    // otvaranje veze
                    HttpsURLConnection myConnection =
                            (HttpsURLConnection) mojEndpoint.openConnection();
                    // dodavanje custom user agenta
                    // postavi zahtjev na post
                    myConnection.setRequestMethod("POST");
                    myConnection.setRequestProperty("Content-Type", "application/json");
                    HashMap<String, String> m = new HashMap<String, String>();


                    JSONObject source1 = new JSONObject();


                    source1.put("imageUri", "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png");
                    JSONObject image1 = new JSONObject();
                    image1.put("source", source1);

                    JSONObject image2 = new JSONObject();
                    image2.put("image", image1);

                    JSONArray arF = new JSONArray();
                    JSONObject arFF = new JSONObject();

                    arFF.put("type", "LOGO_DETECTION");
                    arFF.put("maxResults", 1);
                    arF.put(arFF);


                    image2.put("features", arF);

                    JSONArray ar = new JSONArray();

                    ar.put(image2);


                    Log.d("Zombie", ar.getString(0));
                    JSONObject req = new JSONObject();

                    req.put("requests", ar);

                    Log.d("Zombie", req.toString());

                    // Enable writing
                    myConnection.setDoOutput(true);

                    /* DA NE TROŠIMO PODATKE HEHE

                   // myConnection.getOutputStream().write(myData.getBytes());
                    OutputStreamWriter myWrt = new OutputStreamWriter(myConnection.getOutputStream());

                    myWrt.write(req.toString());
                    myWrt.flush();
                    myWrt.close();


                    Log.d("Zombie ",String.valueOf(myConnection.getResponseCode()));
                    Log.d("Zombie ",String.valueOf(myConnection.getResponseMessage()));

                    InputStream is = myConnection.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);

                    String myResponse ="";

                    int cc = isr.read();

                    while(cc != -1){
                        char theChar = (char) cc;
                        cc = isr.read();
                        myResponse += theChar;
                    }
                    Log.d("Zombie", myResponse);
                    isr.close();


                    myConnection.disconnect();
                        */

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
