package com.brainbear.webrtc_vad_android;

import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "brainbear";
    private static final int frequency = 16000;
    private static final int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private Button btnStop;
    private Button btnStart;
    private TextView tvVad;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvVad = findViewById(R.id.tv_vad);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecord();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecord();
            }
        });
    }

    private boolean recording = false;

    private void startRecord() {
        if (recording) {
            return;
        }

        recording = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                int minBufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
                AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency, channelConfiguration, audioEncoding, minBufferSize);
                audioRecord.startRecording();

                VADHandler vadHandler = new VADHandler();

                int ret = vadHandler.create(3);
                Log.i(TAG, "create vad handler:" + ret);


                while (recording) {
                    byte[] frame = new byte[320];
                    int read = audioRecord.read(frame, 0, frame.length);
                    Log.d(TAG, "run: " + read);

                    short[] vad_data = new short[160];
                    ByteBuffer.wrap(frame).order(ByteOrder.LITTLE_ENDIAN)
                            .asShortBuffer().get(vad_data);

                    int vad = vadHandler.process(16000, vad_data, 160);
                    Log.i(TAG, "vad:" + vad);
                    refreshView(vad);
                }

                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;

                vadHandler.release();
            }
        }).start();
    }


    private void stopRecord() {
        recording = false;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecord();
    }

    private void refreshView(final int code) {
        tvVad.post(new Runnable() {
            @Override
            public void run() {
                switch (code) {
                    case 0:
                        tvVad.setText("Non-active Voice");
                        tvVad.setBackgroundColor(Color.BLUE);
                        break;
                    case 1:
                        tvVad.setText("Active Voice");
                        tvVad.setBackgroundColor(Color.RED);
                        break;
                    default:
                        tvVad.setText("Error");
                        tvVad.setBackgroundColor(Color.YELLOW);
                        break;
                }
            }
        });
    }

}
