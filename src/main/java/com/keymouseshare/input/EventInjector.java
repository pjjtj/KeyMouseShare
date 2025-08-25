package com.keymouseshare.input;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.win32.W32APIOptions;

/**
 * 基于Robot和JNA的事件注入器
 * 用于在本地设备上注入鼠标和键盘事件
 */
public class EventInjector {
    private static final Logger logger = Logger.getLogger(EventInjector.class.getName());
    
    private Robot robot;
    
    // Windows API接口
    public interface User32 extends Library {
        User32 INSTANCE = Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);
        
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
    
    // macOS CoreGraphics API接口
    public interface CoreGraphics extends Library {
        CoreGraphics INSTANCE = Native.load("CoreGraphics", CoreGraphics.class);
        
        /**
         * 创建鼠标事件
         * @param source 事件源
         * @param type 事件类型
         * @param point 位置点
         * @param mouseButton 鼠标按钮
         * @return 事件指针
         */
        Pointer CGEventCreateMouseEvent(Pointer source, int type, CGPoint point, int mouseButton);
        
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
        void CGEventSetLocation(Pointer event, CGPoint point);
        
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
        public double x;
        public double y;
        
        @Override
        protected java.util.List<String> getFieldOrder() {
            return java.util.Arrays.asList("x", "y");
        }
        
        public CGPoint() {
            super();
        }
        
        public CGPoint(double x, double y) {
            super();
            this.x = x;
            this.y = y;
        }
    }
    
    // Linux X11 API接口
    public interface X11 extends Library {
        X11 INSTANCE = Native.load("X11", X11.class);
        
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
    
    public EventInjector() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            logger.log(Level.SEVERE, "无法创建Robot实例", e);
        }
    }
    
    /**
     * 移动鼠标到指定位置
     * @param x X坐标
     * @param y Y坐标
     */
    public void mouseMove(int x, int y) {
        if (robot != null) {
            // 尝试使用JNA在Windows上注入事件
            if (Platform.isWindows()) {
                try {
                    // 在Windows上使用JNA注入鼠标移动事件
                    User32.INSTANCE.mouse_event(0x0001, x, y, 0, 0);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                }
            }
            
            // 尝试使用JNA在macOS上注入事件
            if (Platform.isMac()) {
                try {
                    CGPoint point = new CGPoint(x, y);
                    Pointer event = CoreGraphics.INSTANCE.CGEventCreateMouseEvent(null, 1, point, 0); // kCGEventMouseMoved
                    CoreGraphics.INSTANCE.CGEventPost(0, event); // kCGHIDEventTap
                    CoreGraphics.INSTANCE.CFRelease(event);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                }
            }
            
            // 尝试使用JNA在Linux上注入事件
            if (Platform.isLinux()) {
                try {
                    Pointer display = X11.INSTANCE.XOpenDisplay(null);
                    if (display != null) {
                        X11.INSTANCE.XWarpPointer(display, null, null, 0, 0, 0, 0, x, y);
                        X11.INSTANCE.XFlush(display);
                        X11.INSTANCE.XCloseDisplay(display);
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
    
    /**
     * 鼠标按下
     * @param button 按钮编号 (1=左键, 2=中键, 3=右键)
     */
    public void mousePress(int button) {
        if (robot != null) {
            int buttonMask = getButtonMask(button);
            
            // 尝试使用JNA在Windows上注入事件
            if (Platform.isWindows()) {
                try {
                    // 在Windows上使用JNA注入鼠标按下事件
                    int flags = getMouseEventFlags(button, true);
                    User32.INSTANCE.mouse_event(flags, 0, 0, 0, 0);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                }
            }
            
            // 尝试使用JNA在macOS上注入事件
            if (Platform.isMac()) {
                try {
                    int eventType = getMacMouseEventType(button, true);
                    CGPoint point = new CGPoint(0, 0); // 当前鼠标位置
                    Pointer event = CoreGraphics.INSTANCE.CGEventCreateMouseEvent(null, eventType, point, getMacMouseButton(button));
                    CoreGraphics.INSTANCE.CGEventPost(0, event); // kCGHIDEventTap
                    CoreGraphics.INSTANCE.CFRelease(event);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                }
            }
            
            // 尝试使用JNA在Linux上注入事件
            if (Platform.isLinux()) {
                try {
                    Pointer display = X11.INSTANCE.XOpenDisplay(null);
                    if (display != null) {
                        X11.INSTANCE.XTestFakeButtonEvent(display, button, true, 0);
                        X11.INSTANCE.XFlush(display);
                        X11.INSTANCE.XCloseDisplay(display);
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
    
    /**
     * 鼠标释放
     * @param button 按钮编号 (1=左键, 2=中键, 3=右键)
     */
    public void mouseRelease(int button) {
        if (robot != null) {
            int buttonMask = getButtonMask(button);
            
            // 尝试使用JNA在Windows上注入事件
            if (Platform.isWindows()) {
                try {
                    // 在Windows上使用JNA注入鼠标释放事件
                    int flags = getMouseEventFlags(button, false);
                    User32.INSTANCE.mouse_event(flags, 0, 0, 0, 0);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                }
            }
            
            // 尝试使用JNA在macOS上注入事件
            if (Platform.isMac()) {
                try {
                    int eventType = getMacMouseEventType(button, false);
                    CGPoint point = new CGPoint(0, 0); // 当前鼠标位置
                    Pointer event = CoreGraphics.INSTANCE.CGEventCreateMouseEvent(null, eventType, point, getMacMouseButton(button));
                    CoreGraphics.INSTANCE.CGEventPost(0, event); // kCGHIDEventTap
                    CoreGraphics.INSTANCE.CFRelease(event);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                }
            }
            
            // 尝试使用JNA在Linux上注入事件
            if (Platform.isLinux()) {
                try {
                    Pointer display = X11.INSTANCE.XOpenDisplay(null);
                    if (display != null) {
                        X11.INSTANCE.XTestFakeButtonEvent(display, button, false, 0);
                        X11.INSTANCE.XFlush(display);
                        X11.INSTANCE.XCloseDisplay(display);
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

    /**
     * 鼠标点击
     * @param x 鼠标X坐标
     * @param y 鼠标Y坐标
     */
    public void mouseClick(int x, int y) {
        if (robot != null) {
            // 尝试使用JNA在Windows上注入事件
            if (Platform.isWindows()) {
                try {
                    // 先移动鼠标到指定位置
                    User32.INSTANCE.mouse_event(0x0001, x, y, 0, 0);
                    // 模拟鼠标左键点击（按下然后释放）
                    User32.INSTANCE.mouse_event(0x0002, 0, 0, 0, 0); // 左键按下
                    User32.INSTANCE.mouse_event(0x0004, 0, 0, 0, 0); // 左键释放
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                }
            }

            // 尝试使用JNA在macOS上注入事件
            if (Platform.isMac()) {
                try {
                    CGPoint point = new CGPoint(x, y);
                    // 创建并注入鼠标左键按下事件
                    Pointer mouseDownEvent = CoreGraphics.INSTANCE.CGEventCreateMouseEvent(null, 1, point, 0); // kCGEventLeftMouseDown
                    CoreGraphics.INSTANCE.CGEventPost(0, mouseDownEvent); // kCGHIDEventTap
                    
                    // 创建并注入鼠标左键释放事件
                    Pointer mouseUpEvent = CoreGraphics.INSTANCE.CGEventCreateMouseEvent(null, 2, point, 0); // kCGEventLeftMouseUp
                    CoreGraphics.INSTANCE.CGEventPost(0, mouseUpEvent); // kCGHIDEventTap
                    
                    // 释放事件
                    CoreGraphics.INSTANCE.CFRelease(mouseDownEvent);
                    CoreGraphics.INSTANCE.CFRelease(mouseUpEvent);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                }
            }

            // 尝试使用JNA在Linux上注入事件
            if (Platform.isLinux()) {
                try {
                    Pointer display = X11.INSTANCE.XOpenDisplay(null);
                    if (display != null) {
                        // 移动鼠标到指定位置
                        X11.INSTANCE.XWarpPointer(display, null, null, 0, 0, 0, 0, x, y);
                        // 模拟鼠标左键点击（按下然后释放）
                        X11.INSTANCE.XTestFakeButtonEvent(display, 1, true, 0);  // 左键按下
                        X11.INSTANCE.XTestFakeButtonEvent(display, 1, false, 0); // 左键释放
                        X11.INSTANCE.XFlush(display);
                        X11.INSTANCE.XCloseDisplay(display);
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


    /**
     * 鼠标拖拽
     * @param fromX 起始X坐标
     * @param fromY 起始Y坐标
     * @param toX 目标X坐标
     * @param toY 目标Y坐标
     * @param button 按钮编号 (1=左键, 2=中键, 3=右键)
     */
    public void mouseDragged(int fromX, int fromY, int toX, int toY, int button) {
        if (robot != null) {
            // 尝试使用JNA在Windows上注入事件
            if (Platform.isWindows()) {
                try {
                    // 移动到起始位置
                    User32.INSTANCE.mouse_event(0x0001, fromX, fromY, 0, 0);
                    // 按下鼠标按钮
                    int pressFlags = getMouseEventFlags(button, true);
                    User32.INSTANCE.mouse_event(pressFlags, 0, 0, 0, 0);
                    // 移动到目标位置（拖拽）
                    User32.INSTANCE.mouse_event(0x0001, toX, toY, 0, 0);
                    // 释放鼠标按钮
                    int releaseFlags = getMouseEventFlags(button, false);
                    User32.INSTANCE.mouse_event(releaseFlags, 0, 0, 0, 0);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                }
            }

            // 尝试使用JNA在macOS上注入事件
            if (Platform.isMac()) {
                try {
                    // 移动到起始位置并按下鼠标按钮
                    CGPoint startPoint = new CGPoint(fromX, fromY);
                    int pressEventType = getMacMouseEventType(button, true);
                    Pointer pressEvent = CoreGraphics.INSTANCE.CGEventCreateMouseEvent(null, pressEventType, startPoint, getMacMouseButton(button));
                    CoreGraphics.INSTANCE.CGEventPost(0, pressEvent); // kCGHIDEventTap

                    // 移动到目标位置（拖拽）
                    CGPoint endPoint = new CGPoint(toX, toY);
                    CoreGraphics.INSTANCE.CGEventSetLocation(pressEvent, endPoint);
                    CoreGraphics.INSTANCE.CGEventPost(0, pressEvent);

                    // 释放鼠标按钮
                    int releaseEventType = getMacMouseEventType(button, false);
                    Pointer releaseEvent = CoreGraphics.INSTANCE.CGEventCreateMouseEvent(null, releaseEventType, endPoint, getMacMouseButton(button));
                    CoreGraphics.INSTANCE.CGEventPost(0, releaseEvent); // kCGHIDEventTap

                    // 释放事件
                    CoreGraphics.INSTANCE.CFRelease(pressEvent);
                    CoreGraphics.INSTANCE.CFRelease(releaseEvent);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                }
            }

            // 尝试使用JNA在Linux上注入事件
            if (Platform.isLinux()) {
                try {
                    Pointer display = X11.INSTANCE.XOpenDisplay(null);
                    if (display != null) {
                        // 移动到起始位置
                        X11.INSTANCE.XWarpPointer(display, null, null, 0, 0, 0, 0, fromX, fromY);
                        // 按下鼠标按钮
                        X11.INSTANCE.XTestFakeButtonEvent(display, button, true, 0);
                        // 移动到目标位置（拖拽）
                        X11.INSTANCE.XWarpPointer(display, null, null, 0, 0, 0, 0, toX, toY);
                        // 释放鼠标按钮
                        X11.INSTANCE.XTestFakeButtonEvent(display, button, false, 0);
                        X11.INSTANCE.XFlush(display);
                        X11.INSTANCE.XCloseDisplay(display);
                        return;
                    }
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                }
            }

            // 回退到Robot
            robot.mouseMove(fromX, fromY);
            robot.mousePress(getButtonMask(button));
            robot.mouseMove(toX, toY);
            robot.mouseRelease(getButtonMask(button));
        }
    }

    /**
     * 键盘按键按下
     * @param keyCode 虚拟键码
     */
    public void keyPress(int keyCode) {
        if (robot != null) {
            // 尝试使用JNA在Windows上注入事件
            if (Platform.isWindows()) {
                try {
                    // 在Windows上使用JNA注入键盘按下事件
                    User32.INSTANCE.keybd_event((byte) keyCode, (byte) 0, 0, 0);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入键盘事件，回退到Robot", e);
                }
            }
            
            // 尝试使用JNA在macOS上注入事件
            if (Platform.isMac()) {
                try {
                    short macKeyCode = convertToMacKeyCode(keyCode);
                    Pointer event = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, macKeyCode, true);
                    CoreGraphics.INSTANCE.CGEventPost(0, event); // kCGHIDEventTap
                    CoreGraphics.INSTANCE.CFRelease(event);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入键盘事件，回退到Robot", e);
                }
            }
            
            // 尝试使用JNA在Linux上注入事件
            if (Platform.isLinux()) {
                try {
                    Pointer display = X11.INSTANCE.XOpenDisplay(null);
                    if (display != null) {
                        // 注意：这里需要将Java键码转换为X11键码
                        X11.INSTANCE.XTestFakeKeyEvent(display, keyCode, true, 0);
                        X11.INSTANCE.XFlush(display);
                        X11.INSTANCE.XCloseDisplay(display);
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
    
    /**
     * 键盘按键释放
     * @param keyCode 虚拟键码
     */
    public void keyRelease(int keyCode) {
        if (robot != null) {
            // 尝试使用JNA在Windows上注入事件
            if (Platform.isWindows()) {
                try {
                    // 在Windows上使用JNA注入键盘释放事件
                    User32.INSTANCE.keybd_event((byte) keyCode, (byte) 0, 0x0002, 0);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入键盘事件，回退到Robot", e);
                }
            }
            
            // 尝试使用JNA在macOS上注入事件
            if (Platform.isMac()) {
                try {
                    short macKeyCode = convertToMacKeyCode(keyCode);
                    Pointer event = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, macKeyCode, false);
                    CoreGraphics.INSTANCE.CGEventPost(0, event); // kCGHIDEventTap
                    CoreGraphics.INSTANCE.CFRelease(event);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入键盘事件，回退到Robot", e);
                }
            }
            
            // 尝试使用JNA在Linux上注入事件
            if (Platform.isLinux()) {
                try {
                    Pointer display = X11.INSTANCE.XOpenDisplay(null);
                    if (display != null) {
                        // 注意：这里需要将Java键码转换为X11键码
                        X11.INSTANCE.XTestFakeKeyEvent(display, keyCode, false, 0);
                        X11.INSTANCE.XFlush(display);
                        X11.INSTANCE.XCloseDisplay(display);
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

    /**
     * 获取鼠标按钮对应的掩码
     * @param button 按钮编号
     * @return 掩码
     */
    private int getButtonMask(int button) {
        switch (button) {
            case 1: // 左键
                return InputEvent.BUTTON1_DOWN_MASK;
            case 2: // 中键
                return InputEvent.BUTTON2_DOWN_MASK;
            case 3: // 右键
                return InputEvent.BUTTON3_DOWN_MASK;
            default:
                return 0;
        }
    }
    
    /**
     * 获取鼠标事件对应的Windows标志
     * @param button 按钮编号
     * @param isPress 是否按下
     * @return 标志
     */
    private int getMouseEventFlags(int button, boolean isPress) {
        if (isPress) {
            switch (button) {
                case 1: // 左键
                    return 0x0002; // MOUSEEVENTF_LEFTDOWN
                case 2: // 中键
                    return 0x0020; // MOUSEEVENTF_MIDDLEDOWN
                case 3: // 右键
                    return 0x0008; // MOUSEEVENTF_RIGHTDOWN
                default:
                    return 0;
            }
        } else {
            switch (button) {
                case 1: // 左键
                    return 0x0004; // MOUSEEVENTF_LEFTUP
                case 2: // 中键
                    return 0x0040; // MOUSEEVENTF_MIDDLEUP
                case 3: // 右键
                    return 0x0010; // MOUSEEVENTF_RIGHTUP
                default:
                    return 0;
            }
        }
    }
    
    /**
     * 获取macOS鼠标事件类型
     * @param button 按钮编号
     * @param isPress 是否按下
     * @return 事件类型
     */
    private int getMacMouseEventType(int button, boolean isPress) {
        if (isPress) {
            switch (button) {
                case 1: // 左键
                    return 1; // kCGEventLeftMouseDown
                case 2: // 中键
                    return 3; // kCGEventOtherMouseDown
                case 3: // 右键
                    return 3; // kCGEventRightMouseDown
                default:
                    return 0;
            }
        } else {
            switch (button) {
                case 1: // 左键
                    return 2; // kCGEventLeftMouseUp
                case 2: // 中键
                    return 4; // kCGEventOtherMouseUp
                case 3: // 右键
                    return 4; // kCGEventRightMouseUp
                default:
                    return 0;
            }
        }
    }
    
    /**
     * 获取macOS鼠标按钮编号
     * @param button 按钮编号
     * @return macOS按钮编号
     */
    private int getMacMouseButton(int button) {
        switch (button) {
            case 1: // 左键
                return 0; // kCGMouseButtonLeft
            case 2: // 中键
                return 2; // kCGMouseButtonCenter
            case 3: // 右键
                return 1; // kCGMouseButtonRight
            default:
                return 0;
        }
    }
    
    /**
     * 将Java虚拟键码转换为macOS键码
     * @param keyCode Java虚拟键码
     * @return macOS键码
     */
    private short convertToMacKeyCode(int keyCode) {
        // 这里只是一个简单的映射示例，实际应用中需要更完整的映射表
        switch (keyCode) {
            case KeyEvent.VK_A:
                return 0x00; // kVK_ANSI_A
            case KeyEvent.VK_B:
                return 0x0B; // kVK_ANSI_B
            case KeyEvent.VK_C:
                return 0x08; // kVK_ANSI_C
            // 更多键码映射...
            default:
                return (short) keyCode;
        }
    }
}