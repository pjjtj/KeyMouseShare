package com.keymouseshare.keyboard;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.keymouseshare.util.KeyBoardUtils.getButtonMask;

public class LinuxMouseKeyBoard implements MouseKeyBoard {
    private static final Logger logger = Logger.getLogger(LinuxMouseKeyBoard.class.getName());

    private Robot robot;

    // Linux X11 API接口
    public interface X11 extends Library {
        LinuxMouseKeyBoard.X11 INSTANCE = Native.load("X11", LinuxMouseKeyBoard.X11.class);

        /**
         * 打开显示连接
         * @param name 显示名称
         * @return 显示连接指针
         */
        Pointer XOpenDisplay(String name);

        /**
         * 关闭显示连接
         * @param display 显示连接指针
         */
        void XCloseDisplay(Pointer display);

        /**
         * 移动鼠标
         * @param display 显示连接指针
         * @param src_w 源窗口
         * @param dest_w 目标窗口
         * @param src_x 源X坐标
         * @param src_y 源Y坐标
         * @param src_width 源宽度
         * @param src_height 源高度
         * @param dest_x 目标X坐标
         * @param dest_y 目标Y坐标
         */
        void XWarpPointer(Pointer display, Pointer src_w, Pointer dest_w, int src_x, int src_y,
                          int src_width, int src_height, int dest_x, int dest_y);

        /**
         * 按下按键
         * @param display 显示连接指针
         * @param button 按钮编号
         * @param is_press 是否按下
         * @param delay 延迟
         */
        void XTestFakeButtonEvent(Pointer display, int button, boolean is_press, long delay);

        /**
         * 按下键盘键
         * @param display 显示连接指针
         * @param keycode 键码
         * @param is_press 是否按下
         * @param delay 延迟
         */
        void XTestFakeKeyEvent(Pointer display, int keycode, boolean is_press, long delay);

        /**
         * 刷新显示
         * @param display 显示连接指针
         */
        int XFlush(Pointer display);
    }

    public LinuxMouseKeyBoard() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            logger.log(Level.SEVERE, "无法创建Robot实例", e);
        }
    }


    @Override
    public void mouseMove(int x, int y) {
        if (robot != null) {
            // 尝试使用JNA在Linux上注入事件
            if (Platform.isLinux()) {
                try {
                    Pointer display = LinuxMouseKeyBoard.X11.INSTANCE.XOpenDisplay(null);
                    if (display != null) {
                        LinuxMouseKeyBoard.X11.INSTANCE.XWarpPointer(display, null, null, 0, 0, 0, 0, x, y);
                        LinuxMouseKeyBoard.X11.INSTANCE.XFlush(display);
                        LinuxMouseKeyBoard.X11.INSTANCE.XCloseDisplay(display);
                        return;
                    }
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

            // 尝试使用JNA在Linux上注入事件
            if (Platform.isLinux()) {
                try {
                    Pointer display = LinuxMouseKeyBoard.X11.INSTANCE.XOpenDisplay(null);
                    if (display != null) {
                        LinuxMouseKeyBoard.X11.INSTANCE.XTestFakeButtonEvent(display, button, true, 0);
                        LinuxMouseKeyBoard.X11.INSTANCE.XFlush(display);
                        LinuxMouseKeyBoard.X11.INSTANCE.XCloseDisplay(display);
                        return;
                    }
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

            // 尝试使用JNA在Linux上注入事件
            if (Platform.isLinux()) {
                try {
                    Pointer display = LinuxMouseKeyBoard.X11.INSTANCE.XOpenDisplay(null);
                    if (display != null) {
                        LinuxMouseKeyBoard.X11.INSTANCE.XTestFakeButtonEvent(display, button, false, 0);
                        LinuxMouseKeyBoard.X11.INSTANCE.XFlush(display);
                        LinuxMouseKeyBoard.X11.INSTANCE.XCloseDisplay(display);
                        return;
                    }
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
            // 尝试使用JNA在Linux上注入事件
            if (Platform.isLinux()) {
                try {
                    Pointer display = LinuxMouseKeyBoard.X11.INSTANCE.XOpenDisplay(null);
                    if (display != null) {
                        // 移动鼠标到指定位置
                        LinuxMouseKeyBoard.X11.INSTANCE.XWarpPointer(display, null, null, 0, 0, 0, 0, x, y);
                        // 模拟鼠标左键点击（按下然后释放）
                        LinuxMouseKeyBoard.X11.INSTANCE.XTestFakeButtonEvent(display, 1, true, 0);  // 左键按下
                        LinuxMouseKeyBoard.X11.INSTANCE.XTestFakeButtonEvent(display, 1, false, 0); // 左键释放
                        LinuxMouseKeyBoard.X11.INSTANCE.XFlush(display);
                        LinuxMouseKeyBoard.X11.INSTANCE.XCloseDisplay(display);
                        return;
                    }
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
            // 尝试使用JNA在Linux上注入事件
            if (Platform.isLinux()) {
                try {
                    Pointer display = LinuxMouseKeyBoard.X11.INSTANCE.XOpenDisplay(null);
                    if (display != null) {
                        // 注意：这里需要将Java键码转换为X11键码
                        LinuxMouseKeyBoard.X11.INSTANCE.XTestFakeKeyEvent(display, keyCode, true, 0);
                        LinuxMouseKeyBoard.X11.INSTANCE.XFlush(display);
                        LinuxMouseKeyBoard.X11.INSTANCE.XCloseDisplay(display);
                        return;
                    }
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

            // 尝试使用JNA在Linux上注入事件
            if (Platform.isLinux()) {
                try {
                    Pointer display = LinuxMouseKeyBoard.X11.INSTANCE.XOpenDisplay(null);
                    if (display != null) {
                        // 注意：这里需要将Java键码转换为X11键码
                        LinuxMouseKeyBoard.X11.INSTANCE.XTestFakeKeyEvent(display, keyCode, false, 0);
                        LinuxMouseKeyBoard.X11.INSTANCE.XFlush(display);
                        LinuxMouseKeyBoard.X11.INSTANCE.XCloseDisplay(display);
                        return;
                    }
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
