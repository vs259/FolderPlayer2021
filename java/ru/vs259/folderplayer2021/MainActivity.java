package ru.vs259.folderplayer2021;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
//import androidx.annotation.*;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.TextView;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener,
        MediaController.MediaPlayerControl, View.OnTouchListener
{

    private static final int ID_ABOUT = 0;
    private TextView mTextView1;
    private TextView mTextView3;
    private ImageButton varPrevBtn;
    private ImageButton varNextBtn;
    private View theView;
    private MediaPlayer mp = new MediaPlayer();
    private MediaController mediaController;
    private int currentPosition = 0;
//    public static String START_DIR = "/mnt/sdcard";  /storage/090D-5F26    /storage/emulated/0
    public static String START_DIR = "";
    public static String MAIN_DIR = "/storage/emulated/0";
    private List<String> songs = new ArrayList<String>();
    final String ParentString = "..";

    private static final int MY_REQUEST_CODE_PERMISSION = 1000;

    private Handler handler = new Handler();

    private int oldPosition = 0;
    private int newPosition = 0;


    ActivityResultLauncher<Intent> mStartForSettingsResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == Activity.RESULT_OK){
                    SharedPreferences settings= PreferenceManager.getDefaultSharedPreferences(this);
                    MAIN_DIR = settings.getString("main_dir", MAIN_DIR);
                    START_DIR = settings.getString("start_dir", MAIN_DIR);
                    String MyDir = settings.getString("MyDir", MAIN_DIR);
                    mTextView1.setText(MyDir);
                    updateSongList(START_DIR);
                }
            });

    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {

                File f = null;
                if(result.getResultCode() == Activity.RESULT_OK){
                    Intent data = result.getData();
                        if (data.getStringExtra("text").equals(ParentString))          // ???????? ???????????? "???? ?????????????? ??????????"
                        {
                            f = new File(START_DIR);
                            START_DIR=f.getParent();
                            mTextView3.setText(R.string.upSelected);
                        }
                        else                                                           // ???????? ???????????? ???????? ?????? ??????????
                        {
                            f = new File(START_DIR+"/"+data.getStringExtra("text"));

                            if (f.isDirectory())                                      // ?????????????? ??????????
                            {
                                START_DIR = START_DIR+"/"+data.getStringExtra("text");

                                // ???????????????? ?????????????? - ??????????????????
                                updateSongList(START_DIR);

                                // ?????????? ?????????????????????????? ?????????????????? ?????? ?????? ????????????
                                if (songs.size()>0)
                                {
                                    playSong(START_DIR + "/"+songs.get(currentPosition), songs.get(currentPosition),0);
                                }
                                else
                                {
                                    mTextView3.setText(R.string.textNoFiles);
                                }
                            }
                            else                                                    // ???????????? ????????
                            {
                                // ???????????????? ?????????????? - ??????????????????
                                updateSongList(START_DIR);
                                if (songs.size()>0)
                                    playSong(START_DIR + "/"+songs.get(data.getIntExtra("position", 0)-1), songs.get(data.getIntExtra("position", 0)-1),0);
                            }
                        }

                    Visualisation(START_DIR);
                }
                else{
                    Visualisation("???????????? ??????????????");
                }
            });


    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ???????????????? ?????????????? ?????????????????????? ????????????????????
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Level 23

            // Check if we have Call permission
            int permisson = ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE);

            if (permisson != PackageManager.PERMISSION_GRANTED) {

                // If don't have permission so prompt the user.
                this.requestPermissions(
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_REQUEST_CODE_PERMISSION
                );
            }
        }


        // ?????????????????? ???????????????????? ???? ??????????????
        mTextView3 = (TextView)findViewById(R.id.textView3);
        mTextView1 = (TextView)findViewById(R.id.textView1);
        varPrevBtn = (ImageButton) findViewById(R.id.prevBtn);
        varNextBtn = (ImageButton) findViewById(R.id.nextBtn);

        theView = this.findViewById(R.id.theview);
        theView.setOnTouchListener((View.OnTouchListener) this);

        mp.setOnPreparedListener((MediaPlayer.OnPreparedListener) this);
        mediaController = new MediaController(this);

        TelephonyManager teleMngr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        teleMngr.listen(new MyPhoneStateListener(), PhoneStateListener.LISTEN_CALL_STATE);

        // ?????????????????? ???????? ?? ?????????????? ???????? ??????????

        String EXT_CARD_DIR = "/storage/emulated/0";
        try {
            StorageManager storageManager = (StorageManager) this.getSystemService(Context.STORAGE_SERVICE);
            Method method = storageManager.getClass().getMethod("getVolumeList");
            StorageVolume[] storageVolumes = (StorageVolume[]) method.invoke(storageManager);
            if (storageVolumes != null && storageVolumes.length > 0) {

                for (StorageVolume volume : storageVolumes) {
                    if(volume.isRemovable() && !volume.isEmulated() && !volume.isPrimary()) {

                        // ???????????? ???????????? API
                        if (Build.VERSION.SDK_INT <= 30) {
                            String str = volume.toString();
                            str = "/storage/" + str.substring (str.indexOf("(")+1, str.indexOf(")"));
                            EXT_CARD_DIR = str;
                        }else
                            EXT_CARD_DIR = volume.getDirectory().toString();
                    }
                }


            }

        } catch (Exception e) {
            mTextView1.setText(e.getMessage());
        }

        // ???????????????????? ?????????????????????? ????????????????
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        MAIN_DIR = settings.getString("main_dir",EXT_CARD_DIR);
        START_DIR = settings.getString("start_dir",EXT_CARD_DIR);
        String MyDir = settings.getString("MyDir",EXT_CARD_DIR);
        String PlayedFile = settings.getString("PlayedFile"," ");
        currentPosition = settings.getInt("FilePos", 0);
        final int PlayBackPos = settings.getInt("PlayBackPos", 0);



        // ?????????????????????????????? ?? ??????????????????, ???????? ?????????????? ???? ????????????
        File file = new File(MyDir);
        if (!file.isDirectory()) {
            settings();
            return;
        }

        // ?????????????????? ?????????????????? ????????????????
        if (!(MyDir.equals(" ")))
            START_DIR = MyDir;

        if (!(PlayedFile.equals(" ")))
        {
            // ?????????????? ?????????????? (??????????????????)
            songs = new ArrayList<String>();

            // ???????????????? ?????????????? - ??????????????????
            updateSongList(START_DIR);

            if (songs.size()>0)
            {
                mTextView3.setText(START_DIR + "/"+songs.get(currentPosition));
                playSong(START_DIR + "/"+songs.get(currentPosition), songs.get(currentPosition), PlayBackPos);
            }
            else
            {
                mTextView3.setText(R.string.textNoFiles);
            }
        };


        // ????????????????????????
        Visualisation(START_DIR);


        // ?????????????????????? ??????????????, ???????????????????? ???? ???????????????? ?????????????????????????????????? ????????????????????
        // ???????????? ???????????????????? ????????
        varPrevBtn.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                if (mp.isPlaying())
                    mp.stop();

                decCurrentPos();

            }
        });

        // ???????????? ?????????????????? ????????
        varNextBtn.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                if (mp.isPlaying())
                    mp.stop();

                incCurrentPos();

            }
        });
        // ?????????????????? ?????????????????? ???????????????? ??????????

    }

    public void onMyButtonClick(View view)
    {
        // ???????????????????? ??????????
        if (mp.isPlaying())
            mp.pause();

        startMyListActivity();
    }


    // ???????????? ?????????????????????????????? ??????????
    private void startMyListActivity()
    {
        // ?????????????? ?????????????? (??????????????????)
        songs = new ArrayList<String>();

        // ?????? ???? ???? ???????? ??????????????, ???????????? ?? ?????????????? ??????????????
        currentPosition=0;

        // ?????????? ?????????????????????????????? ??????????
        Intent intent = new Intent(this, FolderListClass.class);
        intent.putExtra("StartDir", START_DIR);
        if(START_DIR.equals(MAIN_DIR))
            intent.putExtra("isParentLevel", false);
        else
            intent.putExtra("isParentLevel", true);
        intent.putExtra("ParentString", ParentString);

        mStartForResult.launch(intent);

    }

    // ???????????????????? ?????????????? ?????????????? ???????????????? ???????????? ?? ??????????????
    private void incCurrentPos()
    {
        if (songs.size()>0)
        {
            if (currentPosition<songs.size()-1)
                currentPosition=currentPosition+1;

            mTextView3.setText(songs.get(currentPosition));
            playSong(START_DIR + "/"+songs.get(currentPosition), songs.get(currentPosition), 0);
        }
    }


    // ???????????????????? ?????????????? ?????????????? ???????????????? ???????????? ?? ??????????????
    private void decCurrentPos()
    {
        if (songs.size()>0)
        {
            if (currentPosition>0)
                currentPosition=currentPosition-1;

            mTextView3.setText(songs.get(currentPosition));
            playSong(START_DIR + "/"+songs.get(currentPosition), songs.get(currentPosition), 0);
        }
    }

    // ???????????????? ???????????? ??????????????????????????????
    public void updateSongList(String _dir)
    {

        File home = new File(_dir);
        Mp3Filter filter = new Mp3Filter();

        if (home.list(filter).length > 0)
        {
            String[] files = home.list(filter);   // NB !!! ?????? ????????????, ?? ???? ????????????
            Arrays.sort(files);

            for (String file : files)
            {
                songs.add(file);
            }
        }
    }

    // ?????????????????????? ???????????? ???? ?????????????? onPrepared
    @Override
    public void onPrepared(MediaPlayer mp) {
        mediaController.setMediaPlayer(this);
        mediaController.setAnchorView(findViewById(R.id.main_audio_view));

        handler.post(new Runnable()
        {
            public void run()
            {
                mediaController.setEnabled(true);
                mediaController.show();
            }
        });
    }

    @Override
    public void start() {
        mp.start();
    }

    @Override
    public void pause() {
        Saving(mp.getCurrentPosition());
        mp.pause();
    }

    @Override
    public int getDuration() {
        return mp.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mp.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        mp.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return mp.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    // ???????????????????? ???????????????????????? ?????????? ?????????????? ????????????????????????
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // ???????????? ?????????? ?????????????????????? ???????????? ?????????? 3 ??????????????
        // ?????????????????? ????????????, ?????????? ?????????????? ???? ??????????
        mediaController.show();
        return false;
    }


    @Override
    // ?????????????????????? ?????????????????? ?????? ???????????????? ?????????????? ???? ????????????
    public boolean onTouch(View v, MotionEvent me)
    {

        // ???????????????? ?????????????? ?????????????? ???????????? ?? ??????????, ???????? ????????????, ???? currentPosition+1, ???????? ????????????, ???? -1
        int Action=me.getAction();

        if (mp.isPlaying()) mp.pause();

        switch(Action)
        {
            case MotionEvent.ACTION_DOWN: oldPosition=(int) me.getX();break;
            case MotionEvent.ACTION_UP: newPosition=(int) me.getX();break;
        };

        if (newPosition>=oldPosition)
        {
            incCurrentPos();
        }
        else
        {
            decCurrentPos();
        }

        return true;
    }


    // ???????????????? ?????????????? ???? ?????????????????????? ????????????
    class Mp3Filter implements FilenameFilter
    {
        public boolean accept(File dir, @NonNull String name)
        {
            return (name.endsWith("mp3"));
        }
    }

    // ???????????????????????? ???????????? ????????????????????
    protected void Visualisation(String text1)
    {
        mTextView1.setText(text1);
    }

    // ?????????????????? ?????????????? ??????????????????????????
    private void playSong(String songPath, String songName,int _PlayBackPos)
    {
        try
        {
            // ???????????????????????? - ???????????????????? ?? ?????????????????????????? ??????????
            mTextView3.setText(songName+"      ("+(currentPosition+1)+"/"+songs.size()+")");

            // ?????????? ?? ???????????????????? ??????????????????????????
            mp.reset();

            mp.setDataSource(songPath);
            mp.prepare();
            mp.seekTo(_PlayBackPos);
            mp.start();


            // ?????????????????? ?????????????? ???? ?????????????????? ????????????????????????
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
            {
                public void onCompletion(MediaPlayer arg0)
                {
                    if (++currentPosition >= songs.size())
                    {
                        // ?????? ???????????????????? ?????????? ????????????, ?????????????????? ?????????????????????????????? ?? ???????????? ?? ?????????????????????????? ??????????????????????????????
                        currentPosition = 0;
                        arg0.stop();
                    }
                    else
                    {
                        // ???????????????????????? ?????????????????? ??????????????
                        playSong(START_DIR + "/"+ songs.get(currentPosition), songs.get(currentPosition),0);
                    }
                }
            });

        }
        catch (IOException e)
        {
//            Log.v(getString(R.string.app_name), e.getMessage());
//            System.out.println("error = "+e.getMessage());
//            mTextView3.setText("error = "+e.getMessage());
            mTextView3.setText("?????? - ??????????, ?? ???? ????????");
        }
    }


    // ???????????????????? ????????????????
    @Override
    protected void onDestroy()
    {

        int PlayBackPos;

        // ?????????????????????????? ?????????? ??????????
        if (mp.isPlaying())
        {
            mp.stop();
        };

        PlayBackPos=mp.getCurrentPosition();

        // ?????????????????? ???????????? ?????? ???????????????? ?????????????? ???? ?? ????????
        Saving(PlayBackPos);

        // ?????????????????????? ???? ??????????????????????
        mp.release();
        super.onDestroy();

    }

    // ???????????????????? ????????????
    private void Saving(int _PlayBackPos)
    {
        // ???????????????? ???????????????????? ?? ?????????????? ??????????????
        String MyDir;
        MyDir = mTextView1.getText().toString().trim();
        if(!MyDir.startsWith("/")) MyDir = START_DIR;
        String PlayedFile = mTextView3.getText().toString().trim();


        // ?????????????????? ???????????????? ???????????????? ?? ?????????????????? ?????????? ????????????????
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        settings.edit().clear().commit();          // ???????????? ??????????????
        SharedPreferences.Editor editor = settings.edit();

        editor.putString("MyDir", MyDir);
        editor.putString("start_dir", START_DIR);
        editor.putString("main_dir", MAIN_DIR);
        editor.putString("PlayedFile", PlayedFile);
        editor.putInt("FilePos", currentPosition);
        editor.putInt("PlayBackPos", _PlayBackPos);

        // ?????????????????? ????????????. ???????? ???? ?????????????????? - ???????????? ???? ????????????????????
        editor.commit();
    }

    // ???????????????? ????????
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.icon_menu, menu);
        return true;
    }

    // ?????????????????? ???????????? ???????????? ????????
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.quit:
                quit();
                return true;
            case R.id.settings:
                settings();
                return true;
            case R.id.about:
                about();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // ???????????????????? ???????????? ???????? "??????????????????"
    private void settings(){

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("main_dir", MAIN_DIR);
        editor.commit();

        // ?????????? ?????????? ????????????????
        Intent intent = new Intent(this, SettingsActivity.class);
        mStartForSettingsResult.launch(intent);
    }

    // ?????????????????? ???????????? ???????? ??????????
    private void quit()
    {
        finish();
    }

    // ?????????????????? ???????????? ???????? ?? ??????????????????
    private void about()
    {
        // ???????????????? ????????????
        showDialog(ID_ABOUT);
    }

    // ???????????????? ???????? ?????????????? ?????? ???????????? ?????????????????? ??????????????????
    @Override
    protected Dialog onCreateDialog(int id)
    {
        switch (id)
        {
            case ID_ABOUT:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.copiRight);

                // ?????????????? ???????????? "Yes" ?? ???????????????????? ??????????????
                builder.setPositiveButton(R.string.OKBtn, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        // ???????????? ???? ????????????
                    }
                });

                builder.setCancelable(false);
                return builder.create();
            default:
        }
        return null;
    }

    // ??????????, ?????????????????????????? ?????????????????? ????????????????
    class MyPhoneStateListener extends PhoneStateListener
    {
        @Override
        public void onCallStateChanged(int state, String incomingNumber)
        {
            super.onCallStateChanged(state, incomingNumber);

            switch (state)
            {
                case TelephonyManager.CALL_STATE_IDLE:
                    mp.start();
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    mp.pause();
                    Saving(mp.getCurrentPosition());
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    break;
                default:
                    break;
            }
        }
    }

}