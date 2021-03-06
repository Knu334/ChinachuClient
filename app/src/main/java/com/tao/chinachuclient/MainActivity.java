package com.tao.chinachuclient;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.tao.chinachuclient.data.Server;

import java.util.ArrayList;

import Chinachu4j.Chinachu4j;

public class MainActivity extends AppCompatActivity implements OnItemClickListener{

    private SharedPreferences pref;
    private Chinachu4j chinachu;
    private ApplicationClass appClass;
    private String chinachuAddress, username, password;
    private ListView mainList;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        mainList = new ListView(this);
        setContentView(mainList);

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        appClass = (ApplicationClass)getApplicationContext();

        try{
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            int versionCode = pi.versionCode;
            if(pref.getInt("versionCode", 0) < versionCode){
                new DBUtils(this).close();
                pref.edit().putInt("versionCode", versionCode).commit();
            }
        }catch(NameNotFoundException e){
        }

        chinachuAddress = pref.getString("chinachuAddress", null);
        username = pref.getString("username", null);
        password = pref.getString("password", null);
        if(chinachuAddress == null || username == null || password == null){
            Intent i = new Intent(this, AddServer.class);
            i.putExtra("startMain", true);
            startActivity(i);
            finish();
            return;
        }
        username = new String(Base64.decode(username, Base64.DEFAULT));
        password = new String(Base64.decode(password, Base64.DEFAULT));

        String[] listItem = getResources().getStringArray(R.array.main_list_names);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItem);
        mainList.setAdapter(adapter);
        mainList.setOnItemClickListener(this);

        chinachu = new Chinachu4j(chinachuAddress, username, password);

        appClass.setChinachu(chinachu);

        appClass.setStreaming(pref.getBoolean("streaming", false));
        appClass.setEncStreaming(pref.getBoolean("encStreaming", false));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id){
        if(position == 0)
            startActivity(new Intent(this, ChannelScheduleActivity.class));
        else if(position == 1)
            startActivity(new Intent(this, RuleActivity.class));
        else if(position > 1){
            Intent i = new Intent(this, ProgramActivity.class);
            i.putExtra("type", position);
            startActivity(i);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuItem changeServer = menu.add(0, Menu.FIRST, Menu.NONE, R.string.change_server);
        changeServer.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        MenuItem item = menu.add(0, Menu.FIRST + 1, Menu.NONE, R.string.settings);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(item.getItemId() == Menu.FIRST){
            final DBUtils dbUtils = new DBUtils(this);
            final ArrayList<String> address = new ArrayList<String>();
            Server[] servers = dbUtils.getServers();
            for(Server s : servers)
                address.add(s.getChinachuAddress());

            int settingNow = address.indexOf(pref.getString("chinachuAddress", ""));
            new AlertDialog.Builder(this).setTitle(R.string.select_server)
                    .setSingleChoiceItems((String[])address.toArray(new String[0]), settingNow, new OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int which){
                            String selectedAddress = address.get(which);
                            Server server = dbUtils.getServerFromAddress(selectedAddress);
                            dbUtils.serverPutPref(MainActivity.this, server);

                            chinachu = new Chinachu4j(server.getChinachuAddress(),
                                    new String(Base64.decode(server.getUsername(), Base64.DEFAULT)),
                                    new String(Base64.decode(server.getPassword(), Base64.DEFAULT)));
                            appClass.setChinachu(chinachu);
                            appClass.setStreaming(server.getStreaming());
                            appClass.setStreaming(server.getEncStreaming());

                            dbUtils.close();
                            dialog.dismiss();
                        }
                    }).setPositiveButton(R.string.cancel, null).show();
        }else if(item.getItemId() == Menu.FIRST + 1){
            startActivity(new Intent(this, Preference.class));
        }
        return super.onOptionsItemSelected(item);
    }
}