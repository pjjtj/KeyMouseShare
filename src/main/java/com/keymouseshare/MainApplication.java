package com.keymouseshare;

import com.keymouseshare.bean.ControlEvent;
import com.keymouseshare.bean.ControlEventType;
import com.keymouseshare.bean.DeviceInfo;
import com.keymouseshare.bean.ScreenInfo;
import com.keymouseshare.input.JNativeHookInputMonitor;
import com.keymouseshare.keyboard.MouseKeyBoard;
import com.keymouseshare.keyboard.MouseKeyBoardFactory;
import com.keymouseshare.listener.DeviceListener;
import com.keymouseshare.listener.VirtualDesktopStorageListener;
import com.keymouseshare.network.ControlRequestManager;
import com.keymouseshare.network.DeviceDiscovery;
import com.keymouseshare.storage.DeviceStorage;
import com.keymouseshare.storage.VirtualDesktopStorage;
import com.keymouseshare.uifx.DeviceListUI;
import com.keymouseshare.uifx.MousePositionDisplay;
import com.keymouseshare.uifx.ScreenPreviewUI;
import com.keymouseshare.uifx.TransparentFullScreenFxUtils;
import com.keymouseshare.util.MacOSAccessibilityHelper;
import com.keymouseshare.util.NetUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketException;
import java.util.Map;

/**
 * 主应用程序类
 */
public class MainApplication extends Application implements DeviceListener, VirtualDesktopStorageListener, JNativeHookInputMonitor.MouseKeyBoardEventListener {

    private static final Logger logger = LoggerFactory.getLogger(MainApplication.class);

    private DeviceDiscovery deviceDiscovery;
    private DeviceListUI deviceListUI;
    private ScreenPreviewUI screenPreviewUI;
    private JNativeHookInputMonitor jNativeHookInputMonitor;
    private MousePositionDisplay mousePositionDisplay;
    private ControlRequestManager controlRequestManager;
    private MouseKeyBoard mouseKeyBoard;
    private DeviceStorage deviceStorage = DeviceStorage.getInstance();
    private VirtualDesktopStorage virtualDesktopStorage = VirtualDesktopStorage.getInstance();

    public MainApplication() throws SocketException {
    }


    public static void main(String[] args) {
        // 设置高DPI相关的系统属性，以获取真实的屏幕尺寸
        // 启用高分辨率渲染
        System.setProperty("sun.java2d.uiScale", "1.0");
        // 禁用DPI缩放感知，获取真实像素坐标
        System.setProperty("sun.java2d.dpiaware", "false");
        // 对于JavaFX，设置使用真正的像素而不是逻辑像素
        System.setProperty("javafx.css.scalingFactor", "1.0");
        System.setProperty("prism.allowhidpi", "false");

        // 启动JavaFX应用程序
        launch(args);

    }

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        // 检查并提示macOS辅助功能授权
        checkAndPromptAccessibilityPermission();

        // 初始化网络设备发现
        initDeviceDiscovery();

        // 创建设备列表UI（左侧）
        deviceListUI = new DeviceListUI(deviceDiscovery);
        root.setLeft(deviceListUI);

        // 创建屏幕预览UI（中心）
        screenPreviewUI = new ScreenPreviewUI(virtualDesktopStorage);
        root.setCenter(screenPreviewUI);

        // 创建鼠标位置显示器（底部）
        mousePositionDisplay = new MousePositionDisplay();
        root.setBottom(mousePositionDisplay);

        // 设置设备选中回调，实现设备列表与屏幕预览的联动
        deviceListUI.setOnDeviceSelectedWithIP((ipAddress) -> {
            // 处理设备选中逻辑
            System.out.println("设备 " + ipAddress + " 被选中");

            // 根据选中的设备IP选择对应的屏幕
            screenPreviewUI.selectScreen(ipAddress);
        });

        // 初始化控制请求管理器
        initControlRequestManager(primaryStage);

        // 初始化JNativeHook输入监听
        initJNativeHookInputMonitoring();

        // 初始化Windows鼠标键盘钩子
        initMouseKeyBoard();

        // 创建场景并显示主窗口
        Scene scene = new Scene(root, 900, 600);
        primaryStage.setTitle("KeyMouseShare - 键盘鼠标共享工具");
        primaryStage.setScene(scene);
        primaryStage.show();

        // 在窗口显示后设置ControlRequestManager的父窗口
        if (controlRequestManager != null) {
            controlRequestManager.setParentWindow(primaryStage.getScene().getWindow());
        }

        // 将DeviceListUI与控制请求管理器关联
        if (deviceListUI != null && controlRequestManager != null) {
            deviceListUI.setControlRequestManager(controlRequestManager);
        }

        virtualDesktopStorage.addListener(this);
    }

    /**
     * 检查并提示macOS辅助功能授权
     */
    private void checkAndPromptAccessibilityPermission() {
        // 使用工具类检查并提示macOS辅助功能授权
        MacOSAccessibilityHelper.checkAndPromptAccessibilityPermission();
    }

    /**
     * 初始化设备发现服务
     */
    private void initDeviceDiscovery() {
        try {
            deviceDiscovery = new DeviceDiscovery();

            // 设置设备发现监听器
            deviceDiscovery.setDeviceListener(this); // 使用this作为监听器

            // 启动设备发现服务
            deviceDiscovery.startDiscovery();

            // 设置DeviceListUI的设备发现服务
            if (deviceListUI != null) {
                deviceListUI.setDeviceDiscovery(deviceDiscovery);
            }

            // 初始化设备列表
            Platform.runLater(this::updateDeviceList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDeviceLost(DeviceInfo device) {
        System.out.println("设备离线: " + device.getIpAddress());
        // 可以在这里更新UI，从设备列表中移除离线的设备
        Platform.runLater(this::updateDeviceList);
    }

    @Override
    public void onDeviceUpdate(DeviceInfo device) {
        Platform.runLater(this::updateDeviceList);
    }

    @Override
    public void onServerStart() {
        Platform.runLater(this::updateDeviceList);
        Platform.runLater(this::serverDeviceStart);
    }

    @Override
    public void onServerClose() {
        Platform.runLater(this::updateDeviceList);
        Platform.runLater(this::serverDeviceStop);
    }

    @Override
    public void onControlRequest(String requesterIpAddress) {
        // 显示控制请求对话框
        if (controlRequestManager != null) {
            controlRequestManager.showPermissionDialog(requesterIpAddress)
                    .thenAccept(permissionGranted -> {
                        // 这里可以处理权限授予或拒绝后的逻辑
                        if (permissionGranted) {
                            System.out.println("用户授权控制请求: " + requesterIpAddress);
                            // 建立与服务器的Netty连接
                            try {
                                controlRequestManager.establishConnection(requesterIpAddress);
                            } catch (Exception e) {
                                System.err.println("建立Netty连接失败: " + e.getMessage());
                                e.printStackTrace();
                            }
                        } else {
                            System.out.println("用户拒绝控制请求: " + requesterIpAddress);
                            // TODO 处理拒绝逻辑
                            controlRequestManager.rejectConnection(requesterIpAddress);
                        }
                    });
        }
    }

    /**
     * 初始化控制请求管理器
     */
    private void initControlRequestManager(Stage primaryStage) {
        controlRequestManager = new ControlRequestManager();
        // 先初始化，但暂时不设置父窗口，因为此时场景可能还没有创建
        // 在需要时再重新设置父窗口
    }

    /**
     * 更新设备列表
     */
    private void updateDeviceList() {
        if (deviceDiscovery != null && deviceListUI != null) {
            deviceListUI.updateDeviceList();
        }
    }

    /**
     * 更新设备列表
     */
    private void serverDeviceStart() {
        if (deviceDiscovery != null && deviceListUI != null && screenPreviewUI != null) {
            deviceListUI.serverDeviceStart();
            screenPreviewUI.serverDeviceStart();
            screenPreviewUI.refreshScreens();
        }
    }

    /**
     * 停止设备列表
     */
    private void serverDeviceStop() {
        if (deviceDiscovery != null && deviceListUI != null && screenPreviewUI != null) {
            deviceListUI.serverDeviceStop();
            screenPreviewUI.serverDeviceStop();
        }
    }

    /**
     * 初始化JNativeHook输入监听
     */
    private void initJNativeHookInputMonitoring() {
        jNativeHookInputMonitor = new JNativeHookInputMonitor();
        // 设置主应用程序引用
        jNativeHookInputMonitor.setMainApplication(this);

        // 设置鼠标事件监听器
        jNativeHookInputMonitor.setMouseEventListener(this);

        jNativeHookInputMonitor.startMonitoring();
    }

    /**
     * 初始化Windows鼠标键盘钩子
     */
    private void initMouseKeyBoard() {
        mouseKeyBoard = MouseKeyBoardFactory.getFactory();
        System.out.println("鼠标键盘已初始化");
    }


    @Override
    public void stop() throws Exception {
        // 应用程序关闭时停止设备发现服务
        try {
            if (deviceStorage.getSeverDevice().getIpAddress().equals(NetUtil.getLocalIpAddress())) {
                deviceDiscovery.sendServerCloseBroadcast();
            } else {
                // TODO 是否即刻发送下线广播

            }
        } catch (Exception e) {
            logger.error("发送服务器关闭广播失败: {}", e.getMessage());
        }
        deviceDiscovery.stopDiscovery();

        // 停止JNativeHook输入监听
        if (jNativeHookInputMonitor != null) {
            jNativeHookInputMonitor.stopMonitoring();
        }

        // 停止控制请求管理器
        if (controlRequestManager != null) {
            controlRequestManager.disconnect();
            controlRequestManager.setServerMode(false); // 停止服务端
        }

        virtualDesktopStorage.setApplyVirtualDesktopScreen(false);
        mouseKeyBoard.stopMouseKeyController();

    }

    @Override
    public void onVirtualDesktopChanged() {
        if (virtualDesktopStorage != null && screenPreviewUI != null) {
            Platform.runLater(() -> screenPreviewUI.refreshScreens());
            ;
        }
    }

    @Override
    public void onApplyVirtualDesktopScreen(Map<StackPane, String> screenMap, double scale) {
        screenMap.keySet().forEach(screen -> {
            ScreenInfo screenInfo = virtualDesktopStorage.getScreens().get(screenMap.get(screen));
            // 更新屏幕在画布中的位置
            screenInfo.setMx((int) screen.getBoundsInParent().getMinX());
            screenInfo.setMy((int) screen.getBoundsInParent().getMinY());
            // 更新屏幕在虚拟桌面中的位置
            screenInfo.setVx((int) (screen.getBoundsInParent().getMinX() * scale));
            screenInfo.setVy((int) (screen.getBoundsInParent().getMinY() * scale));
            System.out.println(screenInfo.getVx() + "-----" + screenInfo.getVy());
            virtualDesktopStorage.applyScreen(screenInfo);
        });

        virtualDesktopStorage.setApplyVirtualDesktopScreen(true);

        // 初始化鼠标在虚拟桌面中的位置、更新当前激活的虚拟屏幕
        mouseKeyBoard.initVirtualMouseLocation();

        // 开启鼠标位置检测控制
        mouseKeyBoard.startMouseKeyController();

    }

    public void cancelKeyMouseShare() {
        mouseKeyBoard.stopMouseKeyController();
    }

    @Override
    public void onMouseMove(int x, int y) {
        Platform.runLater(() -> {
            if (mousePositionDisplay != null) {
                mousePositionDisplay.updateMousePosition(x, y);
            }
        });

        if (virtualDesktopStorage.isApplyVirtualDesktopScreen()) {
            ScreenInfo vScreenInfo = virtualDesktopStorage.getActiveScreen();
            //  vScreenInfo.getVx()+ pt.x-screenInfo.getDx(),vScreenInfo.getVy()+pt.y-screenInfo.getDy() 控制器虚拟桌面的绝对坐标位置
            if (vScreenInfo != null) {
                if (mouseKeyBoard.isEdgeMode()) {
                    virtualDesktopStorage.setMouseLocation(vScreenInfo.getVx() + x, vScreenInfo.getVy() + y);
                    // 鼠标移动事件处理
                    // 这里可以添加鼠标移动的特殊处理逻辑
                    // 例如：发送鼠标移动事件到远程设备
                    if (controlRequestManager != null) {
                        // 发送鼠标移动事件到远程设备
                        if (x != 0 || y != 0) {
//                            System.out.println("鼠标相对位置：" + x + " " + y);
                            controlRequestManager.sendControlRequest(new ControlEvent(virtualDesktopStorage.getActiveScreen().getDeviceIp(), ControlEventType.MouseMoved.name(),
                                    virtualDesktopStorage.getMouseLocation()[0] - virtualDesktopStorage.getActiveScreen().getVx(),
                                    virtualDesktopStorage.getMouseLocation()[1] - virtualDesktopStorage.getActiveScreen().getVy()));
                        }
                    }
                } else {
//                    System.out.println("当前不是边缘模式，鼠标位置：" + x + " " + y);
                    virtualDesktopStorage.setMouseLocation(vScreenInfo.getVx() + x - vScreenInfo.getDx(), vScreenInfo.getVy() + y - vScreenInfo.getDy());
                }
            }
        }
    }

    @Override
    public void onMousePress(int button, int x, int y) {
        // 鼠标按下事件处理
        if (controlRequestManager != null && mouseKeyBoard.isEdgeMode()) {
            // 发送鼠标按下事件到远程设备
            // 如果有激活的屏幕，设置设备IP和屏幕名
            if (virtualDesktopStorage.getActiveScreen() != null) {
                ControlEvent event = new ControlEvent(virtualDesktopStorage.getActiveScreen().getDeviceIp(), ControlEventType.MousePressed.name(), x, y);
                event.setButton(button);
                event.setScreenName(virtualDesktopStorage.getActiveScreen().getScreenName());
                controlRequestManager.sendControlRequest(event);
            }
        }
    }

    @Override
    public void onMouseRelease(int button, int x, int y) {
        // 鼠标释放事件处理
        if (controlRequestManager != null && mouseKeyBoard.isEdgeMode()) {
            // 发送鼠标释放事件到远程设备
            // 如果有激活的屏幕，设置设备IP和屏幕名
            if (virtualDesktopStorage.getActiveScreen() != null) {
                ControlEvent event = new ControlEvent(virtualDesktopStorage.getActiveScreen().getDeviceIp(), ControlEventType.MouseReleased.name(), x, y);
                event.setButton(button);
                event.setScreenName(virtualDesktopStorage.getActiveScreen().getScreenName());
                controlRequestManager.sendControlRequest(event);
            }
        }
    }

    @Override
    public void onMouseWheel(int rotation, int x, int y) {
        // 鼠标滚轮事件处理
        if (controlRequestManager != null && mouseKeyBoard.isEdgeMode()) {
            // 创建一个特殊的控制事件来表示滚轮事件
            // 如果有激活的屏幕，设置设备IP和屏幕名
            if (virtualDesktopStorage.getActiveScreen() != null) {
                ControlEvent event = new ControlEvent(virtualDesktopStorage.getActiveScreen().getDeviceIp(), ControlEventType.MouseWheel.name(), x, y);
                event.setButton(rotation); // 使用button字段存储滚轮旋转值
                event.setScreenName(virtualDesktopStorage.getActiveScreen().getScreenName());
                controlRequestManager.sendControlRequest(event);
            }
        }
    }

    @Override
    public void onKeyPress(int keyCode) {
        // 鼠标滚轮事件处理
        if (controlRequestManager != null && mouseKeyBoard.isEdgeMode()) {
            // 创建一个特殊的控制事件来表示滚轮事件
            // 如果有激活的屏幕，设置设备IP和屏幕名
            if (virtualDesktopStorage.getActiveScreen() != null) {
                ControlEvent event = new ControlEvent(virtualDesktopStorage.getActiveScreen().getDeviceIp(), ControlEventType.KeyPressed.name(), keyCode);
                event.setScreenName(virtualDesktopStorage.getActiveScreen().getScreenName());
                controlRequestManager.sendControlRequest(event);
            }
        }
    }

    @Override
    public void onKeyRelease(int keyCode) {
        // 鼠标滚轮事件处理
        if (controlRequestManager != null && mouseKeyBoard.isEdgeMode()) {
            // 创建一个特殊的控制事件来表示滚轮事件
            // 如果有激活的屏幕，设置设备IP和屏幕名
            if (virtualDesktopStorage.getActiveScreen() != null) {
                ControlEvent event = new ControlEvent(virtualDesktopStorage.getActiveScreen().getDeviceIp(), ControlEventType.KeyReleased.name(), keyCode);
                event.setScreenName(virtualDesktopStorage.getActiveScreen().getScreenName());
                controlRequestManager.sendControlRequest(event);
            }
        }
    }

    @Override
    public void onEnterEdgeMode() {
        Platform.runLater(TransparentFullScreenFxUtils::openTransparentOverlayHiddenCursor);
    }

    @Override
    public void onExitEdgeMode() {
        Platform.runLater(TransparentFullScreenFxUtils::closeFullScreenAndRestoreCursor);
    }
}