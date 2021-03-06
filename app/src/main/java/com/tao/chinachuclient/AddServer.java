package com.tao.chinachuclient;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.tao.chinachuclient.data.Encode;
import com.tao.chinachuclient.data.Server;

import org.json.JSONException;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import Chinachu4j.Chinachu4j;
import Chinachu4j.Program;

public class AddServer extends AppCompatActivity{

    private EditText chinachuAddress, username, password;
    private boolean startMain;

    private Spinner type, videoBitrateFormat, audioBitrateFormat;
    private EditText containerFormat, videoCodec, audioCodec, videoBitrate, audioBitrate, videoSize, frame;

    private String raw_chinachuAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        startMain = getIntent().getBooleanExtra("startMain", false);

        chinachuAddress = (EditText)findViewById(R.id.chinachuAddress);
        username = (EditText)findViewById(R.id.username);
        password = (EditText)findViewById(R.id.password);

        type = (Spinner)findViewById(R.id.enc_setting_type_spinner);
        containerFormat = (EditText)findViewById(R.id.enc_setting_container_edit);
        videoCodec = (EditText)findViewById(R.id.enc_setting_videoCodec_edit);
        audioCodec = (EditText)findViewById(R.id.enc_setting_audioCodec_edit);

        videoBitrate = (EditText)findViewById(R.id.enc_setting_videoBitrate);
        videoBitrateFormat = (Spinner)findViewById(R.id.enc_setting_video_bitrate_spinner);
        audioBitrate = (EditText)findViewById(R.id.enc_setting_audioBitrate);
        audioBitrateFormat = (Spinner)findViewById(R.id.enc_setting_audio_bitrate_spinner);
        videoSize = (EditText)findViewById(R.id.enc_setting_videoSize);
        frame = (EditText)findViewById(R.id.enc_setting_frame);
    }

    public void ok(View v){
        raw_chinachuAddress = chinachuAddress.getText().toString();
        if(!(raw_chinachuAddress.startsWith("http://") || raw_chinachuAddress.startsWith("https://"))){
            Toast.makeText(this, R.string.wrong_server_address, Toast.LENGTH_SHORT).show();
            return;
        }

        final DBUtils dbUtils = new DBUtils(this);
        if(dbUtils.serverExists(raw_chinachuAddress)){
            Toast.makeText(this, R.string.already_register, Toast.LENGTH_SHORT).show();
            dbUtils.close();
            return;
        }

        new AsyncTask<Void, Void, Program[]>(){
            private ProgressDialog progDialog;

            @Override
            protected void onPreExecute(){
                progDialog = new ProgressDialog(AddServer.this);
                progDialog.setMessage(getString(R.string.getting_channel_list));
                progDialog.setIndeterminate(false);
                progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progDialog.setCancelable(true);
                progDialog.show();
            }

            @Override
            protected Program[] doInBackground(Void... params){
                try{
                    Chinachu4j chinachu = new Chinachu4j(raw_chinachuAddress, username.getText().toString(), password.getText().toString());
                    return chinachu.getAllSchedule();
                }catch(KeyManagementException | NoSuchAlgorithmException | IOException | JSONException e){
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Program[] result){
                progDialog.dismiss();
                if(result == null){
                    Toast.makeText(AddServer.this, R.string.error_get_channel_list, Toast.LENGTH_SHORT).show();
                    return;
                }
                ArrayList<String> id_name = new ArrayList<String>();
                for(Program p : result){
                    if(id_name.indexOf(p.getChannel().getId() + "," + p.getChannel().getName()) == -1){
                        id_name.add(p.getChannel().getId() + "," + p.getChannel().getName());
                    }
                }
                String channelIds = "";
                String channelNames = "";
                for(String s : id_name){
                    channelIds += s.split(",")[0] + ",";
                    channelNames += s.split(",")[1] + ",";
                }

                Encode encode = ((ApplicationClass)getApplicationContext()).getEncodeSetting(
                        type, containerFormat, videoCodec, audioCodec,
                        videoBitrate, videoBitrateFormat, audioBitrate, audioBitrateFormat, videoSize, frame);

                Server server = new Server(raw_chinachuAddress,
                        Base64.encodeToString(username.getText().toString().getBytes(), Base64.DEFAULT),
                        Base64.encodeToString(password.getText().toString().getBytes(), Base64.DEFAULT),
                        false, false, encode, channelIds, channelNames, false);

                dbUtils.insertServer(server);
                dbUtils.close();

                if(startMain){
                    dbUtils.serverPutPref(AddServer.this, server);
                    startActivity(new Intent(AddServer.this, MainActivity.class));
                }
                finish();
            }
        }.execute();
    }

    public void background(View v){
        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(item.getItemId() == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
