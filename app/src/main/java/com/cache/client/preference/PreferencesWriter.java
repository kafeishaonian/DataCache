package com.cache.client.preference;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreferences存储基类
 * Created by Hongmingwei on 2017/11/20.
 * Email: 648600445@qq.com
 */

public abstract class PreferencesWriter {

    protected Context mContext;
    private String mFileName;//文件名

    public static final String KEY_PREFERENCES_VERSION = "preferences_version";
    public static final int DEFAULT_PREFERENCES_VERSION = 0;

    public PreferencesWriter(Context context, String fileName) {
        this.mContext = context;
        this.mFileName = fileName;
        initPreferencChange();
    }

    /**
     * 初始化，通过判断当前版本号，作出相应的修改
     */
    protected abstract void initPreferencChange();

    public Context getContext() {
        return mContext;
    }

    /**
     * 获取当前的版本号
     *
     * @return 版本号
     */
    protected int getVersion() {
        return getInt(KEY_PREFERENCES_VERSION, DEFAULT_PREFERENCES_VERSION);
    }

    /**
     * 更新当前的版本号
     *
     * @param version 版本号
     * @return boolean
     */
    protected boolean updateVersion(int version) {
        if (version > 0) {
            return updateValue(KEY_PREFERENCES_VERSION, version);
        }
        return false;
    }

    /**
     * 清楚整个文件中的数据
     *
     * @return Boolean
     */
    public boolean clear() {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.clear();
        return editor.commit();
    }

    /**
     * 根据Key清楚数据
     *
     * @param key key
     * @return boolean
     */
    public boolean removeKey(String key) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.remove(key);
        return editor.commit();
    }

    /**
     * 通过Key-Value更新String值
     *
     * @param key   键
     * @param value 值
     * @return Boolean
     */
    protected boolean updateValue(String key, String value) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(key, value);
        return editor.commit();
    }

    /**
     * 通过Key-Value更新Int值
     *
     * @param key   键
     * @param value 值
     * @return Boolean
     */
    protected boolean updateValue(String key, int value) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putInt(key, value);
        return editor.commit();
    }

    /**
     * 通过Key-Value更新Float值
     *
     * @param key   键
     * @param value 值
     * @return Boolean
     */
    protected boolean updateValue(String key, float value) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putFloat(key, value);
        return editor.commit();
    }

    /**
     * 通过Key-Value更新Long值
     *
     * @param key   键
     * @param value 值
     * @return Boolean
     */
    private boolean updateValue(String key, long value) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putLong(key, value);
        return editor.commit();
    }

    /**
     * 通过Key-Value更新boolean值
     *
     * @param key   键
     * @param value 值
     * @return Boolean
     */
    private boolean updateValue(String key, boolean value) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putBoolean(key, value);
        return editor.commit();
    }

    /**
     * 通过Key-Value获取存储String值
     *
     * @param key          key
     * @param defaultValue value
     * @return String
     */
    protected String getString(String key, String defaultValue) {
        return getPreferences().getString(key, defaultValue);
    }

    /**
     * 通过Key-Value获取存储Int值
     *
     * @param key          key
     * @param defaultValue value
     * @return Int
     */
    protected int getInt(String key, int defaultValue) {
        return getPreferences().getInt(key, defaultValue);
    }

    /**
     * 通过Key-Value获取存储Float值
     *
     * @param key          key
     * @param defaultValue value
     * @return float
     */
    protected float getFloat(String key, float defaultValue) {
        return getPreferences().getFloat(key, defaultValue);
    }

    /**
     * 通过Key-Value获取存储Long值
     *
     * @param key          key
     * @param defaultValue value
     * @return long
     */
    protected long getLong(String key, long defaultValue) {
        return getPreferences().getLong(key, defaultValue);
    }


    /**
     * 通过Key-Value获取存储Boolean值
     *
     * @param key          key
     * @param defaultValue value
     * @return boolean
     */
    protected boolean getBoolean(String key, boolean defaultValue) {
        return getPreferences().getBoolean(key, defaultValue);
    }

    /**
     * 通过文件名获取SharedPreferences
     *
     * @return
     */
    protected SharedPreferences getPreferences() {
        if (mContext.getSharedPreferences(mFileName, Context.MODE_PRIVATE) != null) {
            return mContext.getSharedPreferences(mFileName, Context.MODE_PRIVATE);
        }
        return null;
    }

}
