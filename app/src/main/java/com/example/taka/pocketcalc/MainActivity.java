package com.example.taka.pocketcalc;

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

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int maxKeta = 15;
    private static final double maxDouble = 999999999999999.0;

    private static final String OVERFLOW = "Overflow (Push AC)";
    private static final String ZERODIV = "Div by 0 (Push AC)";
    private String operand1 = "";
    private String operator = "";
    private boolean firstNum = true;

    private TextView textViewDisplay;
    private ClipboardManager clipboard;

    //音声認識フィールド
    private static final int REQUEST_CODE = 1000;
    private TextView textView;
    private Button buttonStart;
    private String temp;
    private String[] array_num;


    private int lang;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        Button buttonCopy = (Button) findViewById(R.id.buttonCopy);
        if (buttonCopy == null) {
            Log.d("ERROR", "buttonCopy == null");
        } else {
            buttonCopy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipData clip = ClipData.newPlainText("copied_text", textViewDisplay.getText().toString());
                    clipboard.setPrimaryClip(clip);
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

    // 画面回転の前に状態を保存する
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("display", textViewDisplay.getText().toString());
        outState.putString("operand1", operand1);
        outState.putString("operator", operator);
        outState.putBoolean("firstNum", firstNum);
    }


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
                for(i = 0;i<candidates.get(0).length();i++) {
                    array_num = candidates.get(0).split("");
                    //ログに表示
                    Log.d("debug", array_num[i]);
                }
            }
        }


        /*for(int i=0;) {
            buttonStr =
        }*/
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
                                textViewDisplay.setText(Long.toString(operand1Long / operand2Long));
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
                break;

            case "AC":
                textViewDisplay.setText("0");
                operand1 = "";
                operator = "";
                break;

            default:
                Log.d("ERROR", "識別できないボタンが押された：" + buttonStr);
        }
    }
}


