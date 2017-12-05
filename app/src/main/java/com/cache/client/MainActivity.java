package com.cache.client;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.cache.client.disk.DiskCacheManager;
import com.cache.client.greendao.CommonUtils;
import com.cache.client.greendao.Student;
import com.cache.client.sqlCipher.DBCipherManager;
import com.cache.client.util.ThreadTools;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


    }


    private void transModel() {
        ThreadTools.startNormalPriorityThread(new Runnable() {

            @Override
            public void run() {
                DiskCacheManager.getInstance(MainActivity.this).put(DiskCacheManager.KEY_CHART + String.valueOf(0), "model");
            }
        });

        String dataModel = DiskCacheManager.getInstance(MainActivity.this).getAsSerializable(DiskCacheManager.KEY_CHART + String.valueOf(0));
    }

    private void dbc(){
        //清空数据
        DBCipherManager.getInstance(MainActivity.this).deleteDatas();
        //插入数据
        for (int i = 0; i < 10; i++) {
            DBCipherManager.getInstance(MainActivity.this).insertData(String.valueOf(i));
        }
        //删除数据
        DBCipherManager.getInstance(MainActivity.this).deleteData(String.valueOf(5));
        //更新数据
        DBCipherManager.getInstance(MainActivity.this).updateData(String.valueOf(3));
        //查询数据
        DBCipherManager.getInstance(MainActivity.this).queryDatas();
    }

    private void greendao(){
        Student student = new Student();
        CommonUtils.getInstance(MainActivity.this).ineserStudent(student);
    }
}
