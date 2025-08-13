package com.keymouseshare.screen;

import com.keymouseshare.config.DeviceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 屏幕布局配置管理器，用于管理多个设备屏幕的相对位置关系
 */
public class ScreenLayoutConfig {
    private static final Logger logger = LoggerFactory.getLogger(ScreenLayoutConfig.class);
    
    // 屏幕布局列表
    private List<DeviceScreen> screenLayout;
    
    // 当前设备屏幕
    private DeviceScreen currentScreen;
    
    public ScreenLayoutConfig() {
        this.screenLayout = new CopyOnWriteArrayList<>();
    }
    
    /**
     * 添加设备屏幕到布局中
     * @param deviceScreen 设备屏幕信息
     */
    public void addScreen(DeviceScreen deviceScreen) {
        if (deviceScreen != null) {
            screenLayout.add(deviceScreen);
            logger.info("Added screen to layout: {}", deviceScreen);
        }
    }
    
    /**
     * 从布局中移除设备屏幕
     * @param deviceId 设备ID
     */
    public void removeScreen(String deviceId) {
        screenLayout.removeIf(screen -> screen.getDeviceId().equals(deviceId));
        logger.info("Removed screen from layout: {}", deviceId);
    }
    
    /**
     * 更新设备屏幕信息
     * @param deviceScreen 设备屏幕信息
     */
    public void updateScreen(DeviceScreen deviceScreen) {
        if (deviceScreen != null) {
            removeScreen(deviceScreen.getDeviceId());
            addScreen(deviceScreen);
            logger.info("Updated screen in layout: {}", deviceScreen);
        }
    }
    
    /**
     * 根据设备ID获取设备屏幕信息
     * @param deviceId 设备ID
     * @return 设备屏幕信息，未找到返回null
     */
    public DeviceScreen getScreen(String deviceId) {
        for (DeviceScreen screen : screenLayout) {
            if (screen.getDeviceId().equals(deviceId)) {
                return screen;
            }
        }
        return null;
    }
    
    /**
     * 获取所有设备屏幕信息
     * @return 设备屏幕列表的不可变视图
     */
    public List<DeviceScreen> getAllScreens() {
        return Collections.unmodifiableList(screenLayout);
    }
    
    /**
     * 根据鼠标坐标计算目标设备
     * @param x 鼠标x坐标（相对于当前屏幕）
     * @param y 鼠标y坐标（相对于当前屏幕）
     * @return 目标设备屏幕，如果仍在当前设备上则返回null
     */
    public DeviceScreen calculateTargetScreen(double x, double y) {
        if (currentScreen == null) {
            logger.warn("Current screen is not set");
            return null;
        }
        
        // 计算鼠标在全局坐标系中的位置
        int globalX = currentScreen.getX() + (int)x;
        int globalY = currentScreen.getY() + (int)y;
        
        // 查找包含该坐标的屏幕
        for (DeviceScreen screen : screenLayout) {
            if (screen != currentScreen && // 不是当前屏幕
                globalX >= screen.getX() && globalX < screen.getX() + screen.getWidth() &&
                globalY >= screen.getY() && globalY < screen.getY() + screen.getHeight()) {
                return screen;
            }
        }
        
        return null; // 鼠标仍在当前设备屏幕内
    }
    
    /**
     * 获取当前设备屏幕信息
     * @return 当前设备屏幕信息
     */
    public DeviceScreen getCurrentScreen() {
        return currentScreen;
    }
    
    /**
     * 设置当前设备屏幕信息
     * @param currentScreen 当前设备屏幕信息
     */
    public void setCurrentScreen(DeviceScreen currentScreen) {
        this.currentScreen = currentScreen;
        // 确保当前屏幕在布局中
        if (currentScreen != null && !screenLayout.contains(currentScreen)) {
            screenLayout.add(currentScreen);
        }
    }
    
    /**
     * 根据设备配置创建设备屏幕对象
     * @param deviceConfig 设备配置
     * @return 设备屏幕对象
     */
    public static DeviceScreen createFromDeviceConfig(DeviceConfig deviceConfig) {
        DeviceScreen screen = new DeviceScreen();
        screen.setDeviceId(deviceConfig.getDeviceId());
        screen.setDeviceName(deviceConfig.getDeviceName());
        screen.setWidth(deviceConfig.getScreenWidth());
        screen.setHeight(deviceConfig.getScreenHeight());
        screen.setX(deviceConfig.getNetworkX());
        screen.setY(deviceConfig.getNetworkY());
        return screen;
    }
    
    /**
     * 根据设备屏幕对象更新设备配置
     * @param deviceConfig 设备配置
     * @param screen 设备屏幕对象
     */
    public static void updateDeviceConfig(DeviceConfig deviceConfig, DeviceScreen screen) {
        deviceConfig.setDeviceId(screen.getDeviceId());
        deviceConfig.setDeviceName(screen.getDeviceName());
        deviceConfig.setScreenWidth(screen.getWidth());
        deviceConfig.setScreenHeight(screen.getHeight());
        deviceConfig.setNetworkX(screen.getX());
        deviceConfig.setNetworkY(screen.getY());
    }
}