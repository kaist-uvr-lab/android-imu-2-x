package kr.ac.kaist.arrc.imustreamlib;

/**
 * Created by arrc on 4/16/2018.
 */


public class ReturningValues {

    public Boolean msgSending = false;
    public Boolean msgWriting = false;
    public String NAME = "";

    public ReturningValues(){

    }

    public ReturningValues(boolean msgSending, boolean msgWriting, String NAME) {
        this.msgSending = msgSending;
        this.msgWriting = msgWriting;
        this.NAME = NAME;
    }
}