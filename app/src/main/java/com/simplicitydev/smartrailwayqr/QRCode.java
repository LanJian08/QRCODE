package com.simplicitydev.smartrailwayqr;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.WriterException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;
import androidmads.library.qrgenearator.QRGSaver;

public class QRCode extends AppCompatActivity {

    private static final String IMAGE_DIRECTORY =  Environment.getExternalStorageDirectory().getPath() + "/EventQR/";

    String TAG = "GenerateQrCode";

    ImageView qrimg;
    Bitmap mBitmap;

    QRGEncoder mQRGEncoder;

    Button mm, exit,save;

    FirebaseDatabase database=FirebaseDatabase.getInstance();
    DatabaseReference reference= database.getReference("users");

    RequestQueue mRequestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_qrcode);

        final User mUser = (User) getIntent().getSerializableExtra("user");
        final String input = mUser.getPnr();

        qrimg = findViewById(R.id.qr);
        mm = findViewById(R.id.main_menu);
        exit = findViewById(R.id.close);
        save = findViewById(R.id.save_img);


        storeLatestQRCodePNR(input);

        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        int width = point.x;
        int height = point.y;
        int smallerdimen = width < height ? width : height;
        smallerdimen = smallerdimen * 3 / 4;
        mQRGEncoder = new QRGEncoder(input, null, QRGContents.Type.TEXT, smallerdimen);

        try {
            mBitmap = mQRGEncoder.encodeAsBitmap();
            qrimg.setImageBitmap(mBitmap);
        } catch (WriterException e) {
            Log.v(TAG, e.toString());
        }

        mRequestQueue = Volley.newRequestQueue(this);

        Toast.makeText(this, input, Toast.LENGTH_LONG).show();

        final ProgressDialog pd = new ProgressDialog(QRCode.this);
        pd.setMessage("Please Wait...");
        pd.setCanceledOnTouchOutside(false);
      //  pd.show();

        String cName = reference.push().getKey();
        reference.child(cName).setValue(mUser);


        StringRequest serverRq = new StringRequest(Request.Method.POST, "http://16.171.10.192", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

                System.out.println("kaishi21");
                Display display = windowManager.getDefaultDisplay();
                Point point = new Point();
                display.getSize(point);
                int width = point.x;
                int height = point.y;
                int smallerdimen = width < height ? width : height;
                smallerdimen = smallerdimen * 3 / 4;
                mQRGEncoder = new QRGEncoder(input, null, QRGContents.Type.TEXT, smallerdimen);

                try {
                    mBitmap = mQRGEncoder.encodeAsBitmap();
                    qrimg.setImageBitmap(mBitmap);
                } catch (WriterException e) {
                    Log.v(TAG, e.toString());
                }
                pd.dismiss();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }) {
            public Map<String, String> getParams() throws AuthFailureError {

                Map<String, String> data = new HashMap<>();

                data.put("pnr", mUser.getPnr());
                data.put("name", mUser.getName());
                data.put("activityname", mUser.getActivityname());
                data.put("activityno", mUser.getActivityname());
                data.put("state", mUser.getState());
                data.put("city", mUser.getCity());
                data.put("street", mUser.getStreet());
                data.put("postcode", mUser.getPostCode());
                data.put("date", mUser.getDate());
                data.put("emergency", mUser.getEmergencey());
                data.put("mobile", mUser.getMobile());

                return data;
            }
        };

        mRequestQueue.add(serverRq);


        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
//                    QRGSaver.save(IMAGE_DIRECTORY,input.trim(),mBitmap,QRGContents.ImageType.IMAGE_JPEG);
                    QRGSaver.save(IMAGE_DIRECTORY,"Team 17 Event",mBitmap,QRGContents.ImageType.IMAGE_JPEG);
                }

                catch (Exception e){
                    e.printStackTrace();
                }

                AlertDialog.Builder ab=new AlertDialog.Builder(QRCode.this);
                ab.setTitle("Info");
                ab.setMessage("File Saved to "+IMAGE_DIRECTORY);
                ab.setPositiveButton("OK",null);
                ab.show();
            }
        });

        mm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(QRCode.this, Home.class);
                startActivity(i);
                finish();
            }
        });

        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishAffinity();
            }
        });

    }

    private void storeLatestQRCodePNR(String input) {
        try {
            File file = new File(getFilesDir(), "latest_QR_PNR.txt");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
            bufferedWriter.write(input);
        } catch (Exception e) {
            Log.e(TAG, "storeLatestQRCodePNR: ", e );
            e.printStackTrace();
        }
    }


}
