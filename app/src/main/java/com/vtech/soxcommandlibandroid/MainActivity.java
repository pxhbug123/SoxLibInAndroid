package com.vtech.soxcommandlibandroid;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.vtech.audio.helper.SoxCommandLib;
import com.vtech.soxcommandlibandroid.R;

public class MainActivity extends AppCompatActivity {
    //    static {
//        System.loadLibrary("mySox");
//    }
    private EditText input;
    private TextView result;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        input=findViewById(R.id.InputCMD);
        input.setText("sox /sdcard/vtech/a.wav /sdcard/vtech/noise_audio.wav trim 0 0.900");
    }
    public void Excuate(View view){
        String cmdJni=input.getText().toString();
        int result= SoxCommandLib.ExcuateCommand(cmdJni);
        input.setText("sox /sdcard/vtech/noise_audio.wav -n noiseprof /sdcard/vtech/noise.prof");
        result=SoxCommandLib.ExcuateCommand(input.getText().toString());
        input.setText("sox /sdcard/vtech/a.wav /sdcard/vtech/b.wav noisered /sdcard/vtech/noise.prof 0.21 rate 22050 pitch 660 tempo 1.25 echo 1.0 0.7 10 0.7 echo 1.0 0.7 12 0.7 echo 1.0 0.88 12 0.7 echo 1.0 0.88 30 0.7 echo 1.0 0.6 60 0.7  echo 0.8 0.88 6 0.4 vol 12dB bass 15 rate 44100");
        result=SoxCommandLib.ExcuateCommand(input.getText().toString());
    }
}
