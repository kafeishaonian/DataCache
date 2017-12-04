package com.cache.client.preference;

import android.content.Context;

/**
 * Created by Hongmingwei on 2017/11/20.
 * Email: 648600445@qq.com
 */

public class BaseConfigPreference extends PreferencesWriter {

    //版本号
    private static final int VERSION = 1;
    //文件名
    protected static final String FILE_NAME = "configInfo";

    public BaseConfigPreference(Context context) {
        super(context, FILE_NAME);
    }

    @Override
    protected void initPreferencChange() {
        int version = getDeviceVersion();
        if (version != VERSION){
            updateDeviceVersion(VERSION);
        }
    }

    /**
     * 更新版本号
     * @param version
     * @return
     */
    public boolean updateDeviceVersion(int version) {
        return updateVersion(version);
    }

    /**
     * 获取当前版本号
     * @return boolean
     */
    public int getDeviceVersion() {
        return getVersion();
    }

}
