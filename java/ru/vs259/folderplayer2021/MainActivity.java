package ru.vs259.folderplayer2021;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private TextView mTextView1;
    private Button varSelectFolderBtn;
    private TextView mTextView3;
    private ImageButton varPrevBtn;
    private ImageButton varNextBtn;
    private View theView;
    private MediaPlayer mp = new MediaPlayer();
    private MediaController mediaController;
    public static UUID MY_UUID;
    public static final String PREFS_NAME = "MyPrefsFile";            // Файл для хранения настроек
    private int currentPosition = 0;
//    public static String START_DIR = "/mnt/sdcard";
    public static String START_DIR = "/sdcard";
    public static String MAIN_DIR = "/sdcard";
    private List<String> songs = new ArrayList<String>();
//    private static final int REQUEST_SELECT_DIR=99;
    final String ParentString = "..";

    private static final int MY_REQUEST_CODE_PERMISSION = 1000;


    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {

                File f = null;
                if(result.getResultCode() == Activity.RESULT_OK){
                    Intent data = result.getData();
//                    String selectedDir = intent.getStringExtra("text");
//                    textView.setText(selectedDir);
//                    Visualisation(intent.getStringExtra("text"));
//                    if (resultCode == RESULT_OK)
//                    {
                        if (data.getStringExtra("text").equals(ParentString))          // Если выбран "На уровень вверх"
                        {
                            f = new File(START_DIR);
                            START_DIR=f.getParent();
                            mTextView3.setText(R.string.upSelected);
                            if (!START_DIR.equals("/"))
                                startMyListActivity();
                            else
                                START_DIR = MAIN_DIR;
                        }
                        else                                                           // Если выбран файл или папка
                        {
                            f = new File(START_DIR+"/"+data.getStringExtra("text"));
                            if (f.isDirectory())                                      // Выбрана папка
                            {
                                START_DIR = START_DIR+"/"+data.getStringExtra("text");

                                // Старт проигрывателя перенесен под эту кнопку
                                // Создание массива - плэйлиста
                                updateSongList(START_DIR);

                                if (songs.size()>0)
                                {
//                                    playSong(START_DIR + "/"+songs.get(currentPosition), songs.get(currentPosition),0);
                                }
                                else
                                {
                                    mTextView3.setText(R.string.textNoFiles);
                                }
                            }
                            else                                                    // Выбран файл
                            {
//                                startMyListActivity();
                            }
                        }
//                    }
//                    else
//                    {
//                        mTextView3.setText();
//                    }

                    Visualisation(START_DIR);
                }
                else{
//                    textView.setText("Ошибка доступа");

                    Visualisation("Ошибка доступа");
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Проверка наличия необходимых разрешений
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) { // Level 23

            // Check if we have Call permission
            int permisson = ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE);

            if (permisson != PackageManager.PERMISSION_GRANTED) {
                // If don't have permission so prompt the user.
                this.requestPermissions(
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_REQUEST_CODE_PERMISSION
                );
//                return;
            }
        }

        // Получение указателей на объекты
        mTextView3= (TextView)findViewById(R.id.textView3);
        mTextView1= (TextView)findViewById(R.id.textView1);
        varSelectFolderBtn = (Button) findViewById(R.id.SelectFolder);
        varPrevBtn = (ImageButton) findViewById(R.id.prevBtn);
        varNextBtn = (ImageButton) findViewById(R.id.nextBtn);

        theView = this.findViewById(R.id.theview);
//        theView.setOnTouchListener((View.OnTouchListener) this);

//        mp.setOnPreparedListener((MediaPlayer.OnPreparedListener) this);
        mediaController = new MediaController(this);

        TelephonyManager teleMngr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
//        teleMngr.listen(new MyPhoneStateListener(), PhoneStateListener.LISTEN_CALL_STATE);
//        MY_UUID = UUID.fromString(teleMngr.getDeviceId());
        MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

        // Считывание сохраненных значений
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String MyDir = settings.getString("MyDir"," ");
        String PlayedFile = settings.getString("PlayedFile"," ");
        currentPosition = settings.getInt("FilePos", 0);
        final int PlayBackPos = settings.getInt("PlayBackPos", 0);

        // Обработка считанных значений
        if (!(MyDir.equals(" ")))
            START_DIR = MyDir;

        if (!(PlayedFile.equals(" ")))
        {
            // Очистка массива (плэйлиста)
            songs = new ArrayList<String>();

            // Создание массива - плэйлиста
            updateSongList(START_DIR);

            if (songs.size()>0)
            {
                mTextView3.setText(START_DIR + "/"+songs.get(currentPosition));
//                playSong(START_DIR + "/"+songs.get(currentPosition), songs.get(currentPosition), PlayBackPos);
            }
            else
            {
                mTextView3.setText(R.string.textNoFiles);
            }
        };


        // Визуализация
        Visualisation(START_DIR);

        // Обработчики событий, получаемых от объектов пользовательского интерфейса
        // Кнопка ПРЕДЫДУЩИЙ ФАЙЛ
        varPrevBtn.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                if (mp.isPlaying())
                    mp.stop();

                decCurrentPos();

            }
        });

        // Кнопка СЛЕДУЮЩИЙ ФАЙЛ
        varNextBtn.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                if (mp.isPlaying())
                    mp.stop();

                incCurrentPos();

            }
        });

//        startServ();
        // Окончание процедуры создания формы

    }

    public void onMyButtonClick(View view)
    {
        // Остановить плеер
        if (mp.isPlaying())
            mp.pause();

        startMyListActivity();

    }


    // Запуск вспомогательной формы
    private void startMyListActivity()
    {
        // Очистка массива (плэйлиста)
        songs = new ArrayList<String>();

        // Что бы не было выбрано, начало в нулевой позиции
        currentPosition=0;

        // Вызов вспомогательной формы
        Intent intent = new Intent(this, FolderListClass.class);
        intent.putExtra("StartDir", START_DIR);

        mStartForResult.launch(intent);

    }

    // Увеличение текущей позиции счетчика треков в массиве
    private void incCurrentPos()
    {
        if (songs.size()>0)
        {
            if (currentPosition<songs.size()-1)
                currentPosition=currentPosition+1;

            mTextView3.setText(songs.get(currentPosition));
//            playSong(START_DIR + "/"+songs.get(currentPosition), songs.get(currentPosition), 0);
        }
    }


    // Уменьшение текущей позиции счетчика треков в массиве
    private void decCurrentPos()
    {
        if (songs.size()>0)
        {
            if (currentPosition>0)
                currentPosition=currentPosition-1;

            mTextView3.setText(songs.get(currentPosition));
//            playSong(START_DIR + "/"+songs.get(currentPosition), songs.get(currentPosition), 0);
        }
        System.out.println("currentPosition = " + currentPosition);
    }

    // Создание списка воспроизведения
    public void updateSongList(String _dir)
    {

        File home = new File(_dir);
        Mp3Filter filter = new Mp3Filter();

        if (home.list(filter).length > 0)
        {
            String[] songs = home.list(filter);
            Arrays.sort(songs);
        }
    }

    // Создание фильтра по расширениям файлов
    class Mp3Filter implements FilenameFilter
    {
        public boolean accept(File dir, @NonNull String name)
        {
            return (name.endsWith("mp3"));
        }
    }

    // Визуализация выбора в текстовых полях
    protected void Visualisation(String text1)
    {
        mTextView1.setText(text1);
    }

}