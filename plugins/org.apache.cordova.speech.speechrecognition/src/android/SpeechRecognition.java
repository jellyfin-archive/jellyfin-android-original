package org.apache.cordova.speech;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import android.util.Log;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

/**
 * Style and such borrowed from the TTS and PhoneListener plugins
 */
public class SpeechRecognition extends CordovaPlugin {
    private static final String LOG_TAG = SpeechRecognition.class.getSimpleName();
    public static final String ACTION_INIT = "init";
    public static final String ACTION_SPEECH_RECOGNIZE_START = "start";
    public static final String ACTION_SPEECH_RECOGNIZE_STOP = "stop";
    public static final String ACTION_SPEECH_RECOGNIZE_ABORT = "abort";
    public static final String NOT_PRESENT_MESSAGE = "Speech recognition is not present or enabled";

    private CallbackContext speechRecognizerCallbackContext;
    private boolean recognizerPresent = false;
    private SpeechRecognizer recognizer;
    private boolean aborted = false;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        // Dispatcher
        if (ACTION_INIT.equals(action)) {
            // init
            if (DoInit()) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
                
                Handler loopHandler = new Handler(Looper.getMainLooper());
                loopHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        recognizer = SpeechRecognizer.createSpeechRecognizer(cordova.getActivity().getBaseContext());
                        recognizer.setRecognitionListener(new SpeechRecognitionListner());
                    }
                    
                });
            } else {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, NOT_PRESENT_MESSAGE));
            }
        }
        else if (ACTION_SPEECH_RECOGNIZE_START.equals(action)) {
            // recognize speech
            if (!recognizerPresent) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, NOT_PRESENT_MESSAGE));
            }
            
            String lang = args.optString(0, "en");

            this.speechRecognizerCallbackContext = callbackContext;

            final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);        
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"voice.recognition.test");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,lang);

            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5); 
            
            Handler loopHandler = new Handler(Looper.getMainLooper());
            loopHandler.post(new Runnable() {

                @Override
                public void run() {
                    recognizer.startListening(intent);
                }
                
            });
            
            PluginResult res = new PluginResult(PluginResult.Status.NO_RESULT);
            res.setKeepCallback(true);
            callbackContext.sendPluginResult(res);
        }
        else if (ACTION_SPEECH_RECOGNIZE_STOP.equals(action)) {
            stop(false);
        }
        else if (ACTION_SPEECH_RECOGNIZE_ABORT.equals(action)) {
            stop(true);
        }
        else {
            // Invalid action
            String res = "Unknown action: " + action;
            return false;
        }
        return true;
    }
    
    private void stop(boolean abort) {
        this.aborted = abort;
        Handler loopHandler = new Handler(Looper.getMainLooper());
        loopHandler.post(new Runnable() {

            @Override
            public void run() {
                recognizer.stopListening();
            }
            
        });
    }

    /**
     * Initialize the speech recognizer by checking if one exists.
     */
    private boolean DoInit() {
        this.recognizerPresent = SpeechRecognizer.isRecognitionAvailable(this.cordova.getActivity().getBaseContext());
        return this.recognizerPresent;
    }

    private void fireRecognitionEvent(ArrayList<String> transcripts, float[] confidences) {
        JSONObject event = new JSONObject();
        JSONArray results = new JSONArray();
        try {
            for(int i=0; i<transcripts.size(); i++) {
                JSONArray alternatives = new JSONArray();
                JSONObject result = new JSONObject();
                result.put("transcript", transcripts.get(i));
                result.put("final", true);
                if (confidences != null) {
                    result.put("confidence", confidences[i]);
                }
                alternatives.put(result);
                results.put(alternatives);
            }
            event.put("type", "result");
            event.put("emma", null);
            event.put("interpretation", null);
            event.put("results", results);
        } catch (JSONException e) {
            // this will never happen
        }
        PluginResult pr = new PluginResult(PluginResult.Status.OK, event);
        pr.setKeepCallback(true);
        this.speechRecognizerCallbackContext.sendPluginResult(pr); 
    }

    private void fireEvent(String type) {
        JSONObject event = new JSONObject();
        try {
            event.put("type",type);
        } catch (JSONException e) {
            // this will never happen
        }
        PluginResult pr = new PluginResult(PluginResult.Status.OK, event);
        pr.setKeepCallback(true);
        this.speechRecognizerCallbackContext.sendPluginResult(pr); 
    }

    private void fireErrorEvent() {
        JSONObject event = new JSONObject();
        try {
            event.put("type","error");
        } catch (JSONException e) {
            // this will never happen
        }
        PluginResult pr = new PluginResult(PluginResult.Status.ERROR, event);
        pr.setKeepCallback(false);
        this.speechRecognizerCallbackContext.sendPluginResult(pr); 
    }

    class SpeechRecognitionListner implements RecognitionListener {

        @Override
        public void onBeginningOfSpeech() {
            Log.d(LOG_TAG, "begin speech");
            fireEvent("start");
            fireEvent("audiostart");
            fireEvent("soundstart");
            fireEvent("speechstart");
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            Log.d(LOG_TAG, "buffer received");
        }

        @Override
        public void onEndOfSpeech() {
            Log.d(LOG_TAG, "end speech");
            fireEvent("speechend");
            fireEvent("soundend");
            fireEvent("audioend");
            fireEvent("end");
        }

        @Override
        public void onError(int error) {
            Log.d(LOG_TAG, "error speech");
            fireErrorEvent();
            fireEvent("end");
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            Log.d(LOG_TAG, "event speech");
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            Log.d(LOG_TAG, "partial results");
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d(LOG_TAG, "ready for speech");
        }

        @Override
        public void onResults(Bundle results) {
            Log.d(LOG_TAG, "results");
            String str = new String();
            Log.d(LOG_TAG, "onResults " + results);
            ArrayList<String> transcript = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            float[] confidence = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
            if (transcript.size() > 0) {
                Log.d(LOG_TAG, "fire recognition event");
                fireRecognitionEvent(transcript, confidence);
            } else {
                Log.d(LOG_TAG, "fire no match event");
                fireEvent("nomatch");
            }
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            Log.d(LOG_TAG, "rms changed");
        }
        
    }
}