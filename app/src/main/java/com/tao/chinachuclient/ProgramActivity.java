package com.tao.chinachuclient;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;

import Chinachu4j.ChinachuResponse;
import Chinachu4j.Program;
import Chinachu4j.Recorded;
import Chinachu4j.Reserve;

public class ProgramActivity extends AppCompatActivity implements OnRefreshListener{

    private SwipeRefreshLayout swipeRefresh;
    private ProgramListAdapter programListAdapter;
    private ApplicationClass appClass;
    private ActionBar actionbar;

    private int type;
    private String query;
    private SearchView searchView;
    private Object[] programList;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_program);

        ListView list = (ListView)findViewById(R.id.programList);
        swipeRefresh = (SwipeRefreshLayout)findViewById(R.id.swipe_refresh);

        actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);

        appClass = (ApplicationClass)getApplicationContext();
        // type 1: ルール 2: 予約済み 3: 録画中 4: 録画済み 5: 番組検索
        Intent intent = getIntent();
        type = intent.getIntExtra("type", -1);
        if(type == -1)
            finish();
        else if(type == Type.SEARCH_PROGRAM)
            query = intent.getStringExtra("query");

        programListAdapter = new ProgramListAdapter(this, type);
        list.setAdapter(programListAdapter);

        list.setOnItemClickListener(new ProgramListClickListener(this, type));
        swipeRefresh.setColorSchemeColors(Color.parseColor("#2196F3"));
        swipeRefresh.setOnRefreshListener(this);

        setActionBarTitle(-1);

        asyncLoad(false);
    }

    public void setActionBarTitle(int programCount){
        int titleRes = -1;
        switch(type){
            case Type.RESERVES:
                titleRes = R.string.reserved;
                break;
            case Type.RECORDING:
                titleRes = R.string.recording;
                break;
            case Type.RECORDED:
                titleRes = R.string.recorded;
                break;
            case Type.SEARCH_PROGRAM:
                titleRes = R.string.search_result;
                break;
            default:
                finish();
                return;
        }
        String title = getString(titleRes);
        if(programCount >= 0){
            title += " (" + programCount + ")";
        }
        actionbar.setTitle(title);
    }

    public void asyncLoad(final boolean isRefresh){
        programListAdapter.clear();
        new AsyncTask<Void, Void, Object[]>(){
            private ProgressDialog progDialog;

            @Override
            protected void onPreExecute(){
                if(!isRefresh){
                    progDialog = new ProgressDialog(ProgramActivity.this);
                    progDialog.setMessage(getString(R.string.loading));
                    progDialog.setIndeterminate(false);
                    progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    progDialog.setCancelable(true);
                    progDialog.show();
                }
            }

            @Override
            protected Object[] doInBackground(Void... params){
                return load();
            }

            @Override
            protected void onPostExecute(Object[] result){
                if(!isRefresh)
                    progDialog.dismiss();
                else
                    swipeRefresh.setRefreshing(false);
                if(result == null){
                    Toast.makeText(ProgramActivity.this, R.string.error_get_schedule, Toast.LENGTH_SHORT).show();
                    return;
                }
                programList = result;
                programListAdapter.addAll(result);
                setActionBarTitle(result.length);
            }
        }.execute();
    }

    public Object[] load(){
        try{
            if(type == Type.RESERVES){
                return appClass.getChinachu().getReserves();
            }else if(type == Type.RECORDING){
                return appClass.getChinachu().getRecording();
            }else if(type == Type.RECORDED){
                Recorded[] recorded = appClass.getChinachu().getRecorded();
                Recorded[] result = new Recorded[recorded.length];
                for(int i = 0; i < recorded.length; i++)
                    result[recorded.length - i - 1] = recorded[i];
                return result;
            }else if(type == Type.SEARCH_PROGRAM){
                return appClass.getChinachu().searchProgram(query);
            }
            return null;
        }catch(KeyManagementException | NoSuchAlgorithmException | IOException | JSONException e){
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        if(type == Type.RECORDED)
            menu.add(0, Menu.FIRST, Menu.NONE, R.string.cleanup).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        if(type == Type.SEARCH_PROGRAM)
            return true;

        getMenuInflater().inflate(R.menu.search, menu);
        searchView = (SearchView)menu.findItem(R.id.search_view).getActionView();
        searchView.setQueryHint(getString(R.string.search_of_list));
        searchView.setOnQueryTextListener(new OnQueryTextListener(){

            @Override
            public boolean onQueryTextSubmit(String query){
                return onQueryTextChange(query);
            }

            @Override
            public boolean onQueryTextChange(String newText){
                programListAdapter.clear();
                if(newText.isEmpty()){
                    programListAdapter.addAll(programList);
                    return false;
                }

                ArrayList<Object> resultPrograms = new ArrayList<Object>();
                for(int i = 0; i < programList.length; i++){
                    Program item;
                    if(type == Type.RESERVES)
                        item = ((Reserve)programList[i]).getProgram();
                    else if(type == Type.RECORDED)
                        item = ((Recorded)programList[i]).getProgram();
                    else
                        item = (Program)programList[i];
                    String itemTitle = Normalizer.normalize(item.getFullTitle(), Normalizer.Form.NFKC).toLowerCase(Locale.getDefault());
                    String searchText = newText.toLowerCase(Locale.getDefault());
                    if(itemTitle.contains(searchText))
                        resultPrograms.add(programList[i]);
                }
                programListAdapter.addAll(resultPrograms);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(item.getItemId() == android.R.id.home){
            finish();
            return true;
        }
        if(item.getItemId() == Menu.FIRST){
            new AlertDialog.Builder(this)
                    .setTitle(R.string.cleanup)
                    .setMessage(R.string.is_cleanup_of_recorded_list)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.ok, new OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int which){
                            new AsyncTask<Void, Void, ChinachuResponse>(){
                                private ProgressDialog progDialog;

                                @Override
                                protected void onPreExecute(){
                                    progDialog = new ProgressDialog(ProgramActivity.this);
                                    progDialog.setMessage(getString(R.string.loading));
                                    progDialog.setIndeterminate(false);
                                    progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                    progDialog.setCancelable(true);
                                    progDialog.show();
                                }

                                @Override
                                protected ChinachuResponse doInBackground(Void... params){
                                    try{
                                        return appClass.getChinachu().recordedCleanUp();
                                    }catch(KeyManagementException | NoSuchAlgorithmException | IOException e){
                                        return null;
                                    }
                                }

                                @Override
                                protected void onPostExecute(ChinachuResponse result){
                                    progDialog.dismiss();
                                    if(result == null){
                                        Toast.makeText(ProgramActivity.this, R.string.error_access, Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    if(!result.getResult()){
                                        Toast.makeText(ProgramActivity.this, result.getMessage(), Toast.LENGTH_LONG).show();
                                        return;
                                    }

                                    new AlertDialog.Builder(ProgramActivity.this)
                                            .setTitle(R.string.done)
                                            .setMessage(R.string.cleanup_done_is_refresh)
                                            .setNegativeButton(R.string.cancel, null)
                                            .setPositiveButton(R.string.ok, new OnClickListener(){

                                                @Override
                                                public void onClick(DialogInterface dialog, int which){
                                                    onRefresh();
                                                }

                                            }).show();
                                }
                            }.execute();
                        }
                    }).show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh(){
        asyncLoad(true);
    }

    @Override
    public void onResume(){
        super.onResume();
        if(appClass.getReloadList()){
            asyncLoad(true);
            appClass.setReloadList(false);
        }
    }

    @Override
    public void onBackPressed(){
        if(searchView == null){
            super.onBackPressed();
            return;
        }
        if(!searchView.isIconified()){
            searchView.setIconified(true);
        }else{
            super.onBackPressed();
        }
    }
}