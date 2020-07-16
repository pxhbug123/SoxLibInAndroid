package com.vtech.record;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

//import org.apache.cordova.PluginResult.Status;

public class ExtAudioRecorder {
    private final static int[] sampleRates = {44100, 22050, 11025, 8000};
    static private ExtAudioRecorder EAR;

    public static ExtAudioRecorder getInstanse() {
        return ExtAudioRecorder.getInstance("0", 0, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    }

    private static ExtAudioRecorder getInstance(String id, int sampleRate, int channels, int encoding) {
        if (EAR != null)
            return EAR;
        int[] processedSampleRates = sampleRates;

        if (0 != sampleRate) {
            processedSampleRates = new int[1];
            processedSampleRates[0] = sampleRate;
        }

        int i = 0;
        do {
            EAR = new ExtAudioRecorder(
                    id,
                    AudioSource.MIC,
                    processedSampleRates[i],
                    channels,
                    encoding);

        } while ((++i < processedSampleRates.length) & !(EAR.getState() == State.INITIALIZING));

        return EAR;
    }

    /**
     * INITIALIZING : recorder is initializing;
     * READY : recorder has been initialized, recorder not yet started
     * RECORDING : recording
     * ERROR : reconstruction needed
     * STOPPED: reset needed
     */
    public enum State {INITIALIZING, READY, RECORDING, ERROR, STOPPED}

    ;

    // The interval in which the recorded samples are output to the file
    // Used only in uncompressed mode
    private static final int TIMER_INTERVAL = 120;

    // AudioPlayer message ids
    private static int MEDIA_STATE = 1;
    private static int MEDIA_BUFFER = 2;

    // Recorder used for uncompressed recording
    private AudioRecord audioRecorder = null;

    // Stores current amplitude (only in uncompressed mode)
    private int cAmplitude = 0;

    // Output file path
    private String filePath = null;

    // Recorder state; see State
    private State state;

    // Error message if any
    private String errorMessage = "";

    // File writer (only in uncompressed mode)
    private RandomAccessFile randomAccessWriter;

    // Number of channels, sample rate, sample size(size in bits), buffer size, audio source, sample size(see AudioFormat)
    private short nChannels;
    private int channelConfig;
    private int sRate;
    private short bSamples;
    private int bufferSize;
    private int aSource;
    private int aFormat;

    // Number of frames written to file on each output(only in uncompressed mode)
    private int framePeriod;

    // Buffer for output(only in uncompressed mode)
    private byte[] buffer;
    // Buffer intended for other processing
    private JSONArray humanBuffer;

    // Number of bytes written to file after header(only in uncompressed mode)
    // after stop() is called, this size is written to the header/data chunk in the wave file
    private int payloadSize;

    // If we are recording a set amount of data, declare it here
    private int maxPayloadSize = Integer.MAX_VALUE;

    // Reference back to cordova plugin in order to sent messages to javascript
//	private WAVRecorder 			 handler;

    // Recorder ID
    private String id;

    /**
     * Returns the state of the recorder in a RehearsalAudioRecord.State typed object.
     * Useful, as no exceptions are thrown.
     *
     * @return recorder state
     */
    public State getState() {
        return state;
    }

    /*
     *
     * Method used for recording.
     *
     */
    private AudioRecord.OnRecordPositionUpdateListener updateListener = new AudioRecord.OnRecordPositionUpdateListener() {
        public void onPeriodicNotification(AudioRecord recorder) {
            audioRecorder.read(buffer, 0, buffer.length); // Fill buffer
            if (randomAccessWriter != null) {
                try {
                    randomAccessWriter.write(buffer); // Write buffer to file
                    payloadSize += buffer.length;
                    if (bSamples == 16) {
                        humanBuffer = new JSONArray();
                        for (int i = 0; i < buffer.length / 2; i++) { // 16bit sample size

                            short curSample = getShort(buffer[i * 2], buffer[i * 2 + 1]);
                            humanBuffer.put(getInteger(buffer[i * 2], buffer[i * 2 + 1]));
                            if (curSample > cAmplitude) { // Check amplitude
                                cAmplitude = curSample;
                            }
                        }
                    } else { // 8bit sample size
                        humanBuffer = new JSONArray();
                        for (int i = 0; i < buffer.length; i++) {
                            humanBuffer.put((short) buffer[i]);
                            if (buffer[i] > cAmplitude) { // Check amplitude
                                cAmplitude = buffer[i];
                            }
                        }
                    }
                    // send the buffer to javascript
//					if (handler != null) {
//						//handler.webView.sendJavascript("cordova.require('ro.martinescu.audio.WAVRecorder').onStatus('" + id + "', " + MEDIA_BUFFER + ", " + humanBuffer.toString() + ");");
//					}
                } catch (IOException e) {
                    Log.e(ExtAudioRecorder.class.getName(), "Error occured in updateListener, recording is aborted");
                    errorMessage = e.getMessage();
                    //stop();
                    setState(State.ERROR);
                }

                if (maxPayloadSize <= payloadSize) stop();
            }
        }

        public void onMarkerReached(AudioRecord recorder) {
            // NOT USED
        }
    };

    /**
     * Default constructor
     * <p>
     * Instantiates a new recorder, in case of compressed recording the parameters can be left as 0.
     * In case of errors, no exception is thrown, but the state is set to ERROR
     */
    private ExtAudioRecorder(String id, int audioSource, int sampleRate, int channelConfig, int audioFormat) {
        this.id = id;

        try {
            if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) {
                bSamples = 16;
            } else {
                bSamples = 8;
            }

            // Force MONO recording for simplicity
            nChannels = 1;
            this.channelConfig = channelConfig;

            aSource = audioSource;
            sRate = sampleRate;
            aFormat = audioFormat;

            framePeriod = sampleRate * TIMER_INTERVAL / 1000;
            bufferSize = framePeriod * 2 * bSamples * nChannels / 8;
            if (bufferSize < AudioRecord.getMinBufferSize(sampleRate, this.channelConfig, audioFormat)) { // Check to make sure buffer size is not smaller than the smallest allowed one
                bufferSize = AudioRecord.getMinBufferSize(sampleRate, this.channelConfig, audioFormat);
                // Set frame period and timer interval accordingly
                framePeriod = bufferSize / (2 * bSamples * nChannels / 8);
                Log.w(ExtAudioRecorder.class.getName(), "Increasing buffer size to " + Integer.toString(bufferSize));
            }

            audioRecorder = new AudioRecord(audioSource, sampleRate, this.channelConfig, audioFormat, bufferSize);

            if (audioRecorder.getState() != AudioRecord.STATE_INITIALIZED)
                throw new Exception("AudioRecord initialization failed");
            audioRecorder.setRecordPositionUpdateListener(updateListener);
            audioRecorder.setPositionNotificationPeriod(framePeriod);

            cAmplitude = 0;
            filePath = null;
            this.setState(State.INITIALIZING);
        } catch (Exception e) {
            if (e.getMessage() != null) {
                Log.e(ExtAudioRecorder.class.getName(), e.getMessage());
                errorMessage = e.getMessage();
            } else {
                errorMessage = "Unknown error occured while initializing recording";
                Log.e(ExtAudioRecorder.class.getName(), errorMessage);
            }
            this.setState(State.ERROR);
        }
    }

    /**
     * Sets output file path, call directly after construction/reset.
     *
     * @param output file path
     */
    public void setOutputFile(String argPath) {
        try {
            if (state == State.INITIALIZING) {
                if (argPath.indexOf('/') == 0) // assume absolute path
                {
                    filePath = argPath;
                } else {
                    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                        filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + argPath;
                    } else {
                        filePath = "/sdcard/" + argPath;
                    }
                }
                Log.d(ExtAudioRecorder.class.getName(), "writing to " + filePath);
            }
        } catch (Exception e) {
            if (e.getMessage() != null) {
                Log.e(ExtAudioRecorder.class.getName(), e.getMessage());
                errorMessage = e.getMessage();
            } else {
                errorMessage = "Unknown error occured while setting output path";
                Log.e(ExtAudioRecorder.class.getName(), errorMessage);
            }
            this.setState(State.ERROR);
        }
    }

    /**
     * Returns the largest amplitude sampled since the last call to this method.
     *
     * @return returns the largest amplitude since the last call, or 0 when not in recording state.
     */
    public int getMaxAmplitude() {
        if (state == State.RECORDING) {
            int result = cAmplitude;
            cAmplitude = 0;
            return result;
        } else {
            return 0;
        }
    }


    /**
     * Prepares the recorder for recording, in case the recorder is not in the INITIALIZING state and the file path was not set
     * the recorder is set to the ERROR state, which makes a reconstruction necessary.
     * In case uncompressed recording is toggled, the header of the wave file is written.
     * In case of an exception, the state is changed to ERROR
     */
    public void prepare() {
        try {
            if (state == State.INITIALIZING) {
                if ((audioRecorder.getState() == AudioRecord.STATE_INITIALIZED) & (filePath != null)) {
                    // write file header

                    randomAccessWriter = new RandomAccessFile(filePath, "rw");

                    randomAccessWriter.setLength(0); // Set file length to 0, to prevent unexpected behavior in case the file already existed
                    randomAccessWriter.writeBytes("RIFF");
                    randomAccessWriter.writeInt(0); // Final file size not known yet, write 0
                    randomAccessWriter.writeBytes("WAVE");
                    randomAccessWriter.writeBytes("fmt ");
                    randomAccessWriter.writeInt(Integer.reverseBytes(16)); // Sub-chunk size, 16 for PCM
                    randomAccessWriter.writeShort(Short.reverseBytes((short) 1)); // AudioFormat, 1 for PCM
                    randomAccessWriter.writeShort(Short.reverseBytes(nChannels));// Number of channels, 1 for mono, 2 for stereo
                    randomAccessWriter.writeInt(Integer.reverseBytes(sRate)); // Sample rate
                    randomAccessWriter.writeInt(Integer.reverseBytes(sRate * bSamples * nChannels / 8)); // Byte rate, SampleRate*NumberOfChannels*BitsPerSample/8
                    randomAccessWriter.writeShort(Short.reverseBytes((short) (nChannels * bSamples / 8))); // Block align, NumberOfChannels*BitsPerSample/8
                    randomAccessWriter.writeShort(Short.reverseBytes(bSamples)); // Bits per sample
                    randomAccessWriter.writeBytes("data");
                    randomAccessWriter.writeInt(0); // Data chunk size not known yet, write 0

                    buffer = new byte[framePeriod * bSamples / 8 * nChannels];
                    this.setState(State.READY);
                } else {
                    errorMessage = "prepare() method called on uninitialized recorder";
                    Log.e(ExtAudioRecorder.class.getName(), errorMessage);
                    this.setState(State.ERROR);
                }
            } else {
                errorMessage = "prepare() method called on illegal state";
                Log.e(ExtAudioRecorder.class.getName(), errorMessage);
                release();
                this.setState(State.ERROR);
            }
        } catch (Exception e) {
            if (e.getMessage() != null) {
                errorMessage = e.getMessage();
                Log.e(ExtAudioRecorder.class.getName(), e.getMessage());
            } else {
                errorMessage = "Unknown error occured in prepare()";
                Log.e(ExtAudioRecorder.class.getName(), errorMessage);
            }
            this.setState(State.ERROR);
        }
    }

    /**
     * Releases the resources associated with this class, and removes the unnecessary files, when necessary
     */
    public void release() {
        if (state == State.RECORDING) {
            stop();
        } else {
            if (state == State.READY) {
                try {
                    randomAccessWriter.close(); // Remove prepared file
                } catch (IOException e) {
                    errorMessage = e.getMessage();
                    Log.e(ExtAudioRecorder.class.getName(), "I/O exception occured while closing output file");
                }
                (new File(filePath)).delete();
            }
        }

        if (audioRecorder != null) {
            audioRecorder.release();
        }

        if (EAR != null) {
            EAR = null;
        }
    }

    /**
     * Resets the recorder to the INITIALIZING state, as if it was just created.
     * In case the class was in RECORDING state, the recording is stopped.
     * In case of exceptions the class is set to the ERROR state.
     */
    public void reset() {
        try {
            if (state != State.ERROR) {
                cAmplitude = 0; // Reset amplitude
                this.setState(State.INITIALIZING);
            }
        } catch (Exception e) {
            errorMessage = e.getMessage();
            Log.e(ExtAudioRecorder.class.getName(), e.getMessage());
            this.setState(State.ERROR);
        }
    }

    /**
     * Starts the recording, and sets the state to RECORDING.
     * Call after prepare().
     */
    public void start() {
        if (state == State.READY) {
            payloadSize = 0;
            audioRecorder.startRecording();
            audioRecorder.read(buffer, 0, buffer.length);
            this.setState(State.RECORDING);
        } else {
            errorMessage = "start() called on illegal state";
            Log.e(ExtAudioRecorder.class.getName(), errorMessage);
            this.setState(State.ERROR);
        }
    }

    /**
     * Like start, but with a timeout after a certoin number of milliseconds
     */
    public void recordFor(int durationMS) {
        if (state == State.READY) {
            payloadSize = 0;
            maxPayloadSize = durationMS * sRate / (1000 * 8) * nChannels * bSamples;
            Log.d(ExtAudioRecorder.class.getName(), "We're going to record a payload of " + maxPayloadSize);
            audioRecorder.startRecording();
            audioRecorder.read(buffer, 0, buffer.length);
            this.setState(State.RECORDING);
        } else {
            errorMessage = "start() called on illegal state";
            Log.e(ExtAudioRecorder.class.getName(), errorMessage);
            this.setState(State.ERROR);
        }
    }

    /**
     * Stops the recording, and sets the state to STOPPED.
     * In case of further usage, a reset is needed.
     * Also finalizes the wave file in case of uncompressed recording.
     */
    public void stop() {
        if (state == State.RECORDING) {
            audioRecorder.stop();

            try {
                randomAccessWriter.seek(4); // Write size to RIFF header
                randomAccessWriter.writeInt(Integer.reverseBytes(36 + payloadSize));

                randomAccessWriter.seek(40); // Write size to Subchunk2Size field
                randomAccessWriter.writeInt(Integer.reverseBytes(payloadSize));

                randomAccessWriter.close();
                randomAccessWriter = null;
            } catch (IOException e) {
                errorMessage = e.getMessage();
                Log.e(ExtAudioRecorder.class.getName(), "I/O exception occured while closing output file");
                this.setState(State.ERROR);
            }
            this.setState(State.STOPPED);
        } else {
            errorMessage = "stop() called on illegal state";
            Log.e(ExtAudioRecorder.class.getName(), errorMessage);
            this.setState(State.ERROR);
        }
    }

    /*
     *
     * Converts a byte[2] to a short, in LITTLE_ENDIAN format
     *
     */
    private short getShort(byte argB1, byte argB2) {
        return (short) getInteger(argB1, argB2);
    }

    /*
     *
     * Converts a byte[2] to a int, in LITTLE_ENDIAN format
     *
     */
    private int getInteger(byte argB1, byte argB2) {
        return (int) (argB1 | (argB2 << 8));
    }

    private void setState(State state) {
//		if (this.handler != null) {
//            this.handler.webView.sendJavascript("cordova.require('ro.martinescu.audio.WAVRecorder').onStatus('" + this.id + "', " + MEDIA_STATE + ", '" + state.toString() + "', '" + errorMessage + "' );");
//        }
        this.state = state;
    }

    public String getFilePath() {
        return filePath;
    }

}
