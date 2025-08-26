package com.keymouseshare.keyboard;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.win32.W32APIOptions;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.keymouseshare.util.KeyBoardUtils.*;

public class WindowMouseKeyBoard implements MouseKeyBoard {

    private static final Logger logger = Logger.getLogger(WindowMouseKeyBoard.class.getName());

    private Robot robot;

    // Windows API接口
    public interface User32 extends Library {
        WindowMouseKeyBoard.User32 INSTANCE = Native.load("user32", WindowMouseKeyBoard.User32.class, W32APIOptions.DEFAULT_OPTIONS);

        /**
         * 注入鼠标事件
         * @param dwFlags 事件标志
         * @param dx X坐标
         * @param dy Y坐标
         * @param dwData 数据
         * @param dwExtraInfo 额外信息
         */
        void mouse_event(int dwFlags, int dx, int dy, int dwData, int dwExtraInfo);

        /**
         * 注入键盘事件
         * @param bVk 虚拟键码
         * @param bScan 扫描码
         * @param dwFlags 事件标志
         * @param dwExtraInfo 额外信息
         */
        void keybd_event(byte bVk, byte bScan, int dwFlags, int dwExtraInfo);
    }

    public WindowMouseKeyBoard() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            logger.log(Level.SEVERE, "无法创建Robot实例", e);
        }
    }

    @Override
    public void mouseMove(int x, int y) {
        if (robot != null) {
            // 尝试使用JNA在Windows上注入事件
            if (Platform.isWindows()) {
                try {
                    // 在Windows上使用JNA注入鼠标移动事件
                    WindowMouseKeyBoard.User32.INSTANCE.mouse_event(0x0001, x, y, 0, 0);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                }
            }
            // 回退到Robot
            robot.mouseMove(x, y);
        }
    }

    @Override
    public void mousePress(int button) {
        if (robot != null) {
            int buttonMask = getButtonMask(button);

            // 尝试使用JNA在Windows上注入事件
            if (Platform.isWindows()) {
                try {
                    // 在Windows上使用JNA注入鼠标按下事件
                    int flags = getMouseEventFlags(button, true);
                    WindowMouseKeyBoard.User32.INSTANCE.mouse_event(flags, 0, 0, 0, 0);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                }
            }

            // 回退到Robot
            robot.mousePress(buttonMask);
        }
    }

    @Override
    public void mouseRelease(int button) {
        if (robot != null) {
            int buttonMask = getButtonMask(button);

            // 尝试使用JNA在Windows上注入事件
            if (Platform.isWindows()) {
                try {
                    // 在Windows上使用JNA注入鼠标释放事件
                    int flags = getMouseEventFlags(button, false);
                    WindowMouseKeyBoard.User32.INSTANCE.mouse_event(flags, 0, 0, 0, 0);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                }
            }

            // 回退到Robot
            robot.mouseRelease(buttonMask);
        }
    }

    @Override
    public void mouseClick(int x, int y) {
        if (robot != null) {
            // 尝试使用JNA在Windows上注入事件
            if (Platform.isWindows()) {
                try {
                    // 先移动鼠标到指定位置
                    WindowMouseKeyBoard.User32.INSTANCE.mouse_event(0x0001, x, y, 0, 0);
                    // 模拟鼠标左键点击（按下然后释放）
                    WindowMouseKeyBoard.User32.INSTANCE.mouse_event(0x0002, 0, 0, 0, 0); // 左键按下
                    WindowMouseKeyBoard.User32.INSTANCE.mouse_event(0x0004, 0, 0, 0, 0); // 左键释放
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                }
            }
            // 回退到Robot
            robot.mouseMove(x, y);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        }
    }

    @Override
    public void mouseDragged() {

    }

    @Override
    public void keyPress(int keyCode) {
        if (robot != null) {
            // 尝试使用JNA在Windows上注入事件
            if (Platform.isWindows()) {
                try {
                    // 在Windows上使用JNA注入键盘按下事件
                    WindowMouseKeyBoard.User32.INSTANCE.keybd_event((byte) keyCode, (byte) 0, 0, 0);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入键盘事件，回退到Robot", e);
                }
            }

            // 回退到Robot
            robot.keyPress(keyCode);
        }
    }

    @Override
    public void keyRelease(int keyCode) {
        if (robot != null) {
            // 尝试使用JNA在Windows上注入事件
            if (Platform.isWindows()) {
                try {
                    // 在Windows上使用JNA注入键盘释放事件
                    WindowMouseKeyBoard.User32.INSTANCE.keybd_event((byte) keyCode, (byte) 0, 0x0002, 0);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入键盘事件，回退到Robot", e);
                }
            }

            // 回退到Robot
            robot.keyRelease(keyCode);
        }
    }

    @Override
    public void startIntercept() {

    }

    @Override
    public void stopIntercept() {

    }
}
