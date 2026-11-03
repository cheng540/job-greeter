package com.liule.talkhelper;

import android.app.Activity;
import android.content.ClipboardManager;
import android.os.Bundle;

/**
 * 透明的剪贴板读取窗口。
 * Android 10 起后台服务读不到剪贴板，必须有一个有焦点的界面读一次，读完立刻关掉。
 */
public class ClipGrabActivity extends Activity {

    public static volatile String last = null;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        last = null;
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null
                    && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence t = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
                if (t != null) last = t.toString().trim();
            }
        } catch (Exception ignored) {
        }
        finish();
        overridePendingTransition(0, 0);
    }
}
