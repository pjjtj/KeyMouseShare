package com.keymouseshare.keyboard.win;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

public class WinHookEvent {

    public enum EventType {
        KEY_DOWN, KEY_UP,
        MOUSE_MOVE, MOUSE_DOWN, MOUSE_UP, MOUSE_WHEEL,
        UNKNOWN
    }

    private final EventType type;
    private final int keyCode;     // 键盘按键码
    private final int mouseX;      // 鼠标X坐标
    private final int mouseY;      // 鼠标Y坐标
    private final boolean block;   // 是否阻止事件继续传递

    public WinHookEvent(EventType type, int keyCode, int mouseX, int mouseY, boolean block) {
        this.type = type;
        this.keyCode = keyCode;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.block = block;
    }

    public EventType getType() {
        return type;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    public boolean shouldBlock() {
        return block;
    }

    // 从键盘事件构造
    public static WinHookEvent fromKeyboard(WinDef.WPARAM wParam, WinUser.KBDLLHOOKSTRUCT kbdllhookstruct) {

        EventType type;
        switch (wParam.intValue()) {
            case WinUser.WM_KEYDOWN:
            case WinUser.WM_SYSKEYDOWN:
                type = EventType.KEY_DOWN;
                break;
            case WinUser.WM_KEYUP:
            case WinUser.WM_SYSKEYUP:
                type = EventType.KEY_UP;
                break;
            default:
                type = EventType.UNKNOWN;
        }

        int keyCode = kbdllhookstruct.vkCode;

        // 示例逻辑：屏蔽 ESC 键
        boolean block = (keyCode == 27); // VK_ESCAPE

        return new WinHookEvent(type, keyCode, -1, -1, block);
    }

    // 从鼠标事件构造
    public static WinHookEvent fromMouse(WinDef.WPARAM wParam, WinUser.MSLLHOOKSTRUCT msllhookstruct) {
        EventType type;
        switch (wParam.intValue()) {
            case WinUserExtra.WM_MOUSEMOVE:
                type = EventType.MOUSE_MOVE;
                break;
            case WinUserExtra.WM_LBUTTONDOWN:
            case WinUserExtra.WM_RBUTTONDOWN:
                type = EventType.MOUSE_DOWN;
                break;
            case WinUserExtra.WM_LBUTTONUP:
            case WinUserExtra.WM_RBUTTONUP:
                type = EventType.MOUSE_UP;
                break;
            case WinUserExtra.WM_MOUSEWHEEL:
                type = EventType.MOUSE_WHEEL;
                break;
            default:
                type = EventType.UNKNOWN;
        }

        return new WinHookEvent(type, -1, msllhookstruct.pt.x, msllhookstruct.pt.y, false);
    }
}
