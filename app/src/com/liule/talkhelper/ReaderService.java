package com.liule.talkhelper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 核心服务：悬浮球 + 读屏 + 打分生成 + 记录。
 * 流程为「先攒后生成」：点球只采集当前屏，攒几屏由用户决定，点「开始生成」才调模型。
 */
public class ReaderService extends AccessibilityService {

    private WindowManager wm;
    private View ball;
    private WindowManager.LayoutParams ballLp;
    private View panel;
    private TextView panelMeta, panelBody, panelAction, panelScore, panelReason;
    private ScrollView panelScroll;

    private final Handler ui = new Handler(Looper.getMainLooper());

    private int seed = 0;
    private String lastRaw = "";
    private String lastText = "";
    private boolean busy = false;
    private boolean appendMode = false;
    private int screens = 1;

    private String shownMeta = "";
    private int shownScore = -1;
    private String shownReason = "";

    private static final int MODE_COLLECT = 0;
    private static final int MODE_RESULT = 1;
    private int panelMode = MODE_COLLECT;

    private static final String[] SKIP = {
            "返回", "分享", "收藏", "举报", "更多", "搜索", "首页", "消息", "我的",
            "立即沟通", "继续沟通", "打招呼", "确定", "取消", "关闭"
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        addBall();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() { }

    @Override
    public boolean onUnbind(Intent intent) {
        removeBall();
        hidePanel();
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        if (wm == null) wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (ball == null) addBall();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeBall();
        hidePanel();
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

    /* ================= 悬浮球 ================= */

    private void addBall() {
        if (ball != null) return;

        TextView tv = new TextView(this);
        tv.setText("话");
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(null, Typeface.BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor("#2563EB"));
        bg.setStroke(dp(2), Color.parseColor("#BFDBFE"));
        tv.setBackground(bg);
        tv.setElevation(dp(6));

        ballLp = new WindowManager.LayoutParams(
                dp(52), dp(52),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        ballLp.gravity = Gravity.TOP | Gravity.START;
        ballLp.x = 0;
        ballLp.y = dp(260);

        tv.setOnTouchListener(new View.OnTouchListener() {
            float dx0, dy0;
            int x0, y0;
            boolean moved;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dx0 = e.getRawX();
                        dy0 = e.getRawY();
                        x0 = ballLp.x;
                        y0 = ballLp.y;
                        moved = false;
                        v.setAlpha(0.75f);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (e.getRawX() - dx0);
                        int dy = (int) (e.getRawY() - dy0);
                        if (Math.abs(dx) > dp(6) || Math.abs(dy) > dp(6)) moved = true;
                        if (moved) {
                            ballLp.x = x0 + dx;
                            ballLp.y = y0 + dy;
                            wm.updateViewLayout(ball, ballLp);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        v.setAlpha(1f);
                        if (!moved) trigger();
                        return true;
                }
                return false;
            }
        });

        ball = tv;
        wm.addView(ball, ballLp);
    }

    private void removeBall() {
        if (ball != null) {
            try { wm.removeView(ball); } catch (Exception ignored) { }
            ball = null;
        }
    }

    /** 悬浮球状态：蓝色「话」= 开新采集；橙色「续」= 下一屏追加进当前采集 */
    private void setAppendMode(boolean on) {
        appendMode = on;
        if (on) panelMode = MODE_COLLECT;
        if (ball instanceof TextView) {
            TextView tv = (TextView) ball;
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(Color.parseColor(on ? "#EA580C" : "#2563EB"));
            d.setStroke(dp(2), Color.parseColor(on ? "#FED7AA" : "#BFDBFE"));
            tv.setBackground(d);
            tv.setText(on ? "续" : "话");
        }
        if (on) toast("滚到下一屏再点球，攒好后点「开始生成」");
    }

    /* ================= 主流程：先攒后生成 ================= */

    private void trigger() {
        if (busy) { toast("正在生成中，等几秒再点"); return; }
        hidePanel();
        final boolean appending = appendMode;
        if (appending) toast("读取中…");
        // 延迟再读：滚动后系统的无障碍节点树有更新延迟，立刻读会拿到滚动前的缓存快照
        int delay = appending ? 500 : 150;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override public void run() { doTrigger(appending); }
        }, delay);
    }

    private void doTrigger(boolean appending) {
        String raw = readScreen();
        boolean thin = raw.replaceAll("\\s", "").length() < 15;

        if (appending) {
            if (thin) {
                toast("这一屏没读到内容，等页面加载完再点一次");
                setAppendMode(true);
                return;
            }
            int fresh = countNewLines(raw, lastRaw);
            if (fresh < 3) {
                // 读回来的内容几乎都在上一屏里 → 页面没滚动到位或又读到了旧缓存
                toast("这一屏和前面基本一样（新增 " + fresh + " 行）。多滚一段距离，停顿半秒再点球");
                setAppendMode(true);
                return;
            }
            lastRaw = merge(lastRaw, raw);
            screens++;
            setAppendMode(false);
        } else {
            if (thin) {
                toast("没读到内容，等页面加载完再点一次");
                return;
            }
            lastRaw = raw;
            screens = 1;
        }
        panelMode = MODE_COLLECT;
        showPanel();
    }

    /** 规范化：去掉所有空白，用于模糊去重 */
    private static String norm(String s) {
        return s.replaceAll("\\s+", "");
    }

    /** 统计 newText 里有多少行是 oldText 没有的（模糊匹配） */
    private int countNewLines(String newText, String oldText) {
        Set<String> have = new HashSet<String>();
        for (String l : oldText.split("\n")) {
            String t = norm(l);
            if (!t.isEmpty()) have.add(t);
        }
        int c = 0;
        for (String l : newText.split("\n")) {
            String t = norm(l);
            if (t.length() < 2) continue;
            if (!have.contains(t)) c++;
        }
        return c;
    }

    /** 把新读到的一屏并进已有文本，重复行只保留一份 */
    private String merge(String oldText, String newText) {
        Set<String> have = new HashSet<String>();
        for (String l : oldText.split("\n")) {
            String t = norm(l);
            if (!t.isEmpty()) have.add(t);
        }
        StringBuilder sb = new StringBuilder(oldText);
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') sb.append('\n');
        for (String l : newText.split("\n")) {
            String t = norm(l);
            if (t.length() < 2) continue;
            if (have.contains(t)) continue;
            have.add(t);
            sb.append(t).append('\n');
        }
        if (sb.length() > 12000) sb.setLength(12000);
        return sb.toString();
    }

    private void startGenerate() {
        if (busy) { toast("正在生成中，别急"); return; }
        final Cfg cfg = Cfg.load(this);
        busy = true;
        panelMode = MODE_RESULT;
        setBody("正在生成…");
        final boolean configured = cfg.ready();
        setMeta(configured ? "调用 " + cfg.model + " …" : "未配置 API，先用内置模板");
        setScore(-1, "");
        panelAction.setEnabled(false);
        panelAction.setAlpha(0.6f);

        final String jd = lastRaw;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    genBody(cfg, jd);
                } catch (final Throwable t) {
                    // 兜底：生成过程任何崩溃都不能把 busy 卡死，否则悬浮球从此永远没反应
                    ui.post(new Runnable() {
                        @Override public void run() {
                            busy = false;
                            toast("生成出错：" + shortMsg(String.valueOf(t)));
                            panelAction.setEnabled(true);
                            panelAction.setAlpha(1f);
                        }
                    });
                }
            }
        }).start();
    }

    private void genBody(final Cfg cfg, final String jd) {
        {
                String url = "";
                if (cfg.autoLink) {
                    try {
                        url = grabLink();
                    } catch (Exception ignored) {
                    }
                }

                String out = null;
                String reason = "";
                int score = -1;
                String err = null;
                final boolean configured = cfg.ready();
                if (configured) {
                    try {
                        Llm.Out lo = Llm.generate(cfg, jd, cfg.words);
                        out = lo.text;
                        score = lo.score;
                        reason = lo.reason;
                    } catch (Exception e) {
                        err = e.getMessage();
                    }
                }
                boolean usedLlm = configured && err == null && out != null && !out.trim().isEmpty();
                if (out == null || out.trim().isEmpty()) {
                    TalkGen.Result r = TalkGen.parse(jd);
                    out = TalkGen.build(r, r.type, seed, cfg.resume);
                    score = -1;
                    reason = "";
                    if (err != null) {
                        final String e2 = err;
                        ui.post(new Runnable() {
                            @Override public void run() {
                                toast("AI 调用失败，已用内置模板：" + shortMsg(e2));
                            }
                        });
                    }
                }

                TalkGen.Result pj = TalkGen.parse(jd);
                Db.Rec rec = new Db.Rec();
                rec.ts = System.currentTimeMillis();
                rec.company = pj.company;
                rec.title = pj.title;
                rec.salary = pj.salary;
                rec.score = usedLlm ? score : -1;
                rec.reason = usedLlm ? reason : "";
                rec.talk = out;
                rec.url = url;
                rec.sent = false;

                Db db = new Db(ReaderService.this);
                if (!db.existsSame(rec.company, rec.title, 90000)) {
                    db.add(rec);
                }

                final String text = out;
                final int fScore = score;
                final String fReason = reason;
                final boolean fUsed = usedLlm;
                final boolean fConfigured = configured;

                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        busy = false;
                        lastText = text;
                        copy(text);
                        panelMode = MODE_RESULT;
                        String sc = screens > 1 ? " · 已读 " + screens + " 屏" : "";
                        String wt = cfg.words > 0 ? " · 目标 " + cfg.words : "";
                        shownMeta = (fUsed ? "AI 生成 · " + text.length() + " 字" + wt
                                : "内置模板 · " + text.length() + " 字") + sc;
                        shownScore = fUsed ? fScore : -1;
                        shownReason = fReason;
                        if (appendMode) {
                            toast("已生成并复制（" + fScore + " 分），攒完屏点「开始生成」");
                        } else {
                            showPanel();
                        }
                        if (!fConfigured) toast("还没配 API，去 App 里填好地址/模型/Key");
                    }
                });
        }
    }

    /* ================= 读屏 ================= */

    private String readScreen() {
        Set<String> out = new LinkedHashSet<String>();

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            try { root.refresh(); } catch (Exception ignored) { }   // 强制重新取节点，避免读到滚动前缓存
            walk(root, out, 0);
        }

        if (out.size() < 5) {
            List<AccessibilityWindowInfo> wins = getWindows();
            if (wins != null) {
                for (AccessibilityWindowInfo w : wins) {
                    AccessibilityNodeInfo r = w.getRoot();
                    if (r != null) walk(r, out, 0);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (String s : out) sb.append(s).append('\n');
        return sb.toString();
    }

    private void walk(AccessibilityNodeInfo n, Set<String> out, int depth) {
        if (n == null || depth > 60 || out.size() > 600) return;
        CharSequence t = n.getText();
        if (t != null) addClean(out, t.toString());
        CharSequence d = n.getContentDescription();
        if (d != null) addClean(out, d.toString());
        int c = n.getChildCount();
        for (int i = 0; i < c; i++) {
            AccessibilityNodeInfo ch = n.getChild(i);
            if (ch != null) walk(ch, out, depth + 1);
        }
    }

    private void addClean(Set<String> out, String s) {
        String v = s.trim();
        if (v.length() < 2) return;
        if (v.length() > 400) v = v.substring(0, 400);
        for (String skip : SKIP) if (skip.equals(v)) return;
        out.add(v);
    }

    /* ================= 面板 ================= */

    private void showPanel() {
        boolean collect = (panelMode == MODE_COLLECT);   // 必须在 hidePanel 之前取，hidePanel 不再改写模式
        hidePanel();

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(round(Color.WHITE, 16));
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.setElevation(dp(10));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(titleRow);

        TextView title = new TextView(this);
        title.setText(collect ? ("已读第 " + screens + " 屏") : "开场白");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTextColor(Color.parseColor("#111827"));
        title.setTypeface(null, Typeface.BOLD);
        titleRow.addView(title);

        panelScore = new TextView(this);
        panelScore.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        panelScore.setTypeface(null, Typeface.BOLD);
        panelScore.setGravity(Gravity.CENTER);
        panelScore.setPadding(dp(9), dp(2), dp(9), dp(2));
        panelScore.setVisibility(View.GONE);
        LinearLayout.LayoutParams psLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        psLp.leftMargin = dp(10);
        titleRow.addView(panelScore, psLp);

        panelMeta = new TextView(this);
        panelMeta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        panelMeta.setTextColor(Color.parseColor("#9CA3AF"));
        panelMeta.setPadding(0, dp(3), 0, dp(6));
        if (collect) {
            panelMeta.setText("攒够几屏了点「开始生成」，中途不消耗模型额度");
        }
        box.addView(panelMeta);

        panelReason = new TextView(this);
        panelReason.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        panelReason.setTextColor(Color.parseColor("#94A3B8"));
        panelReason.setLineSpacing(dp(2), 1f);
        panelReason.setPadding(0, 0, 0, dp(8));
        panelReason.setVisibility(View.GONE);
        box.addView(panelReason);

        panelBody = new TextView(this);
        panelBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        panelBody.setTextColor(Color.parseColor("#1F2937"));
        panelBody.setLineSpacing(dp(5), 1f);
        panelBody.setTextIsSelectable(true);
        panelBody.setText(collect ? lastRaw : lastText);

        panelScroll = new ScrollView(this);
        panelScroll.setBackground(round(Color.parseColor("#F8FAFC"), 10));
        panelScroll.setPadding(dp(10), dp(8), dp(10), dp(8));
        panelScroll.addView(panelBody);
        box.addView(panelScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        panelAction = new TextView(this);
        panelAction.setText(collect ? "开始生成" : "复制到剪贴板");
        panelAction.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        panelAction.setTextColor(Color.WHITE);
        panelAction.setGravity(Gravity.CENTER);
        panelAction.setTypeface(null, Typeface.BOLD);
        panelAction.setBackground(round(Color.parseColor(
                collect ? "#16A34A" : "#2563EB"), 10));
        panelAction.setPadding(0, dp(11), 0, dp(11));
        LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        aLp.topMargin = dp(10);
        box.addView(panelAction, aLp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rLp.topMargin = dp(8);
        box.addView(row, rLp);

        if (collect) {
            TextView more = smallBtn("补充下一屏", "#FFF7ED", "#C2410C");
            more.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { setAppendMode(true); hidePanel(); }
            });
            row.addView(more, weightLp());

            TextView close = smallBtn("关闭", "#FFFFFF", "#9CA3AF");
            close.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { hidePanel(); }
            });
            LinearLayout.LayoutParams clp = weightLp();
            clp.leftMargin = dp(8);
            row.addView(close, clp);

            panelAction.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { startGenerate(); }
            });
        } else {
            TextView again = smallBtn("重写", "#F1F5F9", "#334155");
            again.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (busy || lastRaw.isEmpty()) return;
                    seed++;
                    startGenerate();
                }
            });
            row.addView(again, weightLp());

            TextView fill = smallBtn("填入输入框", "#EFF6FF", "#1D4ED8");
            fill.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (lastText.isEmpty()) return;
                    if (fillInput(lastText)) { hidePanel(); toast("已填入"); }
                    else { copy(lastText); toast("这个输入框不支持直接填，已复制，长按粘贴"); }
                }
            });
            LinearLayout.LayoutParams flp = weightLp();
            flp.leftMargin = dp(8);
            row.addView(fill, flp);

            TextView more = smallBtn("补充下一屏", "#FFF7ED", "#C2410C");
            more.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { setAppendMode(true); hidePanel(); }
            });
            LinearLayout.LayoutParams mlp = weightLp();
            mlp.leftMargin = dp(8);
            row.addView(more, mlp);

            TextView close = smallBtn("关闭", "#FFFFFF", "#9CA3AF");
            close.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { hidePanel(); }
            });
            LinearLayout.LayoutParams clp = weightLp();
            clp.leftMargin = dp(8);
            row.addView(close, clp);

            panelAction.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    copy(lastText);
                    toast("已复制，回聊天框长按粘贴");
                }
            });
        }

        setMeta(collect ? "攒够几屏了点「开始生成」，中途不消耗模型额度" : shownMeta);
        setScore(collect ? -1 : shownScore, collect ? "" : shownReason);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                getResources().getDisplayMetrics().widthPixels - dp(32), dp(430),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = dp(80);

        panel = box;
        wm.addView(panel, lp);
    }

    private LinearLayout.LayoutParams weightLp() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    }

    private TextView smallBtn(String text, String bg, String fg) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        tv.setTextColor(Color.parseColor(fg));
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(round(Color.parseColor(bg), 9));
        tv.setPadding(0, dp(10), 0, dp(10));
        return tv;
    }

    private void setBody(String s) {
        if (panelBody != null) panelBody.setText(s);
        if (panelScroll != null) {
            panelScroll.post(new Runnable() {
                @Override public void run() { panelScroll.scrollTo(0, 0); }
            });
        }
    }

    private void setMeta(String s) {
        if (panelMeta != null) panelMeta.setText(s);
    }

    private void setScore(int score, String reason) {
        if (panelScore == null) return;
        if (score < 0) {
            panelScore.setVisibility(View.GONE);
        } else {
            int fg, bg;
            if (score >= 75) { fg = Color.parseColor("#15803D"); bg = Color.parseColor("#DCFCE7"); }
            else if (score >= 50) { fg = Color.parseColor("#B45309"); bg = Color.parseColor("#FEF3C7"); }
            else { fg = Color.parseColor("#B91C1C"); bg = Color.parseColor("#FEE2E2"); }
            panelScore.setText("适配度 " + score + "%");
            panelScore.setTextColor(fg);
            panelScore.setBackground(round(bg, 999));
            panelScore.setVisibility(View.VISIBLE);
        }
        if (panelReason != null) {
            if (reason == null || reason.trim().isEmpty()) {
                panelReason.setVisibility(View.GONE);
            } else {
                panelReason.setText(reason.trim());
                panelReason.setVisibility(View.VISIBLE);
            }
        }
    }

    private void hidePanel() {
        if (panel != null) {
            try { wm.removeView(panel); } catch (Exception ignored) { }
            panel = null;
            panelScroll = null;
            panelBody = null;
            panelMeta = null;
            panelAction = null;
            panelScore = null;
            panelReason = null;
        }
    }

    /* ================= 抓链接 ================= */

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    private void back() {
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    /** 走一遍「分享 → 复制链接」把岗位链接抓下来，失败返回空串 */
    private String grabLink() {
        if (!clickLabel(new String[]{"分享"}, new String[]{"分享"})) return "";
        sleep(900);
        if (!clickLabel(new String[]{"复制链接", "复制链接地址"}, null)) {
            back();
            sleep(300);
            return "";
        }
        sleep(700);
        String url = readClipViaActivity();
        back();
        sleep(300);
        if (url == null) return "";
        url = url.trim();
        if (!url.startsWith("http")) return "";
        return url;
    }

    private String readClipViaActivity() {
        try {
            ClipGrabActivity.last = null;
            Intent i = new Intent(this, ClipGrabActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(i);
        } catch (Exception e) {
            return null;
        }
        for (int k = 0; k < 20; k++) {
            sleep(100);
            if (ClipGrabActivity.last != null) break;
        }
        return ClipGrabActivity.last;
    }

    private boolean clickLabel(String[] texts, String[] descs) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        AccessibilityNodeInfo n = findLabel(root, texts, descs, 0);
        if (n == null) return false;
        AccessibilityNodeInfo t = n;
        int guard = 0;
        while (t != null && !t.isClickable() && guard < 6) {
            t = t.getParent();
            guard++;
        }
        if (t == null) t = n;
        return t.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    private AccessibilityNodeInfo findLabel(AccessibilityNodeInfo n, String[] texts,
                                            String[] descs, int d) {
        if (n == null || d > 40) return null;
        CharSequence tx = n.getText();
        if (tx != null && matchAny(tx.toString(), texts)) return n;
        CharSequence cd = n.getContentDescription();
        if (cd != null) {
            if (matchAny(cd.toString(), texts)) return n;
            if (descs != null && matchAny(cd.toString(), descs)) return n;
        }
        int c = n.getChildCount();
        for (int i = 0; i < c; i++) {
            AccessibilityNodeInfo r = findLabel(n.getChild(i), texts, descs, d + 1);
            if (r != null) return r;
        }
        return null;
    }

    private boolean matchAny(String s, String[] arr) {
        if (s == null || arr == null) return false;
        String v = s.trim();
        if (v.isEmpty()) return false;
        for (String a : arr) {
            if (v.equals(a) || v.contains(a)) return true;
        }
        return false;
    }

    /* ================= 复制 / 填入 ================= */

    private void copy(String text) {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("开场白", text));
        } catch (Exception e) {
            toast("复制失败：" + e.getMessage());
        }
    }

    private boolean fillInput(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        List<AccessibilityNodeInfo> found = new ArrayList<AccessibilityNodeInfo>();
        findEditable(root, found, 0);
        if (found.isEmpty()) return false;
        AccessibilityNodeInfo target = found.get(0);
        for (AccessibilityNodeInfo n : found) if (n.isFocused()) { target = n; break; }
        target.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        Bundle b = new Bundle();
        b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        return target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b);
    }

    private void findEditable(AccessibilityNodeInfo n, List<AccessibilityNodeInfo> out, int depth) {
        if (n == null || depth > 40 || out.size() > 20) return;
        if (n.isEditable() && n.isVisibleToUser()) out.add(n);
        int c = n.getChildCount();
        for (int i = 0; i < c; i++) {
            AccessibilityNodeInfo ch = n.getChild(i);
            if (ch != null) findEditable(ch, out, depth + 1);
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private String shortMsg(String s) {
        if (s == null) return "";
        return s.length() > 70 ? s.substring(0, 70) + "…" : s;
    }
}
