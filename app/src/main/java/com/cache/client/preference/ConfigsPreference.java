package com.cache.client.preference;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Hongmingwei on 2017/11/20.
 * Email: 648600445@qq.com
 */

public class ConfigsPreference extends BaseConfigPreference {

    protected static final String DEFAULT_STRING = "";
    protected static final String KEY_DEVICE_ID = "device_id";

    public ConfigsPreference(Context context) {
        super(context);
    }

    /**
     * 注册监听
     * @param listener
     */
    public void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener){
        getPreferences().registerOnSharedPreferenceChangeListener(listener);
    }

    /**
     * 取消监听
     * @param listener
     */
    public void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener){
        getPreferences().unregisterOnSharedPreferenceChangeListener(listener);
    }


    /**
     * 跟新id
     * @param id
     * @return
     */
    public boolean updateDeviceId(String id){
        return updateValue(KEY_DEVICE_ID, id);
    }

    /**
     * 获取原有ID
     * @return
     */
    public String getDeviceId(){
        return getString(KEY_DEVICE_ID, DEFAULT_STRING);
    }
}
