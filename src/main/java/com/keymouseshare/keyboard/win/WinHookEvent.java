package com.keymouseshare.keyboard.win;

import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

/**
 * Windows钩子事件类
 * 用于封装从Windows钩子回调中捕获的鼠标和键盘事件
 */
public class WinHookEvent {
    private final EventType type;
    private final int keyCode;
    private final int mouseX;
    private final int mouseY;
    private final int mouseButton;
    
    public enum EventType {
        MOUSE_MOVE,
        MOUSE_DOWN,
        MOUSE_UP,
        KEY_DOWN,
        KEY_UP
    }
    
    private WinHookEvent(EventType type, int keyCode, int mouseX, int mouseY, int mouseButton) {
        this.type = type;
        this.keyCode = keyCode;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.mouseButton = mouseButton;
    }

    // Windows鼠标消息常量
    private static final int WM_MOUSEMOVE = 0x0200;
    private static final int WM_LBUTTONDOWN = 0x0201;
    private static final int WM_LBUTTONUP = 0x0202;
    private static final int WM_RBUTTONDOWN = 0x0204;
    private static final int WM_RBUTTONUP = 0x0205;
    private static final int WM_MBUTTONDOWN = 0x0207;
    private static final int WM_MBUTTONUP = 0x0208;

    public static WinHookEvent fromMouse(WinDef.WPARAM wParam, WinDef.LPARAM lParam) {
        WinUser.MSLLHOOKSTRUCT mouseStruct = new WinUser.MSLLHOOKSTRUCT();
        // 使用反射方式访问useMemory方法，避免访问保护成员的问题
        try {
            java.lang.reflect.Method useMemoryMethod = com.sun.jna.Structure.class.getDeclaredMethod("useMemory", com.sun.jna.Pointer.class);
            useMemoryMethod.setAccessible(true);
            useMemoryMethod.invoke(mouseStruct, new com.sun.jna.Pointer(lParam.longValue()));
        } catch (Exception e) {
            // 异常处理，但不执行任何操作，因为read()方法会处理内存访问
        }
        mouseStruct.read();
        
        int mouseX = mouseStruct.pt.x;
        int mouseY = mouseStruct.pt.y;
        int mouseButton = 0;
        EventType type = EventType.MOUSE_MOVE;

        switch (wParam.intValue()) {
            case WM_MOUSEMOVE:
                type = EventType.MOUSE_MOVE;
                break;
            case WM_LBUTTONDOWN:
                type = EventType.MOUSE_DOWN;
                mouseButton = 1;
                break;
            case WM_LBUTTONUP:
                type = EventType.MOUSE_UP;
                mouseButton = 1;
                break;
            case WM_RBUTTONDOWN:
                type = EventType.MOUSE_DOWN;
                mouseButton = 2;
                break;
            case WM_RBUTTONUP:
                type = EventType.MOUSE_UP;
                mouseButton = 2;
                break;
            case WM_MBUTTONDOWN:
                type = EventType.MOUSE_DOWN;
                mouseButton = 3;
                break;
            case WM_MBUTTONUP:
                type = EventType.MOUSE_UP;
                mouseButton = 3;
                break;
            default:
                // 对于未识别的鼠标事件，保持默认值 EventType.MOUSE_MOVE 和 mouseButton = 0
                break;
        }

        return new WinHookEvent(type, 0, mouseX, mouseY, mouseButton);
    }
    
    public static WinHookEvent fromKeyboard(WinDef.WPARAM wParam, WinDef.LPARAM lParam) {
        // 解析键盘事件
        int keyCode = lParam.intValue() & 0xFFFF;
        EventType type = EventType.KEY_DOWN;
        
        // 根据wParam的值确定事件类型
        switch (wParam.intValue()) {
            case 0x0100: // WM_KEYDOWN
            case 0x0104: // WM_SYSKEYDOWN
                type = EventType.KEY_DOWN;
                break;
            case 0x0101: // WM_KEYUP
            case 0x0105: // WM_SYSKEYUP
                type = EventType.KEY_UP;
                break;
        }
        
        return new WinHookEvent(type, keyCode, 0, 0, 0);
    }
    
    // Getters
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
    
    public int getMouseButton() {
        return mouseButton;
    }
}