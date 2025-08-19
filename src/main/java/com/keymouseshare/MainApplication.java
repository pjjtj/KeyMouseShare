package com.keymouseshare;

import com.keymouseshare.bean.DeviceInfo;
import com.keymouseshare.input.JNAInputMonitor;
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
import com.keymouseshare.util.JNAMousePosition;

import java.net.SocketException;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * 主应用程序类
 */
public class MainApplication extends Application {
    
    private static final Logger logger = Logger.getLogger(MainApplication.class.getName());
    
    private DeviceDiscovery deviceDiscovery;
    private DeviceListUI deviceListUI;
    private ScreenPreviewUI screenPreviewUI;
    private JNAInputMonitor jnaInputMonitor;
    private MousePositionDisplay mousePositionDisplay;
    private Timer mousePositionTimer;
    
    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        
        // 检查并提示macOS辅助功能授权
        checkAndPromptAccessibilityPermission();
        
        // 创建设备列表UI（左侧）
        deviceListUI = new DeviceListUI();
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

        // 初始化并启动设备发现服务
        initDeviceDiscovery();
        
        // 初始化并启动JNA输入监听
        initJNAInputMonitoring();
        
        // 启动鼠标位置定时更新
        startMousePositionUpdates();
        
        Scene scene = new Scene(root, 1000, 600);
        primaryStage.setTitle("KeyMouseShare");
        primaryStage.setScene(scene);
        primaryStage.show();
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
            deviceDiscovery.setDeviceDiscoveryListener(new DeviceDiscovery.DeviceDiscoveryListener() {
                @Override
                public void onDeviceDiscovered(DeviceInfo device) {
                    System.out.println("发现设备: " + device.getIpAddress());
                    // 可以在这里更新UI，添加新发现的设备到设备列表
                }
                
                @Override
                public void onDeviceLost(DeviceInfo device) {
                    System.out.println("设备离线: " + device.getIpAddress());
                    // 可以在这里更新UI，从设备列表中移除离线设备
                }
            });
            
            // 启动设备发现服务
            deviceDiscovery.startDiscovery();
            
        } catch (SocketException e) {
            System.err.println("创建设备发现服务时出错: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("启动设备发现服务时出错: " + e.getMessage());
        }
    }
    
    /**
     * 初始化JNA输入监听
     */
    private void initJNAInputMonitoring() {
        jnaInputMonitor = new JNAInputMonitor();
        jnaInputMonitor.startMonitoring();
        System.out.println("JNA输入监听已启动");
    }
    
    /**
     * 启动鼠标位置定时更新
     */
    private void startMousePositionUpdates() {
        mousePositionTimer = new Timer(true);
        mousePositionTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (mousePositionDisplay != null) {
                        // 使用JNA获取鼠标位置并更新显示
                        java.awt.Point mousePoint = JNAMousePosition.getMousePosition();
                        if (mousePoint != null) {
                            mousePositionDisplay.updateMousePosition(mousePoint.x, mousePoint.y);
                        }
                    }
                });
            }
        }, 0, 100); // 每100毫秒更新一次
    }
    
    @Override
    public void stop() {
        // 应用程序关闭时停止设备发现服务
        if (deviceDiscovery != null) {
            deviceDiscovery.stopDiscovery();
        }
        
        // 停止JNA输入监听
        if (jnaInputMonitor != null) {
            jnaInputMonitor.stopMonitoring();
        }
        
        // 停止鼠标位置更新定时器
        if (mousePositionTimer != null) {
            mousePositionTimer.cancel();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}