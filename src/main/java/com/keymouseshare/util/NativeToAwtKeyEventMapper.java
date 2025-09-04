package com.keymouseshare.util;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

import java.awt.*;
import java.awt.event.KeyEvent;

public class NativeToAwtKeyEventMapper {

    /**
     * 将 NativeKeyEvent 转换为 AWT KeyEvent
     */
    public static KeyEvent toAwtKeyEvent(Component source, NativeKeyEvent nativeEvent) {
        int id = toAwtEventType(nativeEvent.getID());
        int keyCode = toAwtKeyCode(nativeEvent.getKeyCode());
        char keyChar = nativeEvent.getKeyChar();
        int location = toAwtKeyLocation(nativeEvent.getKeyLocation());
        int modifiers = toAwtModifiers(nativeEvent.getModifiers());
        long when = System.currentTimeMillis();

        return new KeyEvent(source, id, when, modifiers, keyCode, keyChar, location);
    }

    /** 类型映射 */
    public static int toAwtEventType(int nativeType) {
        switch (nativeType) {
            case NativeKeyEvent.NATIVE_KEY_PRESSED: return KeyEvent.KEY_PRESSED;
            case NativeKeyEvent.NATIVE_KEY_RELEASED: return KeyEvent.KEY_RELEASED;
            case NativeKeyEvent.NATIVE_KEY_TYPED: return KeyEvent.KEY_TYPED;
            default: return KeyEvent.KEY_PRESSED;
        }
    }

    /** 键位位置映射 */
    public static int toAwtKeyLocation(int nativeLocation) {
        switch (nativeLocation) {
            case NativeKeyEvent.KEY_LOCATION_LEFT: return KeyEvent.KEY_LOCATION_LEFT;
            case NativeKeyEvent.KEY_LOCATION_RIGHT: return KeyEvent.KEY_LOCATION_RIGHT;
            case NativeKeyEvent.KEY_LOCATION_NUMPAD: return KeyEvent.KEY_LOCATION_NUMPAD;
            case NativeKeyEvent.KEY_LOCATION_STANDARD: return KeyEvent.KEY_LOCATION_STANDARD;
            default: return KeyEvent.KEY_LOCATION_UNKNOWN;
        }
    }

    /** 修饰键映射 */
    public static int toAwtModifiers(int nativeMods) {
        int mods = 0;
        if ((nativeMods & NativeKeyEvent.SHIFT_MASK) != 0) mods |= KeyEvent.SHIFT_DOWN_MASK;
        if ((nativeMods & NativeKeyEvent.CTRL_MASK) != 0) mods |= KeyEvent.CTRL_DOWN_MASK;
        if ((nativeMods & NativeKeyEvent.ALT_MASK) != 0) mods |= KeyEvent.ALT_DOWN_MASK;
        if ((nativeMods & NativeKeyEvent.META_MASK) != 0) mods |= KeyEvent.META_DOWN_MASK;
        return mods;
    }

    /** 核心：键码映射 */
    public static int toAwtKeyCode(int nativeKeyCode) {
        switch (nativeKeyCode) {
            // --- 控制键 ---
            case NativeKeyEvent.VC_ESCAPE: return KeyEvent.VK_ESCAPE;
            case NativeKeyEvent.VC_TAB: return KeyEvent.VK_TAB;
            case NativeKeyEvent.VC_CAPS_LOCK: return KeyEvent.VK_CAPS_LOCK;
            case NativeKeyEvent.VC_SHIFT: return KeyEvent.VK_SHIFT;
            case NativeKeyEvent.VC_CONTROL: return KeyEvent.VK_CONTROL;
            case NativeKeyEvent.VC_ALT: return KeyEvent.VK_ALT;
            case NativeKeyEvent.VC_META: return KeyEvent.VK_META;
            case NativeKeyEvent.VC_CONTEXT_MENU: return KeyEvent.VK_CONTEXT_MENU;
            case NativeKeyEvent.VC_SPACE: return KeyEvent.VK_SPACE;
            case NativeKeyEvent.VC_ENTER: return KeyEvent.VK_ENTER;
            case NativeKeyEvent.VC_BACKSPACE: return KeyEvent.VK_BACK_SPACE;
            case NativeKeyEvent.VC_DELETE: return KeyEvent.VK_DELETE;
            case NativeKeyEvent.VC_INSERT: return KeyEvent.VK_INSERT;
            case NativeKeyEvent.VC_HOME: return KeyEvent.VK_HOME;
            case NativeKeyEvent.VC_END: return KeyEvent.VK_END;
            case NativeKeyEvent.VC_PAGE_UP: return KeyEvent.VK_PAGE_UP;
            case NativeKeyEvent.VC_PAGE_DOWN: return KeyEvent.VK_PAGE_DOWN;

            // --- 方向键 ---
            case NativeKeyEvent.VC_UP: return KeyEvent.VK_UP;
            case NativeKeyEvent.VC_DOWN: return KeyEvent.VK_DOWN;
            case NativeKeyEvent.VC_LEFT: return KeyEvent.VK_LEFT;
            case NativeKeyEvent.VC_RIGHT: return KeyEvent.VK_RIGHT;

            // --- F1-F24 ---
            case NativeKeyEvent.VC_F1: return KeyEvent.VK_F1;
            case NativeKeyEvent.VC_F2: return KeyEvent.VK_F2;
            case NativeKeyEvent.VC_F3: return KeyEvent.VK_F3;
            case NativeKeyEvent.VC_F4: return KeyEvent.VK_F4;
            case NativeKeyEvent.VC_F5: return KeyEvent.VK_F5;
            case NativeKeyEvent.VC_F6: return KeyEvent.VK_F6;
            case NativeKeyEvent.VC_F7: return KeyEvent.VK_F7;
            case NativeKeyEvent.VC_F8: return KeyEvent.VK_F8;
            case NativeKeyEvent.VC_F9: return KeyEvent.VK_F9;
            case NativeKeyEvent.VC_F10: return KeyEvent.VK_F10;
            case NativeKeyEvent.VC_F11: return KeyEvent.VK_F11;
            case NativeKeyEvent.VC_F12: return KeyEvent.VK_F12;
            case NativeKeyEvent.VC_F13: return KeyEvent.VK_F13;
            case NativeKeyEvent.VC_F14: return KeyEvent.VK_F14;
            case NativeKeyEvent.VC_F15: return KeyEvent.VK_F15;
            case NativeKeyEvent.VC_F16: return KeyEvent.VK_F16;
            case NativeKeyEvent.VC_F17: return KeyEvent.VK_F17;
            case NativeKeyEvent.VC_F18: return KeyEvent.VK_F18;
            case NativeKeyEvent.VC_F19: return KeyEvent.VK_F19;
            case NativeKeyEvent.VC_F20: return KeyEvent.VK_F20;
            case NativeKeyEvent.VC_F21: return KeyEvent.VK_F21;
            case NativeKeyEvent.VC_F22: return KeyEvent.VK_F22;
            case NativeKeyEvent.VC_F23: return KeyEvent.VK_F23;
            case NativeKeyEvent.VC_F24: return KeyEvent.VK_F24;

            // --- 字母键 ---
            case NativeKeyEvent.VC_A: return KeyEvent.VK_A;
            case NativeKeyEvent.VC_B: return KeyEvent.VK_B;
            case NativeKeyEvent.VC_C: return KeyEvent.VK_C;
            case NativeKeyEvent.VC_D: return KeyEvent.VK_D;
            case NativeKeyEvent.VC_E: return KeyEvent.VK_E;
            case NativeKeyEvent.VC_F: return KeyEvent.VK_F;
            case NativeKeyEvent.VC_G: return KeyEvent.VK_G;
            case NativeKeyEvent.VC_H: return KeyEvent.VK_H;
            case NativeKeyEvent.VC_I: return KeyEvent.VK_I;
            case NativeKeyEvent.VC_J: return KeyEvent.VK_J;
            case NativeKeyEvent.VC_K: return KeyEvent.VK_K;
            case NativeKeyEvent.VC_L: return KeyEvent.VK_L;
            case NativeKeyEvent.VC_M: return KeyEvent.VK_M;
            case NativeKeyEvent.VC_N: return KeyEvent.VK_N;
            case NativeKeyEvent.VC_O: return KeyEvent.VK_O;
            case NativeKeyEvent.VC_P: return KeyEvent.VK_P;
            case NativeKeyEvent.VC_Q: return KeyEvent.VK_Q;
            case NativeKeyEvent.VC_R: return KeyEvent.VK_R;
            case NativeKeyEvent.VC_S: return KeyEvent.VK_S;
            case NativeKeyEvent.VC_T: return KeyEvent.VK_T;
            case NativeKeyEvent.VC_U: return KeyEvent.VK_U;
            case NativeKeyEvent.VC_V: return KeyEvent.VK_V;
            case NativeKeyEvent.VC_W: return KeyEvent.VK_W;
            case NativeKeyEvent.VC_X: return KeyEvent.VK_X;
            case NativeKeyEvent.VC_Y: return KeyEvent.VK_Y;
            case NativeKeyEvent.VC_Z: return KeyEvent.VK_Z;

            // --- 数字键 ---
            case NativeKeyEvent.VC_0: return KeyEvent.VK_0;
            case NativeKeyEvent.VC_1: return KeyEvent.VK_1;
            case NativeKeyEvent.VC_2: return KeyEvent.VK_2;
            case NativeKeyEvent.VC_3: return KeyEvent.VK_3;
            case NativeKeyEvent.VC_4: return KeyEvent.VK_4;
            case NativeKeyEvent.VC_5: return KeyEvent.VK_5;
            case NativeKeyEvent.VC_6: return KeyEvent.VK_6;
            case NativeKeyEvent.VC_7: return KeyEvent.VK_7;
            case NativeKeyEvent.VC_8: return KeyEvent.VK_8;
            case NativeKeyEvent.VC_9: return KeyEvent.VK_9;

            // --- 符号键 ---
            case NativeKeyEvent.VC_BACKQUOTE: return KeyEvent.VK_BACK_QUOTE;
            case NativeKeyEvent.VC_MINUS: return KeyEvent.VK_MINUS;
            case NativeKeyEvent.VC_EQUALS: return KeyEvent.VK_EQUALS;
            case NativeKeyEvent.VC_OPEN_BRACKET: return KeyEvent.VK_OPEN_BRACKET;
            case NativeKeyEvent.VC_CLOSE_BRACKET: return KeyEvent.VK_CLOSE_BRACKET;
            case NativeKeyEvent.VC_BACK_SLASH: return KeyEvent.VK_BACK_SLASH;
            case NativeKeyEvent.VC_SEMICOLON: return KeyEvent.VK_SEMICOLON;
            case NativeKeyEvent.VC_QUOTE: return KeyEvent.VK_QUOTE;
            case NativeKeyEvent.VC_COMMA: return KeyEvent.VK_COMMA;
            case NativeKeyEvent.VC_PERIOD: return KeyEvent.VK_PERIOD;
            case NativeKeyEvent.VC_SLASH: return KeyEvent.VK_SLASH;

            // --- 小键盘（部分缺失，映射近似） ---
            case NativeKeyEvent.VC_NUM_LOCK: return KeyEvent.VK_NUM_LOCK;
            case NativeKeyEvent.VC_SEPARATOR: return KeyEvent.VK_SEPARATOR;

            // --- 媒体键/应用键 (AWT 无原生支持 → 返回 VK_UNDEFINED) ---
            case NativeKeyEvent.VC_VOLUME_UP: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_VOLUME_DOWN: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_VOLUME_MUTE: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_MEDIA_PLAY: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_MEDIA_STOP: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_MEDIA_PREVIOUS: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_MEDIA_NEXT: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_MEDIA_EJECT: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_APP_MAIL: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_APP_CALCULATOR: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_APP_MUSIC: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_APP_PICTURES: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_BROWSER_SEARCH: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_BROWSER_HOME: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_BROWSER_BACK: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_BROWSER_FORWARD: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_BROWSER_STOP: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_BROWSER_REFRESH: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_BROWSER_FAVORITES: return KeyEvent.VK_UNDEFINED;

            // --- 日文/特殊键 (无对应) ---
            case NativeKeyEvent.VC_KATAKANA: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_HIRAGANA: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_KANJI: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_YEN: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_FURIGANA: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_UNDERSCORE: return KeyEvent.VK_UNDEFINED;

            // --- Sun 特殊键 ---
            case NativeKeyEvent.VC_SUN_HELP: return KeyEvent.VK_HELP;
            case NativeKeyEvent.VC_SUN_STOP: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_SUN_PROPS: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_SUN_FRONT: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_SUN_OPEN: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_SUN_FIND: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_SUN_AGAIN: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_SUN_UNDO: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_SUN_COPY: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_SUN_INSERT: return KeyEvent.VK_UNDEFINED;
            case NativeKeyEvent.VC_SUN_CUT: return KeyEvent.VK_UNDEFINED;

            default: return KeyEvent.VK_UNDEFINED;
        }
    }
}
