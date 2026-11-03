package com.liule.talkhelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 浏览记录：今天看过哪些岗位、打了多少分、发没发。只存本机。 */
public class HistoryActivity extends Activity {

    private LinearLayout listBox;
    private TextView summary;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#F2F4F8"));

        /* 顶栏 */
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(Color.parseColor("#1E40AF"));
        bar.setPadding(dp(14), dp(40), dp(14), dp(14));
        root.addView(bar);

        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        back.setTextColor(Color.WHITE);
        back.setPadding(dp(4), 0, dp(14), 0);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        bar.addView(back);

        TextView title = new TextView(this);
        title.setText("浏览记录");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        bar.addView(title, tLp);

        TextView clear = new TextView(this);
        clear.setText("清空");
        clear.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        clear.setTextColor(Color.parseColor("#BFDBFE"));
        clear.setPadding(dp(10), dp(4), dp(4), dp(4));
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { confirmClear(); }
        });
        bar.addView(clear);

        summary = new TextView(this);
        summary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        summary.setTextColor(Color.parseColor("#6B7280"));
        summary.setPadding(dp(16), dp(12), dp(16), dp(4));
        root.addView(summary);

        ScrollView sv = new ScrollView(this);
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        listBox.setPadding(dp(12), dp(6), dp(12), dp(30));
        sv.addView(listBox);
        root.addView(sv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        listBox.removeAllViews();
        List<Db.Rec> all = new Db(this).list(300);

        if (all.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("还没有记录。\n去招聘 App 的岗位页点一下悬浮球，这里就会记下来。");
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            empty.setTextColor(Color.parseColor("#9CA3AF"));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(60), 0, 0);
            empty.setLineSpacing(dp(4), 1f);
            listBox.addView(empty);
            summary.setText("");
            return;
        }

        SimpleDateFormat dayFmt = new SimpleDateFormat("MM 月 dd 日", Locale.CHINA);
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.CHINA);
        String today = dayFmt.format(new Date());

        int todayCount = 0, todaySum = 0, scored = 0;
        String curDay = "";
        for (Db.Rec r : all) {
            String d = dayFmt.format(new Date(r.ts));
            if (!d.equals(curDay)) {
                curDay = d;
                TextView h = new TextView(this);
                h.setText(d.equals(today) ? d + " · 今天" : d);
                h.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
                h.setTextColor(Color.parseColor("#6B7280"));
                h.setTypeface(null, android.graphics.Typeface.BOLD);
                h.setPadding(dp(4), dp(14), 0, dp(6));
                listBox.addView(h);
            }
            if (d.equals(today)) {
                todayCount++;
                if (r.score >= 0) { todaySum += r.score; scored++; }
            }
            listBox.addView(row(r, timeFmt));
        }

        if (todayCount == 0) {
            summary.setText("今天还没有记录");
        } else if (scored > 0) {
            summary.setText("今天 " + todayCount + " 个岗位 · 平均 " + (todaySum / scored) + " 分");
        } else {
            summary.setText("今天 " + todayCount + " 个岗位");
        }
    }

    private View row(final Db.Rec r, SimpleDateFormat timeFmt) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackground(round(Color.WHITE, 12));
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        card.setLayoutParams(lp);

        TextView score = new TextView(this);
        if (r.score < 0) {
            score.setText("—");
            score.setTextColor(Color.parseColor("#9CA3AF"));
            score.setBackground(round(Color.parseColor("#F1F5F9"), 10));
        } else {
            score.setText(String.valueOf(r.score));
            if (r.score >= 75) {
                score.setTextColor(Color.parseColor("#15803D"));
                score.setBackground(round(Color.parseColor("#DCFCE7"), 10));
            } else if (r.score >= 50) {
                score.setTextColor(Color.parseColor("#B45309"));
                score.setBackground(round(Color.parseColor("#FEF3C7"), 10));
            } else {
                score.setTextColor(Color.parseColor("#B91C1C"));
                score.setBackground(round(Color.parseColor("#FEE2E2"), 10));
            }
        }
        score.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        score.setTypeface(null, android.graphics.Typeface.BOLD);
        score.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(dp(42), dp(42));
        sLp.rightMargin = dp(12);
        card.addView(score, sLp);

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        card.addView(mid, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView t1 = new TextView(this);
        String head = r.title.isEmpty() ? "(未识别岗位)" : r.title;
        if (!r.salary.isEmpty()) head += "  " + r.salary;
        t1.setText(head);
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        t1.setTextColor(Color.parseColor("#111827"));
        t1.setTypeface(null, android.graphics.Typeface.BOLD);
        mid.addView(t1);

        TextView t2 = new TextView(this);
        StringBuilder sb = new StringBuilder();
        sb.append(r.company.isEmpty() ? "未知公司" : r.company);
        sb.append(" · ").append(timeFmt.format(new Date(r.ts)));
        if (!r.url.isEmpty()) sb.append(" · 有链接");
        t2.setText(sb.toString());
        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        t2.setTextColor(Color.parseColor("#9CA3AF"));
        t2.setPadding(0, dp(2), 0, 0);
        mid.addView(t2);

        if (!r.reason.isEmpty()) {
            TextView t3 = new TextView(this);
            t3.setText(r.reason);
            t3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            t3.setTextColor(Color.parseColor("#64748B"));
            t3.setPadding(0, dp(3), 0, 0);
            mid.addView(t3);
        }

        TextView badge = new TextView(this);
        badge.setText(r.sent ? "已发送" : "未发送");
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        badge.setGravity(Gravity.CENTER);
        if (r.sent) {
            badge.setTextColor(Color.WHITE);
            badge.setBackground(round(Color.parseColor("#16A34A"), 999));
        } else {
            badge.setTextColor(Color.parseColor("#94A3B8"));
            badge.setBackground(round(Color.parseColor("#F1F5F9"), 999));
        }
        badge.setPadding(dp(8), dp(3), dp(8), dp(3));
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bLp.leftMargin = dp(8);
        card.addView(badge, bLp);

        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { open(r); }
        });
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) { copyTalk(r); return true; }
        });
        return card;
    }

    private void open(Db.Rec r) {
        if (r.url == null || r.url.trim().isEmpty()) {
            Toast.makeText(this, "这条没抓到链接，长按可以复制话术", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(r.url.trim()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "打不开这个链接：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void copyTalk(Db.Rec r) {
        if (r.talk == null || r.talk.trim().isEmpty()) return;
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("开场白", r.talk));
        Toast.makeText(this, "话术已复制", Toast.LENGTH_SHORT).show();
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setTitle("清空全部记录？")
                .setMessage("只会删掉本机这份记录，不影响招聘 App 里的任何东西。")
                .setPositiveButton("清空", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        new Db(HistoryActivity.this).clearAll();
                        render();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusDp >= 999 ? 999 : dp(radiusDp));
        return d;
    }
}
