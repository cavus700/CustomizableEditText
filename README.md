# CutomizableEditText
(![editor](https://user-images.githubusercontent.com/24620015/48557515-47883b80-e8e7-11e8-8e39-c51cbab13853.PNG))

## Description 
The CustomizableEditText allows you to easily put colored, bold, underlined or italic text into the normal EditText but
it has a few limitation: 
* You can only change the appearance of already written code. For example there is no option to start writting in bold or italic.
* There is no functionality to undo an adaption. If you changed the color of your text it will remain in this color.

## How To
### Add the CustomizableEditText to your layout
`<de.cavus700.record.CustomizableEditText
                android:id="@+id/yourId"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:gravity="top|start"
                app:layout_constraintBottom_toBottomOf="parent"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toBottomOf="parent" />`

### Use it in your code
`CustomizableEditText cet = findViewById(R.id.customEditText);`

`String demoText1 = "<font color=\"#ff0000\">This</font> <b>is <font color=\"#0000ff\">a</font></b> <i>demo</i> <u>text</u>.<br/>";`

`String demoText2 = "Feel free to try this editor yourself.";`

`cet.setText(demoText1 + demoText2);`

`String currentText = cet.getText().toString();`
`cet.addSpan(currentText.length() - demoText2.length(),`
`             currentText.length(),`
`             CustomizableEditText.SpanType.bold,`
`             null);`

`cet.addSpan(currentText.length() - demoText2.length(),`
`             currentText.length(),`
`             CustomizableEditText.SpanType.color,`
`             "00ff00");`
 

