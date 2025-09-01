package com.keymouseshare.keyboard;

import com.keymouseshare.bean.ScreenInfo;
import com.keymouseshare.storage.DeviceStorage;
import com.keymouseshare.storage.VirtualDesktopStorage;
import com.keymouseshare.util.MouseEdgeDetector;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private static volatile boolean edgeMode = false;

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
    public void mouseDragged(int x,int y) {
        if (robot != null) {
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
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
            robot.mouseWheel(wheelAmount);
        }
    }

    private void virtualScreenEdgeCheck() {
        if (virtualDesktopStorage.getActiveScreen() == null) {
            return;
        }
        int x = virtualDesktopStorage.getMouseLocation()[0];
        int y = virtualDesktopStorage.getMouseLocation()[1];
        ScreenInfo screenInfo = MouseEdgeDetector.isAtScreenEdge();
        if (screenInfo != null) {
            // 更新激活屏幕
            if(!(screenInfo.getDeviceIp()+screenInfo.getScreenName()).equals(virtualDesktopStorage.getActiveScreen().getDeviceIp()+virtualDesktopStorage.getActiveScreen().getScreenName())){
                System.out.println("激活设备："+screenInfo.getDeviceIp()+",屏幕："+screenInfo.getScreenName());
                virtualDesktopStorage.setActiveScreen(screenInfo);
                // 如果是当前设备进行鼠标控制
                if (screenInfo.getDeviceIp().equals(deviceStorage.getSeverDevice().getIpAddress())) {
                    System.out.println("当前设备是控制器，需要退出鼠标隐藏");
                    cleanup();
                    mouseMove(x-screenInfo.getVx(), y-screenInfo.getVy());
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
    public void startMouseKeyController() {
        // 启动"右边界监视"线程：当光标到达右边界 → 进入边界模式（隐藏+锁定）
        if(edgeWatcherExecutor==null||edgeWatcherExecutor.isTerminated()){
            edgeWatcherExecutor = Executors.newScheduledThreadPool(1);
        }
        edgeWatcherExecutor.scheduleAtFixedRate(this::virtualScreenEdgeCheck, 0, 5, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stopMouseKeyController() {
        cleanup();
    }

    private void enterEdgeMode() {        // [40]
        edgeMode = true;
        System.out.println("[EDGE] Enter edge-mode: hide cursor + clip to right edge");

        System.out.println("[READY] Move cursor to the RIGHT edge to enter edge-mode. Press Ctrl+Alt+Esc to quit.");
    }

    private void exitEdgeMode() {
        if (!edgeMode) return;
        edgeMode = false;
        System.out.println("[EDGE] Exit edge-mode: unclip + show cursor");
    }

    @Override
    public boolean isEdgeMode() {
        return edgeMode;
    }

    @Override
    public void stopEdgeDetection() {
        edgeWatcherExecutor.close();
    }

    private void cleanup() {
        if(edgeMode){
            exitEdgeMode();
        }

        System.out.println("[CLEAN] resources released.");
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