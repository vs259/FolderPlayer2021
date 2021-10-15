package ru.vs259.folderplayer2021;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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
import java.util.ArrayList;
import java.util.Arrays;
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
    public static String START_DIR = "/storage/emulated/0";
    public static String MAIN_DIR = "/storage/emulated/0";
    private List<String> songs = new ArrayList<String>();
    final String ParentString = "..";

    private static final int MY_REQUEST_CODE_PERMISSION = 1000;

    private Handler handler = new Handler();

    private int oldPosition = 0;
    private int newPosition = 0;


    ActivityResultLauncher<Intent> mStartForSettingsResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
System.out.println(result.getResultCode());
            });

    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {

                File f = null;
                if(result.getResultCode() == Activity.RESULT_OK){
                    Intent data = result.getData();
                        if (data.getStringExtra("text").equals(ParentString))          // Если выбран "На уровень вверх"
                        {
                            f = new File(START_DIR);
                            START_DIR=f.getParent();
                            mTextView3.setText(R.string.upSelected);
                        }
                        else                                                           // Если выбран файл или папка
                        {
                            f = new File(START_DIR+"/"+data.getStringExtra("text"));

                            // Создание массива - плэйлиста
                            updateSongList(START_DIR);
                            if (f.isDirectory())                                      // Выбрана папка
                            {
                                START_DIR = START_DIR+"/"+data.getStringExtra("text");

                                // Старт проигрывателя перенесен под эту кнопку
                                if (songs.size()>0)
                                {
                                    playSong(START_DIR + "/"+songs.get(currentPosition), songs.get(currentPosition),0);
                                }
                                else
                                {
                                    mTextView3.setText(R.string.textNoFiles);
                                }
                            }
                            else                                                    // Выбран файл
                            {
                                if (songs.size()>0)
                                    playSong(START_DIR + "/"+songs.get(data.getIntExtra("position", 0)-1), songs.get(data.getIntExtra("position", 0)-1),0);
                            }
                        }

                    Visualisation(START_DIR);
                }
                else{
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
            }
        }

        // Получение указателей на объекты
        mTextView3= (TextView)findViewById(R.id.textView3);
        mTextView1= (TextView)findViewById(R.id.textView1);
        varPrevBtn = (ImageButton) findViewById(R.id.prevBtn);
        varNextBtn = (ImageButton) findViewById(R.id.nextBtn);

        theView = this.findViewById(R.id.theview);
        theView.setOnTouchListener((View.OnTouchListener) this);

        mp.setOnPreparedListener((MediaPlayer.OnPreparedListener) this);
        mediaController = new MediaController(this);

        TelephonyManager teleMngr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        teleMngr.listen(new MyPhoneStateListener(), PhoneStateListener.LISTEN_CALL_STATE);

        // Считывание сохраненных значений
//        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        START_DIR = settings.getString("start_dir","/storage/emulated/0");
        MAIN_DIR = settings.getString("main_dir","/storage/emulated/0");
        String MyDir = settings.getString("MyDir"," ");
        String PlayedFile = settings.getString("PlayedFile"," ");
        currentPosition = settings.getInt("FilePos", 0);
        final int PlayBackPos = settings.getInt("PlayBackPos", 0);

        // Перенаправление в настройки, если каталог не найден
        File file = new File(MyDir);
        if (!file.isDirectory()) {
            settings();
            return;
        }

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
                playSong(START_DIR + "/"+songs.get(currentPosition), songs.get(currentPosition), PlayBackPos);
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
        if(START_DIR.equals(MAIN_DIR))
            intent.putExtra("isParentLevel", false);
        else
            intent.putExtra("isParentLevel", true);
        intent.putExtra("ParentString", ParentString);

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
            playSong(START_DIR + "/"+songs.get(currentPosition), songs.get(currentPosition), 0);
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
            playSong(START_DIR + "/"+songs.get(currentPosition), songs.get(currentPosition), 0);
        }
    }

    // Создание списка воспроизведения
    public void updateSongList(String _dir)
    {

        File home = new File(_dir);
        Mp3Filter filter = new Mp3Filter();

        if (home.list(filter).length > 0)
        {
            String[] files = home.list(filter);   // NB !!! Это массив, а не список
            Arrays.sort(files);

            for (String file : files)
            {
                songs.add(file);
            }
        }
    }

    // Организация потока по событию onPrepared
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

    // Управление медиаплеером путем касания прогрессбара
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // Панель медиа контроллера гаснет через 3 секунды
        // Коснитесь экрана, чтобы вызвать ее вновь
        mediaController.show();
        return false;
    }


    @Override
    // Организация перемотки при движении пальцем по экрану
    public boolean onTouch(View v, MotionEvent me)
    {

        // Сравнить текущую позицию плеера с новой, если больше, то currentPosition+1, если меньше, то -1
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


    // Создание фильтра по расширениям файлов
    class Mp3Filter implements FilenameFilter
    {
        public boolean accept(File dir, @NonNull String name)
        {
            return (name.endsWith("mp3"));
        }
    }

    // Визуализация выбора директории
    protected void Visualisation(String text1)
    {
        mTextView1.setText(text1);
    }

    // Процедура запуска проигрывателя
    private void playSong(String songPath, String songName,int _PlayBackPos)
    {
        try
        {
            // Визуализация - информация о проигрываемом файле
            mTextView3.setText(songName+"      ("+(currentPosition+1)+"/"+songs.size()+")");

            // Сброс и подготовка проигрывателя
            mp.reset();

            mp.setDataSource(songPath);
            mp.prepare();
            mp.seekTo(_PlayBackPos);
            mp.start();


            // Установка реакции на окончание проигрывания
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
            {
                public void onCompletion(MediaPlayer arg0)
                {
                    if (++currentPosition >= songs.size())
                    {
                        // При достижении конца списка, указатель устанавливается в начало и проигрыватель останавливается
                        currentPosition = 0;
                        arg0.stop();
                    }
                    else
                    {
                        // Проигрывание следующей мелодии
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
            mTextView3.setText("Это - папка, а не файл");
        }
    }


    // Деструктор активити
    @Override
    protected void onDestroy()
    {

        int PlayBackPos;

        // Останавливаем медиа плеер
        if (mp.isPlaying())
        {
            mp.stop();
        };

        PlayBackPos=mp.getCurrentPosition();

        // Сохраняем данные для будущего запуска не с нуля
        Saving(PlayBackPos);

        // Избавляемся от медиаплеера
        mp.release();

//        stopServ();

        super.onDestroy();

    }

/*
    // По возврату из формы настроек
    protected void onResume() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
//        START_DIR = settings.getString("start_dir","/storage/emulated/0");
//        MAIN_DIR = settings.getString("main_dir","/storage/emulated/0");
        String MyDir = settings.getString("MyDir","/storage/emulated/0");
//        currentPosition = settings.getInt("FilePos", 0);
//        mTextView1.setText(START_DIR);


        super.onResume();
//        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

System.out.println("OK "+ MyDir);
    }

 */
/*
    SharedPreferences.OnSharedPreferenceChangeListener prefListener =
            new SharedPreferences.OnSharedPreferenceChangeListener()
            {
                public void onSharedPreferenceChanged(SharedPreferences prefs,String key)
                {
                    if(key.equals("MyDir"))
                    {
System.out.println("OK");
                    }
                }
            };
*/
    // Сохранение данных
    private void Saving(int _PlayBackPos)
    {
        // Получаем содержимое и удаляем пробелы
        String MyDir = mTextView1.getText().toString().trim();
        String PlayedFile = mTextView3.getText().toString().trim();


        // Загружаем редактор настроек и вписываем новые значения
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        settings.edit().clear().commit();          // Полная очистка
        SharedPreferences.Editor editor = settings.edit();

        editor.putString("MyDir", MyDir);
        editor.putString("start_dir", START_DIR);
        editor.putString("main_dir", MAIN_DIR);
        editor.putString("PlayedFile", PlayedFile);
        editor.putInt("FilePos", currentPosition);
        editor.putInt("PlayBackPos", _PlayBackPos);

        // Сохраняем данные. Если не выполнить - ничего не сохранится
        editor.commit();
    }

    // Создание меню
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.icon_menu, menu);
        return true;
    }

    // Обработка выбора пункта меню
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

    // Обработчик пункта меню "Настройки"
    private void settings(){

        // Вызов формы настроек
        Intent intent = new Intent(this, SettingsActivity.class);
        mStartForSettingsResult.launch(intent);

    }

    // Обработка пункта меню Выход
    private void quit()
    {
        finish();
    }

    // Обработка пункта меню О программе
    private void about()
    {
        // вызываем диалог
        showDialog(ID_ABOUT);
    }

    // Создание окна диалога для вывода различных сообщений
    @Override
    protected Dialog onCreateDialog(int id)
    {
        switch (id)
        {
            case ID_ABOUT:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.copiRight);

                // создаем кнопку "Yes" и обработчик события
                builder.setPositiveButton(R.string.OKBtn, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        // Ничего не делаем
                    }
                });

                builder.setCancelable(false);
                return builder.create();
            default:
        }
        return null;
    }

    // Класс, отслеживающий состояние телефона
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

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    private String getSDcardPath() {
        return Environment.getExternalStorageDirectory().getPath();
    }
}