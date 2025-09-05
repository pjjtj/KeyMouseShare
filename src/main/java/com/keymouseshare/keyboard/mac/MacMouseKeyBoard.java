package com.keymouseshare.keyboard.mac;

import com.keymouseshare.bean.MoveTargetScreenInfo;
import com.keymouseshare.bean.ScreenInfo;
import com.keymouseshare.keyboard.BaseMouseKeyBoard;
import com.keymouseshare.keyboard.MouseKeyBoard;
import com.keymouseshare.storage.DeviceStorage;
import com.keymouseshare.storage.VirtualDesktopStorage;
import com.keymouseshare.util.MouseEdgeDetector;
import com.sun.jna.Platform;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


public class MacMouseKeyBoard extends BaseMouseKeyBoard implements MouseKeyBoard {

    private static final Logger logger = Logger.getLogger(MacMouseKeyBoard.class.getName());

    private static final MacMouseKeyBoard INSTANCE = new MacMouseKeyBoard();

    public static MacMouseKeyBoard getInstance() {
        return INSTANCE;
    }

    private final DeviceStorage deviceStorage = DeviceStorage.getInstance();
    private final VirtualDesktopStorage virtualDesktopStorage = VirtualDesktopStorage.getInstance();
    private ScheduledExecutorService edgeWatcherExecutor;


    private static volatile boolean edgeMode = false;


    private MacMouseKeyBoard(){
        super();
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