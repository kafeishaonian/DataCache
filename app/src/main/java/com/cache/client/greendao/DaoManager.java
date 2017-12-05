package com.cache.client.greendao;

import android.content.Context;

import de.greenrobot.dao.query.QueryBuilder;

/**
 * 数据库操作类
 * Created by Hongmingwei on 2017/12/4.
 * Email: 648600445@qq.com
 */

public class DaoManager {
    /**
     * TAG
     */
    private static final String TAG = DaoManager.class.getSimpleName();
    //数据库名称
    private static final String DB_NAME = "greendao.db";
    //多线程访问
    private volatile static DaoManager manager;
    //操作类
    private static DaoMaster.DevOpenHelper helper;
    //上下文
    private Context mContext;
    //核心类
    private static DaoMaster daoMaster;
    private DaoSession daoSession;

    private DaoManager(){

    }

    /**
     * 单例模式
     * @return
     */
    public static DaoManager getInstance(){
        DaoManager instance = null;
        if (manager == null){
            synchronized (DaoManager.class){
                if (instance == null){
                    instance = new DaoManager();
                    manager = instance;
                }
            }
        }
        return instance;
    }

    /**
     * 传递上下文
     * @param context Context
     */
    public void initManager(Context context){
        mContext = context;
    }

    /**
     * 判断是否存在数据库，如果没有则创建
     * @return
     */
    public DaoMaster getDaoMaster(){
        if (daoMaster == null){
            helper = new DaoMaster.DevOpenHelper(mContext, DB_NAME, null);
            daoMaster = new DaoMaster(helper.getWritableDatabase());
        }
         return daoMaster;
    }

    /**
     * 完成对数据库的操作，只是个接口
     * @return
     */
    public DaoSession getDaoSession(){
        if (daoSession == null){
            if (daoMaster == null){
                daoMaster = getDaoMaster();
            }
            daoSession = daoMaster.newSession();
        }
        return daoSession;
    }

    /**
     * 打开输出日志，默认关闭
     */
    public void setDebug(){
        QueryBuilder.LOG_SQL = true;
        QueryBuilder.LOG_VALUES = true;
    }

    /**
     * 关闭DaoSession
     */
    public void closeDaoSession(){
        if (daoSession != null){
            daoSession.clear();
            daoSession = null;
        }
    }

    /**
     * 关闭Helper
     */
    public void closeHelper(){
        if (helper != null){
            helper.close();
            helper = null;
        }
    }

    /**
     * 关闭所有的操作
     */
    public void closeConnection(){
        closeHelper();
        closeDaoSession();
    }


}
