package com.cache.client.greendao;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.AbstractDaoSession;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.internal.DaoConfig;

/**
 * Created by Hongmingwei on 2017/12/4.
 * Email: 648600445@qq.com
 */

public class StudentDao extends AbstractDao<Student, Long> {
    /**
     * TAG
     */
    public static final String TAG =  "STUDENT";

    public static class Properties{
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property Name = new Property(1, String.class, "name", false, "NAME");
        public final static Property Age = new Property(2, Integer.class, "age", false, "AGE");
        public final static Property Address = new Property(3, String.class, "address", false, "ADDRESS");
    }

    public StudentDao(DaoConfig config) {
        super(config);
    }

    public StudentDao(DaoConfig config, DaoSession daoSession){
        super(config, daoSession);
    }

    /**
     * 创建表
     * @param ifNotExists 是否存在
     */
    public static void createTable(SQLiteDatabase db, boolean ifNotExists){
        String constraint = ifNotExists? "IF NOT EXISTS" : "";
        db.execSQL("CREATE TABLE " + constraint + "\"STUDENT\" (" +
                "\"_id\" INTEGER PRIMARY KEY ," + // 0: id
                "\"NAME\" TEXT," + // 1: name
                "\"AGE\" INTEGER," + // 2: age
                "\"ADDRESS\" TEXT);"); // 3: address
    }

    /**
     * 删除表
     */
    public static void dropTable(SQLiteDatabase db, boolean ifExists){
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"STUDENT\"";
        db.execSQL(sql);
    }


    @Override
    protected void bindValues(SQLiteStatement stmt, Student entity) {
        stmt.clearBindings();

        Long id = entity.getId();
        if (id != null){
            stmt.bindLong(1, id);
        }

        String name = entity.getName();
        if (name != null) {
            stmt.bindString(2, name);
        }

        Integer age = entity.getAge();
        if (age != null) {
            stmt.bindLong(3, age);
        }

        String address = entity.getAddress();
        if (address != null) {
            stmt.bindString(4, address);
        }
    }

    @Override
    protected Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }

    /** @inheritdoc */
    @Override
    public Student readEntity(Cursor cursor, int offset) {
        Student entity = new Student( //
                cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
                cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1), // name
                cursor.isNull(offset + 2) ? null : cursor.getInt(offset + 2), // age
                cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3) // address
        );
        return entity;
    }

    /** @inheritdoc */
    @Override
    public void readEntity(Cursor cursor, Student entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setName(cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1));
        entity.setAge(cursor.isNull(offset + 2) ? null : cursor.getInt(offset + 2));
        entity.setAddress(cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3));
    }

    /** @inheritdoc */
    @Override
    protected Long updateKeyAfterInsert(Student entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }

    /** @inheritdoc */
    @Override
    public Long getKey(Student entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    /** @inheritdoc */
    @Override
    protected boolean isEntityUpdateable() {
        return true;
    }
}
