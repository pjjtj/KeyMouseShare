package com.keymouseshare.core;

import com.keymouseshare.config.ConfigManager;
import com.keymouseshare.config.DeviceConfig;
import com.keymouseshare.filetransfer.FileTransferManager;
import com.keymouseshare.input.*;
import com.keymouseshare.network.NetworkManager;
import com.keymouseshare.screen.DeviceScreen;
import com.keymouseshare.screen.ScreenLayoutManager;
import com.keymouseshare.screen.ScreenLayoutConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 主控制器，协调各个模块的工作
 */
public class Controller implements InputListener {
    private static final Logger logger = LoggerFactory.getLogger(Controller.class);
    
    private DeviceConfig deviceConfig;
    private ConfigManager configManager;
    private NetworkManager networkManager;
    private ScreenLayoutManager screenLayoutManager;
    private FileTransferManager fileTransferManager;
    private InputListenerManager inputListenerManager;
    
    public Controller() {
        this.configManager = new ConfigManager();
        this.deviceConfig = configManager.getConfig();
        this.networkManager = new NetworkManager(this);
        this.screenLayoutManager = new ScreenLayoutManager();
        this.fileTransferManager = new FileTransferManager();
    }
    
    /**
     * 初始化控制器
     */
    public void initialize() {
        logger.info("Initializing controller...");
        logger.info("Device ID: {}", deviceConfig.getDeviceId());
        logger.info("Device Name: {}", deviceConfig.getDeviceName());
        
        // 初始化屏幕布局
        initializeScreenLayout();
    }
    
    /**
     * 初始化屏幕布局
     */
    private void initializeScreenLayout() {
        // 设置当前设备屏幕
        DeviceScreen currentScreen = ScreenLayoutConfig.createFromDeviceConfig(deviceConfig);
        screenLayoutManager.getLayoutConfig().setCurrentScreen(currentScreen);
        
        // 添加当前设备到布局中
        screenLayoutManager.getLayoutConfig().addScreen(currentScreen);
        
        logger.info("Screen layout initialized with current device: {}", deviceConfig.getDeviceName());
    }
    
    /**
     * 启动控制器
     */
    public void start() {
        logger.info("Starting controller...");
        // TODO: 实现启动逻辑
    }
    
    /**
     * 停止控制器
     */
    public void stop() {
        logger.info("Stopping controller...");
        if (networkManager != null) {
            networkManager.shutdown();
        }
        if (fileTransferManager != null) {
            fileTransferManager.shutdown();
        }
        if (inputListenerManager != null) {
            inputListenerManager.stopListening();
        }
        // 保存配置
        if (configManager != null) {
            configManager.updateConfig(deviceConfig);
        }
    }
    
    /**
     * 当客户端连接时调用
     * @param device 连接的设备
     */
    public void onClientConnected(DeviceConfig.Device device) {
        logger.info("Client connected: {}", device.getDeviceName());
        
        // 添加设备到屏幕布局
        screenLayoutManager.addDevice(device);
    }
    
    /**
     * 当客户端断开连接时调用
     * @param deviceId 断开连接的设备ID
     */
    public void onClientDisconnected(String deviceId) {
        logger.info("Client disconnected: {}", deviceId);
        
        // 从屏幕布局中移除设备
        screenLayoutManager.removeDevice(deviceId);
    }
    
    @Override
    public void onMouseMove(MouseEvent event) {
        logger.debug("Mouse moved to: ({}, {})", event.getX(), event.getY());
        
        // 检查是否需要切换到其他设备
        // DeviceConfig.Device targetDevice = screenLayoutManager.calculateTargetDevice(deviceConfig, event.getX(), event.getY());
        // if (targetDevice != null) {
        //     // 切换到目标设备
        //     networkManager.sendData(event);
        // }
    }
    
    @Override
    public void onMousePress(MouseEvent event) {
        logger.debug("Mouse pressed: {}", event.getButton());
        // networkManager.sendData(event);
    }
    
    @Override
    public void onMouseRelease(MouseEvent event) {
        logger.debug("Mouse released: {}", event.getButton());
        // networkManager.sendData(event);
    }
    
    @Override
    public void onMouseWheel(MouseEvent event) {
        logger.debug("Mouse wheel: {}", event.getWheelAmount());
        // networkManager.sendData(event);
    }
    
    @Override
    public void onKeyPress(KeyEvent event) {
        logger.debug("Key pressed: {}", event.getKeyCode());
        // networkManager.sendData(event);
    }
    
    @Override
    public void onKeyRelease(KeyEvent event) {
        logger.debug("Key released: {}", event.getKeyCode());
        // networkManager.sendData(event);
    }
    
    @Override
    public void onFileDragStart(FileDragEvent event) {
        logger.debug("File drag start");
        fileTransferManager.handleDragStart(event);
    }
    
    @Override
    public void onFileDragEnd(FileDragEvent event) {
        logger.debug("File drag end");
        fileTransferManager.handleDragEnd(event);
        // 如果拖拽到其他设备，则开始文件传输
        if (event.getTargetDeviceId() != null && 
            !event.getTargetDeviceId().equals(deviceConfig.getDeviceId())) {
            fileTransferManager.startFileTransfer(event.getFilePaths(), event.getTargetDeviceId());
        }
    }
    
    @Override
    public boolean isListening() {
        return inputListenerManager != null && inputListenerManager.isListening();
    }
    
    // Getters and setters
    public DeviceConfig getDeviceConfig() {
        return deviceConfig;
    }
    
    public NetworkManager getNetworkManager() {
        return networkManager;
    }
    
    public ScreenLayoutManager getScreenLayoutManager() {
        return screenLayoutManager;
    }
    
    public FileTransferManager getFileTransferManager() {
        return fileTransferManager;
    }
    
    public InputListenerManager getInputListenerManager() {
        return inputListenerManager;
    }
    
    public void setInputListenerManager(InputListenerManager inputListenerManager) {
        this.inputListenerManager = inputListenerManager;
        if (this.inputListenerManager != null) {
            this.inputListenerManager.setEventListener(this);
        }
    }
}