package com.example.nikola.zombieseminary.app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;


public class MainActivity extends AppCompatActivity {

    
    /*
    * @TODO Dovršiti tutorial, dodati gumb za slikanje, spremanje fotke itd.
    * @TODO I prednja i zadnja kamera, i rezolucije
    * Spremanje napraviti u asynctasku, na finishu prikazati sliku u fragmentu
    * U fragmentu dodati dva gumba, jedan za slanje, drugi za brisanje
    *
    * KORISTI VISIBILITY - GONE, nikakvi dodatni activitiji!
    *
    *
     */

    private static final String TAG = "VELIKIDREK";

    private static final int PICK_IMAGE = 50;

    private static final int CAMERA_REQUEST_CODE = 22;
    private CameraManager cameraManager;
    private int cameraFacing;
    private TextureView.SurfaceTextureListener surfaceTextureListener;
    private String cameraId;
    private Size previewSize;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private CameraDevice.StateCallback stateCallback;
    private CameraDevice cameraDevice;
    private TextureView textureView;
    private CaptureRequest.Builder captureRequestBuilder;
    private CaptureRequest cameraRequest;
    private CameraCaptureSession cameraCaptureSession;
    private StreamConfigurationMap streamConfigurationMap;
    private FragmentManager fragmentManager;
    private File galleryFolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        

//        startMe();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.INTERNET},
                    0);

        }

        Button buttonGallery = findViewById(R.id.buttonGallery);

        buttonGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
            }


        });



        textureView = findViewById(R.id.textureView);
        NavigationView nav = findViewById(R.id.nav_view);
        DrawerLayout drawer =  findViewById(R.id.drawer_layout);


        nav.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        menuItem.setChecked(true);


                        changeCamDimen(menuItem.getItemId());

                        drawer.closeDrawers();


                        return false;
                    }
                }
        );


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    CAMERA_REQUEST_CODE);

        }

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        cameraFacing = CameraCharacteristics.LENS_FACING_BACK;

        surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                // TextureView je slobodann
                setUpCamera();
                openCamera();

                ConstraintLayout layout = findViewById(R.id.ConLay);

                Log.e("Zombie1", previewSize.toString());
                
                float ratio = (float) previewSize.getWidth() / (float) previewSize.getHeight();
                int newH = (int) (layout.getWidth() * ratio);
                Log.e("Zombie", String.valueOf(newH));
                if(newH > layout.getHeight()){
                    newH = layout.getHeight();
                }



                ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(layout.getWidth(), newH);

                textureView.setLayoutParams(params);

                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(layout);
                constraintSet.connect(R.id.textureView,ConstraintSet.TOP,R.id.ConLay,ConstraintSet.TOP,0);
                constraintSet.connect(R.id.textureView,ConstraintSet.BOTTOM,R.id.ConLay,ConstraintSet.BOTTOM,0);
                constraintSet.applyTo(layout);

                Menu m = nav.getMenu();

                for(int i = 0; i < streamConfigurationMap.getOutputSizes(SurfaceTexture.class).length; i++){
                    String name = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[i].toString();
                    m.add(R.id.group_res, i, 0, name).setCheckable(true);
                }
                m.getItem(0).setChecked(true);


            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        };

        stateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                MainActivity.this.cameraDevice = camera;
                createPreviewSession();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                camera.close();
                MainActivity.this.cameraDevice = null;

            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                camera.close();
                MainActivity.this.cameraDevice = null;
            }
        };



        Button btn = findViewById(R.id.button3);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                Bitmap bitmap = textureView.getBitmap();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream);
                byte[] bytes = stream.toByteArray();
                String encoded = Base64.encodeToString(bytes, Base64.DEFAULT);

                try {
                    cameraCaptureSession.stopRepeating();
                    //cameraCaptureSession.capture(captureRequestBuilder.build(), null, backgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }*/

               /* Bitmap bitmap = textureView.getBitmap();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream);
                byte[] bytes = stream.toByteArray();
                String encoded = Base64.encodeToString(bytes, Base64.DEFAULT);
/*
                ImageView temp_img = new ImageView(MainActivity.this);
                temp_img.setImageBitmap( bitmap);


                FrameLayout relativeLayout = (FrameLayout) findViewById(R.id.content_frame);
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT

                );


                relativeLayout.addView(temp_img, layoutParams);


*/
                /*
                                                                 
                fragment = new BlankFragment();
                fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                fragmentTransaction.add(R.id.content_frame, fragment).commit();
*/

                Task tt = new Task();
                tt.execute();

/*
                ImageView imageView = new ImageView(v.getContext());

                imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                imageView.setBackgroundColor(Color.argb(255,200,200,200));
                imageView.setAdjustViewBounds(true);
                imageView.setImageResource(R.drawable.wakeupcat);


                FrameLayout layout = findViewById(R.id.content_frame);
                layout.addView(imageView);

                for(int index=0; index<((ViewGroup)layout).getChildCount(); ++index) {
                    View nextChild = ((ViewGroup)layout).getChildAt(index);
                    if(nextChild instanceof ImageView){
                        layout.removeView(nextChild);
                        break;
                    }
                }*/
                //startMe(encoded)
            }
        });


    }


    private void changeCamDimen(int index){

        previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[index];

        ConstraintLayout layout = findViewById(R.id.ConLay);

        Log.e("Zombie1", previewSize.toString());

        float ratio = (float) previewSize.getWidth() / (float) previewSize.getHeight();
        int newH = (int) (layout.getWidth() * ratio);
        Log.e("Zombie", String.valueOf(newH));
        if(newH > layout.getHeight()){
            newH = layout.getHeight();
        }

        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(layout.getWidth(), newH);

        textureView.setLayoutParams(params);

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(layout);
        constraintSet.connect(R.id.textureView,ConstraintSet.TOP,R.id.ConLay,ConstraintSet.TOP,0);
        constraintSet.connect(R.id.textureView,ConstraintSet.BOTTOM,R.id.ConLay,ConstraintSet.BOTTOM,0);
        constraintSet.applyTo(layout);

        openCamera();

    }

    class Task2 extends AsyncTask<Bitmap, Void, String>
    {
        private Bitmap bmp;

        @Override
        protected String doInBackground(Bitmap... bitmaps) {
            try {
                // Mora biti mutable
                bmp = bitmaps[0].copy(Bitmap.Config.ARGB_8888, true);


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
/*

                    source1.put("imageUri", "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png");
                    JSONObject image1 = new JSONObject();
                    image1.put("source", source1);


                    JSONObject image2 = new JSONObject();
                    image2.put("image", image1);
*/
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] bytes = stream.toByteArray();

                String con = Base64.encodeToString(bytes, Base64.DEFAULT);


                JSONObject imageCon = new JSONObject();
                imageCon.put("content", con);

                JSONObject image2 = new JSONObject();
                image2.put("image", imageCon);

                JSONArray arF = new JSONArray();
                JSONObject arFF = new JSONObject();

                // arFF.put("type", "OBJECT_LOCALIZATION");
                // To je za traženje multiple objekta
                arFF.put("type", "LABEL_DETECTION");



                //arFF.put("maxResults", 1);
                arF.put(arFF);


                image2.put("features", arF);

                JSONArray ar = new JSONArray();

                ar.put(image2);


                Log.d("Zombie", ar.getString(0));
                JSONObject req = new JSONObject();

                req.put("requests", ar);

                Log.d("Zombie", req.toString());

/*
                    int maxLogSize = 1000;
                    for(int i = 0; i <= req.toString().length() / maxLogSize; i++) {
                        int start = i * maxLogSize;
                        int end = (i+1) * maxLogSize;
                        end = end > req.toString().length() ? req.toString().length() : end;
                        Log.v("Zombie", req.toString().substring(start, end));
                    }
*/
                //  DA NE TROŠIMO PODATKE HEHE
                // Enable writing
                myConnection.setDoOutput(true);


                // myConnection.getOutputStream().write(myData.getBytes());
                OutputStreamWriter myWrt = new OutputStreamWriter(myConnection.getOutputStream());

                myWrt.write(req.toString());
                myWrt.flush();
                myWrt.close();


                Log.d("Zombie ",String.valueOf(myConnection.getResponseCode()));
                Log.d("Zombie ",String.valueOf(myConnection.getResponseMessage()));

                InputStream is = myConnection.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);

                String jsonString ="";

                int cc = isr.read();

                while(cc != -1){
                    char theChar = (char) cc;
                    cc = isr.read();
                    jsonString += theChar;
                }
                Log.d("ZombieString", jsonString);
                isr.close();


                myConnection.disconnect();

/*
                    String jsonString = "" +
                            "{ \"responses\": [\n" +
                            "        {\n" +
                            "          \"localizedObjectAnnotations\": [\n" +
                            "            {\n" +
                            "              \"mid\": \"/m/0k4j\",\n" +
                            "              \"name\": \"Car\",\n" +
                            "              \"score\": 0.73251706,\n" +
                            "              \"boundingPoly\": {\n" +
                            "                \"normalizedVertices\": [\n" +
                            "                  {\n" +
                            "                    \"x\": 0.73255426,\n" +
                            "                    \"y\": 0.7711425\n" +
                            "                  },\n" +
                            "                  {\n" +
                            "                    \"x\": 0.88020134,\n" +
                            "                    \"y\": 0.7711425\n" +
                            "                  },\n" +
                            "                  {\n" +
                            "                    \"x\": 0.88020134,\n" +
                            "                    \"y\": 0.88584834\n" +
                            "                  },\n" +
                            "                  {\n" +
                            "                    \"x\": 0.73255426,\n" +
                            "                    \"y\": 0.88584834\n" +
                            "                  }\n" +
                            "                ]\n" +
                            "              }\n" +
                            "            },\n" +
                            "            {\n" +
                            "              \"mid\": \"/m/0k4j\",\n" +
                            "              \"name\": \"Car\",\n" +
                            "              \"score\": 0.66700697,\n" +
                            "              \"boundingPoly\": {\n" +
                            "                \"normalizedVertices\": [\n" +
                            "                  {\n" +
                            "                    \"x\": 0.5958359,\n" +
                            "                    \"y\": 0.7551425\n" +
                            "                  },\n" +
                            "                  {\n" +
                            "                    \"x\": 0.71148294,\n" +
                            "                    \"y\": 0.7551425\n" +
                            "                  },\n" +
                            "                  {\n" +
                            "                    \"x\": 0.71148294,\n" +
                            "                    \"y\": 0.8627895\n" +
                            "                  },\n" +
                            "                  {\n" +
                            "                    \"x\": 0.5958359,\n" +
                            "                    \"y\": 0.8627895\n" +
                            "                  }\n" +
                            "                ]\n" +
                            "              }\n" +
                            "            }\n" +
                            "          ]\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }";
/*
                    String jsonString = "{\n" +
                            "  \"responses\": [\n" +
                            "    {\n" +
                            "      \"localizedObjectAnnotations\": [\n" +
                            "        {\n" +
                            "          \"mid\": \"/m/01bqk0\",\n" +
                            "          \"name\": \"Bicycle wheel\",\n" +
                            "          \"score\": 0.89648587,\n" +
                            "          \"boundingPoly\": {\n" +
                            "            \"normalizedVertices\": [\n" +
                            "              {\n" +
                            "                \"x\": 0.32076266,\n" +
                            "                \"y\": 0.78941387\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.43812272,\n" +
                            "                \"y\": 0.78941387\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.43812272,\n" +
                            "                \"y\": 0.97331065\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.32076266,\n" +
                            "                \"y\": 0.97331065\n" +
                            "              }\n" +
                            "            ]\n" +
                            "          }\n" +
                            "        },\n" +
                            "        {\n" +
                            "          \"mid\": \"/m/0199g\",\n" +
                            "          \"name\": \"Bicycle\",\n" +
                            "          \"score\": 0.886761,\n" +
                            "          \"boundingPoly\": {\n" +
                            "            \"normalizedVertices\": [\n" +
                            "              {\n" +
                            "                \"x\": 0.312,\n" +
                            "                \"y\": 0.6616471\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.638353,\n" +
                            "                \"y\": 0.6616471\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.638353,\n" +
                            "                \"y\": 0.9705882\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.312,\n" +
                            "                \"y\": 0.9705882\n" +
                            "              }\n" +
                            "            ]\n" +
                            "          }\n" +
                            "        },\n" +
                            "        {\n" +
                            "          \"mid\": \"/m/01bqk0\",\n" +
                            "          \"name\": \"Bicycle wheel\",\n" +
                            "          \"score\": 0.6345275,\n" +
                            "          \"boundingPoly\": {\n" +
                            "            \"normalizedVertices\": [\n" +
                            "              {\n" +
                            "                \"x\": 0.5125398,\n" +
                            "                \"y\": 0.760708\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.6256646,\n" +
                            "                \"y\": 0.760708\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.6256646,\n" +
                            "                \"y\": 0.94601655\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.5125398,\n" +
                            "                \"y\": 0.94601655\n" +
                            "              }\n" +
                            "            ]\n" +
                            "          }\n" +
                            "        },\n" +
                            "        {\n" +
                            "          \"mid\": \"/m/06z37_\",\n" +
                            "          \"name\": \"Picture frame\",\n" +
                            "          \"score\": 0.6207608,\n" +
                            "          \"boundingPoly\": {\n" +
                            "            \"normalizedVertices\": [\n" +
                            "              {\n" +
                            "                \"x\": 0.79177403,\n" +
                            "                \"y\": 0.16160682\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.97047985,\n" +
                            "                \"y\": 0.16160682\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.97047985,\n" +
                            "                \"y\": 0.31348917\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.79177403,\n" +
                            "                \"y\": 0.31348917\n" +
                            "              }\n" +
                            "            ]\n" +
                            "          }\n" +
                            "        },\n" +
                            "        {\n" +
                            "          \"mid\": \"/m/0h9mv\",\n" +
                            "          \"name\": \"Tire\",\n" +
                            "          \"score\": 0.55886006,\n" +
                            "          \"boundingPoly\": {\n" +
                            "            \"normalizedVertices\": [\n" +
                            "              {\n" +
                            "                \"x\": 0.32076266,\n" +
                            "                \"y\": 0.78941387\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.43812272,\n" +
                            "                \"y\": 0.78941387\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.43812272,\n" +
                            "                \"y\": 0.97331065\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.32076266,\n" +
                            "                \"y\": 0.97331065\n" +
                            "              }\n" +
                            "            ]\n" +
                            "          }\n" +
                            "        },\n" +
                            "        {\n" +
                            "          \"mid\": \"/m/02dgv\",\n" +
                            "          \"name\": \"Door\",\n" +
                            "          \"score\": 0.5160098,\n" +
                            "          \"boundingPoly\": {\n" +
                            "            \"normalizedVertices\": [\n" +
                            "              {\n" +
                            "                \"x\": 0.77569866,\n" +
                            "                \"y\": 0.37104446\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.9412425,\n" +
                            "                \"y\": 0.37104446\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.9412425,\n" +
                            "                \"y\": 0.81507325\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.77569866,\n" +
                            "                \"y\": 0.81507325\n" +
                            "              }\n" +
                            "            ]\n" +
                            "          }\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}";

                    */
                byte[] s = Base64.decode(con,Base64.DEFAULT);
                BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inMutable=true;
                Bitmap bit = BitmapFactory.decodeByteArray(s, 0, s.length, opt);



                try {
                    MainActivity.this.cameraCaptureSession.setRepeatingRequest(cameraRequest, null, backgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }

                return jsonString;

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        protected void onPostExecute(String jsonResponse) {
            super.onPostExecute(jsonResponse);

            myCanvas c = new myCanvas(getApplicationContext());

            Canvas c1 = new Canvas(bmp);

            /*
             *
             *
             *
             *
             *       PARSIRANJE TimE
             *
             *
             *
             */
//
//                jsonResponse = " {\n" +
//                            "      \"responses\": [\n" +
//                            "        {\n" +
//                            "          \"labelAnnotations\": [\n" +
//                            "            {\n" +
//                            "              \"mid\": \"/m/02psyd2\",\n" +
//                            "              \"description\": \"zombie\",\n" +
//                            "              \"score\": 0.9018883,\n" +
//                            "              \"topicality\": 0.9018883\n" +
//                            "            },\n" +
//                            "            {\n" +
//                            "              \"mid\": \"/m/02h7lkt\",\n" +
//                            "              \"description\": \"fictional character\",\n" +
//                            "              \"score\": 0.5292452,\n" +
//                            "              \"topicality\": 0.5292452\n" +
//                            "            },\n" +
//                            "            {\n" +
//                            "              \"mid\": \"/m/03qtwd\",\n" +
//                            "              \"description\": \"crowd\",\n" +
//                            "              \"score\": 0.51949716,\n" +
//                            "              \"topicality\": 0.51949716\n" +
//                            "            }\n" +
//                            "          ]\n" +
//                            "        }\n" +
//                            "      ]\n" +
//                            "    }\n";


            InputStream inputStream = new ByteArrayInputStream(jsonResponse.getBytes());

            InputStreamReader reader = new InputStreamReader(inputStream);

            JsonReader jR = new JsonReader(reader);
            /*
                List<PicObject> l = null;

                try {
                    l = PicObject(jR);
                } catch (IOException e) {
                    e.printStackTrace();
                }
*/
            List<Labela> l = new ArrayList<>();

            try {
                l = parseLabelaArray(jR);
                jR.close();
            } catch (IOException e) {
                e.printStackTrace();
            }



            Paint p = new Paint();
            p.setTextSize(50.0f);
            p.setColor(Color.WHITE);
            p.setStrokeWidth(5.0f);
            LightingColorFilter filter;

            int screenColor = Color.GREEN;


            String[] okuzen = {"blood", "mythical creature", "fictional character", "zombie"};

            for(int i = 0; i < l.size(); i++){
                c1.drawText(l.get(i).desc, 50,  50 * i, p);
                for(String s: okuzen){
                    if(l.get(i).desc.equals(s)){
                        screenColor = Color.RED;
                    }
                }
            }
            filter = new LightingColorFilter(screenColor, 1);

            p.setColorFilter(filter);


            ImageView moje = findViewById(R.id.imageView);


            c1 = new Canvas(bmp);

            c1.drawBitmap(bmp,0,0, p);

            moje.setImageBitmap(bmp);


            Log.e("Zombie", "x" + c1.getWidth());
            Log.e("Zombie", "y" + c1.getHeight());



/*
                                             \"x\": 0.73255426,\n" +
                        "                    \"y\": 0.7711425\n" +
                        "                  },\n" +
                        "                  {\n" +
                        "                    \"x\": 0.88020134,\n" +
                        "                    \"y\": 0.7711425\n" +
                        "                  },\n" +
                        "                  {\n" +
                        "                    \"x\": 0.88020134,\n" +
                        "                    \"y\": 0.88584834\n" +
                        "                  },\n" +
                        "                  {\n" +
                        "                    \"x\": 0.7325F5426,\n" +
                        "                    \"y\": 0.88584834\n" +
*/
/*
                float yT = 0.7551425f * bmp.getHeight();
                float yB = 0.8627895f * bmp.getHeight();
                float xL = 0.5958359f * bmp.getWidth();
                float xR = 0.71148294f * bmp.getWidth();
                */


            // Rect r = new Rect(((int) xL), ((int) yT), ((int) xR), ((int) yB));

            /*
                if(l != null){
                    for(PicObject obj: l){
                        double yT = obj.getLoc().get("yT") * bmp.getHeight();
                        double yB = obj.getLoc().get("yB") * bmp.getHeight();
                        double xL = obj.getLoc().get("xL") * bmp.getWidth();
                        double xR = obj.getLoc().get("xR") * bmp.getWidth();

                        board.add(new Rect(((int) xL), ((int) yT),((int) xR),((int) yB)));
                    }
                }

                if(board != null){
                    for(Rect r: board){
                        c1.drawRect(r, p);
                    }
                }

               // c1.drawRect(r, p);
                //c.draw(c1);
                //c.setR(r);
                //c.setBmp(bitmap);
                Log.e("Zombie", "Tusam");

               // ConstraintLayout lay = findViewById(R.id.con_layout);

                //lay.addView(c, 1);
*/


        }

        class Labela{
            private String mid;
            private String desc;
            private double score;
            private double topic;

            Labela(String mid, String desc, double score, double topic){
                this.mid = mid;
                this.desc = desc;
                this.score = score;
                this.topic = topic;
            }

            public String getMid() {
                return mid;
            }

            public String getDesc() {
                return desc;
            }

            public double getScore() {
                return score;
            }

            public double getTopic() {
                return topic;
            }
        }

        private Labela parseLabela(JsonReader jr) throws IOException {

                /*
                {
                  "mid": "/m/02psyd2",
                  "description": "zombie",
                    "score": 0.9018883,
                    "topicality": 0.9018883
                 },
                */

            String node = "";

            String mid = "";
            String desc = "";
            double score = 0f;
            double topicality = 0f;


            jr.beginObject();

            while(jr.hasNext()){
                node = jr.nextName();

                if(node.equals("mid")) mid = jr.nextString();
                else if(node.equals("description")) desc = jr.nextString();
                else if(node.equals("score")) score = jr.nextDouble();
                else if(node.equals("topicality")) jr.nextDouble();
                else jr.skipValue();
            }

            jr.endObject();

            return new Labela(mid, desc, score, topicality);

        }

        private List<Labela> parseLabelaArray(JsonReader jr) throws IOException {

                /*
                [
            {
              "mid": "/m/02psyd2",
              "description": "zombie",
              "score": 0.9018883,
              "topicality": 0.9018883
            },
            {
              "mid": "/m/02h7lkt",
              "description": "fictional character",
              "score": 0.5292452,
              "topicality": 0.5292452
            },
            {
              "mid": "/m/03qtwd",
              "description": "crowd",
              "score": 0.51949716,
              "topicality": 0.51949716
            }
          ]
                 */
            List<Labela> l = new ArrayList<>();

            jr.beginObject();
            jr.nextName(); // responses
            jr.beginArray();
            jr.beginObject();


            // Ako je JSON response prazan
            if(jr.peek() == JsonToken.END_OBJECT) return l;

            jr.nextName(); // labelAnnotations


            jr.beginArray();

            while(jr.peek() == JsonToken.BEGIN_OBJECT){
                l.add(parseLabela(jr));

            }

            jr.endArray();


            jr.endObject();
            jr.endArray();
            jr.endObject();


            return l;

        }

        class PicObject{

            private String mid;
            private String name;
            private double score;
            private Map<String, Double> loc;

            PicObject(String mid, String name, double score, Map<String, Double> map){

                this.mid = mid;
                this.name = name;
                this.score = score;
                this.loc = map;

            }

            public String getMid() {
                return mid;
            }

            public String getName() {
                return name;
            }

            public double getScore() {
                return score;
            }

            public Map<String, Double> getLoc() {
                return loc;
            }
        }



        private List<PicObject> PicObject(JsonReader jR) throws IOException {
            // Glavna funkcija za parsiranje JSON-a

            List<PicObject> list = new ArrayList<>();

            jR.beginObject();
            jR.nextName();

            jR.beginArray();
            jR.beginObject();
            jR.nextName();


            jR.beginArray();

            while(jR.peek() == JsonToken.BEGIN_OBJECT){
                list.add(parseObject(jR));
            }

            jR.endArray();
            jR.endObject();
            jR.endArray();
            jR.endObject();

            Log.e("Zombie", jR.peek().toString());

            return list;

        }

        private Map<String, Double> parseKor(JsonReader jR) throws IOException{

            String node = "";

            double xL = 0.0f;
            double xR = 0.0f;
            double yT = 0.0f;
            double yB = 0.0f;

            jR.beginObject();

            while(jR.hasNext()){
                node = jR.nextName();

                if(node.equals("normalizedVertices")){
                    jR.beginArray();

                    jR.beginObject();
                    jR.nextName();
                    xL = jR.nextDouble();
                    jR.nextName();
                    yT = jR.nextDouble();
                    jR.endObject();

                    jR.beginObject();
                    jR.nextName();
                    jR.skipValue();
                    jR.nextName();
                    jR.skipValue();
                    jR.endObject();

                    jR.beginObject();
                    jR.nextName();
                    xR = jR.nextDouble();
                    jR.nextName();
                    yB = jR.nextDouble();
                    jR.endObject();

                    jR.beginObject();
                    jR.nextName();
                    jR.skipValue();
                    jR.nextName();
                    jR.skipValue();
                    jR.endObject();

                    jR.endArray();

                }
            }
            jR.endObject();

            Map<String, Double> map = new HashMap<String, Double>();

            map.put("xL", xL);
            map.put("xR", xR);
            map.put("yT", yT);
            map.put("yB", yB);

            return map;
        }

        private PicObject parseObject(JsonReader jR) throws IOException {

            String mid = "";
            String name = "";
            double score = 0.0f;
            float pos_x = 0.0f;
            float pos_y = 0.0f;

            Map<String, Double> map = new HashMap<String, Double>();

            String node = "";

            jR.beginObject();

            while(jR.hasNext()){
                node = jR.nextName();

                if(node.equals("mid")) mid = jR.nextString();
                else if(node.equals("name")) name = jR.nextString();
                else if(node.equals("score")) score = jR.nextDouble();
                else if(node.equals("boundingPoly")){
                    // Array koordinata
                    map = parseKor(jR);
                }
            }
            jR.endObject();

            // TODO Return objekt - treba kretirati

            return new PicObject(mid, name, score, map);

        }

        class myCanvas extends View{

            private Rect r = null;

            private Paint p;

            private Bitmap bmp;

            public myCanvas(Context context) {
                super(context);
                this.p = new Paint();
                p.setTextSize(20.0f);
                p.setColor(Color.BLUE);
                p.setStrokeWidth(5.0f);
                p.setStyle(Paint.Style.STROKE);
                this.setBackgroundColor(Color.RED);

            }

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                if(r!=null){
                    canvas.drawRect(r, p);
                }
            }

            public void setR(Rect r) {
                this.r = r;
            }

            public void setBmp(Bitmap bmp) {
                this.bmp = bmp;
            }
        }
    }

    class Task extends AsyncTask<Bitmap, Void, byte[]> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ConstraintLayout constraintLayout = findViewById(R.id.con_layout);
            constraintLayout.setVisibility(View.VISIBLE);
            constraintLayout.setBackgroundColor(Color.argb(255,200,200,200));
        }

        @Override
        protected byte[] doInBackground(Bitmap... bitmaps) {

            // Get bitmap if not null

            Bitmap bitmap;

            if(bitmaps.length > 0){
                bitmap = bitmaps[0];
            }else{
                bitmap = textureView.getBitmap();
            }


            // BITMAP

            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.test2);

            Bitmap bmp2 = Bitmap.createScaledBitmap(bmp, bmp.getWidth()/4, bmp.getHeight()/4, false);

            ByteArrayOutputStream bt = new ByteArrayOutputStream();



            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bt);

            byte[] bajtovi = bt.toByteArray();

            Log.e("S1", String.valueOf(bmp.getByteCount()));
            Log.e("S2", String.valueOf(bmp2.getByteCount()));

            return bajtovi;
        }

        @Override
        protected void onPostExecute(byte[] bajtovi) {
            super.onPostExecute(bajtovi);

            Bitmap bitmap = BitmapFactory.decodeByteArray(bajtovi,0, bajtovi.length);

            ImageView imageView = findViewById(R.id.imageView);
            imageView.setImageBitmap(bitmap);


            String enc = Base64.encodeToString(bajtovi, Base64.DEFAULT);



            ConstraintLayout layout = findViewById(R.id.con_layout);
            FloatingActionButton button1 = findViewById(R.id.floatingActionButton);
            FloatingActionButton button2 = findViewById(R.id.floatingActionButton2);

            ProgressBar progressBar = findViewById(R.id.progressBar);
            progressBar.setVisibility(View.GONE);



            button1.show();

            button1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //startMe(enc);

                    Task2 t1 = new Task2();
                    t1.execute(bitmap);


                }
            });

            // TODO funkcija za slanje fotke

            button2.show();


            button2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    button1.hide();
                    button2.hide();
                    progressBar.setVisibility(View.VISIBLE);
                    layout.setVisibility(View.GONE);
                    imageView.setImageBitmap(null);
                }
            });

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.e("Zombie", String.valueOf(requestCode));

        if(requestCode == PICK_IMAGE){
            if(resultCode < 0){

                Uri u = data.getData();
                Log.e("Zombie", u.getPath());

                Bitmap bmp = null;

                try {
                    bmp = MediaStore.Images.Media.getBitmap(this.getContentResolver(), u);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Task tt = new Task();
                tt.execute(bmp);

            }


        }

    }

    @Override
    public void onBackPressed() {

        if(getSupportFragmentManager().getFragments().size() > 0){
            for(Fragment f : getSupportFragmentManager().getFragments()){
                getSupportFragmentManager().beginTransaction().remove(f).commit();
            }
        }else{
            finishAndRemoveTask();
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        openBackgroundThread();
        if(textureView.isAvailable()){
            setUpCamera();
            openCamera();
        } else{
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeCamera();
        closeBackgroundThread();
    }

    private void closeCamera(){
        if(cameraCaptureSession != null){
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if(cameraDevice != null){
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void createPreviewSession(){


        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);


        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        captureRequestBuilder.addTarget(previewSurface);

        try {
            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if(cameraDevice == null) return;

                    cameraRequest = captureRequestBuilder.build();
                    MainActivity.this.cameraCaptureSession = session;
                    try {
                        MainActivity.this.cameraCaptureSession.setRepeatingRequest(cameraRequest, null, backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, backgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    private void closeBackgroundThread(){
        if(backgroundHandler != null){
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    private void openCamera() {

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

    }

    private void setUpCamera() {
        try{
            for(String cameraId : cameraManager.getCameraIdList()){

                CameraCharacteristics cameraCharacteristics =
                        cameraManager.getCameraCharacteristics(cameraId);

                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == cameraFacing){
                    streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[1];

                    for(Size s : streamConfigurationMap.getOutputSizes(SurfaceTexture.class)){
                        Log.d(TAG, s.toString());
                    }


                    this.cameraId = cameraId;

                }

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCamera2() {
        try{
            for(String cameraId : cameraManager.getCameraIdList()){

                CameraCharacteristics cameraCharacteristics =
                        cameraManager.getCameraCharacteristics(cameraId);

                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == cameraFacing){
                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];

                    for(Size s : streamConfigurationMap.getOutputSizes(SurfaceTexture.class)){
                        Log.d(TAG, s.toString());
                    }

                    this.cameraId = cameraId;
                }

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openBackgroundThread(){
        backgroundThread = new HandlerThread("camera_bg_thread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    void startMe(String con){
        AsyncTask.execute(new Runnable() {

            /*
            *
            *
            *
            *
            *
            *
            */

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
/*

                    source1.put("imageUri", "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png");
                    JSONObject image1 = new JSONObject();
                    image1.put("source", source1);


                    JSONObject image2 = new JSONObject();
                    image2.put("image", image1);
*/

                    JSONObject imageCon = new JSONObject();
                    imageCon.put("content", con);

                    JSONObject image2 = new JSONObject();
                    image2.put("image", imageCon);

                    JSONArray arF = new JSONArray();
                    JSONObject arFF = new JSONObject();

                    arFF.put("type", "OBJECT_LOCALIZATION");
                    //arFF.put("maxResults", 1);
                    arF.put(arFF);


                    image2.put("features", arF);

                    JSONArray ar = new JSONArray();

                    ar.put(image2);


                    Log.d("Zombie", ar.getString(0));
                    JSONObject req = new JSONObject();

                    req.put("requests", ar);

                    Log.d("Zombie", req.toString());

/*
                    int maxLogSize = 1000;
                    for(int i = 0; i <= req.toString().length() / maxLogSize; i++) {
                        int start = i * maxLogSize;
                        int end = (i+1) * maxLogSize;
                        end = end > req.toString().length() ? req.toString().length() : end;
                        Log.v("Zombie", req.toString().substring(start, end));
                    }
*//*
                   //  DA NE TROŠIMO PODATKE HEHE
                    // Enable writing
                    myConnection.setDoOutput(true);


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
                    /*
                    String jsonString = "" +
                            "{ \"responses\": [\n" +
                            "        {\n" +
                            "          \"localizedObjectAnnotations\": [\n" +
                            "            {\n" +
                            "              \"mid\": \"/m/0k4j\",\n" +
                            "              \"name\": \"Car\",\n" +
                            "              \"score\": 0.73251706,\n" +
                            "              \"boundingPoly\": {\n" +
                            "                \"normalizedVertices\": [\n" +
                            "                  {\n" +
                            "                    \"x\": 0.73255426,\n" +
                            "                    \"y\": 0.7711425\n" +
                            "                  },\n" +
                            "                  {\n" +
                            "                    \"x\": 0.88020134,\n" +
                            "                    \"y\": 0.7711425\n" +
                            "                  },\n" +
                            "                  {\n" +
                            "                    \"x\": 0.88020134,\n" +
                            "                    \"y\": 0.88584834\n" +
                            "                  },\n" +
                            "                  {\n" +
                            "                    \"x\": 0.73255426,\n" +
                            "                    \"y\": 0.88584834\n" +
                            "                  }\n" +
                            "                ]\n" +
                            "              }\n" +
                            "            },\n" +
                            "            {\n" +
                            "              \"mid\": \"/m/0k4j\",\n" +
                            "              \"name\": \"Car\",\n" +
                            "              \"score\": 0.66700697,\n" +
                            "              \"boundingPoly\": {\n" +
                            "                \"normalizedVertices\": [\n" +
                            "                  {\n" +
                            "                    \"x\": 0.5958359,\n" +
                            "                    \"y\": 0.7551425\n" +
                            "                  },\n" +
                            "                  {\n" +
                            "                    \"x\": 0.71148294,\n" +
                            "                    \"y\": 0.7551425\n" +
                            "                  },\n" +
                            "                  {\n" +
                            "                    \"x\": 0.71148294,\n" +
                            "                    \"y\": 0.8627895\n" +
                            "                  },\n" +
                            "                  {\n" +
                            "                    \"x\": 0.5958359,\n" +
                            "                    \"y\": 0.8627895\n" +
                            "                  }\n" +
                            "                ]\n" +
                            "              }\n" +
                            "            }\n" +
                            "          ]\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }";
*/


                    String jsonString = "{\n" +
                            "  \"responses\": [\n" +
                            "    {\n" +
                            "      \"localizedObjectAnnotations\": [\n" +
                            "        {\n" +
                            "          \"mid\": \"/m/01bqk0\",\n" +
                            "          \"name\": \"Bicycle wheel\",\n" +
                            "          \"score\": 0.89648587,\n" +
                            "          \"boundingPoly\": {\n" +
                            "            \"normalizedVertices\": [\n" +
                            "              {\n" +
                            "                \"x\": 0.32076266,\n" +
                            "                \"y\": 0.78941387\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.43812272,\n" +
                            "                \"y\": 0.78941387\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.43812272,\n" +
                            "                \"y\": 0.97331065\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.32076266,\n" +
                            "                \"y\": 0.97331065\n" +
                            "              }\n" +
                            "            ]\n" +
                            "          }\n" +
                            "        },\n" +
                            "        {\n" +
                            "          \"mid\": \"/m/0199g\",\n" +
                            "          \"name\": \"Bicycle\",\n" +
                            "          \"score\": 0.886761,\n" +
                            "          \"boundingPoly\": {\n" +
                            "            \"normalizedVertices\": [\n" +
                            "              {\n" +
                            "                \"x\": 0.312,\n" +
                            "                \"y\": 0.6616471\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.638353,\n" +
                            "                \"y\": 0.6616471\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.638353,\n" +
                            "                \"y\": 0.9705882\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.312,\n" +
                            "                \"y\": 0.9705882\n" +
                            "              }\n" +
                            "            ]\n" +
                            "          }\n" +
                            "        },\n" +
                            "        {\n" +
                            "          \"mid\": \"/m/01bqk0\",\n" +
                            "          \"name\": \"Bicycle wheel\",\n" +
                            "          \"score\": 0.6345275,\n" +
                            "          \"boundingPoly\": {\n" +
                            "            \"normalizedVertices\": [\n" +
                            "              {\n" +
                            "                \"x\": 0.5125398,\n" +
                            "                \"y\": 0.760708\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.6256646,\n" +
                            "                \"y\": 0.760708\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.6256646,\n" +
                            "                \"y\": 0.94601655\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.5125398,\n" +
                            "                \"y\": 0.94601655\n" +
                            "              }\n" +
                            "            ]\n" +
                            "          }\n" +
                            "        },\n" +
                            "        {\n" +
                            "          \"mid\": \"/m/06z37_\",\n" +
                            "          \"name\": \"Picture frame\",\n" +
                            "          \"score\": 0.6207608,\n" +
                            "          \"boundingPoly\": {\n" +
                            "            \"normalizedVertices\": [\n" +
                            "              {\n" +
                            "                \"x\": 0.79177403,\n" +
                            "                \"y\": 0.16160682\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.97047985,\n" +
                            "                \"y\": 0.16160682\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.97047985,\n" +
                            "                \"y\": 0.31348917\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.79177403,\n" +
                            "                \"y\": 0.31348917\n" +
                            "              }\n" +
                            "            ]\n" +
                            "          }\n" +
                            "        },\n" +
                            "        {\n" +
                            "          \"mid\": \"/m/0h9mv\",\n" +
                            "          \"name\": \"Tire\",\n" +
                            "          \"score\": 0.55886006,\n" +
                            "          \"boundingPoly\": {\n" +
                            "            \"normalizedVertices\": [\n" +
                            "              {\n" +
                            "                \"x\": 0.32076266,\n" +
                            "                \"y\": 0.78941387\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.43812272,\n" +
                            "                \"y\": 0.78941387\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.43812272,\n" +
                            "                \"y\": 0.97331065\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.32076266,\n" +
                            "                \"y\": 0.97331065\n" +
                            "              }\n" +
                            "            ]\n" +
                            "          }\n" +
                            "        },\n" +
                            "        {\n" +
                            "          \"mid\": \"/m/02dgv\",\n" +
                            "          \"name\": \"Door\",\n" +
                            "          \"score\": 0.5160098,\n" +
                            "          \"boundingPoly\": {\n" +
                            "            \"normalizedVertices\": [\n" +
                            "              {\n" +
                            "                \"x\": 0.77569866,\n" +
                            "                \"y\": 0.37104446\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.9412425,\n" +
                            "                \"y\": 0.37104446\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.9412425,\n" +
                            "                \"y\": 0.81507325\n" +
                            "              },\n" +
                            "              {\n" +
                            "                \"x\": 0.77569866,\n" +
                            "                \"y\": 0.81507325\n" +
                            "              }\n" +
                            "            ]\n" +
                            "          }\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}";

                    byte[] s = Base64.decode(con,Base64.DEFAULT);
                    BitmapFactory.Options opt = new BitmapFactory.Options();
                    opt.inMutable=true;
                    Bitmap bit = BitmapFactory.decodeByteArray(s, 0, s.length, opt);

                    myCanvas c = new myCanvas(getApplicationContext());

                    Canvas c1 = new Canvas(bit);
                    Paint p = new Paint();
                    p.setTextSize(20.0f);
                    p.setColor(Color.BLUE);
                    c1.drawText("Aaaa",100,100,p);
                    c.draw(c1);

                    Log.e("Zombie", "Tusam");

                    ConstraintLayout lay = findViewById(R.id.con_layout);


                    lay.addView(c);




                    try {
                        MainActivity.this.cameraCaptureSession.setRepeatingRequest(cameraRequest, null, backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            class myCanvas extends View{

                public myCanvas(Context context) {
                    super(context);
                }

                @Override
                protected void onDraw(Canvas canvas) {
                    super.onDraw(canvas);
                }
            }

            class User{
                private String name;
                private float score;
                private Rect rect;

                User(String name, float score, Rect r){
                    this.name = name;
                    this.score = score;
                    this.rect = r;
                }


                public String getName() {
                    return name;
                }

                public Rect getRect() {
                    return rect;
                }

                public void setRect(Rect rect) {
                    this.rect = rect;
                }

                public float getScore() {
                    return score;
                }
            }
        });
    }
}
