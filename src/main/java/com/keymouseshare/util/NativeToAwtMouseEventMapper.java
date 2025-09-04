package com.keymouseshare.util;

import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

public class NativeToAwtMouseEventMapper {

    /**
     * 将 NativeMouseEvent 转换为 AWT MouseEvent
     */
    public static MouseEvent toAwtMouseEvent(Component source, NativeMouseEvent nativeEvent) {
        int id = toAwtEventType(nativeEvent.getID());
        long when = System.currentTimeMillis();
        int modifiers = toAwtModifiers(nativeEvent.getModifiers());
        int x = nativeEvent.getX();
        int y = nativeEvent.getY();
        int clickCount = nativeEvent.getClickCount();
        boolean popupTrigger = false; // JNativeHook 不直接提供
        int button = toAwtButton(nativeEvent.getButton());

        return new MouseEvent(source, id, when, modifiers, x, y, clickCount, popupTrigger, button);
    }

    /** 类型映射 */
    public static int toAwtEventType(int nativeType) {
        switch (nativeType) {
            case NativeMouseEvent.NATIVE_MOUSE_CLICKED: return MouseEvent.MOUSE_CLICKED;
            case NativeMouseEvent.NATIVE_MOUSE_PRESSED: return MouseEvent.MOUSE_PRESSED;
            case NativeMouseEvent.NATIVE_MOUSE_RELEASED: return MouseEvent.MOUSE_RELEASED;
            case NativeMouseEvent.NATIVE_MOUSE_MOVED: return MouseEvent.MOUSE_MOVED;
            case NativeMouseEvent.NATIVE_MOUSE_DRAGGED: return MouseEvent.MOUSE_DRAGGED;
            case NativeMouseEvent.NATIVE_MOUSE_WHEEL: return MouseEvent.MOUSE_WHEEL; // 注意：一般用 NativeMouseWheelEvent 单独处理
            default: return MouseEvent.MOUSE_MOVED;
        }
    }

    /** 按钮映射 */
    public static int toAwtButton(int nativeButton) {
        switch (nativeButton) {
            case NativeMouseEvent.BUTTON1: return MouseEvent.BUTTON1;
            case NativeMouseEvent.BUTTON2: return MouseEvent.BUTTON2;
            case NativeMouseEvent.BUTTON3: return MouseEvent.BUTTON3;
            default: return MouseEvent.NOBUTTON;
        }
    }

    /** 按钮映射 */
    public static int toInputEventButton(int nativeButton) {
       return InputEvent.getMaskForButton(toAwtButton(nativeButton));
    }

    /** 修饰键映射（Shift/Alt/Ctrl/Meta） */
    public static int toAwtModifiers(int nativeMods) {
        int mods = 0;
        if ((nativeMods & NativeMouseEvent.SHIFT_MASK) != 0) mods |= InputEvent.SHIFT_DOWN_MASK;
        if ((nativeMods & NativeMouseEvent.CTRL_MASK) != 0) mods |= InputEvent.CTRL_DOWN_MASK;
        if ((nativeMods & NativeMouseEvent.ALT_MASK) != 0) mods |= InputEvent.ALT_DOWN_MASK;
        if ((nativeMods & NativeMouseEvent.META_MASK) != 0) mods |= InputEvent.META_DOWN_MASK;
        if ((nativeMods & NativeMouseEvent.BUTTON1_MASK) != 0) mods |= InputEvent.BUTTON1_DOWN_MASK;
        if ((nativeMods & NativeMouseEvent.BUTTON2_MASK) != 0) mods |= InputEvent.BUTTON2_DOWN_MASK;
        if ((nativeMods & NativeMouseEvent.BUTTON3_MASK) != 0) mods |= InputEvent.BUTTON3_DOWN_MASK;
        return mods;
    }
}
