package com.cache.client.sqlCipher;

import android.content.Context;
import android.util.Log;

import net.sqlcipher.SQLException;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

/**
 * SQLCipher:
 *    加密性能高，开销小，只要5~15%的开销用于加密
 *    完全做到数据库100%加密
 *    采用良好的加密方式（CCBC加密模式）
 *    使用方便，做到应用级别加密
 *    采用OpenSSL加密库提供的算法
 *
 * Created by Hongmingwei on 2017/10/24.
 * Email: 648600445@qq.com
 */

public class DBCipherHelper extends SQLiteOpenHelper {

    private static final String TAG = DBCipherHelper.class.getSimpleName();

    private static final String DB_NAME = "text_db";//数据库名字
    public static final String DB_PWD = "whm";//数据库密码
    public static String TABLE_NAME = "person";// 表名
    public static String FIELD_ID = "id";// 列名
    public static String  FIELD_NAME= "name";// 列名
    private static final int DB_VERSION = 1;   // 数据库版本


    public DBCipherHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        //不可忽略的  进行so库加载
        SQLiteDatabase.loadLibs(context);
    }

    public DBCipherHelper(Context context) {
        this(context, DB_NAME, null, DB_VERSION);
    }


    /**
     * 创建数据库
     * @param sqLiteDatabase
     */
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        //创建表
        createTable(sqLiteDatabase);
    }

    private void createTable(SQLiteDatabase db){
        String  sql = "CREATE TABLE " + TABLE_NAME + "(" + FIELD_ID + " integer primary key autoincrement , " + FIELD_NAME + " text not null);";
        try {
            db.execSQL(sql);
        } catch (SQLException e) {
            Log.e(TAG, "onCreate " + TABLE_NAME + "Error" + e.toString());
            return;
        }
    }

    /**
     * 数据库升级
     * @param sqLiteDatabase
     * @param i
     * @param i1
     */
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
