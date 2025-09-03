
package com.keymouseshare.keyboard.win;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class WinHookManager {

    private static final Logger logger = LoggerFactory.getLogger(WinHookManager.class);

    private HHOOK mouseHook;
    private HHOOK keyboardHook;

    // 必须保存引用，避免 GC
    private HOOKPROC mouseProc;
    private HOOKPROC keyboardProc;

    private final AtomicBoolean hooksActive = new AtomicBoolean(false);
    private Thread hookThread;

    /**
     * 启动钩子
     */
    public void startHooks(Consumer<WinHookEvent> eventHandler) {
        if (hooksActive.get()) {
            logger.warn("Hooks are already active");
            return;
        }

        hooksActive.set(true);

        hookThread = new Thread(() -> {
            try {
                mouseProc = (LowLevelMouseProc) (nCode, wParam, msllhookstruct) -> {
                    if (nCode >= 0) {
                        WinHookEvent event = WinHookEvent.fromMouse(wParam, msllhookstruct);

                        // 阻止除了移动之外的其它全部事件
                        if (event.getType() != WinHookEvent.EventType.MOUSE_MOVE) {
                            eventHandler.accept(event);
                            return new LRESULT(1); // 阻止继续传递
                        }
                    }
                    return User32.INSTANCE.CallNextHookEx(mouseHook, nCode, wParam, new LPARAM(Pointer.nativeValue(msllhookstruct.getPointer())));
                };

                keyboardProc = (LowLevelKeyboardProc) (nCode, wParam, kbdllhookstruct) -> {
                    if (nCode >= 0) {
                        WinHookEvent event = WinHookEvent.fromKeyboard(wParam, kbdllhookstruct);
                        eventHandler.accept(event);

                        if (event.shouldBlock()) {
                            return new LRESULT(1); // 阻止继续传递
                        }
                    }
                    return User32.INSTANCE.CallNextHookEx(keyboardHook, nCode, wParam, new LPARAM(Pointer.nativeValue(kbdllhookstruct.getPointer())));
                };

                // 设置钩子
                mouseHook = User32.INSTANCE.SetWindowsHookEx(WinUser.WH_MOUSE_LL, mouseProc, null, 0);
                keyboardHook = User32.INSTANCE.SetWindowsHookEx(WinUser.WH_KEYBOARD_LL, keyboardProc, null, 0);

                if (mouseHook == null && keyboardHook == null) {
                    logger.error("Failed to set both hooks");
                    stopHooks();
                    return;
                }

                logger.info("Hooks started successfully");

                // 消息循环（独立线程）
                MSG msg = new MSG();
                while (hooksActive.get() && User32.INSTANCE.GetMessage(msg, null, 0, 0) != 0) {
                    User32.INSTANCE.TranslateMessage(msg);
                    User32.INSTANCE.DispatchMessage(msg);
                }
            } catch (Exception e) {
                logger.error("Error in hook processing", e);
            } finally {
                stopHooks();
            }
        }, "WinHook-Thread");

        hookThread.setDaemon(true);
        hookThread.start();
    }

    /**
     * 停止钩子
     */
    public void stopHooks() {
        if (!hooksActive.get()) return;

        hooksActive.set(false);

        if (mouseHook != null) {
            try {
                User32.INSTANCE.UnhookWindowsHookEx(mouseHook);
                logger.info("Mouse hook removed");
            } catch (Exception e) {
                logger.error("Error removing mouse hook", e);
            }
            mouseHook = null;
        }

        if (keyboardHook != null) {
            try {
                User32.INSTANCE.UnhookWindowsHookEx(keyboardHook);
                logger.info("Keyboard hook removed");
            } catch (Exception e) {
                logger.error("Error removing keyboard hook", e);
            }
            keyboardHook = null;
        }

        logger.info("Hooks stopped");
    }

    public boolean isHooksActive() {
        return hooksActive.get();
    }
}
