package com.keymouseshare.keyboard;

import com.keymouseshare.bean.DeviceStorage;
import com.keymouseshare.bean.ScreenInfo;
import com.keymouseshare.bean.VirtualDesktopStorage;
import com.keymouseshare.util.MouseEdgeDetector;
import com.sun.jna.*;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.Arrays;

import static com.keymouseshare.util.KeyBoardUtils.*;

public class MacMouseKeyBoard implements MouseKeyBoard {

    private static final Logger logger = Logger.getLogger(MacMouseKeyBoard.class.getName());

    private static final MacMouseKeyBoard INSTANCE = new MacMouseKeyBoard();

    public static MacMouseKeyBoard getInstance() {
        return INSTANCE;
    }

    private final DeviceStorage deviceStorage = DeviceStorage.getInstance();
    private final VirtualDesktopStorage virtualDesktopStorage = VirtualDesktopStorage.getInstance();
    private ScheduledExecutorService edgeWatcherExecutor;

    private Robot robot;
    private static volatile boolean edgeMode = false;

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
        
        /**
         * 创建事件
         * @param source 事件源
         * @return 事件指针
         */
        Pointer CGEventCreate(Pointer source);
        
        /**
         * 获取事件位置
         * @param event 事件指针
         * @return 位置点
         */
        CGPoint CGEventGetLocation(Pointer event);
    }



    // macOS CGPoint结构体
    public class CGPoint extends Structure {
        public static class ByValue extends CGPoint implements Structure.ByValue {}
        public double x;
        public double y;
        public CGPoint() {}
        public CGPoint(double x, double y) { this.x = x; this.y = y; }
        @Override
        protected java.util.List<String> getFieldOrder() {
            return java.util.Arrays.asList("x", "y");
        }
    }

    // macOS事件类型常量
    private static final int kCGEventMouseMoved = 5;
    private static final int kCGEventLeftMouseDown = 1;
    private static final int kCGEventLeftMouseUp = 2;
    private static final int kCGEventRightMouseDown = 3;
    private static final int kCGEventRightMouseUp = 4;
    private static final int kCGEventOtherMouseDown = 25;
    private static final int kCGEventOtherMouseUp = 26;
    
    private static final int kCGHIDEventTap = 0;
    private static final int kCGMouseButtonLeft = 0;
    private static final int kCGMouseButtonRight = 1;
    private static final int kCGMouseButtonCenter = 2;

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
                    CGPoint.ByValue point = new CGPoint.ByValue();
                    point.x = x;
                    point.y = y;
                    Pointer event = CoreGraphics.INSTANCE.CGEventCreateMouseEvent(Pointer.NULL, kCGEventMouseMoved, point, kCGMouseButtonLeft); // kCGEventMouseMoved
                    if (event != null) {
                        CoreGraphics.INSTANCE.CGEventPost(kCGHIDEventTap, event); // kCGHIDEventTap
                        CoreGraphics.INSTANCE.CFRelease(event);
                        return;
                    }
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "JNA鼠标移动异常，回退到Robot", e);
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
                    int mouseButton = getMacMouseButton(button);
                    CGPoint point = getCurrentMouseLocation();
                    Pointer event = CoreGraphics.INSTANCE.CGEventCreateMouseEvent(null, eventType, point, mouseButton);
                    if (event != null) {
                        CoreGraphics.INSTANCE.CGEventPost(kCGHIDEventTap, event); // kCGHIDEventTap
                        CoreGraphics.INSTANCE.CFRelease(event);
                        return;
                    }
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "JNA鼠标按下异常，回退到Robot", e);
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
                    int mouseButton = getMacMouseButton(button);
                    CGPoint point = getCurrentMouseLocation();
                    Pointer event = CoreGraphics.INSTANCE.CGEventCreateMouseEvent(null, eventType, point, mouseButton);
                    if (event != null) {
                        CoreGraphics.INSTANCE.CGEventPost(kCGHIDEventTap, event); // kCGHIDEventTap
                        CoreGraphics.INSTANCE.CFRelease(event);
                        return;
                    }
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "JNA鼠标释放异常，回退到Robot", e);
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
                    CGPoint point = new CGPoint(x, y);
                    // 创建并注入鼠标左键按下事件
                    Pointer mouseDownEvent = CoreGraphics.INSTANCE.CGEventCreateMouseEvent(null, kCGEventLeftMouseDown, point, kCGMouseButtonLeft); // kCGEventLeftMouseDown
                    if (mouseDownEvent != null) {
                        CoreGraphics.INSTANCE.CGEventPost(kCGHIDEventTap, mouseDownEvent); // kCGHIDEventTap

                        // 创建并注入鼠标左键释放事件
                        Pointer mouseUpEvent = CoreGraphics.INSTANCE.CGEventCreateMouseEvent(null, kCGEventLeftMouseUp, point, kCGMouseButtonLeft); // kCGEventLeftMouseUp
                        if (mouseUpEvent != null) {
                            CoreGraphics.INSTANCE.CGEventPost(kCGHIDEventTap, mouseUpEvent); // kCGHIDEventTap

                            // 释放事件
                            CoreGraphics.INSTANCE.CFRelease(mouseDownEvent);
                            CoreGraphics.INSTANCE.CFRelease(mouseUpEvent);
                            return;
                        } else {
                            CoreGraphics.INSTANCE.CFRelease(mouseDownEvent);
                        }
                    }
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "JNA鼠标点击异常，回退到Robot", e);
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
                    Pointer event = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, macKeyCode, true);
                    if (event != null) {
                        CoreGraphics.INSTANCE.CGEventPost(kCGHIDEventTap, event); // kCGHIDEventTap
                        CoreGraphics.INSTANCE.CFRelease(event);
                        return;
                    }
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入键盘事件，回退到Robot", e);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "JNA键盘按下异常，回退到Robot", e);
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
                    Pointer event = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, macKeyCode, false);
                    if (event != null) {
                        CoreGraphics.INSTANCE.CGEventPost(kCGHIDEventTap, event); // kCGHIDEventTap
                        CoreGraphics.INSTANCE.CFRelease(event);
                        return;
                    }
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入键盘事件，回退到Robot", e);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "JNA键盘释放异常，回退到Robot", e);
                }
            }

            // 回退到Robot
            robot.keyRelease(keyCode);
        }
    }

    /**
     * 获取当前鼠标位置
     * @return 当前鼠标位置的CGPoint
     */
    private CGPoint getCurrentMouseLocation() {
        try {
            Pointer event = CoreGraphics.INSTANCE.CGEventCreate(null);
            if (event != null) {
                CGPoint point = CoreGraphics.INSTANCE.CGEventGetLocation(event);
                CoreGraphics.INSTANCE.CFRelease(event);
                return point;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "无法获取当前鼠标位置", e);
        }
        // 如果无法获取，返回默认位置(0,0)
        return new CGPoint(0, 0);
    }

    /**
     * 获取macOS鼠标事件类型
     * @param button 按钮类型
     * @param isPress 是否按下
     * @return 对应的事件类型
     */
    private int getMacMouseEventType(int button, boolean isPress) {
        switch (button) {
            case 1: // 左键
                return isPress ? kCGEventLeftMouseDown : kCGEventLeftMouseUp;
            case 2: // 右键
                return isPress ? kCGEventRightMouseDown : kCGEventRightMouseUp;
            case 3: // 中键
                return isPress ? kCGEventOtherMouseDown : kCGEventOtherMouseUp;
            default:
                return isPress ? kCGEventLeftMouseDown : kCGEventLeftMouseUp;
        }
    }

    /**
     * 获取macOS鼠标按钮类型
     * @param button 按钮类型
     * @return 对应的按钮类型
     */
    private int getMacMouseButton(int button) {
        switch (button) {
            case 1: // 左键
                return kCGMouseButtonLeft;
            case 2: // 右键
                return kCGMouseButtonRight;
            case 3: // 中键
                return kCGMouseButtonCenter;
            default:
                return kCGMouseButtonLeft;
        }
    }

    private void virtualScreenEdgeCheck() {
        if (virtualDesktopStorage.getActiveScreen() == null) {
            return;
        }
        int x = virtualDesktopStorage.getMouseLocation()[0];
        int y = virtualDesktopStorage.getMouseLocation()[1];
        ScreenInfo screenInfo = MouseEdgeDetector.isAtScreenEdge(x, y);
        if (screenInfo != null) {
            // 更新激活屏幕
            if(!(screenInfo.getDeviceIp()+screenInfo.getScreenName()).equals(virtualDesktopStorage.getActiveScreen().getDeviceIp()+virtualDesktopStorage.getActiveScreen().getScreenName())){
                System.out.println("激活设备："+screenInfo.getDeviceIp()+",屏幕："+screenInfo.getScreenName());
                virtualDesktopStorage.setActiveScreen(screenInfo);
                // 如果是当前设备进行鼠标控制
                if (screenInfo.getDeviceIp().equals(deviceStorage.getSeverDevice().getIpAddress())) {
                    System.out.println("当前设备是控制器，需要退出鼠标隐藏");
                    exitEdgeMode();
                } else {
                    if (!edgeMode) {
                        System.out.println("当前设备是控制器，需要隐藏鼠标");
                        enterEdgeMode();
                    }
                }
            }
        }
    }

    @Override
    public void initVirtualMouseLocation() {
        if (virtualDesktopStorage.isApplyVirtualDesktopScreen()) {
            try {
                CGPoint point = getCurrentMouseLocation();
                System.out.println(point.x + "," + point.y);
                
                // 获取本地设备屏幕坐标系中的鼠标相对位置
                ScreenInfo screenInfo = deviceStorage.getLocalDevice().getScreens().stream()
                        .filter(s -> s.localContains((int)point.x, (int)point.y))
                        .findFirst()
                        .orElse(null);
                        
                // 修改鼠标虚拟桌面所在坐标
                if (screenInfo != null) {
                    ScreenInfo vScreenInfo = virtualDesktopStorage.getScreens().get(screenInfo.getDeviceIp() + screenInfo.getScreenName());
                    // 控制器上更新当前鼠标所在屏幕
                    virtualDesktopStorage.setActiveScreen(vScreenInfo);
                    // 控制器上更新虚拟桌面鼠标坐标
                    virtualDesktopStorage.setMouseLocation(
                        vScreenInfo.getVx() + (int)point.x - screenInfo.getDx(), 
                        vScreenInfo.getVy() + (int)point.y - screenInfo.getDy()
                    );
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "初始化虚拟鼠标位置失败", e);
            }
        }
    }

    @Override
    public void startMouseKeyController() {
        if (!Platform.isMac()) {
            return; // 仅在macOS上实现
        }

        // 启动边缘监视线程
        if(edgeWatcherExecutor == null || edgeWatcherExecutor.isTerminated()) {
            edgeWatcherExecutor = Executors.newScheduledThreadPool(1);
        }
        edgeWatcherExecutor.scheduleAtFixedRate(this::virtualScreenEdgeCheck, 0, 5, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stopMouseKeyController() {
        cleanup();
    }

    private void enterEdgeMode() {
        edgeMode = true;
        System.out.println("[EDGE] Enter edge-mode: hide cursor");
        // 在macOS中隐藏光标可能需要使用其他方法
        // 这里可以添加具体的实现
    }

    private void exitEdgeMode() {
        if (!edgeMode) return;
        edgeMode = false;
        System.out.println("[EDGE] Exit edge-mode: show cursor");
        // 在macOS中显示光标可能需要使用其他方法
        // 这里可以添加具体的实现
    }

    @Override
    public boolean isEdgeMode() {
        return edgeMode;
    }

    @Override
    public void stopEdgeDetection() {
        if (edgeWatcherExecutor != null) {
            edgeWatcherExecutor.shutdown();
        }
    }

    private void cleanup() {
        try {
            showCursor();
        } catch (Throwable ignored) {
        }
        
        if (edgeMode) {
            exitEdgeMode();
        }

        System.out.println("[CLEAN] resources released.");
    }

    /**
     * 显示光标
     */
    private void showCursor() {
        // 在macOS中显示光标的实现
        // 可能需要使用其他API
    }
}