package ru.vs259.folderplayer2021;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;





import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;


//import android.util.Log;

public class FolderListClass extends ListActivity {

    private List<String> item = null;
    final String ParentString = "..";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        item = new ArrayList<String>();
        item.add(ParentString);

        // Обращение к области бандла для получения переданных аргументов
        Bundle b = this.getIntent().getExtras();
        String s = b.getString("StartDir");

        File[] dir = new File(s).listFiles();
        for(int i = 0; i < dir.length; i++)
        {
            //if(dir[i].isDirectory())
            item.add(dir[i].getName());

//       		Log.v("TAG", "Dirs & files "+item.get(i+1));

        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, item);
        setListAdapter(adapter);

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        // Возвращаем результат
        String item = (String) getListAdapter().getItem(position);
        Intent data = new Intent();
        data.putExtra("text", item);
        setResult(RESULT_OK, data);             // Устанавливаем результат
        finish();                               // Завершаем Activity
        return;                                 // Завершаем исполнение кода
    }

    // Деструктор активити
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }
}
