package com.vtech.audio.helper;

public class SoxCommandLib {
    static {
        System.loadLibrary("mySox");
    }
    public native static int ExcuateCommand(String cmd);
}
