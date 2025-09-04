package com.keymouseshare.keyboard.mac;

import com.keymouseshare.bean.MoveTargetScreenInfo;
import com.keymouseshare.bean.ScreenInfo;
import com.keymouseshare.keyboard.MouseKeyBoard;
import com.keymouseshare.storage.DeviceStorage;
import com.keymouseshare.storage.VirtualDesktopStorage;
import com.keymouseshare.util.KeyBoardUtils;
import com.keymouseshare.util.MouseEdgeDetector;
import com.keymouseshare.util.NativeToAwtKeyEventMapper;
import com.sun.jna.Platform;

import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.keymouseshare.util.KeyBoardUtils.getButtonMask;
import static com.keymouseshare.util.KeyBoardUtils.getMacMouseButton;

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
            // 回退到Robot
            robot.mouseMove(x, y);
        }
    }

    @Override
    public void mousePress(int button) {
        if (robot != null) {
            // 回退到Robot
            robot.mousePress(button);
        }
    }

    @Override
    public void mouseRelease(int button) {
        if (robot != null) {
            // 回退到Robot
            robot.mouseRelease(button);
        }
    }


    @Override
    public void mouseWheel(int wheelAmount) {
        if (robot != null) {
            // 回退到Robot
            robot.mouseWheel(wheelAmount);
        }
    }


    @Override
    public void mouseClick(int x, int y) {
//        if (robot != null) {
//            // 回退到Robot
//            robot.mouseMove(x, y);
//            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
//            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
//        }
    }

    @Override
    public void mouseDragged(int x, int y) {
//        // 鼠标拖拽事件处理
//        // 在这个接口中，拖拽被视为鼠标移动，具体实现在mouseMove方法中
//        if (robot != null) {
//            // 回退到Robot
//            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
//            robot.mouseMove(x, y);
//        }
    }

    @Override
    public void keyPress(int keyCode) {
        if (robot != null) {
            robot.keyPress(keyCode);
        }
    }

    @Override
    public void keyRelease(int keyCode) {
        if (robot != null) {
            robot.keyRelease(keyCode);
        }
    }

    private void virtualScreenEdgeCheck() {
        if (virtualDesktopStorage.getActiveScreen() == null) {
            return;
        }
        int x = virtualDesktopStorage.getMouseLocation()[0];
        int y = virtualDesktopStorage.getMouseLocation()[1];
        MoveTargetScreenInfo moveTargetScreenInfo = MouseEdgeDetector.isAtScreenEdge();
        if (moveTargetScreenInfo != null) {

            ScreenInfo screenInfo = moveTargetScreenInfo.getScreenInfo();
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