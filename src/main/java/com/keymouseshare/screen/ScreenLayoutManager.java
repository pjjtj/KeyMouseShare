package com.keymouseshare.screen;

import com.keymouseshare.config.DeviceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 屏幕布局管理器，负责管理多个设备屏幕的相对位置关系
 */
public class ScreenLayoutManager {
    private static final Logger logger = LoggerFactory.getLogger(ScreenLayoutManager.class);
    
    private ScreenLayoutConfig layoutConfig;
    
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
            DeviceScreen screen = new DeviceScreen();
            screen.setDeviceId(device.getDeviceId());
            screen.setDeviceName(device.getDeviceName());
            screen.setWidth(device.getScreenWidth());
            screen.setHeight(device.getScreenHeight());
            screen.setX(device.getNetworkX());
            screen.setY(device.getNetworkY());
            layoutConfig.addScreen(screen);
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
        
        DeviceScreen screen = new DeviceScreen();
        screen.setDeviceId(device.getDeviceId());
        screen.setDeviceName(device.getDeviceName());
        screen.setWidth(device.getScreenWidth());
        screen.setHeight(device.getScreenHeight());
        screen.setX(device.getNetworkX());
        screen.setY(device.getNetworkY());
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
        layoutConfig.removeScreen(deviceId);
        logger.info("Device removed from layout: {}", deviceId);
    }
    
    /**
     * 获取屏幕布局配置管理器
     * @return 屏幕布局配置管理器
     */
    public ScreenLayoutConfig getLayoutConfig() {
        return layoutConfig;
    }
}