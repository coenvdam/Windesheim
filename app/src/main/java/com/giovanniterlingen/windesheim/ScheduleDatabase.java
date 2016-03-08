package com.giovanniterlingen.windesheim;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * A scheduler app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
public class ScheduleDatabase extends SQLiteOpenHelper {

    private static SQLiteDatabase database;

    public ScheduleDatabase(Context context) {
        super(context, "schedulestore.db", null, 2);
    }

    public void open() {
        database = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE `subject` (_id INTEGER PRIMARY KEY AUTOINCREMENT, `id` TEXT, `date` TEXT, `start` TEXT, `end` TEXT, `name` TEXT, `room` TEXT, `teacher` TEXT, `class_id` TEXT)");
    }

    public void saveScheduleData(String id, String date, String start, String end, String name, String room, String teacher, String classId) {
        try {
            database.execSQL("INSERT INTO `subject` (`id`, `date`, `start`, `end`, `name`, `room`, `teacher`, `class_id`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", new String[]{id, date, start, end, name, room, teacher, classId});
        } catch (SQLiteConstraintException ex) {
            database.execSQL("UPDATE `subject` SET `end` = ? WHERE `id` = ?", new String[]{end, id});
        }
    }

    public void clearScheduleData(String date) {
        database.execSQL("DELETE FROM `subject` WHERE `date` = ?", new String[]{date});
    }

    public void clearOldScheduleData(String date) {
        database.execSQL("DELETE FROM `subject` WHERE `date` < ?", new String[]{date});
    }

    public Cursor getLessons(String date, String classId) {
        return database.rawQuery("SELECT _id, `id`, `date`, MIN(`start`), MAX(`end`), `name`, `room`, `teacher`, `class_id` FROM `subject` WHERE `date` = ? AND `class_id` = ? GROUP BY `id` ORDER BY `start`", new String[]{date, classId});
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            database.execSQL("DROP TABLE `subject`");
            database.execSQL("CREATE TABLE `subject` (_id INTEGER PRIMARY KEY AUTOINCREMENT, `id` TEXT, `date` TEXT, `start` TEXT, `end` TEXT, `name` TEXT, `room` TEXT, `teacher` TEXT, `class_id` TEXT)");
        }
    }

}