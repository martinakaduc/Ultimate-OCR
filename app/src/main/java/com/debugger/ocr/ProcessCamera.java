package com.debugger.ocr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.util.ArrayList;
import java.util.List;

public class ProcessCamera extends AppCompatActivity implements SurfaceHolder.Callback, Detector.Processor<TextBlock> {

    Paint rectPaint;
    Integer screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
    Integer screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
    Integer imgWidth = 720;
    Integer imgHeight = 1280;
    String resultStr;
    List<Rect> listBB;
    private SurfaceView rectView;
    private TextView txtView;
    private CameraSource cameraSource;
    private SurfaceHolder holder;

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    cameraSource.start(holder);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_ocr);
        SurfaceView cameraView = findViewById(R.id.surface_view);
        rectView = findViewById(R.id.rect_view);
        txtView = findViewById(R.id.txtview);
        holder = cameraView.getHolder();

        rectPaint = new Paint();
        rectPaint.setColor(Color.GREEN);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(4.0f);

        listBB = new ArrayList<Rect>();

        TextRecognizer txtRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        if (!txtRecognizer.isOperational()) {
            Log.e("Main Activity", "Detector dependencies are not yet available");
        } else {
            cameraSource = new CameraSource.Builder(getApplicationContext(), txtRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(imgHeight, imgWidth)
                    .setRequestedFps(2.0f)
                    .setAutoFocusEnabled(true)
                    .build();

            holder.addCallback(this);
            txtRecognizer.setProcessor(this);
        }

        rectView.getHolder().setFormat(PixelFormat.TRANSPARENT);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            }

            cameraSource.start(holder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (width > height) {
            imgWidth = imgWidth + imgHeight;
            imgHeight = imgWidth - imgHeight;
            imgWidth = imgWidth - imgHeight;
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        this.holder = null;
        cameraSource.stop();
    }

    @Override
    public void release() {

    }

    @Override
    public void receiveDetections(Detector.Detections detections) {
        SparseArray<TextBlock> items = detections.getDetectedItems();
        final StringBuilder strBuilder = new StringBuilder();
        listBB.clear();

        for (int i = 0; i < items.size(); i++) {
            TextBlock item = items.valueAt(i);
            listBB.add(
                    scaleRect(item.getBoundingBox())
            );

            strBuilder.append(item.getValue());
            strBuilder.append(" ");
        }

        resultStr = strBuilder.toString();
        Log.v("Result: ", resultStr);

        txtView.post(new Runnable() {
            @Override
            public void run() {
                txtView.setText(resultStr);
            }
        });

        rectView.post(new Runnable() {
            @Override
            public void run() {
                Canvas canvas = rectView.getHolder().lockCanvas();

                if (canvas != null) {
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                    for (Rect rect : listBB) {
                        canvas.drawRect(rect, rectPaint);
                    }

                    rectView.getHolder().unlockCanvasAndPost(canvas);
                }

                rectView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // TODO
                        // Send resultStr to server here
                    }
                });

            }
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        listBB.clear();
        resultStr = "";
    }

    private Rect scaleRect(Rect inRect) {
        return new Rect(
                inRect.left * screenWidth / imgWidth,
                inRect.top * screenHeight / imgHeight,
                inRect.right * screenWidth / imgWidth,
                inRect.bottom * screenHeight / imgHeight
        );
    }
}
