package com.liule.talkhelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** 本机浏览记录，不上传任何地方。 */
public class Db extends SQLiteOpenHelper {

    private static final String NAME = "talk.db";
    private static final int VER = 1;

    public static class Rec {
        public long id;
        public long ts;
        public String company = "";
        public String title = "";
        public String salary = "";
        public int score = -1;
        public String reason = "";
        public String talk = "";
        public String url = "";
        public boolean sent = false;
    }

    public Db(Context c) {
        super(c.getApplicationContext(), NAME, null, VER);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE rec("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "ts INTEGER, company TEXT, title TEXT, salary TEXT,"
                + "score INTEGER, reason TEXT, talk TEXT, url TEXT, sent INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int o, int n) { }

    public long add(Rec r) {
        ContentValues v = new ContentValues();
        v.put("ts", r.ts);
        v.put("company", r.company);
        v.put("title", r.title);
        v.put("salary", r.salary);
        v.put("score", r.score);
        v.put("reason", r.reason);
        v.put("talk", r.talk);
        v.put("url", r.url);
        v.put("sent", r.sent ? 1 : 0);
        return getWritableDatabase().insert("rec", null, v);
    }

    public void markSent(long id) {
        ContentValues v = new ContentValues();
        v.put("sent", 1);
        getWritableDatabase().update("rec", v, "id=?", new String[]{String.valueOf(id)});
    }

    /** 今天已经自动发出去几条 */
    public int countSentToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long start = c.getTimeInMillis();
        Cursor cur = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM rec WHERE sent=1 AND ts>=?", new String[]{String.valueOf(start)});
        int n = 0;
        if (cur.moveToFirst()) n = cur.getInt(0);
        cur.close();
        return n;
    }

    public List<Rec> list(int limit) {
        List<Rec> out = new ArrayList<Rec>();
        Cursor cur = getReadableDatabase().rawQuery(
                "SELECT id,ts,company,title,salary,score,reason,talk,url,sent FROM rec ORDER BY ts DESC LIMIT ?",
                new String[]{String.valueOf(limit)});
        while (cur.moveToNext()) {
            Rec r = new Rec();
            r.id = cur.getLong(0);
            r.ts = cur.getLong(1);
            r.company = nz(cur.getString(2));
            r.title = nz(cur.getString(3));
            r.salary = nz(cur.getString(4));
            r.score = cur.getInt(5);
            r.reason = nz(cur.getString(6));
            r.talk = nz(cur.getString(7));
            r.url = nz(cur.getString(8));
            r.sent = cur.getInt(9) == 1;
            out.add(r);
        }
        cur.close();
        return out;
    }

    public void clearAll() {
        getWritableDatabase().delete("rec", null, null);
    }

    /** 同一天同一个岗位，避免来回滑屏重复记录 */
    public boolean existsSame(String company, String title, long withinMs) {
        Cursor cur = getReadableDatabase().rawQuery(
                "SELECT id FROM rec WHERE company=? AND title=? AND ts>=? LIMIT 1",
                new String[]{company, title, String.valueOf(System.currentTimeMillis() - withinMs)});
        boolean has = cur.moveToFirst();
        cur.close();
        return has;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
