package com.liule.talkhelper;

import android.content.Context;
import android.content.SharedPreferences;

public final class Cfg {
    public String url = "https://api.deepseek.com/v1/chat/completions";
    public String model = "deepseek-chat";
    public String key = "";
    public String resume = "";
    public int words = 0;

    /* 求职条件，留空 = 不限 */
    public String recruit = "";
    public String city = "";
    public String salary = "";

    /* 自动化 */
    public int threshold = 85;
    public int dailyLimit = 5;
    public boolean autoSend = true;
    public boolean autoLink = true;

    private static final String P = "talk_cfg";

    public static Cfg load(Context c) {
        SharedPreferences sp = c.getSharedPreferences(P, Context.MODE_PRIVATE);
        Cfg g = new Cfg();
        g.url = sp.getString("url", g.url);
        g.model = sp.getString("model", g.model);
        g.key = sp.getString("key", "");
        g.resume = sp.getString("resume", "");
        g.words = sp.getInt("words", 0);
        g.recruit = sp.getString("recruit", "");
        g.city = sp.getString("city", "");
        g.salary = sp.getString("salary", "");
        g.threshold = sp.getInt("threshold", 85);
        g.dailyLimit = sp.getInt("dailyLimit", 5);
        g.autoSend = sp.getBoolean("autoSend", true);
        g.autoLink = sp.getBoolean("autoLink", true);
        return g;
    }

    public void save(Context c) {
        c.getSharedPreferences(P, Context.MODE_PRIVATE).edit()
                .putString("url", url)
                .putString("model", model)
                .putString("key", key)
                .putString("resume", resume)
                .putInt("words", words)
                .putString("recruit", recruit)
                .putString("city", city)
                .putString("salary", salary)
                .putInt("threshold", threshold)
                .putInt("dailyLimit", dailyLimit)
                .putBoolean("autoSend", autoSend)
                .putBoolean("autoLink", autoLink)
                .apply();
    }

    public boolean ready() {
        return key != null && key.trim().length() > 5
                && url != null && url.startsWith("http")
                && model != null && model.trim().length() > 0;
    }

    public String conditions() {
        StringBuilder sb = new StringBuilder();
        sb.append("- 招聘类型：").append(recruit.trim().isEmpty() ? "不限" : recruit.trim()).append("\n");
        sb.append("- 期望城市：").append(city.trim().isEmpty() ? "不限" : city.trim()).append("\n");
        sb.append("- 期望薪资：").append(salary.trim().isEmpty() ? "不限" : salary.trim()).append("\n");
        return sb.toString();
    }
}
