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

import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.DisplayMode;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
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
    
    // 本地IP地址集合，用于识别本地设备
    private Set<String> localIpAddresses = new HashSet<>();
    
    public Controller() {
        this.configManager = new ConfigManager();
        this.deviceConfig = configManager.getConfig();
        this.networkManager = new NetworkManager(this);
        this.screenLayoutManager = new ScreenLayoutManager();
        this.fileTransferManager = new FileTransferManager();
        this.activeDeviceId = deviceConfig.getDeviceId(); // 初始化为当前设备
        
        // 初始化本地IP地址集合
        initializeLocalIpAddresses();
        
        // 初始化设备配置
        initializeDeviceConfig();
    }
    
    /**
     * 初始化本地IP地址集合
     */
    private void initializeLocalIpAddresses() {
        try {
            // 添加localhost地址
            localIpAddresses.add("127.0.0.1");
            localIpAddresses.add("localhost");
            
            // 获取所有网络接口的IP地址
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    networkInterface.getInterfaceAddresses().stream()
                        .filter(addr -> addr.getAddress() instanceof java.net.Inet4Address)
                        .forEach(addr -> {
                            localIpAddresses.add(addr.getAddress().getHostAddress());
                            logger.debug("Added local IP address: {}", addr.getAddress().getHostAddress());
                        });
                }
            }
            
            logger.info("Initialized local IP addresses: {}", localIpAddresses);
        } catch (SocketException e) {
            logger.warn("Failed to get local IP addresses", e);
        }
    }
    
    /**
     * 检查给定的IP地址是否为本地地址
     * @param ipAddress IP地址
     * @return true表示是本地地址，false表示不是
     */
    public boolean isLocalIpAddress(String ipAddress) {
        if (ipAddress == null) {
            return false;
        }
        
        // 移除端口号（如果存在）
        String ipOnly = ipAddress.split(":")[0];
        return localIpAddresses.contains(ipOnly);
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
        
        // 获取实际屏幕分辨率，如果获取不到则使用默认值1920x1080
        // 即使已有配置值，也重新获取以确保准确性
        int[] screenSize = getScreenSize();
        deviceConfig.setScreenWidth(screenSize[0]);
        deviceConfig.setScreenHeight(screenSize[1]);
        
        logger.info("Initialized device screen resolution: {}x{}", 
            deviceConfig.getScreenWidth(), deviceConfig.getScreenHeight());
    }
    
    /**
     * 获取实际屏幕分辨率
     * @return 包含宽度和高度的数组，如果获取失败则返回默认值1920x1080
     */
    private int[] getScreenSize() {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] screens = ge.getScreenDevices();
            
            // 遍历所有屏幕设备，找到主屏幕或选择最大屏幕
            GraphicsDevice primaryDevice = ge.getDefaultScreenDevice();
            DisplayMode dm = primaryDevice.getDisplayMode();
            
            int width = dm.getWidth();
            int height = dm.getHeight();
            
            // 确保获取到的尺寸是有效的
            if (width > 0 && height > 0) {
                logger.info("Primary screen size detected: {}x{}", width, height);
                return new int[]{width, height};
            }
            
            // 如果主屏幕无效，尝试其他屏幕
            for (GraphicsDevice screen : screens) {
                dm = screen.getDisplayMode();
                width = dm.getWidth();
                height = dm.getHeight();
                
                if (width > 0 && height > 0) {
                    logger.info("Screen size detected from device {}: {}x{}", 
                        screen.getIDstring(), width, height);
                    return new int[]{width, height};
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to get screen size, using default 1920x1080", e);
        }
        
        // 默认返回1920x1080
        logger.info("Using default screen size: 1920x1080");
        return new int[]{1920, 1080};
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
        
        // 创建本地设备对象并添加到连接设备列表中
        DeviceConfig.Device localDevice = new DeviceConfig.Device();
        localDevice.setDeviceId(deviceConfig.getDeviceId());
        localDevice.setDeviceName(deviceConfig.getDeviceName());
        localDevice.setScreenWidth(deviceConfig.getScreenWidth());
        localDevice.setScreenHeight(deviceConfig.getScreenHeight());
        localDevice.setNetworkX(deviceConfig.getNetworkX());
        localDevice.setNetworkY(deviceConfig.getNetworkY());
        localDevice.setDeviceType(DeviceConfig.Device.DeviceType.SERVER);
        localDevice.setConnectionState(DeviceConfig.Device.ConnectionState.CONNECTED);
        
        // 检查设备是否已存在
        boolean deviceExists = false;
        for (DeviceConfig.Device device : deviceConfig.getConnectedDevices()) {
            if (device.getDeviceId() != null && device.getDeviceId().equals(localDevice.getDeviceId())) {
                deviceExists = true;
                // 更新现有设备信息
                device.setDeviceType(DeviceConfig.Device.DeviceType.SERVER);
                device.setConnectionState(DeviceConfig.Device.ConnectionState.CONNECTED);
                break;
            }
        }
        
        // 如果设备不存在，则添加到连接设备列表中
        if (!deviceExists) {
            deviceConfig.addDevice(localDevice);
        }
        
        // 更新屏幕布局
        screenLayoutManager.addDevice(localDevice);
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
        
        // 检查是否为本地设备作为服务器运行的情况
        if (isServer && device.getIpAddress() != null && isLocalIpAddress(device.getIpAddress())) {
            logger.info("Local device connected as server, updating device type and state");
            device.setDeviceType(DeviceConfig.Device.DeviceType.SERVER);
            device.setConnectionState(DeviceConfig.Device.ConnectionState.CONNECTED);
        }
        
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