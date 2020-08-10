package com.harshith.flashwink;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener{


    private SensorManager sensorManager;
    private Sensor sensor;

    private int x,z;
    private Uri uri;
    private ImageView imageView;
    private boolean trackimage;

    private static final int READ_REQUEST_CODE = 42;

    private CoordinatorLayout layt;

    private FirebaseVisionFaceDetectorOptions options;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseInitialisation();
        sensorMethod();
trackimage=true;
        imageView=findViewById(R.id.imageView);
        layt=findViewById(R.id.layt);

        Toast.makeText(this,R.string.info,Toast.LENGTH_LONG).show();

    }
    void firebaseInitialisation(){

        options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .build();

    }



    private void sensorMethod() {

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

    }
    private void registerListener() {
        sensorManager.registerListener(MainActivity.this,sensor,SensorManager.SENSOR_DELAY_NORMAL);

    }
    private void unregisterListener() {
        sensorManager.unregisterListener(MainActivity.this,sensor);
    }
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {


        x=(int)Math.abs(sensorEvent.values[1]);
        z=(int)Math.abs(sensorEvent.values[2]);


        if((x==8||x==9)&&(z==1||z==0)&&trackimage){

            actionImage();
            trackimage=false;
        }

        //  Log.e("FullscreenActivity","X : "+(int)Math.abs(sensorEvent.values[0])+" Y : "+(int)Math.abs(sensorEvent.values[1])+" Z : "+(int)Math.abs(sensorEvent.values[2]));

    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }





    @Override
    protected void onStart() {
        super.onStart();
        registerListener();
    }
    @Override
    protected void onResume() {
        super.onResume();
        registerListener();

    }
    @Override
    protected void onStop() {
        super.onStop();
        unregisterListener();
    }
    @Override
    protected void onPause() {
        super.onPause();
        unregisterListener();

    }



    public void actionImage() {


        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");

        startActivityForResult(intent, READ_REQUEST_CODE);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            if (resultData != null) {
                setImage(resultData);
            }
            else trackimage=true;
        }
        else trackimage=true;
    }
    private void setImage(Intent resultData) {
        uri = resultData.getData();
        //  Log.i(TAG, "Uri: " + uri.toString());
        ParcelFileDescriptor parcelFileDescriptor =null;

        try {
            if (uri != null) {
                parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            }
            else trackimage=true;
            FileDescriptor fileDescriptor = null;
            if (parcelFileDescriptor != null) {
                fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                parcelFileDescriptor.close();
                imageView.setImageBitmap(image);
                setprogress();
                imageDetection(image);

            }
            else trackimage=true;

        } catch (FileNotFoundException e) {
            trackimage=true;
            e.printStackTrace();
        } catch (IOException e) {
            trackimage=true;
            e.printStackTrace();
        }
    }

    private void setprogress() {
        Snackbar mySnackbar = Snackbar.make(layt,
                "Please Wait... Until Results get Ready", Snackbar.LENGTH_LONG);
        mySnackbar.show();

    }

    private void imageDetection(Bitmap bitmap) {

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options);

        Task<List<FirebaseVisionFace>> result =
                detector.detectInImage(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<FirebaseVisionFace>>() {
                                    @Override
                                    public void onSuccess(List<FirebaseVisionFace> faces) {
                                        // Task completed successfully
                                        // ...
                                        printResult(faces);

                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                      trackimage=true;
                                        Log.e("Firebase Image Filaure1",e.getMessage());
                                    }
                                });



    }

    private void printResult(List<FirebaseVisionFace> faces) {
        int index=1;
        String ans="";
        Snackbar mySnackbar;
        for(FirebaseVisionFace face:faces){

            float right=0.0f,left=0.0f;
            if (face.getRightEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                right = face.getRightEyeOpenProbability();
               //Log.e("Righteye",String.valueOf(rightEyeOpenProb));
            }
            if (face.getLeftEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                 left= face.getLeftEyeOpenProbability();
               // Log.e("Lefteye",String.valueOf(leftEyeOpenProb));
            }

            if(right>=0.5 && left>=0.5){
                ans+="Face "+index+" : Both eyes are opened\n";
            }
            else if(right>=0.5){
                ans+="Face "+index+" : Right eye is open\n";
            }
            else if(left>=0.5){
                ans+="Face "+index+" : Left eye is open\n";
            }

            else  ans+="Face "+index+" : Both eyes are closed\n";



            index++;
        }

        if(faces.size()==0){

            mySnackbar = Snackbar.make(layt,
                    "No Faces Detected", Snackbar.LENGTH_LONG);

             mySnackbar.show();

        }
        else{

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setIcon(R.drawable.face);
            alertDialogBuilder.setTitle(faces.size()+" Faces Detected");
            alertDialogBuilder.setMessage(ans);
                    alertDialogBuilder.setPositiveButton("OK",null);

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }

        trackimage=true;

    }


}
