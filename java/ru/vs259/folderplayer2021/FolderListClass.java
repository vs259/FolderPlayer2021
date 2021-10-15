package ru.vs259.folderplayer2021;

import android.os.Bundle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class FolderListClass extends ListActivity {

    private List<String> item = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        item = new ArrayList<String>();

        // Обращение к области бандла для получения переданных аргументов
        Bundle b = this.getIntent().getExtras();

        String path = b.getString("StartDir");
        Boolean isParentLevel = b.getBoolean("isParentLevel");
        String ParentString = b.getString("ParentString");

        if(isParentLevel)
            item.add(ParentString);

        File[] dir = new File(path).listFiles();
        for(int i = 0; i < dir.length; i++)
        {
            item.add(dir[i].getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, item);
        setListAdapter(adapter);

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        // Возвращаем результат
        String item = (String) getListAdapter().getItem(position);
        int pos = (int) getListAdapter().getItemId(position);
        Intent data = new Intent();
        data.putExtra("text", item);
        data.putExtra("position", pos);
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
