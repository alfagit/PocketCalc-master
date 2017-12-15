package com.example.taka.pocketcalc;

import android.os.Build;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

import java.util.HashMap;
import orz.kassy.shakegesture.ShakeGestureManager;


public class MainActivity extends AppCompatActivity implements View.OnClickListener,TextToSpeech.OnInitListener{
    private static final int maxKeta = 15;
    private static final double maxDouble = 999999999999999.0;

    private static final String OVERFLOW = "Overflow (Push AC)";
    private static final String ZERODIV = "Div by 0 (Push AC)";
    private String operand1 = "";
    private String operator = "";
    private boolean firstNum = true;

    private TextView textViewDisplay;
    private ClipboardManager clipboard;
    private Button buttonCopy;
    private ClipData clip;

    //音声認識フィールド
    private static final int REQUEST_CODE = 1000;
    private TextView textView;
    private Button buttonStart;

    //音声出力フィールド
    private TextToSpeech tts;
    private static final String TAG = "TestTTS";

    private String[] array_num;
    private int length;


    private int lang;

    private ShakeGestureManager mGestureManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TTS インスタンス生成
        tts = new TextToSpeech(this, this);
        Button ttsButton = (Button)findViewById(R.id.button_tts);
        ttsButton.setOnClickListener(this);

        // 言語選択 0:日本語、1:英語、2:オフライン、その他:General
        lang = 0;

        // 認識結果を表示させる
        textView = (TextView)findViewById(R.id.text_view);

        buttonStart = (Button) findViewById(R.id.button_s);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 音声認識を開始
                speech();
            }
        });




        textViewDisplay = (TextView) findViewById(R.id.textViewDisplay);
        int[] buttonIDs = {R.id.button0, R.id.button1, R.id.button2, R.id.button3, R.id.button4,
                R.id.button5, R.id.button6, R.id.button7, R.id.button8, R.id.button9,
                R.id.buttonBS, R.id.buttonC, R.id.buttonAC, R.id.buttonAdd, R.id.buttonSub,
                R.id.buttonMul, R.id.buttonEq, R.id.buttonDiv, R.id.buttonMod, R.id.buttonSgn};
        for (int buttonID : buttonIDs) {
            Button button = (Button) findViewById(buttonID);
            if (button == null) {
                Log.d("ERROR", "button == null");
            } else {
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Button button = (Button) v;
                        String buttonStr = button.getText().toString();
                        process(buttonStr);
                    }
                });
            }
        }

        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        buttonCopy = (Button) findViewById(R.id.buttonCopy);
        if (buttonCopy == null) {
            Log.d("ERROR", "buttonCopy == null");
        } else {
            buttonCopy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clip = ClipData.newPlainText("copied_text", textViewDisplay.getText().toString());
                    clipboard.setPrimaryClip(clip);
                    //音声出力
                    tts.speak("コピーしました",TextToSpeech.QUEUE_FLUSH, null);
                    Toast.makeText(MainActivity.this, R.string.copy_label, Toast.LENGTH_SHORT).show();
                }
            });

        }




        // 画面回転の後に状態を元に戻す
        if (savedInstanceState != null) {
            textViewDisplay.setText(savedInstanceState.getString("display"));
            operand1 = savedInstanceState.getString("operand1");
            operator = savedInstanceState.getString("operator");
            firstNum = savedInstanceState.getBoolean("firstNum");
        }
    }


    @Override
    public void onInit(int status) {
        // TTS初期化
        if (TextToSpeech.SUCCESS == status) {
            Log.d(TAG, "initialized");
        } else {
            Log.e(TAG, "faile to initialize");
        }
    }

    @Override
    public void onClick(View v) {
        speechText();
    }
    private void shutDown(){
        if (null != tts) {
            // to release the resource of TextToSpeech
            tts.shutdown();
        }
    }

    private void speechText() {
        TextView textViewDisplay = (TextView)findViewById(R.id.textViewDisplay);

        // textViewDisplayからテキストを取得
        String string = textViewDisplay.getText().toString();

        if (0 < string.length()) {
            if (tts.isSpeaking()) {
                tts.stop();
                return;
            }
            setSpeechRate(1.0f);
            setSpeechPitch(1.0f);

            // tts.speak(text, TextToSpeech.QUEUE_FLUSH, null) に
            // KEY_PARAM_UTTERANCE_ID を HasMap で設定
            HashMap<String, String> map = new HashMap<String, String>();
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"messageID");

            tts.speak(string, TextToSpeech.QUEUE_FLUSH, map);
            setTtsListener();

        }
    }

    // 読み上げのスピード
    private void setSpeechRate(float rate){
        if (null != tts) {
            tts.setSpeechRate(rate);
        }
    }

    // 読み上げのピッチ
    private void setSpeechPitch(float pitch){
        if (null != tts) {
            tts.setPitch(pitch);
        }
    }

    // 読み上げの始まりと終わりを取得
    private void setTtsListener(){
        // android version more than 15th
        // 市場でのシェアが15未満は数パーセントなので除外
        if (Build.VERSION.SDK_INT >= 15)
        {
            int listenerResult = tts.setOnUtteranceProgressListener(new UtteranceProgressListener()
            {
                @Override
                public void onDone(String utteranceId)
                {
                    Log.d(TAG,"progress on Done " + utteranceId);
                }

                @Override
                public void onError(String utteranceId)
                {
                    Log.d(TAG,"progress on Error " + utteranceId);
                }

                @Override
                public void onStart(String utteranceId)
                {
                    Log.d(TAG,"progress on Start " + utteranceId);
                }

            });
            if (listenerResult != TextToSpeech.SUCCESS)
            {
                Log.e(TAG, "failed to add utterance progress listener");
            }
        }
        else {
            Log.e(TAG, "Build VERSION is less than API 15");
        }

    }

    protected void onDestroy() {
        super.onDestroy();
        shutDown();
    }


    // 画面回転の前に状態を保存する
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("display", textViewDisplay.getText().toString());
        outState.putString("operand1", operand1);
        outState.putString("operator", operator);
        outState.putBoolean("firstNum", firstNum);
    }

    //シェイク判定
    protected void onResume() {

        super.onResume();
        mGestureManager = new ShakeGestureManager(this, mListener);
        mGestureManager.startSensing();
    }

    protected void onPause() {

        super.onPause();
        mGestureManager.stopSensing();
    }


    private ShakeGestureManager.GestureListener mListener = new ShakeGestureManager.GestureListener() {
        @Override
        public void onGestureDetected(int gestureType, int gestureCount) {
            // ジェスチャーを認識したらここが呼ばれる
            speech();
        }

        @Override
        public void onMessage(String s) {

        }
    };


    private void speech() {
        // 音声認識が使えるか確認する
        try {
            // 音声認識の　Intent インスタンス
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

            if (lang == 0) {
                // 日本語
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.JAPAN.toString());
            } else if (lang == 1) {
                // 英語
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toString());
            } else if (lang == 2) {
                // Off line mode
                intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
            } else {
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            }

            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 100);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "音声を入力");
            // インテント発行
            startActivityForResult(intent, REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            textView.setText("No Activity ");
        }
    }


    // 結果を受け取るために onActivityResult を設置
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            // 認識結果を ArrayList で取得
            ArrayList<String> candidates = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            if (candidates.size() > 0) {
                // 認識結果候補で一番有力なものを表示
                textView.setText(candidates.get(0));
                //1文字ずつ格納
                int i = 0;
                length=candidates.get(0).length();

                //オールクリアをここで判別（candidatesの中身を比較）
                if(candidates.get(0).contains("オールクリア")) {
                    process("AC");
                }else if(candidates.get(0).contains("クリア")) {
                    process("C");
                }else if(candidates.get(0).contains("戻る")) {
                    process("←");
                }else if(candidates.get(0).contains("読み上げ")) {
                    speechText();
                }else if(candidates.get(0).contains("再生")) {
                    speechText();
                }else if(candidates.get(0).contains("コピー")) {
                    clip = ClipData.newPlainText("copied_text", textViewDisplay.getText().toString());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(MainActivity.this, R.string.copy_label, Toast.LENGTH_SHORT).show();
                    //音声出力
                    tts.speak("コピーしました",TextToSpeech.QUEUE_FLUSH, null);

                }

                //
                array_num = candidates.get(0).split("");

                //ログに表示+1
                Log.d("debug", array_num[i]);


                //数字と文字に分ける
                for(i=0;i<length+1;i++) {

                    if(array_num[i].equals("1"))process("1");
                    else if(array_num[i].equals("2")) process("2");
                    else if(array_num[i].equals("3")) process("3");
                    else if(array_num[i].equals("4")) process("4");
                    else if(array_num[i].equals("5")) process("5");
                    else if(array_num[i].equals("6")) process("6");
                    else if(array_num[i].equals("7")) process("7");
                    else if(array_num[i].equals("8")) process("8");
                    else if(array_num[i].equals("9")) process("9");
                    else if(array_num[i].equals("0")) process("0");
                    else if(array_num[i].equals("+")) process("+");
                    else if(array_num[i].equals("-")) process("-");
                    else if(array_num[i].equals("*")) process("*");
                    else if(array_num[i].equals("/")) process("/");
                    else if(array_num[i].equals("=")) process("=");
                    else if(array_num[i].equals("ク")) process("AC");
                    else if(array_num[i].equals("引")) process("-");
                    else if(array_num[i].equals("か")) process("*");
                    else if(array_num[i].equals("は")) process("=");
                    else if(array_num[i].equals("話")) process("=");
                    else if(array_num[i].equals("イ")) process("=");
                    else if(array_num[i].equals("÷")) process("/");
                    else if(array_num[i].equals("あ")) process("%");

                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void process(String buttonStr) {
        String display = textViewDisplay.getText().toString();
        Log.d("INFO", "buttonStr=" + buttonStr + ", operand1=" + operand1 + ", operator=" + operator + ", display=" + display);
        if (display.equals(OVERFLOW) || display.equals(ZERODIV)) {
            if (!buttonStr.equals("AC")) {
                return;
            }
        }


        long displayNum;
        switch (buttonStr) {
            case "0":
            case "1":
            case "2":
            case "3":
            case "4":
            case "5":
            case "6":
            case "7":
            case "8":
            case "9":

                if (firstNum) {
                    if (!operator.equals("")) {
                        operand1 = display;
                    }
                    display = "0";
                    firstNum = false;
                }
                if (display.length() >= maxKeta) {
                    textViewDisplay.setText(OVERFLOW);
                    break;
                }
                displayNum = Long.parseLong(display);
                long buttonNum = Long.parseLong(buttonStr);
                displayNum = displayNum * 10 + buttonNum;
                textViewDisplay.setText(Long.toString(displayNum));
                break;

            case "+":
            case "-":
            case "*":
            case "/":
            case "=":
            case "%":
                if (!operator.equals("") && !operand1.equals("") && !firstNum) {
                    long operand1Long = Long.parseLong(operand1);
                    long operand2Long = Long.parseLong(display);
                    double operand1Double = (double) operand1Long;
                    double operand2Double = (double) operand2Long;
                    switch (operator) {
                        case "+":
                            if (Math.abs(operand1Double + operand2Double) > maxDouble) {
                                textViewDisplay.setText(OVERFLOW);
                            } else {
                                textViewDisplay.setText(Long.toString(operand1Long + operand2Long));
                            }
                            break;
                        case "-":
                            if (Math.abs(operand1Double - operand2Double) > maxDouble) {
                                textViewDisplay.setText(OVERFLOW);
                            } else {
                                textViewDisplay.setText(Long.toString(operand1Long - operand2Long));
                            }
                            break;
                        case "*":
                            if (Math.abs(operand1Double * operand2Double) > maxDouble) {
                                textViewDisplay.setText(OVERFLOW);
                            } else {

                                textViewDisplay.setText(Long.toString(operand1Long * operand2Long));
                            }
                            break;
                        case "/":
                            if (operand2Long == 0L) {
                                textViewDisplay.setText(ZERODIV);
                            } else {
                                //textViewDisplay.setText(Long.toString(operand1Long / operand2Long));
                                textViewDisplay.setText(Double.toString(operand1Double / operand2Double));
                            }
                            break;
                        case "%":
                            if (operand2Long == 0L) {
                                textViewDisplay.setText(ZERODIV);
                            } else {
                                textViewDisplay.setText(Long.toString(operand1Long % operand2Long));
                            }
                            break;
                        default:
                            Log.d("ERROR", "operatorが識別できない：" + operator);
                    }
                }
                if (buttonStr.equals("=")) {
                    operator = "";
                    speechText();
                } else {
                    operator = buttonStr;
                }
                firstNum = true;

                break;

            case "←":
                displayNum = Long.parseLong(display);
                displayNum = displayNum / 10;
                textViewDisplay.setText(Long.toString(displayNum));
                break;

            case "±":
                displayNum = Long.parseLong(display);
                displayNum = -displayNum;
                textViewDisplay.setText(Long.toString(displayNum));
                break;

            case "C":

                textViewDisplay.setText("0");
                tts.speak("クリアしました",TextToSpeech.QUEUE_FLUSH, null);
                break;

            case "AC":
                textViewDisplay.setText("0");
                operand1 = "";
                operator = "";
                tts.speak("オールクリアしました",TextToSpeech.QUEUE_FLUSH, null);
                break;

            /*case ".":
                textViewDisplay.setText(display + ".");
                break;
            */
            default:
                Log.d("ERROR", "識別できないボタンが押された：" + buttonStr);
        }
    }
}


