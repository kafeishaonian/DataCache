package com.cache.client;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.cache.client.disk.DiskCacheManager;
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
}
