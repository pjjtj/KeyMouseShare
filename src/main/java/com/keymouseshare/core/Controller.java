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

import java.net.InetAddress;
import java.net.UnknownHostException;

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
    private boolean isServer = false;
    private boolean isClient = false;
    
    // 当前活动的设备ID（鼠标所在的设备）
    private String activeDeviceId;
    
    public Controller() {
        this.configManager = new ConfigManager();
        this.deviceConfig = configManager.getConfig();
        this.networkManager = new NetworkManager(this);
        this.screenLayoutManager = new ScreenLayoutManager();
        this.fileTransferManager = new FileTransferManager();
        this.activeDeviceId = deviceConfig.getDeviceId(); // 初始化为当前设备
        
        // 初始化设备配置
        initializeDeviceConfig();
    }
    
    /**
     * 初始化设备配置
     */
    private void initializeDeviceConfig() {
        if (deviceConfig.getDeviceId() == null || deviceConfig.getDeviceId().isEmpty()) {
            deviceConfig.setDeviceId(generateDeviceId());
        }
        
        if (deviceConfig.getDeviceName() == null || deviceConfig.getDeviceName().isEmpty()) {
            deviceConfig.setDeviceName(getHostName());
        }
        
        // 设置默认屏幕尺寸
        if (deviceConfig.getScreenWidth() <= 0) {
            deviceConfig.setScreenWidth(1920);
        }
        
        if (deviceConfig.getScreenHeight() <= 0) {
            deviceConfig.setScreenHeight(1080);
        }
    }
    
    /**
     * 生成设备ID
     * @return 设备ID
     */
    private String generateDeviceId() {
        return "device-" + System.currentTimeMillis();
    }
    
    /**
     * 获取主机名
     * @return 主机名
     */
    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.warn("Failed to get host name, using default", e);
            return "UnknownDevice";
        }
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
     * 当设备作为服务器启动时调用
     */
    public void onServerStarted() {
        isServer = true;
        logger.info("Device is now acting as server");
    }
    
    /**
     * 当设备作为客户端连接时调用
     */
    public void onClientConnected() {
        isClient = true;
        logger.info("Device is now acting as client");
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
        
        // 如果断开连接的是当前活动设备，则切换回本机
        if (deviceId.equals(activeDeviceId)) {
            activeDeviceId = deviceConfig.getDeviceId();
            logger.info("Active device switched back to local device");
        }
    }
    
    @Override
    public void onMouseMove(MouseEvent event) {
        logger.debug("Mouse moved to: ({}, {})", event.getX(), event.getY());
        
        // 检查是否需要切换到其他设备
        DeviceScreen targetScreen = screenLayoutManager.calculateTargetDevice(deviceConfig, event.getX(), event.getY());
        if (targetScreen != null) {
            // 切换到目标设备
            switchToDevice(targetScreen, event);
        } else if (activeDeviceId.equals(deviceConfig.getDeviceId())) {
            // 当前在本地设备上，直接处理鼠标移动事件
            if (networkManager != null) {
                networkManager.sendData(event);
            }
        } else {
            // 当前在远程设备上，转发鼠标移动事件
            if (networkManager != null) {
                networkManager.sendData(event);
            }
        }
    }
    
    /**
     * 切换到指定设备
     * @param targetScreen 目标设备屏幕
     * @param event 鼠标事件
     */
    private void switchToDevice(DeviceScreen targetScreen, MouseEvent event) {
        String targetDeviceId = targetScreen.getDeviceId();
        
        // 如果目标设备就是当前活动设备，则直接转发事件
        if (targetDeviceId.equals(activeDeviceId)) {
            if (networkManager != null) {
                networkManager.sendData(event);
            }
            return;
        }
        
        logger.info("Switching control from {} to {}", activeDeviceId, targetDeviceId);
        
        // 更新活动设备
        String previousDeviceId = activeDeviceId;
        activeDeviceId = targetDeviceId;
        
        // 如果之前在本地设备上，需要停止本地输入监听
        if (previousDeviceId.equals(deviceConfig.getDeviceId())) {
            if (inputListenerManager != null) {
                inputListenerManager.stopListening();
                logger.info("Stopped local input listening");
            }
        }
        
        // 如果切换到本地设备，需要启动本地输入监听
        if (targetDeviceId.equals(deviceConfig.getDeviceId())) {
            if (inputListenerManager != null) {
                inputListenerManager.startListening();
                logger.info("Started local input listening");
            }
        }
        
        // 转发事件到目标设备
        if (networkManager != null) {
            networkManager.sendData(event);
        }
    }
    
    @Override
    public void onMousePress(MouseEvent event) {
        logger.debug("Mouse pressed: {}", event.getButton());
        
        // 转发鼠标按下事件
        if (networkManager != null) {
            networkManager.sendData(event);
        }
    }
    
    @Override
    public void onMouseRelease(MouseEvent event) {
        logger.debug("Mouse released: {}", event.getButton());
        
        // 转发鼠标释放事件
        if (networkManager != null) {
            networkManager.sendData(event);
        }
    }
    
    @Override
    public void onMouseWheel(MouseEvent event) {
        logger.debug("Mouse wheel: {}", event.getWheelAmount());
        
        // 转发鼠标滚轮事件
        if (networkManager != null) {
            networkManager.sendData(event);
        }
    }
    
    @Override
    public void onKeyPress(KeyEvent event) {
        logger.debug("Key pressed: {}", event.getKeyCode());
        
        // 转发键盘按下事件
        if (networkManager != null) {
            networkManager.sendData(event);
        }
    }
    
    @Override
    public void onKeyRelease(KeyEvent event) {
        logger.debug("Key released: {}", event.getKeyCode());
        
        // 转发键盘释放事件
        if (networkManager != null) {
            networkManager.sendData(event);
        }
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
    
    /**
     * 检查设备是否作为服务器运行
     * @return true表示作为服务器运行，false表示不是
     */
    public boolean isServer() {
        return isServer;
    }
    
    /**
     * 检查设备是否作为客户端运行
     * @return true表示作为客户端运行，false表示不是
     */
    public boolean isClient() {
        return isClient;
    }
    
    /**
     * 获取当前活动的设备ID
     * @return 当前活动的设备ID
     */
    public String getActiveDeviceId() {
        return activeDeviceId;
    }
    
    /**
     * 设置当前活动的设备ID
     * @param activeDeviceId 当前活动的设备ID
     */
    public void setActiveDeviceId(String activeDeviceId) {
        this.activeDeviceId = activeDeviceId;
    }
}