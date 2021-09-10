package com.fabricethilaw.textrecoginitionapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import static android.view.View.GONE;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private static final int MY_DATA_CHECK_CODE = 3434;
    final int RequestCameraPermissionID = 1001;
    SurfaceView cameraView;
    TextView textView;
    ProgressBar progressBar;
    CameraSource cameraSource;
    Button snap;
    Button textToSpeechBtn;
    TextToSpeech mTts;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case RequestCameraPermissionID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    try {
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
            break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = (SurfaceView) findViewById(R.id.cameraView);
        textView = (TextView) findViewById(R.id.tvResult);
        snap = findViewById(R.id.snapbtn);
        progressBar = findViewById(R.id.progressbar);
        textToSpeechBtn = findViewById(R.id.speakBtn);
        textToSpeechBtn.setOnClickListener(v -> speakRecognizedText());
        snap.setOnClickListener(v -> cameraSource.stop());
        textToSpeechBtn.setEnabled(false);
        progressBar.setVisibility(GONE);

        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!textRecognizer.isOperational()) {
            Log.w("MainActivity", "Detector dependencies are not yet available");
        } else {
            startTextRecognition(textRecognizer);
        }
    }

    private void speakRecognizedText() {
        textToSpeechBtn.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);

    }

    private void showTranslateText() {
        Intent i = new Intent(MainActivity.this, translater.class);
        i.putExtra("result", textView.getText());
        startActivity(i);
    }

    private void startTextRecognition(TextRecognizer textRecognizer) {
        cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(640, 1024)
                .setRequestedFps(2.0f)
                .setAutoFocusEnabled(true)
                .build();
        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {

                try {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                RequestCameraPermissionID);
                        return;
                    }
                    cameraSource.start(cameraView.getHolder());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                cameraSource.stop();
            }
        });

        textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<TextBlock> detections) {

                final SparseArray<TextBlock> items = detections.getDetectedItems();
                if (items.size() != 0) {
                    textView.post(() -> {
                        StringBuilder stringBuilder = new StringBuilder();
                        for (int i = 0; i < items.size(); ++i) {
                            TextBlock item = items.valueAt(i);
                            stringBuilder.append(item.getValue());
                            stringBuilder.append("\n");
                        }
                        textToSpeechBtn.setEnabled(true);
                        textView.setText(stringBuilder.toString());
                    });
                }
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MY_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // success, create the TTS instance
                mTts = new TextToSpeech(this, this);
            } else {
                // missing data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(
                        TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }
    }

    private void setupTTS(TextToSpeech mTts) {
        mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        textToSpeechBtn.setVisibility(View.VISIBLE);
                        textToSpeechBtn.setText("App is talking");
                        textToSpeechBtn.setEnabled(false);
                        progressBar.setVisibility(GONE);
                    }
                });


            }

            @Override
            public void onDone(String utteranceId) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        textToSpeechBtn.setVisibility(View.VISIBLE);
                        textToSpeechBtn.setText("SPEAK");
                        textToSpeechBtn.setEnabled(true);
                        progressBar.setVisibility(GONE);
                        if (utteranceId == "end of wakeup message ID") {
                            //   playAnnoyingMusic();
                        }
                    }
                });

            }


            @Override
            public void onError(String utteranceId) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    textToSpeechBtn.setVisibility(View.VISIBLE);
                    textToSpeechBtn.setText("SPEAK");
                    textToSpeechBtn.setEnabled(true);
                    progressBar.setVisibility(GONE);
                    Toast.makeText(MainActivity.this, "An error occurred during text-to-speech", Toast.LENGTH_SHORT).show();

                });
            }
        });

        mTts.setLanguage(Locale.US);
        int result = mTts.setLanguage(Locale.US);
        if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("error", "This Language is not supported");
        } else {
            HashMap<String, String> myHashAlarm = new HashMap<>();
            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
                    String.valueOf(AudioManager.STREAM_ALARM));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                mTts.speak(textView.getText().toString(), TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                mTts.speak(textView.getText().toString(), TextToSpeech.QUEUE_FLUSH, myHashAlarm);
            }
            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "end of wakeup message ID");


            // int isUKAvailable =  mTts.isLanguageAvailable(Locale.UK);
            // int isFRAvailable = mTts.isLanguageAvailable(Locale.FRANCE);
            // int isSPAAvailable =  mTts.isLanguageAvailable(new Locale("spa", "ESP"));
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            progressBar.setVisibility(View.VISIBLE);
            textToSpeechBtn.setVisibility(GONE);
            setupTTS(mTts);
        } else Log.e("error", "Initilization Failed!");

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}