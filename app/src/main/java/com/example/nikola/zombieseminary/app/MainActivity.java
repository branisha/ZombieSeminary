package com.example.nikola.zombieseminary.app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.net.ConnectivityManager;
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
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;


public class MainActivity extends AppCompatActivity {


    private static final String TAG = "Zombie";

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
    private OrientationListener eventListener;
    private static final String[] okuzen = {"blood", "mythical creature", "fictional character", "zombie"};



    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, -90);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        

//        startMe();

        eventListener = new OrientationListener(this);
        eventListener.enable();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.INTERNET},
                    0);

        }

        FloatingActionButton buttonGallery = findViewById(R.id.buttonGallery);

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


        surfaceTextureListener = setupListener();

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



        FloatingActionButton btn = findViewById(R.id.button3);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Task tt = new Task();
                tt.execute();


            }
        });


        FloatingActionButton btn_swp = findViewById(R.id.btn_swap);
        btn_swp.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

                closeCamera();
                closeBackgroundThread();

                if(cameraFacing == CameraCharacteristics.LENS_FACING_FRONT){
                    cameraFacing = CameraCharacteristics.LENS_FACING_BACK;
                }else{
                    cameraFacing = CameraCharacteristics.LENS_FACING_FRONT;
                }


                openBackgroundThread();
                if(textureView.isAvailable()){
                    setUpCamera();
                    changeCamDimen(0);
                    openCamera();
                    Log.e("ZombieClick", "Otpiram kameru");


                } else{
                    textureView.setSurfaceTextureListener(surfaceTextureListener);
                    Log.e("ZombieClick", "Settam surface");
                }

                surfaceTextureListener = setupListener();

                Menu m = nav.getMenu();

                m.clear();

                for(int i = 0; i < streamConfigurationMap.getOutputSizes(SurfaceTexture.class).length; i++){
                    String name = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[i].toString();
                    m.add(R.id.group_res, i, 0, name).setCheckable(true);
                }
                m.getItem(0).setChecked(true);



            }
        });


    }

    class OrientationListener extends OrientationEventListener{

        // KLASA ZA ROTACIJU I HANDLANJE PROMJENE TOKOM ROTACIJE

        private int CurrentR = 0;

        public OrientationListener(Context context) {
            super(context);
        }

        public int getRotation(){
            return this.CurrentR;
        }

        private int returnOrientation(int rotation){
            if(rotation >= 45 && rotation < 135) {
                return ORIENTATIONS.get(1);
            }else if(rotation >= 135 && rotation < 225) {
                return ORIENTATIONS.get(3);
            }else if(rotation >= 225 && rotation < 315){
               return ORIENTATIONS.get(2);

            }else{
                return ORIENTATIONS.get(0);
            }
        }

        private void rotateButton(int r){

            if(r == 90 || r == -90){
                r*=-1;
            }

            FloatingActionButton btn1 = findViewById(R.id.button3);
            FloatingActionButton btn2 = findViewById(R.id.btn_swap);
            FloatingActionButton btn3 = findViewById(R.id.buttonGallery);

            List<FloatingActionButton> gumbi = new ArrayList<FloatingActionButton>();

            gumbi.add(btn1);
            gumbi.add(btn2);
            gumbi.add(btn3);

            for(FloatingActionButton btn: gumbi){
                Log.e("ZombieX", String.valueOf(btn.getRotationX()));
                Log.e("ZombieY", String.valueOf(btn.getRotationY()));

                int centerX = (btn.getWidth()/2);
                int centerY = (btn.getHeight()/2);
                RotateAnimation animation = new RotateAnimation(CurrentR*-1, r, centerX, centerY);
                animation.setDuration(500);
                animation.setRepeatCount(0);
                animation.setFillAfter(true);
                btn.startAnimation(animation);
            }

        }


        @Override
        public void onOrientationChanged(int orientation) {

            if(this.CurrentR != returnOrientation(orientation)){
                // Switch rotation
                Log.e("Zombie", String.valueOf(returnOrientation(orientation)));
                rotateButton(returnOrientation(orientation));
                this.CurrentR = returnOrientation(orientation);



            }
        }
    }


    TextureView.SurfaceTextureListener setupListener(){

        TextureView.SurfaceTextureListener listener = surfaceTextureListener = new TextureView.SurfaceTextureListener() {

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

                NavigationView nav = findViewById(R.id.nav_view);

                Menu m = nav.getMenu();

                m.clear();

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

        return listener;

    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

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

                arF.put(arFF);


                image2.put("features", arF);

                JSONArray ar = new JSONArray();

                ar.put(image2);


                Log.d("Zombie", ar.getString(0));
                JSONObject req = new JSONObject();

                req.put("requests", ar);

                Log.d("Zombie", req.toString());


                myConnection.setDoOutput(true);

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

                byte[] s = Base64.decode(con,Base64.DEFAULT);
                BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inMutable=true;

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

            Canvas c1 = new Canvas(bmp);

            InputStream inputStream = new ByteArrayInputStream(jsonResponse.getBytes());

            InputStreamReader reader = new InputStreamReader(inputStream);

            JsonReader jR = new JsonReader(reader);

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
            p.setTextAlign(Paint.Align.CENTER);
            LightingColorFilter filter;

            int screenColor = Color.GREEN;
            String status = "OK";

            Log.e("ZOmbie", "CanvasW" + String.valueOf(c1.getWidth()));
            Log.e("ZOmbie", "CanvasH" + String.valueOf(c1.getHeight()));



            for(int i = 0; i < l.size(); i++){
                for(String s: okuzen){
                    if(l.get(i).desc.equals(s)){
                        screenColor = Color.RED;
                        status = "OKUŽEN";
                    }
                }
            }

            filter = new LightingColorFilter(screenColor, 1);

            p.setColorFilter(filter);

            ImageView moje = findViewById(R.id.imageView);

            c1 = new Canvas(bmp);

            c1.drawBitmap(bmp,0,0, p);

            p.setColorFilter(null);

            TextView tekstic = findViewById(R.id.textView);

            ConstraintLayout layout = findViewById(R.id.con_layout);

            Log.e("ZombieID", String.valueOf(tekstic.getId()));

            tekstic.setTextColor(Color.WHITE);

            tekstic.setText(status);

            tekstic.setTextSize(TypedValue.COMPLEX_UNIT_PX, 50f);

            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(layout);
            constraintSet.connect(tekstic.getId() ,ConstraintSet.LEFT, layout.getId() ,ConstraintSet.LEFT,0);
            constraintSet.connect(tekstic.getId(), ConstraintSet.RIGHT,layout.getId(),ConstraintSet.RIGHT,0);
            constraintSet.connect(tekstic.getId() ,ConstraintSet.TOP, layout.getId() ,ConstraintSet.TOP,0);
            constraintSet.connect(tekstic.getId(), ConstraintSet.BOTTOM,layout.getId(),ConstraintSet.BOTTOM,0);
            constraintSet.applyTo(layout);


            moje.setImageBitmap(bmp);


            Log.e("Zombie", "x" + c1.getWidth());
            Log.e("Zombie", "y" + c1.getHeight());




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


    }

    class Task extends AsyncTask<Bitmap, Void, byte[]> {


        private int rotation = 0;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ConstraintLayout constraintLayout = findViewById(R.id.con_layout);
            constraintLayout.setVisibility(View.VISIBLE);
        }

        @Override
        protected byte[] doInBackground(Bitmap... bitmaps) {

            // Get bitmap if not null

            Bitmap bitmap;



            if(bitmaps.length > 0){
                bitmap = bitmaps[0];
            }else{

                rotation = eventListener.getRotation();
                bitmap = textureView.getBitmap();
            }


            try {
                MainActivity.this.cameraCaptureSession.stopRepeating();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (NullPointerException e){
            }


            // BITMAP


            ByteArrayOutputStream bt = new ByteArrayOutputStream();

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bt);

            byte[] bajtovi = bt.toByteArray();


            return bajtovi;
        }

        @Override
        protected void onPostExecute(byte[] bajtovi) {
            super.onPostExecute(bajtovi);


            Bitmap tmp = BitmapFactory.decodeByteArray(bajtovi,0, bajtovi.length);

            ImageView imageView = findViewById(R.id.imageView);

            imageView.setImageBitmap(tmp);

            TextView tekst = findViewById(R.id.textView);





            Matrix mat =  new Matrix();
            mat.postRotate(rotation);
            Bitmap bitmap = Bitmap.createBitmap(tmp, 0,0,tmp.getWidth(), tmp.getHeight(), mat, true);


            ConstraintLayout layout = findViewById(R.id.con_layout);
            FloatingActionButton button1 = findViewById(R.id.floatingActionButton);
            FloatingActionButton button2 = findViewById(R.id.floatingActionButton2);

            layout.setBackgroundColor(Color.BLACK);



            button1.show();


            button1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //startMe(enc);

                    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

                    if(cm.getActiveNetworkInfo() != null){
                        Log.e("Zombie", "Slika je rotirana" + String.valueOf(rotation));
                        Task2 t1 = new Task2();
                        t1.execute(bitmap);

                    }else{
                        Toast.makeText(MainActivity.this, "Ni dostopa do interneta!", Toast.LENGTH_SHORT).show();

                    }

                    try {
                        MainActivity.this.cameraCaptureSession.setRepeatingRequest(cameraRequest, null, backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }


                }
            });


            button2.show();


            button2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    button1.hide();
                    button2.hide();
                    layout.setVisibility(View.GONE);
                    imageView.setImageBitmap(null);
                    tekst.setText("");
                    try {
                        MainActivity.this.cameraCaptureSession.setRepeatingRequest(cameraRequest, null, backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }


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
        NavigationView nav = findViewById(R.id.nav_view);

        if(textureView.isAvailable()){
            setUpCamera();
            changeCamDimen(nav.getCheckedItem().getOrder());
            openCamera();
        } else{
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
        eventListener.enable();
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeCamera();
        closeBackgroundThread();
        eventListener.disable();

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
                    previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
                    Log.d(TAG, String.valueOf(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)));

                    for(Size s : streamConfigurationMap.getOutputSizes(SurfaceTexture.class)){

                        Log.d(TAG, previewSize.toString());
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
}
