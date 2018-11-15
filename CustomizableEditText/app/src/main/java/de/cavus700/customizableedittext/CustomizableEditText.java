package de.cavus700.customizableedittext;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomizableEditText extends AppCompatEditText implements TextWatcher {
    public enum SpanType{bold, underline, italic, color}

    private ArrayList<HTMLElement> mHtmlElements;
    private String[] mSupportedTags = {"<b>", "</b>", "<u>", "</u>", "<i>", "</i>", "<font color=\".*\">", "</font>"};
    private boolean isTextWatcherActivated = false;

    public CustomizableEditText(Context context) {
        super(context);
    }

    public CustomizableEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomizableEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        //Do nothing if we are not initialized
        if(!isTextWatcherActivated)
            return;

        HTMLText htmlText = getElementForPosition(start);
        //We have no HTMLText so create it
        if(mHtmlElements == null || mHtmlElements.size() == 0){
            if(mHtmlElements == null)
                mHtmlElements = new ArrayList<>();
            mHtmlElements.add(new HTMLText(s.toString()));
            return;
        }

        removeText(start, start + before);
        addText(s.subSequence(start, start + count).toString(), start);
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    /**
     * Is called from TextWatcher if new text is entered
     */
    private void addText(String textToAdd, int start){
        HTMLText htmlText = getElementForPosition(start);
        boolean isEndOfText = htmlText.getText().length() == getPosInHtmlTextElement(start);

        int indexOfTextElement = mHtmlElements.indexOf(htmlText);
        //Has next element
        boolean isNextElementClosingTag = indexOfTextElement < mHtmlElements.size() - 1 &&
            //Is next element closing tag and
            (!mHtmlElements.get(indexOfTextElement+1).isOpenTag() &&
                //Is next element no text element
                !(mHtmlElements.get(indexOfTextElement+1)instanceof HTMLText));

        if(isEndOfText && isNextElementClosingTag) {
            //We have to add a new text element here
            //Example: <i><b>Hello</b></i> if user adds text after "Hello" it will be inserted between the tags
            //We don't want this so we have to create a new text element after the last closing tag.
            HTMLElement tmpElement;
            for (int iIndex = mHtmlElements.indexOf(htmlText) + 1; iIndex < mHtmlElements.size(); ++iIndex) {
                tmpElement = mHtmlElements.get(iIndex);
                //HTMLText elements are returning false for isOpenTag()
                if ((tmpElement instanceof HTMLText) || tmpElement.isOpenTag()) {
                    htmlText = new HTMLText(textToAdd);
                    mHtmlElements.add(iIndex, htmlText);
                    return;
                }
            }
            //Current element was only followed by closing tags so append element to list
            htmlText = new HTMLText(textToAdd);
            mHtmlElements.add(htmlText);
            return;
        }

        String currText = htmlText.getText();
        String tmp1 = currText.substring(0, getPosInHtmlTextElement(start));
        String tmp2 = currText.substring(getPosInHtmlTextElement(start), currText.length());
        htmlText.setText(tmp1 + textToAdd + tmp2);
    }

    /**
     * Is called from TextWatcher if text is removed
     */
    private void removeText(int start, int end){
        if(start >= end)
            return;

        //Text deleted
        HTMLText startText = getElementForPosition(start);
        HTMLText endText = getElementForPosition(end);

        //Text is in only one HTMLText element
        if(startText == endText){
            startText.setText(startText.getText().substring(0, getPosInHtmlTextElement(start)) +
                startText.getText().substring(getPosInHtmlTextElement(end), startText.getText().length()));
        }
        //Text spreads over multiple elements
        else{
            start = getPosInHtmlTextElement(start);
            end = getPosInHtmlTextElement(end);
            startText.setText(startText.getText().substring(0, start));
            endText.setText(endText.getText().substring(end, endText.getText().length()));

            removeElementsBetween(mHtmlElements.indexOf(startText)+1, mHtmlElements.indexOf(endText)-1);
        }
    }

    /**
     * Remove HTMLText elements and make sure to only remove tags with corresponding closing or opening tag
     * @param startIndex First html candidate to be removed
     * @param endIndex  Last html candidate to be removed
     */
    private void removeElementsBetween(int startIndex, int endIndex){
        int iStart = startIndex;
        int iEnd = endIndex;
        HTMLElement startEle = mHtmlElements.get(iStart), endEle = mHtmlElements.get(iEnd);

        while(iStart < iEnd) {
            //Only increment iStart if we don't found an opening element yet. Keep on searching for an opening tag
            if (!startEle.isOpenTag()) {
                startEle = mHtmlElements.get(++iStart);
                continue;
            }

            //Wee have an opening element. Look if we have the corresponding closing tag and if so remove both.
            if (startEle.isOpenTag() && !endEle.isOpenTag() && startEle.getType().equals(endEle.getType())) {
                mHtmlElements.remove(startEle);
                mHtmlElements.remove(endEle);
                //We removed to elements so our end element moved to the front
                endIndex = endIndex - 2;
                removeElementsBetween(startIndex, endIndex);
                return;
            } else {
                //End element was not the matching one so keep on searching
                endEle = mHtmlElements.get(--iEnd);
                continue;
            }
        }

        //We found no matching element with the current startElement so maybe it does not have a closing one.
        //Check if we reached the end or just had an start element from the start or the middle
        if(iStart < endIndex - 1){
            removeElementsBetween(++iStart, endIndex);
        }
    }

    /**
     * Initialize CustomizableEditText with text which only contains the supproted tags.
     * In the best case only text got with {@link #getPlainText()}
     * @param text Text to initialize
     */
    public void setText(String text){
        isTextWatcherActivated = false;
        mHtmlElements = new ArrayList<>();
        if(text != null && text.length() > 0) {
            text = text.replaceAll("<br/>", "\n");
            mHtmlElements.add(new HTMLText(text));

            parseText();
            updateText();
        }
        isTextWatcherActivated = true;
    }

    private void parseText(){
        HTMLElement element;

        //Look in each element if tags have to be replaced
        for(int iEle = 0; iEle < mHtmlElements.size(); ++iEle){
            element = mHtmlElements.get(iEle);
            //Only replace text in HTMLText elements
            if(element instanceof HTMLText){
                //Look for the tags
                for(String tag: mSupportedTags){
                    //If one tag is found start method again because elments have changed
                    if(replaceTag(element,tag, iEle)){
                        parseText();
                        return;
                    }
                }
            }
        }
    }

    /**
     * @return Text from the parent EditText with all added HTML tags
     */
    public String getPlainText(){
        CleanUp();
        String text = "";
        for(HTMLElement ele: mHtmlElements){
            text += ele.getText();
        }

        text = text.replaceAll("\n", "<br/>");

        return text;
    }

    private boolean replaceTag(HTMLElement element, String tag, int iEntry){
        String text = ((HTMLText) element).getText();
        Pattern p = Pattern.compile(tag);  // insert your pattern here
        Matcher m = p.matcher(text);
        if (m.find()) {
            int index = m.start();
            mHtmlElements.add(iEntry, new HTMLText(text.substring(index+(m.end()-m.start()), text.length())));
            mHtmlElements.add(iEntry, getElementForSupportedTag(tag, text.substring(m.start(), m.end())));
            mHtmlElements.add(iEntry, new HTMLText(text.substring(0, index)));
            mHtmlElements.remove(element);
            return true;
        }
        return false;
    }

    private HTMLElement getElementForSupportedTag(String tag, String text){
        if(tag.equals("<b>"))
            return new HTMLBold(true);
        if(tag.equals("</b>"))
            return new HTMLBold(false);
        if(tag.equals("<i>"))
            return new HTMLItalic(true);
        if(tag.equals("</i>"))
            return new HTMLItalic(false);
        if(tag.equals("<u>"))
            return new HTMLUnderline(true);
        if(tag.equals("</u>"))
            return new HTMLUnderline(false);
        if(tag.equals("<font color=\".*\">")){
            int index = text.indexOf('#');
            return new HTMLColor(true, text.substring(index+1, index + 7));
        }
        if(tag.equals("</font>")){
            return new HTMLColor(false, "");
        }
        return null;
    }

    private int getPosInHtmlTextElement(int posInText){
        for(HTMLElement element: mHtmlElements){
            //For non text elements just skip it
            if(!(element instanceof HTMLText)){
                continue;
            }
            //if our position is not in the current element decrement the pos and continue
            if(posInText > element.getSize()){
                posInText -= element.getSize();
            }
            else{
                //Pos is in this element
                return posInText;
            }
        }
        return -1;
    }

    private HTMLText getElementForPosition(int posInText){
        if(mHtmlElements == null)
            return null;

        for(HTMLElement element: mHtmlElements){
            //For non text elements just skip it
            if(!(element instanceof HTMLText)){
                continue;
            }
            //if our position is not in the current element decrement the pos and continue
            if(posInText > element.getSize()){
                posInText -= element.getSize();
            }
            else{
                //Pos is in this element
                return (HTMLText)element;
            }
        }
        return null;
    }

    private void CleanUp(){
        HTMLElement first, second;
        //Try to merge tags when a closing tag is directly followed by an opening tag of the same type.
        //Also remove empty text elements or merge to text elements.
        for(int iEle = 0; iEle < mHtmlElements.size()-1; ++iEle){
            first = mHtmlElements.get(iEle);
            second = mHtmlElements.get(iEle+1);

            if(first instanceof HTMLText && second instanceof HTMLText){
                String text = first.getText() + second.getText();
                mHtmlElements.remove(first);
                mHtmlElements.remove(second);
                mHtmlElements.add(iEle, new HTMLText(text));
                CleanUp();
                return;
            }
            else if(first instanceof HTMLBold && second instanceof HTMLBold &&
                !first.isOpenTag() && second.isOpenTag()){
                mHtmlElements.remove(first);
                mHtmlElements.remove(second);
                CleanUp();
                return;
            }
            else if(first instanceof HTMLUnderline && second instanceof HTMLUnderline &&
                !first.isOpenTag() && second.isOpenTag()){
                mHtmlElements.remove(first);
                mHtmlElements.remove(second);
                CleanUp();
                return;
            }
            else if(first instanceof HTMLItalic && second instanceof HTMLItalic &&
                !first.isOpenTag() && second.isOpenTag()){
                mHtmlElements.remove(first);
                mHtmlElements.remove(second);
                CleanUp();
                return;
            }
            else if(first instanceof HTMLText && first.getText().length() == 0){
                mHtmlElements.remove(first);
                CleanUp();
                return;
            }
        }

        CleanUpNestedSameTags();
    }

    /**
     * Remove nested tags of the same type: <a> <a> </a> </a> -> <a> </a>
     */
    private void CleanUpNestedSameTags(){
        ArrayList<HTMLElement> openedTags = new ArrayList<>();
        HTMLElement tag = null;
        boolean foundNestedTag = false;
        boolean containsNestedTags = true;
        while(containsNestedTags) {
            //Remove nested tags from same type
            for (int iEle = 0; iEle < mHtmlElements.size() - 1; ++iEle) {
                //No nested tag found until now
                if (!foundNestedTag) {
                    tag = mHtmlElements.get(iEle);
                    //Text is irrelevant, skip it
                    if(tag instanceof HTMLText)
                        continue;
                    if (tag.isOpenTag()) {
                        //Found a nested tag from same type
                        if (containsTagFromType(openedTags, tag)) {
                            foundNestedTag = true;
                        } else {
                            openedTags.add(tag);
                        }
                    } else {
                        //Remove the corresponding opened tag from the list
                        for (HTMLElement openEle : openedTags) {
                            if (openEle.getType().compareTo(tag.getType()) == 0 ) {
                                openedTags.remove(openEle);
                                break;
                            }
                        }
                    }
                } else {
                    //It does not matter if closeTag is the corresponding one tag.
                    //If there are multiple nested tags all of them will be deleted
                    //<a> <a> <a> </a> </a> </a> 1.Step
                    //<a>     <a>      </a> </a> 2.Step
                    //<a>                   </a> 3.Step
                    HTMLElement closeTag = mHtmlElements.get(--iEle); //Decrement because nested tag was found in previous round
                    if (tag.getType().compareTo(closeTag.getType()) == 0 ) {
                        mHtmlElements.remove(tag);
                        mHtmlElements.remove(closeTag);
                        //Go back to the while-loop and start again
                        break;
                    }
                }
            }
            //If we reach this point no nested tags were found
            containsNestedTags = false;
        }
    }

    private boolean containsTagFromType(ArrayList<HTMLElement> elements, HTMLElement tag){
        for(HTMLElement element: elements){
            if(element.getType().compareTo(tag.getType()) == 0){
                return true;
            }
        }
        return false;
    }

    private void updateText(){
        String text = "";
        for(HTMLElement ele: mHtmlElements){
            text += ele.getText();
        }

        text = text.replaceAll("\n", "<br/>");

        //Deactivate the textwatcher here because its event are goint to be triggered and we don't want this here.
        isTextWatcherActivated = false;
        super.setText(Html.fromHtml(text));
        isTextWatcherActivated = true;
    }

    /**
     * Add a HTML tag {@link android.text.Spannable} to the text of the parent {@link AppCompatEditText}
     * @param start Pos for open tag in text
     * @param end Pos for close tag in text
     * @param type Span type
     * @param color If type is no color span => ignored
     */
    public void addSpan(int start, int end, SpanType type, String color){
        //Wouldn't make sense to insert this empty tag
        if(start == end)
            return;
        HTMLText startEle = getElementForPosition(start);
        HTMLText endEle = getElementForPosition(end);

        //Simple case we have to add the span in only one tag
        if(startEle == endEle){
            int index = mHtmlElements.indexOf(startEle);
            start = getPosInHtmlTextElement(start);
            end = getPosInHtmlTextElement(end);

            addSpanToElement(startEle, index, type, color, start, end);
        }
        else{
            //We insert the elements from the back to the front to don't get confused with the indices.
            //If we would start at the front we have to keep track how many elements we add to the list.
            int posEndEle = getPosInHtmlTextElement(end);
            int posStartEle = getPosInHtmlTextElement(start);


            //Wrap each text element between in the tag.
            //If the elements appear right after each other the tags will be merged in the clean up routine.
            for(int iEleBetween = mHtmlElements.indexOf(endEle) - 1; iEleBetween > mHtmlElements.indexOf(startEle); --iEleBetween){
                HTMLElement elementBetween = mHtmlElements.get(iEleBetween);
                if(elementBetween instanceof HTMLText){
                    addSpanToElement(elementBetween, iEleBetween, type, color, 0, elementBetween.getText().length());
                }
            }

            addSpanToElement(endEle, mHtmlElements.indexOf(endEle), type, color, 0, posEndEle);
            addSpanToElement(startEle, mHtmlElements.indexOf(startEle), type, color, posStartEle, startEle.getText().length());
        }
        CleanUp();
        updateText();
    }

    /**
     *
     * @param element Element we have to split
     * @param indexOfElement Index in the HTMLElement-List
     * @param type SpanType
     * @param color String which is used as html attribute for the color
     * @param startPos Start position where the element needs to be splitted
     * @param endPos End position where the element needs to be splitted
     */
    private void addSpanToElement(HTMLElement element, int indexOfElement, SpanType type, String color, int startPos, int endPos ){
        //Wouldn't make sense to insert this empty tag
        if(startPos == endPos || element.getText() == null)
            return;

        if(endPos != element.getText().length()){
            //last part of text
            mHtmlElements.add(indexOfElement, new HTMLText(element.getText().substring(endPos, element.getText().length())));
        }
        //Closing span tag
        mHtmlElements.add(indexOfElement, getNewElement(type, false, color));
        //Middle part between the tags
        mHtmlElements.add(indexOfElement, new HTMLText(element.getText().substring(startPos, endPos)));
        //Opening span tag
        mHtmlElements.add(indexOfElement, getNewElement(type, true, color));
        //Only if startPos == 0, otherwise nothing would be added
        if(startPos != 0){
            //first part of text
            mHtmlElements.add(indexOfElement, new HTMLText(element.getText().substring(0,startPos)));
        }

        mHtmlElements.remove(element);
    }

    private HTMLElement getNewElement(SpanType type, boolean isOpen, String color){
        switch(type){
            case bold: return new HTMLBold(isOpen);
            case underline: return new HTMLUnderline(isOpen);
            case italic: return new HTMLItalic(isOpen);
            case color: return new HTMLColor(isOpen, color);
            default: return null;
        }
    }

    private abstract class HTMLElement{
        protected boolean isOpenTag;
        protected String text;
        protected String type;

        public boolean isOpenTag(){ return this.isOpenTag;}
        public int getSize(){return text == null ? 0 : text.length();}
        public String getText(){return this.text;}
        public String getType() {return type;}
    }
    private class HTMLText extends HTMLElement{
        HTMLText(String text){this.text = text; this.type="html_text";}
        public void setText(String text){this.text = text;}
    }
    private class HTMLBold extends HTMLElement{
        HTMLBold(boolean isOpenTag){this.isOpenTag = isOpenTag; text = isOpenTag ? "<b>":"</b>"; this.type=SpanType.bold.name();}
        public boolean isOpenTag(){return this.isOpenTag;}
    }
    private class HTMLUnderline extends HTMLElement{
        HTMLUnderline(boolean isOpenTag){this.isOpenTag = isOpenTag; text = isOpenTag ? "<u>":"</u>"; this.type=SpanType.underline.name();}
        public boolean isOpenTag(){return this.isOpenTag;}
    }
    private class HTMLItalic extends HTMLElement{
        HTMLItalic(boolean isOpenTag){this.isOpenTag = isOpenTag; text = isOpenTag ? "<i>":"</i>"; this.type=SpanType.italic.name();}
        public boolean isOpenTag(){return this.isOpenTag;}
    }
    private class HTMLColor extends HTMLElement{
        HTMLColor(boolean isOpenTag, String color){this.isOpenTag = isOpenTag; text = isOpenTag ? "<font color=\"#"+color+"\">":"</font>"; this.type=SpanType.color.name();}
        public boolean isOpenTag(){return this.isOpenTag;}
    }
}
