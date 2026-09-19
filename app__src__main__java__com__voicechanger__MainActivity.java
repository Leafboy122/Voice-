package com.voicechanger;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = 4096;

    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private Thread recordingThread;
    private boolean isRecording = false;

    private VoiceEffectProcessor effectProcessor;
    private Button startButton, stopButton;
    private Spinner effectSpinner;
    private SeekBar pitchSeekBar, speedSeekBar, volumeSeekBar;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions();
        initializeUI();
        setupEffectSpinner();

        effectProcessor = new VoiceEffectProcessor(SAMPLE_RATE);
    }

    private void initializeUI() {
        startButton = findViewById(R.id.btn_start);
        stopButton = findViewById(R.id.btn_stop);
        effectSpinner = findViewById(R.id.spinner_effects);
        pitchSeekBar = findViewById(R.id.seekbar_pitch);
        speedSeekBar = findViewById(R.id.seekbar_speed);
        volumeSeekBar = findViewById(R.id.seekbar_volume);
        statusText = findViewById(R.id.tv_status);

        startButton.setOnClickListener(v -> startRecording());
        stopButton.setOnClickListener(v -> stopRecording());

        // Pitch control (0.5x to 2.5x)
        pitchSeekBar.setMax(100);
        pitchSeekBar.setProgress(50);
        pitchSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float pitch = 0.5f + (progress / 100f) * 2f;
                effectProcessor.setPitch(pitch);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Speed control (0.5x to 2.5x)
        speedSeekBar.setMax(100);
        speedSeekBar.setProgress(50);
        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float speed = 0.5f + (progress / 100f) * 2f;
                effectProcessor.setSpeed(speed);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Volume control
        volumeSeekBar.setMax(100);
        volumeSeekBar.setProgress(70);
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float volume = progress / 100f;
                effectProcessor.setVolume(volume);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupEffectSpinner() {
        String[] effects = {"Normal", "Robot", "Helium", "Deep Voice", "Echo", "Reverb"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, effects);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        effectSpinner.setAdapter(adapter);

        effectSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                effectProcessor.setEffect(position);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startRecording() {
        if (isRecording) return;

        try {
            int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG,
                    AUDIO_FORMAT, Math.max(minBufferSize, BUFFER_SIZE * 4));

            int minPlayBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AUDIO_FORMAT);
            audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                    AUDIO_FORMAT, Math.max(minPlayBufferSize, BUFFER_SIZE * 4), AudioTrack.MODE_STREAM);

            audioRecord.startRecording();
            audioTrack.play();
            isRecording = true;

            recordingThread = new Thread(this::recordAndProcessAudio);
            recordingThread.start();

            statusText.setText("🎙️ RECORDING");
            statusText.setTextColor(getResources().getColor(R.color.accent));
            startButton.setEnabled(false);
            stopButton.setEnabled(true);

            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void recordAndProcessAudio() {
        short[] audioBuffer = new short[BUFFER_SIZE];

        while (isRecording) {
            try {
                int readSize = audioRecord.read(audioBuffer, 0, BUFFER_SIZE);

                if (readSize > 0) {
                    short[] processedAudio = effectProcessor.processAudio(audioBuffer, readSize);
                    audioTrack.write(processedAudio, 0, readSize);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording() {
        if (!isRecording) return;

        isRecording = false;

        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }

        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }

        try {
            if (recordingThread != null) {
                recordingThread.join();
                recordingThread = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        statusText.setText("Ready");
        statusText.setTextColor(getResources().getColor(R.color.text_primary));
        startButton.setEnabled(true);
        stopButton.setEnabled(false);

        Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecording();
    }
}
