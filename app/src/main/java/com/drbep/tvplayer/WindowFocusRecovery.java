package com.drbep.tvplayer;

import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

final class WindowFocusRecovery {
    static boolean navigationKey(int code) {
        return code==KeyEvent.KEYCODE_DPAD_UP || code==KeyEvent.KEYCODE_DPAD_DOWN
                || code==KeyEvent.KEYCODE_DPAD_LEFT || code==KeyEvent.KEYCODE_DPAD_RIGHT;
    }
    static boolean recover(Window window, View fallback) {
        if(window==null)return false;
        View root=window.getDecorView();
        if(!root.hasWindowFocus() || root.isInTouchMode())return false;
        View focused=root.findFocus();
        if(focused!=null && focused!=root && focused.isShown() && focused.isEnabled())return false;
        if(fallback!=null && fallback.isAttachedToWindow() && fallback.isShown() && fallback.isEnabled()
                && fallback.requestFocus())return true;
        if(root instanceof ViewGroup) {
            for(View child:((ViewGroup)root).getFocusables(View.FOCUS_FORWARD))
                if(child!=root && child.isShown() && child.isEnabled() && child.requestFocus())return true;
        }
        return false;
    }
}
