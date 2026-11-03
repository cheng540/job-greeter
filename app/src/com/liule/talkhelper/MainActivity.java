package com.liule.talkhelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {

    private static final int REQ_FILE = 1001;

    private TextView statusView, btnOpen, headTitle;
    private EditText urlEdit, modelEdit, keyEdit, wordEdit, resumeEdit;
    private EditText recruitEdit, cityEdit, salaryEdit, thresholdEdit, limitEdit;
    private CheckBox cbSend, cbLink;
    private Cfg cfg;

    private final List<View> builtPages = new ArrayList<View>();
    private FrameLayout pageBox;
    private View[] pages;
    private View menu, scrim;
    private LinearLayout menuList;
    private int menuW;
    private int curPage = 0;
    private boolean menuOpen = false;

    private static final String[] PAGE_NAMES = {"首页", "配置", "帮助"};

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        cfg = Cfg.load(this);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#F2F4F8"));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        /* 顶栏 */
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(Color.parseColor("#1E40AF"));
        header.setPadding(dp(12), dp(40), dp(16), dp(14));
        content.addView(header);

        LinearLayout burger = new LinearLayout(this);
        burger.setOrientation(LinearLayout.VERTICAL);
        burger.setGravity(Gravity.CENTER);
        burger.setPadding(dp(10), dp(10), dp(10), dp(10));
        for (int i = 0; i < 3; i++) {
            View line = new View(this);
            GradientDrawable lg = new GradientDrawable();
            lg.setColor(Color.WHITE);
            lg.setCornerRadius(dp(2));
            line.setBackground(lg);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(dp(20), dp(2));
            if (i > 0) llp.topMargin = dp(5);
            burger.addView(line, llp);
        }
        burger.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleMenu(); }
        });
        header.addView(burger);

        headTitle = new TextView(this);
        headTitle.setText("打招呼助手");
        headTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        headTitle.setTextColor(Color.WHITE);
        headTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams htLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        htLp.leftMargin = dp(12);
        header.addView(headTitle, htLp);

        pageBox = new FrameLayout(this);
        content.addView(pageBox, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        buildHome();
        buildConfig();
        buildHelp();
        pages = new View[builtPages.size()];
        for (int i = 0; i < pages.length; i++) pages[i] = builtPages.get(i);
        for (View p : pages) {
            pageBox.addView(p, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        }

        /* 遮罩 */
        scrim = new View(this);
        scrim.setBackgroundColor(Color.parseColor("#66000000"));
        scrim.setVisibility(View.GONE);
        scrim.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { closeMenu(); }
        });
        root.addView(scrim, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        /* 抽屉 */
        menuW = (int) (getResources().getDisplayMetrics().widthPixels * 0.76f);
        menu = buildMenu();
        root.addView(menu, new FrameLayout.LayoutParams(menuW,
                FrameLayout.LayoutParams.MATCH_PARENT));
        menu.setTranslationX(-menuW);

        setContentView(root);
        showPage(0);
    }

    /* ================= 抽屉 ================= */

    private View buildMenu() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(Color.WHITE);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setBackgroundColor(Color.parseColor("#1E40AF"));
        head.setPadding(dp(20), dp(46), dp(20), dp(18));
        box.addView(head);

        TextView t = new TextView(this);
        t.setText("打招呼助手");
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        t.setTextColor(Color.WHITE);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        head.addView(t);

        TextView s = new TextView(this);
        s.setText("招聘 App 岗位页点悬浮球即可");
        s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        s.setTextColor(Color.parseColor("#BFDBFE"));
        s.setPadding(0, dp(3), 0, 0);
        head.addView(s);

        menuList = new LinearLayout(this);
        menuList.setOrientation(LinearLayout.VERTICAL);
        menuList.setPadding(0, dp(8), 0, 0);
        box.addView(menuList);

        addMenuItem("首页", "服务状态与保活", 0);
        addMenuItem("浏览记录", "看过的岗位和评分", -1);
        addMenuItem("配置", "接口、简历、求职条件", 1);
        addMenuItem("帮助", "怎么用、常见问题", 2);

        return box;
    }

    private void addMenuItem(String name, String desc, final int page) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(dp(20), dp(13), dp(20), dp(13));
        item.setTag(Integer.valueOf(page));

        TextView t = new TextView(this);
        t.setText(name);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        t.setTextColor(Color.parseColor("#111827"));
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        item.addView(t);

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        d.setTextColor(Color.parseColor("#9CA3AF"));
        d.setPadding(0, dp(2), 0, 0);
        item.addView(d);

        item.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                closeMenu();
                if (page < 0) {
                    startActivity(new Intent(MainActivity.this, HistoryActivity.class));
                } else {
                    showPage(page);
                }
            }
        });
        menuList.addView(item);
    }

    private void toggleMenu() {
        if (menuOpen) closeMenu(); else openMenu();
    }

    private void openMenu() {
        menuOpen = true;
        scrim.setVisibility(View.VISIBLE);
        scrim.setAlpha(0f);
        scrim.animate().alpha(1f).setDuration(180).start();
        menu.animate().translationX(0).setDuration(200).start();
    }

    private void closeMenu() {
        menuOpen = false;
        scrim.animate().alpha(0f).setDuration(150).start();
        menu.animate().translationX(-menuW).setDuration(180).start();
        scrim.postDelayed(new Runnable() {
            @Override public void run() {
                if (!menuOpen) scrim.setVisibility(View.GONE);
            }
        }, 200);
    }

    private void showPage(int idx) {
        if (idx < 0 || idx >= pages.length) idx = 0;
        curPage = idx;
        for (int i = 0; i < pages.length; i++) {
            pages[i].setVisibility(i == idx ? View.VISIBLE : View.GONE);
        }
        headTitle.setText(PAGE_NAMES[idx]);
        for (int i = 0; i < menuList.getChildCount(); i++) {
            View it = menuList.getChildAt(i);
            Object tag = it.getTag();
            boolean on = (tag instanceof Integer) && ((Integer) tag).intValue() == idx;
            it.setBackgroundColor(on ? Color.parseColor("#EFF6FF") : Color.TRANSPARENT);
        }
        if (idx == 0) refreshStatus();
    }

    /* ================= 首页 ================= */

    private void buildHome() {
        LinearLayout root = page();

        /* --- 手动模式 --- */
        LinearLayout mCard = card();
        root.addView(mCard);
        mCard.addView(label("手动模式"));

        TextView mDesc = new TextView(this);
        mDesc.setText("停在岗位详情页，点屏幕上的蓝色悬浮球「话」，它读当前这一屏 → 打分 → 生成话术。"
                + "分数够线会按你的设置自动发，不够就只记进浏览记录。");
        mDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        mDesc.setTextColor(Color.parseColor("#4B5563"));
        mDesc.setLineSpacing(dp(4), 1f);
        mCard.addView(mDesc);

        TextView mTip = new TextView(this);
        mTip.setText("悬浮球可以拖动，摆在屏幕边上不挡内容的位置。");
        mTip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        mTip.setTextColor(Color.parseColor("#9CA3AF"));
        mTip.setPadding(0, dp(8), 0, 0);
        mCard.addView(mTip);

        root.addView(title("服务状态"));

        LinearLayout sCard = card();
        root.addView(sCard);
        sCard.addView(label("无障碍服务"));

        statusView = new TextView(this);
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        statusView.setTypeface(null, android.graphics.Typeface.BOLD);
        statusView.setPadding(0, dp(3), 0, dp(12));
        sCard.addView(statusView);

        btnOpen = primaryBtn("去开启无障碍服务");
        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
                catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            }
        });
        sCard.addView(btnOpen);

        TextView tip = new TextView(this);
        tip.setText("在无障碍列表里找到「打招呼助手」并打开。若提示「受限设置」，"
                + "去 设置 → 应用 → 打招呼助手 → 右上角三点 → 允许受限设置，再回来开启。");
        tip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tip.setTextColor(Color.parseColor("#9CA3AF"));
        tip.setLineSpacing(dp(3), 1f);
        tip.setPadding(0, dp(10), 0, 0);
        sCard.addView(tip);

        LinearLayout kCard = card();
        root.addView(kCard);
        kCard.addView(label("后台保活（建议全开）"));

        TextView kDesc = new TextView(this);
        kDesc.setText("系统清后台时会把悬浮球一起收掉，表现就是球突然没了、或者点了没反应。");
        kDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        kDesc.setTextColor(Color.parseColor("#6B7280"));
        kDesc.setLineSpacing(dp(3), 1f);
        kDesc.setPadding(0, 0, 0, dp(10));
        kCard.addView(kDesc);

        TextView k1 = secondaryBtn("① 忽略电池优化（最关键）");
        k1.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { askBattery(); }
        });
        kCard.addView(k1);

        TextView k2 = secondaryBtn("② 允许自启动 / 后台活动");
        k2.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { askAutoStart(); }
        });
        LinearLayout.LayoutParams kl2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        kl2.topMargin = dp(8);
        kCard.addView(k2, kl2);

        TextView k3 = secondaryBtn("③ 允许显示在其他应用上层");
        k3.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { askOverlay(); }
        });
        LinearLayout.LayoutParams kl3 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        kl3.topMargin = dp(8);
        kCard.addView(k3, kl3);

        TextView kTip = new TextView(this);
        kTip.setText("还有一步要手动：从屏幕底部上滑打开任务列表，长按「打招呼助手」的卡片把它锁定。");
        kTip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        kTip.setTextColor(Color.parseColor("#9CA3AF"));
        kTip.setLineSpacing(dp(3), 1f);
        kTip.setPadding(0, dp(10), 0, 0);
        kCard.addView(kTip);
    }

    /* ================= 配置 ================= */

    private void buildConfig() {
        LinearLayout root = page();
        root.addView(title("配置"));

        LinearLayout aCard = card();
        root.addView(aCard);
        aCard.addView(label("大模型接口（OpenAI 兼容）"));

        urlEdit = input("接口地址", cfg.url, false);
        aCard.addView(field("接口地址", urlEdit));
        modelEdit = input("模型名", cfg.model, false);
        aCard.addView(field("模型名", modelEdit));
        keyEdit = input("API Key", cfg.key, true);
        aCard.addView(field("API Key", keyEdit));

        TextView aTip = new TextView(this);
        aTip.setText("任何 OpenAI 兼容接口都行（DeepSeek、通义、Kimi、自建中转都支持）。Key 只存在本机。");
        aTip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        aTip.setTextColor(Color.parseColor("#9CA3AF"));
        aTip.setLineSpacing(dp(3), 1f);
        aTip.setPadding(0, dp(6), 0, 0);
        aCard.addView(aTip);

        LinearLayout wCard = card();
        root.addView(wCard);
        wCard.addView(label("字数（留空 = 自动）"));

        wordEdit = new EditText(this);
        wordEdit.setHint("留空 = 自动");
        wordEdit.setText(cfg.words > 0 ? String.valueOf(cfg.words) : "");
        wordEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        wordEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
        wordEdit.setBackground(round(Color.parseColor("#F8FAFC"), 10));
        wordEdit.setPadding(dp(12), dp(10), dp(12), dp(10));
        wCard.addView(wordEdit);

        TextView wTip = new TextView(this);
        wTip.setText("默认留空，由模型按内容决定说多少。想控制长度就填 20–500。");
        wTip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        wTip.setTextColor(Color.parseColor("#9CA3AF"));
        wTip.setLineSpacing(dp(3), 1f);
        wTip.setPadding(0, dp(6), 0, 0);
        wCard.addView(wTip);

        LinearLayout rCard = card();
        root.addView(rCard);
        rCard.addView(label("我的简历"));

        resumeEdit = new EditText(this);
        resumeEdit.setText(cfg.resume);
        resumeEdit.setHint("把简历内容粘贴进来，或点下面的「从文件导入」");
        resumeEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        resumeEdit.setGravity(Gravity.TOP | Gravity.START);
        resumeEdit.setMinLines(6);
        resumeEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        resumeEdit.setBackground(round(Color.WHITE, 10));
        resumeEdit.setPadding(dp(12), dp(12), dp(12), dp(12));
        rCard.addView(resumeEdit, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(200)));

        TextView importBtn = primaryBtn("从文件导入（.txt / .docx）");
        importBtn.setBackground(round(Color.parseColor("#EFF6FF"), 10));
        importBtn.setTextColor(Color.parseColor("#1D4ED8"));
        importBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickFile(); }
        });
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ilp.topMargin = dp(10);
        rCard.addView(importBtn, ilp);

        TextView rTip = new TextView(this);
        rTip.setText("Word（.docx）会直接读文字；PDF 用 WPS 打开复制文本粘进来。简历写得越具体，话术越不像模板。");
        rTip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        rTip.setTextColor(Color.parseColor("#9CA3AF"));
        rTip.setLineSpacing(dp(3), 1f);
        rTip.setPadding(0, dp(8), 0, 0);
        rCard.addView(rTip);

        LinearLayout jCard = card();
        root.addView(jCard);
        jCard.addView(label("求职条件（留空 = 不限）"));

        recruitEdit = input("例如：实习 / 全职 / 校招", cfg.recruit, false);
        jCard.addView(field("招聘类型", recruitEdit));
        cityEdit = input("例如：武汉", cfg.city, false);
        jCard.addView(field("期望城市", cityEdit));
        salaryEdit = input("例如：150-300 元/天，或 8-12K", cfg.salary, false);
        jCard.addView(field("期望薪资", salaryEdit));

        TextView jTip = new TextView(this);
        jTip.setText("这三项参与打分：城市、薪资、招聘类型明显冲突会扣分。");
        jTip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        jTip.setTextColor(Color.parseColor("#9CA3AF"));
        jTip.setLineSpacing(dp(3), 1f);
        jTip.setPadding(0, dp(6), 0, 0);
        jCard.addView(jTip);

        LinearLayout autoCard = card();
        root.addView(autoCard);
        autoCard.addView(label("自动打招呼"));

        cbSend = new CheckBox(this);
        cbSend.setText("达到分数线就自动发送");
        cbSend.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        cbSend.setChecked(cfg.autoSend);
        autoCard.addView(cbSend);

        LinearLayout numRow = new LinearLayout(this);
        numRow.setOrientation(LinearLayout.HORIZONTAL);
        autoCard.addView(numRow);

        thresholdEdit = numInput(cfg.threshold);
        limitEdit = numInput(cfg.dailyLimit);

        LinearLayout tWrap = new LinearLayout(this);
        tWrap.setOrientation(LinearLayout.VERTICAL);
        tWrap.addView(smallLabel("分数线（0-100）"));
        tWrap.addView(thresholdEdit);

        LinearLayout lWrap = new LinearLayout(this);
        lWrap.setOrientation(LinearLayout.VERTICAL);
        lWrap.addView(smallLabel("每天最多发几条"));
        lWrap.addView(limitEdit);

        LinearLayout.LayoutParams n1 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        LinearLayout.LayoutParams n2 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        n2.leftMargin = dp(10);
        numRow.addView(tWrap, n1);
        numRow.addView(lWrap, n2);

        cbLink = new CheckBox(this);
        cbLink.setText("自动抓岗位链接（会闪一下分享面板）");
        cbLink.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        cbLink.setPadding(0, dp(8), 0, 0);
        cbLink.setChecked(cfg.autoLink);
        autoCard.addView(cbLink);

        TextView aTip2 = new TextView(this);
        aTip2.setText("低于分数线的岗位只记进浏览记录，不会发送。");
        aTip2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        aTip2.setTextColor(Color.parseColor("#9CA3AF"));
        aTip2.setLineSpacing(dp(3), 1f);
        aTip2.setPadding(0, dp(8), 0, 0);
        autoCard.addView(aTip2);

        TextView saveBtn = primaryBtn("保存配置");
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { save(); }
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(4);
        root.addView(saveBtn, slp);

        TextView test = primaryBtn("测试接口连通性");
        test.setBackground(round(Color.parseColor("#F1F5F9"), 11));
        test.setTextColor(Color.parseColor("#334155"));
        test.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { testApi(); }
        });
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(10);
        root.addView(test, tlp);
    }

    /* ================= 帮助 ================= */

    private void buildHelp() {
        LinearLayout root = page();
        root.addView(title("怎么用"));

        LinearLayout c1 = card();
        root.addView(c1);
        c1.addView(label("正常流程"));
        c1.addView(step("1", "打开招聘 App，进到岗位详情页"));
        c1.addView(step("2", "点屏幕上的蓝色悬浮球「话」"));
        c1.addView(step("3", "等几秒，自动打分并生成开场白"));
        c1.addView(step("4", "够分数线就自动发，不够就只记进浏览记录"));

        LinearLayout c2 = card();
        root.addView(c2);
        c2.addView(label("岗位页太长怎么办"));
        TextView t2 = new TextView(this);
        t2.setText("结果面板上有「补充下一屏」。点它，球会变成橙色「续」，把页面往下滚，再点一次球，"
                + "新的一屏会追加进去，重复内容自动去掉。");
        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        t2.setTextColor(Color.parseColor("#4B5563"));
        t2.setLineSpacing(dp(4), 1f);
        c2.addView(t2);

        LinearLayout c3 = card();
        root.addView(c3);
        c3.addView(label("常见问题"));

        c3.addView(faq("面板写着「内置模板」是什么意思？",
                "说明没调用到大模型，用的是 App 自带的模板。去配置页检查接口地址、模型名、Key，点「测试接口连通性」看报什么错。"));
        c3.addView(faq("球突然不见了？",
                "系统把后台清了。回首页把「后台保活」三项都点一遍，再去任务列表长按卡片锁定。"));
        c3.addView(faq("自动发送没成功？",
                "话术会自动复制到剪贴板，长按输入框粘一下就行。反复失败把当时屏幕截图反馈。"));
        c3.addView(faq("记录里没有链接？",
                "分享面板里没找到「复制链接」，或者你关了「自动抓岗位链接」。记录照样存，只是点不开岗位。"));
        c3.addView(faq("数据会上传吗？",
                "不会。简历、记录都存在本机。只有点球时，岗位文字和简历会发给你自己配置的模型接口。"));
    }

    private View faq(String q, String a) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(10), 0, dp(10));

        TextView qt = new TextView(this);
        qt.setText(q);
        qt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        qt.setTextColor(Color.parseColor("#111827"));
        qt.setTypeface(null, android.graphics.Typeface.BOLD);
        box.addView(qt);

        TextView at = new TextView(this);
        at.setText(a);
        at.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        at.setTextColor(Color.parseColor("#6B7280"));
        at.setLineSpacing(dp(3), 1f);
        at.setPadding(0, dp(4), 0, 0);
        box.addView(at);

        return box;
    }

    /* ================= 控件工厂 ================= */

    private LinearLayout page() {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(16), dp(14), dp(16), dp(36));
        sv.addView(inner);
        builtPages.add(sv);
        return inner;
    }

    private View title(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        v.setTextColor(Color.parseColor("#111827"));
        v.setTypeface(null, android.graphics.Typeface.BOLD);
        v.setPadding(0, dp(2), 0, dp(12));
        return v;
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

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(round(Color.WHITE, 16));
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        c.setLayoutParams(lp);
        return c;
    }

    private TextView label(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        v.setTextColor(Color.parseColor("#2563EB"));
        v.setTypeface(null, android.graphics.Typeface.BOLD);
        v.setPadding(0, 0, 0, dp(8));
        return v;
    }

    private TextView smallLabel(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        v.setTextColor(Color.parseColor("#6B7280"));
        v.setPadding(0, dp(8), 0, dp(4));
        return v;
    }

    private EditText numInput(int val) {
        EditText e = new EditText(this);
        e.setText(String.valueOf(val));
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setBackground(round(Color.parseColor("#F8FAFC"), 10));
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        return e;
    }

    private EditText input(String hint, String value, boolean password) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        e.setSingleLine(true);
        e.setBackground(round(Color.parseColor("#F8FAFC"), 10));
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        if (password) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        return e;
    }

    private LinearLayout field(String name, EditText e) {
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        w.setLayoutParams(lp);
        TextView n = new TextView(this);
        n.setText(name);
        n.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        n.setTextColor(Color.parseColor("#6B7280"));
        n.setPadding(0, 0, 0, dp(4));
        w.addView(n);
        w.addView(e, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return w;
    }

    private TextView primaryBtn(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        v.setTextColor(Color.WHITE);
        v.setGravity(Gravity.CENTER);
        v.setTypeface(null, android.graphics.Typeface.BOLD);
        v.setBackground(round(Color.parseColor("#2563EB"), 11));
        v.setPadding(0, dp(13), 0, dp(13));
        return v;
    }

    private TextView secondaryBtn(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        v.setTextColor(Color.parseColor("#1D4ED8"));
        v.setGravity(Gravity.CENTER);
        v.setBackground(round(Color.parseColor("#EFF6FF"), 10));
        v.setPadding(0, dp(12), 0, dp(12));
        return v;
    }

    private View step(String n, String t) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(5), 0, dp(5));

        TextView num = new TextView(this);
        num.setText(n);
        num.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        num.setTextColor(Color.WHITE);
        num.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor("#2563EB"));
        num.setBackground(bg);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(dp(21), dp(21));
        nlp.rightMargin = dp(10);
        row.addView(num, nlp);

        TextView txt = new TextView(this);
        txt.setText(t);
        txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        txt.setTextColor(Color.parseColor("#374151"));
        row.addView(txt);
        return row;
    }

    /* ================= 逻辑 ================= */

    private void save() {
        cfg.url = urlEdit.getText().toString().trim();
        cfg.model = modelEdit.getText().toString().trim();
        cfg.key = keyEdit.getText().toString().trim();
        cfg.resume = resumeEdit.getText().toString().trim();

        String wTxt = wordEdit.getText().toString().trim();
        if (wTxt.isEmpty()) {
            cfg.words = 0;
        } else {
            try { cfg.words = Integer.parseInt(wTxt); } catch (Exception e) { cfg.words = 0; }
            if (cfg.words > 0 && cfg.words < 20) cfg.words = 20;
            if (cfg.words > 500) cfg.words = 500;
        }
        wordEdit.setText(cfg.words > 0 ? String.valueOf(cfg.words) : "");

        cfg.recruit = recruitEdit.getText().toString().trim();
        cfg.city = cityEdit.getText().toString().trim();
        cfg.salary = salaryEdit.getText().toString().trim();
        cfg.autoSend = cbSend.isChecked();
        cfg.autoLink = cbLink.isChecked();

        try { cfg.threshold = Integer.parseInt(thresholdEdit.getText().toString().trim()); }
        catch (Exception e) { cfg.threshold = 85; }
        if (cfg.threshold < 0) cfg.threshold = 0;
        if (cfg.threshold > 100) cfg.threshold = 100;
        thresholdEdit.setText(String.valueOf(cfg.threshold));

        try { cfg.dailyLimit = Integer.parseInt(limitEdit.getText().toString().trim()); }
        catch (Exception e) { cfg.dailyLimit = 5; }
        if (cfg.dailyLimit < 0) cfg.dailyLimit = 0;
        if (cfg.dailyLimit > 50) cfg.dailyLimit = 50;
        limitEdit.setText(String.valueOf(cfg.dailyLimit));

        cfg.save(this);
        toast("已保存");
    }

    private void testApi() {
        save();
        if (!cfg.ready()) { toast("接口地址、模型名、Key 要填完整"); return; }
        toast("测试中…");
        new Thread(new Runnable() {
            @Override public void run() {
                String msg;
                try {
                    Llm.Out out = Llm.generate(cfg, "Java 开发工程师，要求熟悉 Spring Boot 与 MySQL。", cfg.words);
                    msg = "连通正常，返回 " + out.text.length() + " 字"
                            + (out.score >= 0 ? "，适配度 " + out.score + "%" : "");
                } catch (Exception e) {
                    String m = e.getMessage();
                    msg = "失败：" + (m != null && m.length() > 120 ? m.substring(0, 120) : m);
                }
                final String f = msg;
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("接口测试")
                                .setMessage(f)
                                .setPositiveButton("知道了", null)
                                .show();
                    }
                });
            }
        }).start();
    }

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "text/plain",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/msword",
                "application/pdf"
        });
        try { startActivityForResult(i, REQ_FILE); }
        catch (Exception e) { toast("打不开文件选择器"); }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_FILE || res != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        String text = readDoc(uri);
        if (text == null) return;
        if (text.trim().isEmpty()) { toast("没从这个文件里读到文字"); return; }
        resumeEdit.setText(text.trim());
        toast("已导入 " + text.trim().length() + " 字");
    }

    private String readDoc(Uri uri) {
        String name = "";
        try {
            android.database.Cursor c = getContentResolver().query(uri, null, null, null, null);
            if (c != null) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0 && c.moveToFirst()) name = c.getString(idx);
                c.close();
            }
        } catch (Exception ignored) { }

        String lower = name == null ? "" : name.toLowerCase();
        try {
            if (lower.endsWith(".docx")) return readDocx(uri);
            if (lower.endsWith(".pdf")) { toast("PDF 读不了，用 WPS 打开复制文本粘贴"); return null; }
            if (lower.endsWith(".doc")) { toast("老版 .doc 读不了，另存为 .docx 或直接粘贴"); return null; }
            return readText(uri);
        } catch (Exception e) {
            toast("读取失败：" + e.getMessage());
            return null;
        }
    }

    private String readText(Uri uri) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getContentResolver().openInputStream(uri);
             BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private String readDocx(Uri uri) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(getContentResolver().openInputStream(uri))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if ("word/document.xml".equals(e.getName())) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = zis.read(buf)) > 0) bos.write(buf, 0, n);
                    String xml = new String(bos.toByteArray(), "UTF-8");
                    xml = xml.replaceAll("</w:p>", "\n").replaceAll("<w:br[^>]*/>", "\n");
                    xml = xml.replaceAll("<[^>]+>", "");
                    return xml.replace("&amp;", "&").replace("&lt;", "<")
                            .replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'");
                }
            }
        }
        return "";
    }

    private void refreshStatus() {
        if (statusView == null) return;
        boolean on = isAccessibilityOn();
        if (on) {
            statusView.setText("已开启");
            statusView.setTextColor(Color.parseColor("#16A34A"));
            btnOpen.setText("已开启，去招聘 App 试试");
        } else {
            statusView.setText("未开启");
            statusView.setTextColor(Color.parseColor("#DC2626"));
            btnOpen.setText("去开启无障碍服务");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (curPage == 0) refreshStatus();
    }

    private boolean isAccessibilityOn() {
        String want = new ComponentName(this, ReaderService.class).flattenToString();
        String enabled = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) return false;
        for (String s : enabled.split(":")) if (s.equalsIgnoreCase(want)) return true;
        return false;
    }

    private void askBattery() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
                toast("已经忽略电池优化了");
                return;
            }
            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception e2) {
                toast("这个系统打不开电池优化设置");
            }
        }
    }

    private void askOverlay() {
        try {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Exception e) {
            toast("这个系统打不开悬浮窗设置");
        }
    }

    private void askAutoStart() {
        String[][] list = {
                {"com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"},
                {"com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"},
                {"com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"},
                {"com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"},
                {"com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"},
                {"com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"},
                {"com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"},
                {"com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"},
                {"com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"}
        };
        for (String[] kv : list) {
            try {
                Intent i = new Intent();
                i.setComponent(new ComponentName(kv[0], kv[1]));
                if (getPackageManager().resolveActivity(i, 0) != null) {
                    startActivity(i);
                    return;
                }
            } catch (Exception ignored) { }
        }
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
            toast("没找到自启动页面，请在应用详情里手动允许后台活动");
        } catch (Exception e) {
            toast("打不开设置");
        }
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
