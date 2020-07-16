package com.vtech.soxcommandlibandroid;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.vtech.audio.helper.SoxCommandLib;
import com.vtech.record.ExtAudioRecorder;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener{
    //    static {
//        System.loadLibrary("mySox");
//    }
    private boolean Swtich=true;
    private int Pstatus=0;
    private EditText input;
    private TextView result;
    private String mFile;
    private Handler mHandler=new Handler(){
        public void handleMessage(Message msg) {

        }
    };
    private Runnable mRunnable=new Runnable() {
        @Override
        public void run() {
            Button btn=findViewById(R.id.Play);
            btn.setEnabled(true);
        }
    };
    private MediaPlayer mediaPlayer;
    //private AudioRecorder audioRecorder;
    private void InitPermission(){
        int REQUEST_EXTERNAL_STORAGE = 1;
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
        };
        if (PackageManager.PERMISSION_GRANTED !=
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        input=findViewById(R.id.InputCMD);
        input.setText("");
        mFile="/sdcard/vtech/a.wav";
        Button play=findViewById(R.id.Play);
        play.setEnabled(false);
        InitPermission();
        mediaPlayer=new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
    }
    public void MickyMouseVoice(View view){
//        String cmdJni=input.getText().toString();
//        int result= SoxCommandLib.ExcuateCommand(cmdJni);
//        input.setText("sox /sdcard/vtech/noise_audio.wav -n noiseprof /sdcard/vtech/noise.prof");
//        result=SoxCommandLib.ExcuateCommand(input.getText().toString());
        File exists=new File("/sdcard/vtech/a.wav");
        if(!exists.exists())
            return;
        NoiseProfFile();
        File deleteFile=new File("/sdcard/vtech/b.wav");
        if(deleteFile.exists())
        {
            deleteFile.delete();
        }
        Button play=findViewById(R.id.Play);
        play.setEnabled(false);
        //input.setText("sox /sdcard/vtech/a.wav /sdcard/vtech/b.wav noisered /sdcard/vtech/noise.prof 0.21 rate 22050 pitch 660 tempo 1.25 echo 1.0 0.7 10 0.7 echo 1.0 0.7 12 0.7 echo 1.0 0.88 12 0.7 echo 1.0 0.88 30 0.7 echo 1.0 0.6 60 0.7  echo 0.8 0.88 6 0.4 vol 12dB bass 15 rate 44100");
        Log.e("SoxCommand->Micky","Start");
        new Thread(){
            public void run(){
                String cmd="sox /sdcard/vtech/a.wav /sdcard/vtech/b.wav noisered /sdcard/vtech/noise.prof 0.21 rate 22050 pitch 880 tempo 0.95 bass 15 rate 44100";
                int result=SoxCommandLib.ExcuateCommand(cmd);
                mFile="/sdcard/vtech/b.wav";
                mHandler.postDelayed(mRunnable,500);
            }
        }.start();
        Log.e("SoxCommand->Micky","End");
    }
    public void RobotVoice(View view){
        File exists=new File("/sdcard/vtech/a.wav");
        if(!exists.exists())
            return;
        NoiseProfFile();
        File deleteFile=new File("/sdcard/vtech/b.wav");
        if(deleteFile.exists())
        {
            deleteFile.delete();
        }
        Log.e("SoxCommand->Robot","Start");
        Button play=findViewById(R.id.Play);
        play.setEnabled(false);
        new Thread(){
            public void run(){
                String cmd="sox /sdcard/vtech/a.wav /sdcard/vtech/b.wav noisered /sdcard/vtech/noise.prof 0.21 rate 22050 pitch 660 tempo 1.25 echo 1.0 0.7 10 0.7 echo 1.0 0.7 12 0.7 echo 1.0 0.88 12 0.7 echo 1.0 0.88 30 0.7 echo 1.0 0.6 60 0.7  echo 0.8 0.88 6 0.4 vol 12dB bass 15 rate 44100";
                int result=SoxCommandLib.ExcuateCommand(cmd);
                mFile="/sdcard/vtech/b.wav";
                mHandler.postDelayed(mRunnable,500);
            }
        }.start();
        Log.e("SoxCommand->Robot","End");
    }
    public void Record(View view){
        Button btn=(Button) view;
        if(Swtich){
            File deleteFile=new File("/sdcard/vtech/a.wav");
            if(deleteFile.exists())
            {
                deleteFile.delete();
            }
            Button play=findViewById(R.id.Play);
            play.setEnabled(false);
            ExtAudioRecorder.getInstanse().reset();
            ExtAudioRecorder.getInstanse().setOutputFile("/sdcard/vtech/a.wav");
            ExtAudioRecorder.getInstanse().prepare();
            ExtAudioRecorder.getInstanse().start();
            Swtich=false;
            btn.setText("Recording");
            return;
        }else{
            ExtAudioRecorder.getInstanse().stop();
            btn.setText("Record");
            mFile="/sdcard/vtech/a.wav";
            Swtich=true;
            mHandler.postDelayed(mRunnable,500);
        }
    }
    private void NoiseProfFile(){
        File deleteFile=new File("/sdcard/vtech/noise_audio.wav");
        if(deleteFile.exists())
        {
            deleteFile.delete();
        }
        deleteFile=new File("/sdcard/vtech/noise.prof");
        if(deleteFile.exists())
        {
            deleteFile.delete();
        }
        String cmd1="sox /sdcard/vtech/a.wav /sdcard/vtech/noise_audio.wav trim 0 0.900";
        int result= SoxCommandLib.ExcuateCommand(cmd1);
        String cmd2="sox /sdcard/vtech/noise_audio.wav -n noiseprof /sdcard/vtech/noise.prof";
        result= SoxCommandLib.ExcuateCommand(cmd2);
    }
//    public void Record(View view){
//        Button btn=(Button) view;
//        switch (status)
//        {
//            case 0:
//                audioRecorder.startRecord(null);
//                btn.setText("Recording");
//                status=1;
//            break;
//            case 1:
//                audioRecorder.stopRecord();
//                btn.setText("RECORD");
//                status=0;
//            break;
//        }
//    }
    @Override
    public void onCompletion(MediaPlayer mp) {
        Button btn=findViewById(R.id.Play);
        Play(btn);
    }
    public void Play(View view){
        Button btn=(Button) view;
        if(Pstatus==0){
            btn.setText("Stop");
            Pstatus=1;
            try {
                mediaPlayer.setDataSource(mFile);
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            mediaPlayer.stop();
            mediaPlayer.reset();
            btn.setText("PLAY");
            mFile="/sdcard/vtech/a.wav";
            Pstatus=0;
        }
    }
}
