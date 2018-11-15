package de.cavus700.customizableedittext;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CustomizableEditText cet = findViewById(R.id.customEditText);

        String demoText1 = "<font color=\"#ff0000\">This</font> <b>is <font color=\"#0000ff\">a</font></b> <i>demo</i> <u>text</u>.<br/>";
        String demoText2 = "Feel free to try this editor yourself.";

        cet.setText(demoText1 + demoText2);

        String currentText = cet.getText().toString();
        cet.addSpan(currentText.length() - demoText2.length(),
            currentText.length(),
            CustomizableEditText.SpanType.bold,
            null);
        cet.addSpan(currentText.length() - demoText2.length(),
            currentText.length(),
            CustomizableEditText.SpanType.color,
            "00ff00");
    }
}
