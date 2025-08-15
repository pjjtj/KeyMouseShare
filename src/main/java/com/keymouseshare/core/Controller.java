package com.keymouseshare.core;

import com.keymouseshare.filetransfer.FileTransferManager;
import com.keymouseshare.input.MouseMovementManager;
import com.keymouseshare.network.DeviceInfo;
import com.keymouseshare.network.NetworkManager;
import com.keymouseshare.screen.ScreenLayoutManager;
import com.keymouseshare.ui.MainWindow;

/**
 * 核心控制器
 * 管理整个应用程序的各个组件
 */
public class Controller {
    private NetworkManager networkManager;
    private ScreenLayoutManager screenLayoutManager;
    private MainWindow mainWindow;
    private FileTransferManager fileTransferManager;
    private MouseMovementManager mouseMovementManager;
    private DeviceControlManager deviceControlManager;
    private boolean isServerMode = false;
    
    public Controller() {
        // 初始化网络管理器
        networkManager = new NetworkManager(this);
        
        // 初始化文件传输管理器
        fileTransferManager = new FileTransferManager(this);
        
        // 初始化鼠标移动管理器
        mouseMovementManager = new MouseMovementManager(this);
        
        // 初始化设备控制管理器
        deviceControlManager = new DeviceControlManager(this);
        
        // 初始化屏幕布局管理器
        screenLayoutManager = new ScreenLayoutManager();
        
        // 初始化主窗口
        mainWindow = new MainWindow(this);
    }
    
    /**
     * 启动控制器
     */
    public void start() {
        // 启动网络管理器
        networkManager.start();
        
        // 显示主窗口
        mainWindow.setVisible(true);
    }
    
    /**
     * 启动服务器模式
     */
    public void startServer() {
        isServerMode = true;
        networkManager.startServer();
        // 通知UI更新
        mainWindow.onServerStarted();
    }
    
    /**
     * 停止服务器模式
     */
    public void stopServer() {
        isServerMode = false;
        networkManager.stopServer();
        // 通知UI更新
        mainWindow.onServerStopped();
    }
    
    /**
     * 当设备控制权限变更时调用
     */
    public void onDeviceControlPermissionChanged(String deviceId, DeviceControlManager.ControlPermission permission) {
        // 检查权限是否被允许
        if (permission == DeviceControlManager.ControlPermission.ALLOWED) {
            // 查找设备信息
            DeviceInfo deviceInfo = null;
            for (DeviceInfo discoveredDevice : networkManager.getDiscoveredDevices()) {
                if (discoveredDevice.getDeviceId().equals(deviceId)) {
                    deviceInfo = discoveredDevice;
                    break;
                }
            }
            
            // 如果找到了设备信息，通知网络管理器
            if (deviceInfo != null) {
                networkManager.onDeviceControlAllowed(deviceInfo);
            }
        }
    }
    
    // Getter方法
    public NetworkManager getNetworkManager() {
        return networkManager;
    }
    
    public ScreenLayoutManager getScreenLayoutManager() {
        return screenLayoutManager;
    }
    
    public MainWindow getMainWindow() {
        return mainWindow;
    }
    
    public FileTransferManager getFileTransferManager() {
        return fileTransferManager;
    }
    
    public MouseMovementManager getMouseMovementManager() {
        return mouseMovementManager;
    }
    
    public DeviceControlManager getDeviceControlManager() {
        return deviceControlManager;
    }
    
    public boolean isServerMode() {
        return isServerMode;
    }
}