package com.keymouseshare.screen;

import com.keymouseshare.config.DeviceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 屏幕布局管理器，负责管理多个设备屏幕的相对位置关系
 */
public class ScreenLayoutManager {
    private static final Logger logger = LoggerFactory.getLogger(ScreenLayoutManager.class);
    
    private ScreenLayoutConfig layoutConfig;
    private AtomicInteger deviceCounter = new AtomicInteger(0);
    
    public ScreenLayoutManager() {
        this.layoutConfig = new ScreenLayoutConfig();
    }
    
    /**
     * 根据鼠标坐标计算目标设备
     * @param currentDevice 当前设备
     * @param x 鼠标x坐标
     * @param y 鼠标y坐标
     * @return 目标设备，如果仍在当前设备上则返回null
     */
    public DeviceScreen calculateTargetDevice(DeviceConfig currentDevice, double x, double y) {
        // 更新当前设备屏幕信息
        DeviceScreen currentScreen = ScreenLayoutConfig.createFromDeviceConfig(currentDevice);
        layoutConfig.setCurrentScreen(currentScreen);
        
        // 计算目标设备
        DeviceScreen targetScreen = layoutConfig.calculateTargetScreen(x, y);
        
        if (targetScreen != null) {
            logger.debug("Mouse moving from {} to {}", currentDevice.getDeviceName(), targetScreen.getDeviceName());
        }
        
        return targetScreen;
    }
    
    /**
     * 设置屏幕布局
     * @param devices 设备列表
     */
    public void setLayout(List<DeviceConfig.Device> devices) {
        if (devices == null) {
            logger.warn("Device list is null");
            return;
        }
        
        layoutConfig.getAllScreens().clear();
        for (DeviceConfig.Device device : devices) {
            // 只添加已连接或服务器类型的设备
            if (device.getConnectionState() == DeviceConfig.Device.ConnectionState.CONNECTED || 
                device.getDeviceType() == DeviceConfig.Device.DeviceType.SERVER) {
                DeviceScreen screen = new DeviceScreen();
                screen.setDeviceId(device.getDeviceId());
                screen.setDeviceName(device.getDeviceName());
                screen.setWidth(device.getScreenWidth());
                screen.setHeight(device.getScreenHeight());
                screen.setX(device.getNetworkX());
                screen.setY(device.getNetworkY());
                layoutConfig.addScreen(screen);
            }
        }
        
        logger.info("Layout set with {} devices", devices.size());
    }
    
    /**
     * 获取屏幕布局
     * @return 设备屏幕列表
     */
    public List<DeviceScreen> getLayout() {
        return layoutConfig.getAllScreens();
    }
    
    /**
     * 添加设备到布局中
     * @param device 设备配置
     */
    public void addDevice(DeviceConfig.Device device) {
        if (device == null) {
            logger.warn("Device is null");
            return;
        }
        
        // 检查设备是否已存在
        DeviceScreen existingScreen = layoutConfig.getScreen(device.getDeviceId());
        if (existingScreen != null) {
            logger.debug("Device {} already exists in layout, updating", device.getDeviceName());
            updateDevice(device);
            return;
        }
        
        // 只有已连接或服务器类型的设备才添加到布局中
        if (device.getConnectionState() != DeviceConfig.Device.ConnectionState.CONNECTED && 
            device.getDeviceType() != DeviceConfig.Device.DeviceType.SERVER) {
            logger.debug("Device {} is not connected or server, not adding to layout", device.getDeviceName());
            return;
        }
        
        DeviceScreen screen = new DeviceScreen();
        screen.setDeviceId(device.getDeviceId());
        screen.setDeviceName(device.getDeviceName());
        screen.setWidth(device.getScreenWidth());
        screen.setHeight(device.getScreenHeight());
        
        // 如果设备没有设置位置，则为其分配一个默认位置
        if (device.getNetworkX() == 0 && device.getNetworkY() == 0) {
            int positionIndex = deviceCounter.getAndIncrement();
            screen.setX(positionIndex * 200);  // 水平排列
            screen.setY(positionIndex * 100);   // 垂直偏移
        } else {
            screen.setX(device.getNetworkX());
            screen.setY(device.getNetworkY());
        }
        
        layoutConfig.addScreen(screen);
        logger.info("Device added to layout: {}", device.getDeviceName());
    }
    
    /**
     * 更新设备在布局中的信息
     * @param device 设备配置
     */
    public void updateDevice(DeviceConfig.Device device) {
        if (device == null) {
            logger.warn("Device is null");
            return;
        }
        
        // 如果设备已断开连接且不是服务器类型，则从布局中移除
        if (device.getConnectionState() == DeviceConfig.Device.ConnectionState.DISCONNECTED && 
            device.getDeviceType() != DeviceConfig.Device.DeviceType.SERVER) {
            removeDevice(device.getDeviceId());
            return;
        }
        
        DeviceScreen screen = new DeviceScreen();
        screen.setDeviceId(device.getDeviceId());
        screen.setDeviceName(device.getDeviceName());
        screen.setWidth(device.getScreenWidth());
        screen.setHeight(device.getScreenHeight());
        screen.setX(device.getNetworkX());
        screen.setY(device.getNetworkY());
        layoutConfig.updateScreen(screen);
        
        logger.info("Device updated in layout: {}", device.getDeviceName());
    }
    
    /**
     * 从布局中移除设备
     * @param deviceId 设备ID
     */
    public void removeDevice(String deviceId) {
        if (deviceId == null) {
            logger.warn("Cannot remove device with null device ID");
            return;
        }
        
        DeviceScreen screen = layoutConfig.getScreen(deviceId);
        if (screen != null) {
            layoutConfig.removeScreen(deviceId);
            logger.info("Device removed from layout: {}", screen.getDeviceName());
        } else {
            logger.debug("Device with ID {} not found in layout", deviceId);
        }
    }
    
    /**
     * 获取屏幕布局配置管理器
     * @return 屏幕布局配置管理器
     */
    public ScreenLayoutConfig getLayoutConfig() {
        return layoutConfig;
    }
    
    /**
     * 同步设备配置到屏幕布局
     * @param deviceConfig 设备配置
     */
    public void syncWithDeviceConfig(DeviceConfig deviceConfig) {
        // 更新当前设备屏幕
        DeviceScreen currentScreen = ScreenLayoutConfig.createFromDeviceConfig(deviceConfig);
        layoutConfig.setCurrentScreen(currentScreen);
        
        // 确保当前设备在布局中
        if (layoutConfig.getScreen(currentScreen.getDeviceId()) == null) {
            layoutConfig.addScreen(currentScreen);
        }
        
        // 同步连接的设备
        for (DeviceConfig.Device device : deviceConfig.getConnectedDevices()) {
            updateDevice(device);
        }
        
        logger.info("Screen layout synchronized with device config");
    }
    
    /**
     * 检查设备是否已在布局中（通过设备ID）
     * @param deviceId 设备ID
     * @return true表示设备已存在，false表示不存在
     */
    public boolean isDeviceInLayout(String deviceId) {
        return layoutConfig.getScreen(deviceId) != null;
    }
}