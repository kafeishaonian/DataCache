package com.cache.client.preference;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Hongmingwei on 2017/11/20.
 * Email: 648600445@qq.com
 */

public class PreferenceTools {


    /**
     * 存储deviceid
     *
     * @param context
     * @param id
     * @return
     */
    public static boolean updateDeviceId(Context context, String id) {
        ConfigsPreference preference = new ConfigsPreference(context);
        return preference.updateDeviceId(id);
    }

    /**
     * 获取deviceid
     *
     * @param context
     * @return
     */
    public static String getDeviceId(Context context) {
        ConfigsPreference preference = new ConfigsPreference(context);
        return preference.getDeviceId();
    }

    /**
     * 监听账户信息，这里以后用到的时候再说具体使用
     *
     * @param context
     * @param listener
     */
    public static void registerAccountOnSharedPreferenceChangeListener(Context context, SharedPreferences.OnSharedPreferenceChangeListener listener) {
        ConfigsPreference preference = new ConfigsPreference(context);
        preference.registerOnSharedPreferenceChangeListener(listener);
    }

    /**
     * 取消监听账户信息
     *
     * @param context
     * @param listener
     */
    public static void unregisterAccountOnSharedPreferenceChangeListener(Context context, SharedPreferences.OnSharedPreferenceChangeListener listener) {
        ConfigsPreference preference = new ConfigsPreference(context);
        preference.unregisterOnSharedPreferenceChangeListener(listener);
    }


}
