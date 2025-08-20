package com.keymouseshare;

import com.keymouseshare.bean.DeviceInfo;
import com.keymouseshare.input.JNativeHookInputMonitor;
import com.keymouseshare.input.EventInjector;
import com.keymouseshare.util.MacOSAccessibilityHelper;
import com.keymouseshare.ui.MousePositionDisplay;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import com.keymouseshare.ui.DeviceListUI;
import com.keymouseshare.ui.ScreenPreviewUI;
import com.keymouseshare.network.DeviceDiscovery;
import com.keymouseshare.network.ControlRequestManager;

import java.net.SocketException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * 主应用程序类
 */
public class MainApplication extends Application implements DeviceDiscovery.ControlRequestListener {
    
    private static final Logger logger = Logger.getLogger(MainApplication.class.getName());
    
    private DeviceDiscovery deviceDiscovery;
    private DeviceListUI deviceListUI;
    private ScreenPreviewUI screenPreviewUI;
    private JNativeHookInputMonitor jNativeHookInputMonitor;
    private MousePositionDisplay mousePositionDisplay;
    private EventInjector eventInjector;
    private ControlRequestManager controlRequestManager;
    
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
        
        // 创建设备列表UI（左侧）
        deviceListUI = new DeviceListUI(deviceDiscovery);
        root.setLeft(deviceListUI);
        
        // 创建屏幕预览UI（中心）
        screenPreviewUI = new ScreenPreviewUI();
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

        // 初始化网络设备发现
        initDeviceDiscovery();
        
        // 初始化控制请求管理器
        initControlRequestManager(primaryStage);
        
        // 初始化JNativeHook输入监听
        initJNativeHookInputMonitoring();
        
        // 初始化事件注入器
        initEventInjector();
        
        // 创建场景并显示主窗口
        Scene scene = new Scene(root, 800, 600);
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
            deviceDiscovery.setDeviceDiscoveryListener(this); // 使用this作为监听器
            
            // 启动设备发现服务
            deviceDiscovery.startDiscovery();
            
            // 设置DeviceListUI的设备发现服务
            if (deviceListUI != null) {
                deviceListUI.setDeviceDiscovery(deviceDiscovery);
            }
            
            // 初始化设备列表
            updateDeviceList();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void onDeviceDiscovered(DeviceInfo device) {
        System.out.println("发现设备: " + device.getIpAddress());
        // 可以在这里更新UI，添加新发现的设备到设备列表
        updateDeviceList();
    }
    
    @Override
    public void onDeviceLost(DeviceInfo device) {
        System.out.println("设备离线: " + device.getIpAddress());
        // 可以在这里更新UI，从设备列表中移除离线的设备
        updateDeviceList();
    }
    
    @Override
    public void onControlRequest(String requesterIpAddress, String requesterDeviceName) {
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
                    }
                });
        }
    }
    
    /**
     * 初始化控制请求管理器
     */
    private void initControlRequestManager(Stage primaryStage) {
        controlRequestManager = new ControlRequestManager(deviceDiscovery);
        // 先初始化，但暂时不设置父窗口，因为此时场景可能还没有创建
        // 在需要时再重新设置父窗口
    }
    
    /**
     * 更新设备列表
     */
    private void updateDeviceList() {
        if (deviceDiscovery != null && deviceListUI != null) {
            List<DeviceInfo> devices = deviceDiscovery.getDiscoveredDevices();
            deviceListUI.updateDeviceList(devices);
        }
    }
    
    /**
     * 初始化JNativeHook输入监听
     */
    private void initJNativeHookInputMonitoring() {
        jNativeHookInputMonitor = new JNativeHookInputMonitor();
        // 设置鼠标位置监听器
        jNativeHookInputMonitor.setMousePositionListener((x, y) -> {
            Platform.runLater(() -> {
                if (mousePositionDisplay != null) {
                    mousePositionDisplay.updateMousePosition(x, y);
                }
            });
        });
        jNativeHookInputMonitor.startMonitoring();
        System.out.println("JNativeHook输入监听已启动");
    }
    
    /**
     * 初始化事件注入器
     */
    private void initEventInjector() {
        eventInjector = new EventInjector();
        System.out.println("事件注入器已初始化");
    }
    
    @Override
    public void stop() {
        // 应用程序关闭时停止设备发现服务
        if (deviceDiscovery != null) {
            deviceDiscovery.stopDiscovery();
        }
        
        // 停止JNativeHook输入监听
        if (jNativeHookInputMonitor != null) {
            jNativeHookInputMonitor.stopMonitoring();
        }
        
        // 停止控制请求管理器
        if (controlRequestManager != null) {
            controlRequestManager.disconnect();
            controlRequestManager.setServerMode(false); // 停止服务端
        }
        
        System.out.println("应用程序已停止");
    }
}
