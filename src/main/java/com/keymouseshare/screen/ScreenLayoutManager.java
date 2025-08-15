package com.keymouseshare.screen;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 屏幕布局管理器
 * 管理多个设备屏幕的布局和相对位置关系
 */
public class ScreenLayoutManager {
    private List<ScreenInfo> screens;
    private ScreenInfo activeScreen;
    
    public ScreenLayoutManager() {
        screens = new ArrayList<>();
        initializeLocalScreens();
    }
    
    /**
     * 初始化本地屏幕信息
     */
    private void initializeLocalScreens() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screenDevices = ge.getScreenDevices();
        
        for (int i = 0; i < screenDevices.length; i++) {
            GraphicsDevice device = screenDevices[i];
            DisplayMode displayMode = device.getDisplayMode();
            
            ScreenInfo screenInfo = new ScreenInfo();
            screenInfo.setId("local:" + device.getIDstring());
            screenInfo.setName("Screen " + (i + 1));
            screenInfo.setWidth(displayMode.getWidth());
            screenInfo.setHeight(displayMode.getHeight());
            screenInfo.setX(device.getDefaultConfiguration().getBounds().x);
            screenInfo.setY(device.getDefaultConfiguration().getBounds().y);
            screenInfo.setDeviceType(ScreenInfo.DeviceType.LOCAL);
            screenInfo.setConnectionStatus(ScreenInfo.ConnectionStatus.CONNECTED);
            
            screens.add(screenInfo);
            
            // 设置第一个屏幕为活动屏幕
            if (activeScreen == null) {
                activeScreen = screenInfo;
            }
        }
    }
    
    /**
     * 添加屏幕
     */
    public void addScreen(ScreenInfo screenInfo) {
        screens.add(screenInfo);
    }
    
    /**
     * 更新屏幕信息
     */
    public void updateScreen(ScreenInfo screenInfo) {
        for (int i = 0; i < screens.size(); i++) {
            ScreenInfo existingScreen = screens.get(i);
            if (existingScreen.getId().equals(screenInfo.getId())) {
                screens.set(i, screenInfo);
                break;
            }
        }
    }
    
    /**
     * 移除屏幕
     */
    public void removeScreen(String screenId) {
        screens.removeIf(screen -> screen.getId().equals(screenId));
    }
    
    /**
     * 根据ID查找屏幕
     */
    public ScreenInfo findScreenById(String id) {
        for (ScreenInfo screen : screens) {
            if (screen.getId().equals(id)) {
                return screen;
            }
        }
        return null;
    }
    
    /**
     * 获取所有屏幕
     */
    public List<ScreenInfo> getAllScreens() {
        return new ArrayList<>(screens);
    }
    
    /**
     * 设置活动屏幕
     */
    public void setActiveScreen(ScreenInfo screen) {
        this.activeScreen = screen;
    }
    
    /**
     * 获取活动屏幕
     */
    public ScreenInfo getActiveScreen() {
        return activeScreen;
    }
    
    /**
     * 检查坐标是否在指定屏幕范围内
     */
    public boolean isPointInScreen(ScreenInfo screen, int x, int y) {
        return x >= screen.getX() && x <= screen.getX() + screen.getWidth() &&
               y >= screen.getY() && y <= screen.getY() + screen.getHeight();
    }
    
    /**
     * 根据坐标查找屏幕
     */
    public ScreenInfo findScreenByPoint(int x, int y) {
        for (ScreenInfo screen : screens) {
            if (isPointInScreen(screen, x, y)) {
                return screen;
            }
        }
        return null;
    }
}