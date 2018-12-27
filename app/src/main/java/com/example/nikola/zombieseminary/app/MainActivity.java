package com.example.nikola.zombieseminary.app;

import android.Manifest;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.internal.NavigationMenu;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.JsonReader;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;



public class MainActivity extends AppCompatActivity {


    /*
    * @TODO Dovršiti tutorial, dodati gumb za slikanje, spremanje fotke itd.
    * Spremanje napraviti u asynctasku, na finishu prikazati sliku u fragmentu
    * U fragmentu dodati dva gumba, jedan za slanje, drugi za brisanje
    *
    * KORISTI VISIBILITY - GONE, nikakvi dodatni activitiji!
    *
    *
     */

    private static final String TAG = "VELIKIDREK";

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
    private FragmentManager fragmentManager;
    private File galleryFolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        startMe();

        textureView = findViewById(R.id.textureView);
        NavigationView nav = findViewById(R.id.nav_view);

        nav.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        menuItem.setChecked(true);

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

                FrameLayout layout = findViewById(R.id.content_frame);
                float ratio = (float) previewSize.getWidth() / (float) previewSize.getHeight();
                int newH = (int) (layout.getWidth() * ratio);
                Log.e("Zombie", String.valueOf(newH));
                if(newH > layout.getHeight()){
                    newH = layout.getHeight();
                }
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(layout.getWidth(), newH);
                textureView.setLayoutParams(params);
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

        class Task extends AsyncTask<Void, Void, Bitmap>{

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                ConstraintLayout constraintLayout = findViewById(R.id.con_layout);
                constraintLayout.setVisibility(View.VISIBLE);
                constraintLayout.setBackgroundColor(Color.argb(255,200,200,200));
            }

            @Override
            protected Bitmap doInBackground(Void... voids) {

                Bitmap bitmap = textureView.getBitmap();
               /* ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                byte[] bytes = outputStream.toByteArray();
                String encoded = Base64.encodeToString(bytes, Base64.DEFAULT);*/


                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                ImageView imageView = findViewById(R.id.imageView);
                imageView.setImageBitmap(bitmap);
                ConstraintLayout layout = findViewById(R.id.con_layout);
                FloatingActionButton button1 = findViewById(R.id.floatingActionButton);
                FloatingActionButton button2 = findViewById(R.id.floatingActionButton2);
                ProgressBar progressBar = findViewById(R.id.progressBar);
                progressBar.setVisibility(View.GONE);
                button1.show();
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

    private void createImageGallery() {
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        galleryFolder = new File(storageDirectory, getResources().getString(R.string.app_name));
        if (!galleryFolder.exists()) {
            boolean wasCreated = galleryFolder.mkdirs();
            if (!wasCreated) {
                Log.e("CapturedImages", "Failed to create directory");
            }
        }
    }

    private File createImageFile(File galleryFolder) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "image_" + timeStamp + "_";
        return File.createTempFile(imageFileName, ".jpg", galleryFolder);
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
                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
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
                    previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[2];

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
                    try {
                        MainActivity.this.cameraCaptureSession.setRepeatingRequest(cameraRequest, null, backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
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
