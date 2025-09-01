package com.keymouseshare.keyboard.win;

import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.win32.StdCallLibrary;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WinHookManager {
    private static final Logger logger = Logger.getLogger(WinHookManager.class.getName());
    
    public interface LowLevelProc extends StdCallLibrary.StdCallCallback {
        WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, WinDef.LPARAM lParam);
    }

    private WinUser.HHOOK mouseHook;
    private WinUser.HHOOK keyboardHook;
    private volatile boolean hooksActive = false;

    public void startHooks(Consumer<WinHookEvent> eventHandler) {
        if (hooksActive) {
            logger.log(Level.WARNING, "Hooks are already active");
            return;
        }
        
        try {
            LowLevelProc mouseProc = (nCode, wParam, lParam) -> {
                if (nCode >= 0) {
                    eventHandler.accept(WinHookEvent.fromMouse(wParam, lParam));
                    return new WinDef.LRESULT(1); // 阻止本地响应
                }
                return WinUser32.INSTANCE.CallNextHookEx(mouseHook, nCode, wParam, lParam);
            };
            
            LowLevelProc keyboardProc = (nCode, wParam, lParam) -> {
                if (nCode >= 0) {
                    eventHandler.accept(WinHookEvent.fromKeyboard(wParam, lParam));
                    return new WinDef.LRESULT(1);
                }
                return WinUser32.INSTANCE.CallNextHookEx(keyboardHook, nCode, wParam, lParam);
            };
            
            mouseHook = WinUser32.INSTANCE.SetWindowsHookEx(WinUser32.WH_MOUSE_LL, mouseProc, null, 0);
            keyboardHook = WinUser32.INSTANCE.SetWindowsHookEx(WinUser32.WH_KEYBOARD_LL, keyboardProc, null, 0);
            
            if (mouseHook == null || keyboardHook == null) {
                logger.log(Level.SEVERE, "Failed to set hooks");
                stopHooks();
                return;
            }
            
            hooksActive = true;
            logger.log(Level.INFO, "Hooks started successfully");
            
            // 消息循环
            WinUser.MSG msg = new WinUser.MSG();
            while (hooksActive) {
                int result = User32.INSTANCE.GetMessage(msg, null, 0, 0);
                if (result == -1) {
                    logger.log(Level.WARNING, "GetMessage returned -1, breaking message loop");
                    break;
                } else if (result == 0) {
                    logger.log(Level.INFO, "GetMessage returned 0, breaking message loop");
                    break;
                } else {
                    User32.INSTANCE.TranslateMessage(msg);
                    User32.INSTANCE.DispatchMessage(msg);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in hook processing", e);
            stopHooks();
        }
    }

    public void stopHooks() {
        hooksActive = false;
        
        if (mouseHook != null) {
            try {
                WinUser32.INSTANCE.UnhookWindowsHookEx(mouseHook);
                logger.log(Level.INFO, "Mouse hook removed");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error removing mouse hook", e);
            }
            mouseHook = null;
        }
        
        if (keyboardHook != null) {
            try {
                WinUser32.INSTANCE.UnhookWindowsHookEx(keyboardHook);
                logger.log(Level.INFO, "Keyboard hook removed");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error removing keyboard hook", e);
            }
            keyboardHook = null;
        }
        
        logger.log(Level.INFO, "Hooks stopped");
    }
    
    public boolean isHooksActive() {
        return hooksActive;
    }
}