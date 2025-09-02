package com.keymouseshare.keyboard.win;

import com.keymouseshare.bean.MoveTargetScreenInfo;
import com.keymouseshare.keyboard.MouseKeyBoard;
import com.keymouseshare.storage.DeviceStorage;
import com.keymouseshare.bean.ScreenInfo;
import com.keymouseshare.storage.VirtualDesktopStorage;
import com.keymouseshare.util.MouseEdgeDetector;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Consumer;

import static com.keymouseshare.util.KeyBoardUtils.getButtonMask;

public class WindowMouseKeyBoard implements MouseKeyBoard {

    private static final Logger logger = Logger.getLogger(WindowMouseKeyBoard.class.getName());


    private static final WindowMouseKeyBoard INSTANCE = new WindowMouseKeyBoard();

    public static WindowMouseKeyBoard getInstance() {
        return INSTANCE;
    }

    private final DeviceStorage deviceStorage = DeviceStorage.getInstance();
    private final VirtualDesktopStorage virtualDesktopStorage = VirtualDesktopStorage.getInstance();
    private ScheduledExecutorService edgeWatcherExecutor;


    private Robot robot;
    private WinHookManager hookManager;
    private Thread hookThread;

    private static volatile boolean edgeMode = false;

    public WindowMouseKeyBoard() {
        try {
            robot = new Robot();
            hookManager = new WinHookManager();
        } catch (AWTException e) {
            logger.log(Level.SEVERE, "无法创建Robot实例", e);
        }
    }

    @Override
    public void mouseMove(int x, int y) {
        if (robot != null) {
            robot.mouseMove(x, y);
        }
    }

    @Override
    public void mousePress(int button) {
        if (robot != null) {
            int buttonMask = getButtonMask(button);
            robot.mousePress(buttonMask);
        }
    }

    @Override
    public void mouseRelease(int button) {
        if (robot != null) {
            int buttonMask = getButtonMask(button);
            robot.mouseRelease(buttonMask);
        }
    }

    @Override
    public void mouseClick(int x, int y) {
        if (robot != null) {
            robot.mouseMove(x, y);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        }
    }

    @Override
    public void mouseDragged(int x, int y) {
        if (robot != null) {
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseMove(x, y);
        }
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

    @Override
    public void mouseWheel(int wheelAmount) {
        if (robot != null) {
            // 回退到Robot
            robot.mouseWheel(wheelAmount);
        }
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

//                    System.out.println("虚拟鼠标位置：" + virtualDesktopStorage.getMouseLocation()[0] + "," + virtualDesktopStorage.getMouseLocation()[1]);
                    exitEdgeMode();

                    // 退出系统钩子
                    stopInputInterception();
                } else { // 被唤醒设备是远程设备
                    // 当前设备是控制器，需要隐藏鼠标，开启系统钩子
                    startInputInterception(event -> {});
                    // 启动成功后调用其他方法
                    enterEdgeMode();
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
            hookThread = new Thread(() -> hookManager.startHooks(eventHandler), "WinHookThread");
            hookThread.setDaemon(true);
            hookThread.start();
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
        robot.mouseMove(0,0);
    }

    private void exitEdgeMode() {
        if (!edgeMode) return;
        edgeMode = false;
        // 根据虚拟鼠标位置转换为控制中心鼠标位置
        System.out.println("控制中心鼠标位置：" + (virtualDesktopStorage.getMouseLocation()[0] - virtualDesktopStorage.getActiveScreen().getVx()) + "," + (virtualDesktopStorage.getMouseLocation()[1] - virtualDesktopStorage.getActiveScreen().getVy()));
        robot.mouseMove(virtualDesktopStorage.getMouseLocation()[0] - virtualDesktopStorage.getActiveScreen().getVx(), virtualDesktopStorage.getMouseLocation()[1] - virtualDesktopStorage.getActiveScreen().getVy());
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

    public static void main(String[] args) {
        WindowMouseKeyBoard.getInstance().mouseMove(3820, 1000);
    }

}