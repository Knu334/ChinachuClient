package com.tao.chinachuclient;

import android.app.Application;
import android.widget.EditText;
import android.widget.Spinner;

import com.tao.chinachuclient.data.Encode;

import Chinachu4j.Chinachu4j;

public class ApplicationClass extends Application{

    private Chinachu4j chinachu;
    private boolean streaming, encStreaming;
    private boolean reloadList;

    public void setChinachu(Chinachu4j chinachu){
        this.chinachu = chinachu;
    }

    public Chinachu4j getChinachu(){
        return chinachu;
    }

    public void setStreaming(boolean streaming){
        this.streaming = streaming;
    }

    public boolean getStreaming(){
        return streaming;
    }

    public void setEncStreaming(boolean encStreaming){
        this.encStreaming = encStreaming;
    }

    public boolean getEncStreaming(){
        return encStreaming;
    }

    public void setReloadList(boolean reloadList){
        this.reloadList = reloadList;
    }

    public boolean getReloadList(){
        return reloadList;
    }

    public Encode getEncodeSetting(Spinner type, EditText containerFormat, EditText videoCodec, EditText audioCodec,
                                   EditText videoBitrate, Spinner videoBitrateFormat,
                                   EditText audioBitrate, Spinner audioBitrateFormat,
                                   EditText videoSize, EditText frame){
        String vb = "";
        String ab = "";
        if(!videoBitrate.getText().toString().isEmpty()){
            int videoBit = Integer.parseInt(videoBitrate.getText().toString());
            if(videoBitrateFormat.getSelectedItemPosition() == 0)
                videoBit *= 1000;
            else
                videoBit *= 1000000;
            vb = String.valueOf(videoBit);
        }

        if(!audioBitrate.getText().toString().isEmpty()){
            int audioBit = Integer.parseInt(audioBitrate.getText().toString());
            if(audioBitrateFormat.getSelectedItemPosition() == 0)
                audioBit *= 1000;
            else
                audioBit *= 1000000;
            ab = String.valueOf(audioBit);
        }

        return new Encode(
                (String)type.getSelectedItem(),
                containerFormat.getText().toString(),
                videoCodec.getText().toString(),
                audioCodec.getText().toString(),
                vb,
                ab,
                videoSize.getText().toString(),
                frame.getText().toString());
    }

}
