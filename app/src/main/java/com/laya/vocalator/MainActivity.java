package com.laya.vocalator;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity
{
    private EditText newNumber;
    private TextView result;
    private TextView operatorView;
    private Double operand1 = null;
    private Double operand2 = null;
    private String pendingOperator = "=";

    private boolean state = false;
    private boolean granted;
    SpeechRecognizer sr;
    ImageButton voice;
    TextView history;

    StringBuilder detectedText;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        result = findViewById(R.id.result);
        newNumber = findViewById(R.id.newNumber);
        operatorView = findViewById(R.id.textView);

        sr = SpeechRecognizer.createSpeechRecognizer(this);
        granted = ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        voice = findViewById(R.id.voice);
        history = findViewById(R.id.voiceDetected);
        detectedText = new StringBuilder();

        ActionBar ab = getSupportActionBar();
        ColorDrawable cd = new ColorDrawable(Color.parseColor("#000000"));
        assert ab != null;
        ab.setBackgroundDrawable(cd);



        history.setMovementMethod(new ScrollingMovementMethod());

        sr.setRecognitionListener(new RecognitionListener()
        {
            @SuppressLint("SetTextI18n")
            @Override
            public void onReadyForSpeech(Bundle params) {}

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onEndOfSpeech()
            {
                state = false;
                voice.setBackground(getDrawable(R.drawable.micoff));
            }

            @Override
            public void onError(int error) {}

            @SuppressLint("SetTextI18n")
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onResults(Bundle results)
            {
                ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                try
                {
                    ArrayList<String> temp = (ArrayList<String>) Arrays.stream(data.get(0).split(" ")).collect(Collectors.toList());

                    for (String i : temp) { Log.d(TAG, "onResults -> " + i); }
                    Log.d(TAG, "onResults: size is :" + temp.size());

                    if (temp.size() == 2)
                    {
                        if (temp.get(0).equals("oneplus"))
                        {
                            String tempOp = temp.get(1);
                            temp.set(0,"1");
                            temp.set(1,"+");
                            temp.add(tempOp);
                        } else
                        {
                            speechModification(temp);
                        }
                    }

                    if (temp.size() == 3)
                    {
                        performOps(temp.get(0),temp.get(1));
                        if(!temp.get(1).equals("="))
                        {
                            operatorView.setText(temp.get(1));
                        } else {operatorView.setText("");}

                        pendingOperator = "=";
                        performOps(temp.get(2), temp.get(1));
                    } else
                    {
                        performOps(temp.get(1),temp.get(0));
                        if(!temp.get(1).equals("="))
                        {
                            operatorView.setText(temp.get(0));
                        } else {operatorView.setText("");}

                    }
                    pendingOperator = "=";


                    for (String i : temp)
                    {
                        detectedText.append(i).append(" ");

                    }
                    detectedText.append("\n");
                    history.setText(detectedText);

                } catch(Exception e)
                {
                    Log.e(TAG, "onResults: Speech to text conversion failed -> " + e.getMessage());
                    result.setText("Speak out a valid number");
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint({"SetTextI18n", "UseCompatLoadingForDrawables"})
    private void performOps(String value, String operation) throws IllegalStateException
    {
        state = false;
        voice.setBackground(getDrawable(R.drawable.micoff));

        Log.d(TAG, "performOps: value is -> " + value);
        Log.d(TAG, "performOps: operator is -> " + operation);

        int error = 0;
        if (null == operand1) { operand1 = Double.valueOf(value); }
        else
        {
            operand2 = Double.valueOf(value);
            if (pendingOperator.equals("=")) { pendingOperator = operation; }
            switch(pendingOperator)
            {
                case "=":
                    operand1 = operand2;
                    break;
                case "/":
                    if (operand2 == 0) { error = 1;}
                    else
                        {
                            Log.d(TAG, "performOps: Division of " + operand1 + " and " + operand2);
                            operand1 /= operand2;
                        }
                    break;
                case "*": case "x":
                    Log.d(TAG, "performOps: Multiplication of " + operand1 + " and " + operand2);
                    operand1 *= operand2;
                    break;
                case "+":
                    Log.d(TAG, "performOps: addition of " + operand1 + " and " + operand2);
                    operand1 += operand2;
                    break;
                case "-":
                    Log.d(TAG, "performOps: Subtraction of " + operand1 + " and " + operand2);
                    operand1 -= operand2;
                    break;
            }
        }
        if (error == 0)
        {
            if (operand2 != null) { result.setText("= " + operand1.toString()); }
            else { result.setText(operand1.toString()); }
        }
        else { result.setText("Undefined");}

        if (operand2 != null)
        {
            operand2 = null;
        }
        Log.d(TAG, "performOps: operand 1 = " + operand1 + " and operand 2 = " + operand2);

        newNumber.setText("");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("SetTextI18n")
    public void ops(View v)
    {
        Button b = (Button) v;

        String operator = b.getText().toString();
        String value = newNumber.getText().toString();

        if (value.equals(".") || value.equals("-") || value.equals("-."))
        {
            result.setText("Enter valid Number");
        } else if (value.length() != 0)
        {
            performOps(value,operator);
        }

        pendingOperator = operator;

        if(!operator.equals("="))
        {
            operatorView.setText(operator);
        } else {operatorView.setText("");}

        if (!b.getText().equals("="))
        {
            detectedText.append(b.getText());
        } else
        {
            detectedText.append("\n");
            history.setText(detectedText);
        }
    }

    public void listener(View v)
    {
        Button b = (Button) v;
        newNumber.append(b.getText().toString());
        detectedText.append(b.getText());
    }

    public void clearOps(View v)
    {
        newNumber.setText("");
        result.setText("");
        operatorView.setText("");
        operand1 = null;
        operand2 = null;
        history.setText("");
        detectedText = new StringBuilder();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void trigger(View v)
    {
        if(granted)
        {
            Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

            if (!state)
            {
                sr.startListening(speechIntent);
                voice.setBackground(getDrawable(R.drawable.micon));
                state = true;
            } else
            {
                sr.stopListening();
                voice.setBackground(getDrawable(R.drawable.micoff));
                state = false;
            }
        } else
        {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},1);
            granted = ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
            Toast t = Toast.makeText(this,"Please give the required permissions",Toast.LENGTH_SHORT);
            t.show();
        }
    }

    private void speechModification(ArrayList<String> voiceData)
    {
        switch(voiceData.get(0))
        {
            case "plus": case "add": case "ad": case "place":
                voiceData.set(0,"+");
                break;
            case "minus": case "subtract": case "deduct":
                voiceData.set(0,"-");
                break;
            case "multiplied": case "multiply": case "into":
                voiceData.set(0,"*");
                break;
            case "divide": case "divided":
                voiceData.set(0,"/");
                break;
            default:
                // Nothing to do !!
        }

        switch(voiceData.get(1))
        {
            case "one":
                voiceData.set(1,"1");
                break;
            case "two": case "to":
            voiceData.set(1,"2");
                break;
            case "three":
                voiceData.set(1,"3");
                break;
            case "four":
                voiceData.set(1,"4");
                break;
            case "five":
                voiceData.set(1,"5");
                break;
            case "six":
                voiceData.set(1,"6");
                break;
            case "seven":
                voiceData.set(1,"7");
                break;
            case "eight":
                voiceData.set(1,"8");
                break;
            case "nine":
                voiceData.set(1,"9");
                break;
            case "zero":
                voiceData.set(1,"0");
            default:
                // Nothing to do !!

        }

        Log.d(TAG, "speechModification: Number is -> " + voiceData.get(1));
    }

    @SuppressLint("SetTextI18n")
    public void neg(View v)
    {
        String value = newNumber.getText().toString();
        if (value.length() == 0)
        {
            newNumber.setText("-");
            detectedText.append("-");
        } else
        {
            try
            {
                double doubleValue = Double.parseDouble(value);
                doubleValue *= -1;
                newNumber.setText(Double.toString(doubleValue));
                detectedText.append(doubleValue);
            } catch(NumberFormatException e)
            {
                newNumber.setText("");
            }
        }
    }
}