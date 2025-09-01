package com.keymouseshare.keyboard.linux;

import com.keymouseshare.keyboard.MouseKeyBoard;
import com.keymouseshare.storage.DeviceStorage;
import com.keymouseshare.bean.ScreenInfo;
import com.keymouseshare.storage.VirtualDesktopStorage;
import com.keymouseshare.util.MouseEdgeDetector;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.keymouseshare.util.KeyBoardUtils.getButtonMask;

public class LinuxMouseKeyBoard implements MouseKeyBoard {

    private static final Logger logger = Logger.getLogger(LinuxMouseKeyBoard.class.getName());

    private static final LinuxMouseKeyBoard INSTANCE = new LinuxMouseKeyBoard();

    public static LinuxMouseKeyBoard getInstance() {
        return INSTANCE;
    }

    private final DeviceStorage deviceStorage = DeviceStorage.getInstance();
    private final VirtualDesktopStorage virtualDesktopStorage = VirtualDesktopStorage.getInstance();
    private ScheduledExecutorService edgeWatcherExecutor;

    private Robot robot;
    private static volatile boolean edgeMode = false;

    // X11库接口
    public interface X11 extends Library {
        X11 INSTANCE = Native.load("X11", X11.class);

        Pointer XOpenDisplay(String displayName);
        int XCloseDisplay(Pointer display);
        int XWarpPointer(Pointer display, Pointer srcW, Pointer destW, int srcX, int srcY, int srcWidth, int srcHeight, int destX, int destY);
        int XFlush(Pointer display);
    }

    private Pointer x11Display;

    public LinuxMouseKeyBoard() {
        try {
            robot = new Robot();
            
            // 尝试加载X11库
            if (Platform.isLinux()) {
                try {
                    x11Display = X11.INSTANCE.XOpenDisplay(null);
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法加载X11库", e);
                }
            }
        } catch (AWTException e) {
            logger.log(Level.SEVERE, "无法创建Robot实例", e);
        }
    }

    @Override
    public void mouseMove(int x, int y) {
        if (robot != null) {
            // 尝试使用X11库移动鼠标
            if (x11Display != null) {
                try {
                    X11.INSTANCE.XWarpPointer(x11Display, null, null, 0, 0, 0, 0, x, y);
                    X11.INSTANCE.XFlush(x11Display);
                    return;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "使用X11移动鼠标失败，回退到Robot", e);
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
    public void mouseDragged() {
        // 鼠标拖拽事件处理
        // 在这个接口中，拖拽被视为鼠标移动，具体实现在mouseMove方法中
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

    private void enterEdgeMode() {
        edgeMode = true;
        System.out.println("[EDGE] Enter edge-mode: hide cursor + clip to right edge");
        System.out.println("[READY] Move cursor to the RIGHT edge to enter edge-mode. Press Ctrl+Alt+Esc to quit.");
        robot.mouseMove(-1,-1);
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
        if (edgeWatcherExecutor != null) {
            edgeWatcherExecutor.shutdown();
        }
    }

    private void cleanup() {
        if(edgeMode){
            exitEdgeMode();
        }
        
        // 关闭X11显示连接
        if (x11Display != null) {
            try {
                X11.INSTANCE.XCloseDisplay(x11Display);
            } catch (Exception e) {
                logger.log(Level.WARNING, "关闭X11显示连接失败", e);
            }
            x11Display = null;
        }

        System.out.println("[CLEAN] resources released.");
    }

    @Override
    public void initVirtualMouseLocation() {
        if (virtualDesktopStorage.isApplyVirtualDesktopScreen()) {
            Point pt = MouseInfo.getPointerInfo().getLocation();
            System.out.println(pt.x + "," + pt.y);
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
    
    /**
     * 注意：鼠标滚轮事件由ControlClientHandler直接使用Robot类处理，
     * 不通过MouseKeyBoard接口，因为标准的MouseKeyBoard接口没有定义滚轮方法。
     * 如果需要在MouseKeyBoard中处理滚轮事件，可以添加mouseWheel方法。
     */
}