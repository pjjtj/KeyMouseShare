package com.keymouseshare.keyboard;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.keymouseshare.util.KeyBoardUtils.*;

public class MacMouseKeyBoard implements MouseKeyBoard {

    private static final Logger logger = Logger.getLogger(MacMouseKeyBoard.class.getName());

    private static final MacMouseKeyBoard INSTANCE = new MacMouseKeyBoard();

    public static MacMouseKeyBoard getInstance() {
        return INSTANCE;
    }

    private Robot robot;

    // macOS CoreGraphics API接口
    public interface CoreGraphics extends Library {
        MacMouseKeyBoard.CoreGraphics INSTANCE = Native.load("CoreGraphics", MacMouseKeyBoard.CoreGraphics.class);

        /**
         * 创建鼠标事件
         * @param source 事件源
         * @param type 事件类型
         * @param point 位置点
         * @param mouseButton 鼠标按钮
         * @return 事件指针
         */
        Pointer CGEventCreateMouseEvent(Pointer source, int type, MacMouseKeyBoard.CGPoint point, int mouseButton);

        /**
         * 创建键盘事件
         * @param source 事件源
         * @param keycode 虚拟键码
         * @param keydown 是否按下
         * @return 事件指针
         */
        Pointer CGEventCreateKeyboardEvent(Pointer source, short keycode, boolean keydown);

        /**
         * 设置事件位置
         * @param event 事件指针
         * @param point 位置点
         */
        void CGEventSetLocation(Pointer event, MacMouseKeyBoard.CGPoint point);

        /**
         * 注入事件
         * @param tap 事件tap
         * @param event 事件指针
         */
        void CGEventPost(int tap, Pointer event);

        /**
         * 释放事件
         * @param event 事件指针
         */
        void CFRelease(Pointer event);
    }

    // macOS CGPoint结构体
    public static class CGPoint extends com.sun.jna.Structure {
        public int x;
        public int y;

        @Override
        protected java.util.List<String> getFieldOrder() {
            return java.util.Arrays.asList("x", "y");
        }

        public CGPoint() {
            super();
        }

        public CGPoint(int x, int y) {
            super();
            this.x = x;
            this.y = y;
        }
    }

    public MacMouseKeyBoard() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            logger.log(Level.SEVERE, "无法创建Robot实例", e);
        }
    }


    @Override
    public void mouseMove(int x, int y) {
        if (robot != null) {
            // 尝试使用JNA在macOS上注入事件
            if (Platform.isMac()) {
                try {
                    MacMouseKeyBoard.CGPoint point = new MacMouseKeyBoard.CGPoint(x, y);
                    Pointer event = MacMouseKeyBoard.CoreGraphics.INSTANCE.CGEventCreateMouseEvent(null, 1, point, 0); // kCGEventMouseMoved
                    MacMouseKeyBoard.CoreGraphics.INSTANCE.CGEventPost(0, event); // kCGHIDEventTap
                    MacMouseKeyBoard.CoreGraphics.INSTANCE.CFRelease(event);
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

            // 尝试使用JNA在macOS上注入事件
            if (Platform.isMac()) {
                try {
                    int eventType = getMacMouseEventType(button, true);
                    MacMouseKeyBoard.CGPoint point = new MacMouseKeyBoard.CGPoint(0, 0); // 当前鼠标位置
                    Pointer event = MacMouseKeyBoard.CoreGraphics.INSTANCE.CGEventCreateMouseEvent(null, eventType, point, getMacMouseButton(button));
                    MacMouseKeyBoard.CoreGraphics.INSTANCE.CGEventPost(0, event); // kCGHIDEventTap
                    MacMouseKeyBoard.CoreGraphics.INSTANCE.CFRelease(event);
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

            // 尝试使用JNA在macOS上注入事件
            if (Platform.isMac()) {
                try {
                    int eventType = getMacMouseEventType(button, false);
                    MacMouseKeyBoard.CGPoint point = new MacMouseKeyBoard.CGPoint(0, 0); // 当前鼠标位置
                    Pointer event = MacMouseKeyBoard.CoreGraphics.INSTANCE.CGEventCreateMouseEvent(null, eventType, point, getMacMouseButton(button));
                    MacMouseKeyBoard.CoreGraphics.INSTANCE.CGEventPost(0, event); // kCGHIDEventTap
                    MacMouseKeyBoard.CoreGraphics.INSTANCE.CFRelease(event);
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

            // 尝试使用JNA在macOS上注入事件
            if (Platform.isMac()) {
                try {
                    MacMouseKeyBoard.CGPoint point = new MacMouseKeyBoard.CGPoint(x, y);
                    // 创建并注入鼠标左键按下事件
                    Pointer mouseDownEvent = MacMouseKeyBoard.CoreGraphics.INSTANCE.CGEventCreateMouseEvent(null, 1, point, 0); // kCGEventLeftMouseDown
                    MacMouseKeyBoard.CoreGraphics.INSTANCE.CGEventPost(0, mouseDownEvent); // kCGHIDEventTap

                    // 创建并注入鼠标左键释放事件
                    Pointer mouseUpEvent = MacMouseKeyBoard.CoreGraphics.INSTANCE.CGEventCreateMouseEvent(null, 2, point, 0); // kCGEventLeftMouseUp
                    MacMouseKeyBoard.CoreGraphics.INSTANCE.CGEventPost(0, mouseUpEvent); // kCGHIDEventTap

                    // 释放事件
                    MacMouseKeyBoard.CoreGraphics.INSTANCE.CFRelease(mouseDownEvent);
                    MacMouseKeyBoard.CoreGraphics.INSTANCE.CFRelease(mouseUpEvent);
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

            // 尝试使用JNA在macOS上注入事件
            if (Platform.isMac()) {
                try {
                    short macKeyCode = convertToMacKeyCode(keyCode);
                    Pointer event = MacMouseKeyBoard.CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, macKeyCode, true);
                    MacMouseKeyBoard.CoreGraphics.INSTANCE.CGEventPost(0, event); // kCGHIDEventTap
                    MacMouseKeyBoard.CoreGraphics.INSTANCE.CFRelease(event);
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

            // 尝试使用JNA在macOS上注入事件
            if (Platform.isMac()) {
                try {
                    short macKeyCode = convertToMacKeyCode(keyCode);
                    Pointer event = MacMouseKeyBoard.CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, macKeyCode, false);
                    MacMouseKeyBoard.CoreGraphics.INSTANCE.CGEventPost(0, event); // kCGHIDEventTap
                    MacMouseKeyBoard.CoreGraphics.INSTANCE.CFRelease(event);
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
    public void initVirtualMouseLocation() {

    }

    @Override
    public void startMouseKeyController() {

    }

    @Override
    public void stopMouseKeyController() {

    }

    @Override
    public void stopEdgeDetection() {

    }

    @Override
    public boolean isEdgeMode() {
        return false;
    }
}
