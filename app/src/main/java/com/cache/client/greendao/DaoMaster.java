package com.cache.client.greendao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import de.greenrobot.dao.AbstractDaoMaster;
import de.greenrobot.dao.identityscope.IdentityScopeType;

/**
 * Created by Hongmingwei on 2017/12/4.
 * Email: 648600445@qq.com
 */

public class DaoMaster extends AbstractDaoMaster {
    private static final String TAG = DaoManager.class.getSimpleName();
    //版本
    public static final int SCHEMA_VERSION = 1;

    public DaoMaster(SQLiteDatabase db) {
        super(db, SCHEMA_VERSION);
    }

    @Override
    public DaoSession newSession() {
        return new DaoSession(db, IdentityScopeType.Session, daoConfigMap);
    }

    @Override
    public DaoSession newSession(IdentityScopeType type) {
        return new DaoSession(db, type, daoConfigMap);
    }




    public static abstract class OpenHelper extends SQLiteOpenHelper{

        public OpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory) {
            super(context, name, factory, SCHEMA_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.i(TAG, "Creating tables for schema version" + SCHEMA_VERSION);
            createAllTables(db, false);
        }
    }

    /**
     * 创建所有表
     * @param db 数据库
     * @param ifNotExists 是否存在
     */
    public static void createAllTables(SQLiteDatabase db, boolean ifNotExists){
        StudentDao.createTable(db, ifNotExists);
    }

    public static class DevOpenHelper extends OpenHelper{

        public DevOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory) {
            super(context, name, factory);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.i("greenDAO", "Upgrading schema from version " + oldVersion + " to " + newVersion + " by dropping all tables");
            dropAllTables(db, true);
            onCreate(db);
        }
    }

    /**
     *  删除所有的表
     * @param db
     * @param ifExists
     */
    public static void dropAllTables(SQLiteDatabase db, boolean ifExists){
        StudentDao.dropTable(db, ifExists);
    }
}
