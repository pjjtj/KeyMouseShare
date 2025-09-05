package com.keymouseshare.keyboard.win;

import com.keymouseshare.bean.MoveTargetScreenInfo;
import com.keymouseshare.bean.ScreenInfo;
import com.keymouseshare.keyboard.BaseMouseKeyBoard;
import com.keymouseshare.keyboard.MouseKeyBoard;
import com.keymouseshare.storage.DeviceStorage;
import com.keymouseshare.storage.VirtualDesktopStorage;
import com.keymouseshare.util.MouseEdgeDetector;

import java.awt.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WindowMouseKeyBoard extends BaseMouseKeyBoard implements MouseKeyBoard {

    private static final Logger logger = Logger.getLogger(WindowMouseKeyBoard.class.getName());


    private static final WindowMouseKeyBoard INSTANCE = new WindowMouseKeyBoard();

    public static WindowMouseKeyBoard getInstance() {
        return INSTANCE;
    }

    private final DeviceStorage deviceStorage = DeviceStorage.getInstance();
    private final VirtualDesktopStorage virtualDesktopStorage = VirtualDesktopStorage.getInstance();
    private ScheduledExecutorService edgeWatcherExecutor;


    private static volatile boolean edgeMode = false;

    public WindowMouseKeyBoard() {
        super();
    }

    private void virtualScreenEdgeCheck() {
        try {
            virtualScreenEdgeCheckInternal();
        } catch (ExecutionException | InterruptedException e) {
            logger.log(Level.WARNING, "Error during virtual screen edge check", e);
            Thread.currentThread().interrupt(); // Restore interrupted state
        }
    }

    private void virtualScreenEdgeCheckInternal() throws ExecutionException, InterruptedException {
        if (virtualDesktopStorage.getActiveScreen() == null) {
            return;
        }
        MoveTargetScreenInfo moveTargetScreenInfo = MouseEdgeDetector.isAtScreenEdge();
        if (moveTargetScreenInfo != null) {
            String direction = moveTargetScreenInfo.getDirection();
            ScreenInfo screenInfo = moveTargetScreenInfo.getScreenInfo();
            // 更新激活屏幕
            if (!(screenInfo.getDeviceIp() + screenInfo.getScreenName()).equals(virtualDesktopStorage.getActiveScreen().getDeviceIp() + virtualDesktopStorage.getActiveScreen().getScreenName())) {
                System.out.println("激活设备：" + screenInfo.getDeviceIp() + ",屏幕：" + screenInfo.getScreenName());
                virtualDesktopStorage.setActiveScreen(screenInfo);
                if (direction.equals("LEFT")) {
                    virtualDesktopStorage.moveMouseLocation(-10, 0);
                }
                if (direction.equals("RIGHT")) {
                    virtualDesktopStorage.moveMouseLocation(+10, 0);
                }
                if (direction.equals("TOP")) {
                    virtualDesktopStorage.moveMouseLocation(0, -10);
                }
                if (direction.equals("BOTTOM")) {
                    virtualDesktopStorage.moveMouseLocation(0, +10);
                }
                // 被唤醒设备是控制中心
                if (screenInfo.getDeviceIp().equals(deviceStorage.getSeverDevice().getIpAddress())) {
                    System.out.println("当前设备是控制器，需要退出鼠标隐藏");

                    // 退出系统钩子
                    stopInputInterception();

                    // System.out.println("虚拟鼠标位置：" + virtualDesktopStorage.getMouseLocation()[0] + "," + virtualDesktopStorage.getMouseLocation()[1]);
                    exitEdgeMode();


                } else { // 被唤醒设备是远程设备
                    // 启动成功后调用其他方法
                    enterEdgeMode();

                    // 当前设备是控制器，需要隐藏鼠标，开启系统钩子
                    startInputInterception(event -> {});

                }
            }
        }
    }

    @Override
    public void startMouseKeyController() {
        if (edgeWatcherExecutor == null || edgeWatcherExecutor.isTerminated()) {
            edgeWatcherExecutor = Executors.newScheduledThreadPool(1);
        }
        edgeWatcherExecutor.scheduleAtFixedRate(this::virtualScreenEdgeCheck, 0, 5, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stopMouseKeyController() {
        stopInputInterception();
        exitEdgeMode();
        stopEdgeDetection();
        System.out.println("[StopMouseKeyController] resources released.");
    }

    public void startInputInterception(Consumer<WinHookEvent> eventHandler) {
        if (hookManager != null && !hookManager.isHooksActive()) {
            hookManager.startHooks(eventHandler);
            logger.log(Level.INFO, "Input interception started");
        } else {
            logger.log(Level.WARNING, "Hook manager is null or hooks are already active");
        }
    }

    public void stopInputInterception() {
        if (hookManager != null) {
            hookManager.stopHooks();
            logger.log(Level.INFO, "Input interception stopped");
        }
    }

    private void enterEdgeMode() {        // [40]
        edgeMode = true;
        virtualDesktopStorage.enterEdgeMode();
        robot.mouseMove(virtualDesktopStorage.getMouseLocation()[0] - virtualDesktopStorage.getActiveScreen().getVx(), virtualDesktopStorage.getMouseLocation()[1] - virtualDesktopStorage.getActiveScreen().getVy());
    }

    private void exitEdgeMode() {
        if (!edgeMode) return;
        edgeMode = false;
        System.out.println("控制中心鼠标位置：" + (virtualDesktopStorage.getMouseLocation()[0] - virtualDesktopStorage.getActiveScreen().getVx()) + "," + (virtualDesktopStorage.getMouseLocation()[1] - virtualDesktopStorage.getActiveScreen().getVy()));
        robot.mouseMove(virtualDesktopStorage.getMouseLocation()[0] - virtualDesktopStorage.getActiveScreen().getVx(), virtualDesktopStorage.getMouseLocation()[1] - virtualDesktopStorage.getActiveScreen().getVy());
        // 根据虚拟鼠标位置转换为控制中心鼠标位置
        virtualDesktopStorage.exitEdgeMode();
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

    @Override
    public void initVirtualMouseLocation() {
        if (virtualDesktopStorage.isApplyVirtualDesktopScreen()) {
            Point pt = MouseInfo.getPointerInfo().getLocation();
            System.out.println(pt.x + "," + pt.y);// [39]
            // 获取本地设备屏幕坐标系中的鼠标相对位置
            ScreenInfo screenInfo = deviceStorage.getLocalDevice().getScreens().stream()
                    .filter(s -> s.localContains(pt.x, pt.y))
                    .findFirst()
                    .orElse(null);
            // 修改鼠标虚拟桌面所在坐标
            if (screenInfo != null) {
                ScreenInfo vScreenInfo = virtualDesktopStorage.getScreens().get(screenInfo.getDeviceIp() + screenInfo.getScreenName());
                // 控制器上更新当前鼠标所在屏幕
                virtualDesktopStorage.setActiveScreen(vScreenInfo);
                // 控制器上更新虚拟桌面鼠标坐标
                //  pt.x-screenInfo.getDx(),pt.y-screenInfo.getDy() 本地虚拟屏幕的相对坐标位置
                //  vScreenInfo.getVx()+ pt.x-screenInfo.getDx(),vScreenInfo.getVy()+pt.y-screenInfo.getDy() 控制器虚拟桌面的绝对坐标位置
                virtualDesktopStorage.setMouseLocation(vScreenInfo.getVx() + pt.x - screenInfo.getDx(), vScreenInfo.getVy() + pt.y - screenInfo.getDy());
            }
        }
    }

}