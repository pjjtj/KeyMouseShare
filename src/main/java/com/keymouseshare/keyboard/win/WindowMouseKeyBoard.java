package com.keymouseshare.keyboard.win;

import com.keymouseshare.bean.MoveTargetScreenInfo;
import com.keymouseshare.bean.ScreenInfo;
import com.keymouseshare.keyboard.BaseMouseKeyBoard;
import com.keymouseshare.keyboard.MouseKeyBoard;
import com.keymouseshare.storage.DeviceStorage;
import com.keymouseshare.storage.VirtualDesktopStorage;
import com.keymouseshare.util.MouseEdgeDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class WindowMouseKeyBoard extends BaseMouseKeyBoard implements MouseKeyBoard {

    private static final Logger logger = LoggerFactory.getLogger(WindowMouseKeyBoard.class);


    private static final WindowMouseKeyBoard INSTANCE = new WindowMouseKeyBoard();

    // 创建调度执行器服务，使用单线程即可
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static WindowMouseKeyBoard getInstance() {
        return INSTANCE;
    }

    private final DeviceStorage deviceStorage = DeviceStorage.getInstance();
    private final VirtualDesktopStorage virtualDesktopStorage = VirtualDesktopStorage.getInstance();
    private ScheduledExecutorService edgeWatcherExecutor;


    private WinHookManager hookManager;

    private static volatile boolean edgeMode = false;

    public WindowMouseKeyBoard() {
        super();
        hookManager = new WinHookManager();
    }

    private void virtualScreenEdgeCheck() {
        try {
            virtualScreenEdgeCheckInternal();
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error during virtual screen edge check {}", e.getMessage());
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
            logger.info("Input interception started");
        } else {
            logger.info("Hook manager is null or hooks are already active");
        }
    }

    public void stopInputInterception() {
        if (hookManager != null) {
            hookManager.stopHooks();
            logger.info("Input interception stopped");
        }
    }

    private void enterEdgeMode() {        // [40]
        edgeMode = true;
        virtualDesktopStorage.enterEdgeMode();

        try {
            Thread.sleep(300);
        }catch (Exception e) {
            e.printStackTrace();
        }

        // 检查调度器是否已经关闭，如果已关闭则创建一个新的
        if (scheduler.isShutdown()) {
            scheduler = Executors.newScheduledThreadPool(1);
        }

        ScheduledFuture<?> scheduledFuture = scheduler.scheduleAtFixedRate(() -> {
            mouseMove(virtualDesktopStorage.getMouseLocation()[0] - virtualDesktopStorage.getActiveScreen().getVx(), virtualDesktopStorage.getMouseLocation()[1] - virtualDesktopStorage.getActiveScreen().getVy());
        }, 0, 50, TimeUnit.MILLISECONDS);

        // 安排一个任务在500ms后停止调度器并取消循环任务
        scheduler.schedule(() -> {
            scheduledFuture.cancel(true); // 取消周期性任务
            scheduler.shutdown();         // 关闭调度器
            System.out.println("调度器已关闭，任务执行完毕。");
        }, 500, TimeUnit.MILLISECONDS);

    }

    private void exitEdgeMode() {
        if (!edgeMode) return;
        edgeMode = false;

        virtualDesktopStorage.exitEdgeMode();

        System.out.println("控制中心鼠标位置：" + (virtualDesktopStorage.getMouseLocation()[0] - virtualDesktopStorage.getActiveScreen().getVx()) + "," + (virtualDesktopStorage.getMouseLocation()[1] - virtualDesktopStorage.getActiveScreen().getVy()));

        // 检查调度器是否已经关闭，如果已关闭则创建一个新的
        if (scheduler.isShutdown()) {
            scheduler = Executors.newScheduledThreadPool(1);
        }

        ScheduledFuture<?> scheduledFuture = scheduler.scheduleAtFixedRate(() -> {
            // 根据虚拟鼠标位置转换为控制中心鼠标位置
            mouseMove(virtualDesktopStorage.getMouseLocation()[0] - virtualDesktopStorage.getActiveScreen().getVx(), virtualDesktopStorage.getMouseLocation()[1] - virtualDesktopStorage.getActiveScreen().getVy());
        }, 0, 50, TimeUnit.MILLISECONDS);

        // 安排一个任务在500ms后停止调度器并取消循环任务
        scheduler.schedule(() -> {
            scheduledFuture.cancel(true); // 取消周期性任务
            scheduler.shutdown();         // 关闭调度器
            System.out.println("调度器已关闭，任务执行完毕。");
            // 创建一个新的调度器以替换已关闭的调度器
            scheduler = Executors.newScheduledThreadPool(1);
        }, 1000, TimeUnit.MILLISECONDS);

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