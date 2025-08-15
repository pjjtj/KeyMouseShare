package com.keymouseshare.screen;

import com.keymouseshare.network.DeviceInfo;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 屏幕布局管理器
 * 管理多个设备屏幕的相对位置关系
 */
public class ScreenLayoutManager {
    private List<ScreenInfo> screens;
    private ScreenInfo activeScreen;
    private Map<String, ScreenInfo> screenMap;
    
    public ScreenLayoutManager() {
        screens = new ArrayList<>();
        screenMap = new ConcurrentHashMap<>();
        initializeDefaultScreens();
    }
    
    /**
     * 初始化默认屏幕（本地屏幕）
     */
    private void initializeDefaultScreens() {
        // 获取本地屏幕设备
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gd = ge.getScreenDevices();
        
        for (int i = 0; i < gd.length; i++) {
            GraphicsDevice device = gd[i];
            GraphicsConfiguration gc = device.getDefaultConfiguration();
            Rectangle bounds = gc.getBounds();
            
            ScreenInfo screen = new ScreenInfo();
            screen.setId("local:screen" + i);
            screen.setName("屏幕 " + i);
            screen.setX(bounds.x);
            screen.setY(bounds.y);
            screen.setWidth(bounds.width);
            screen.setHeight(bounds.height);
            screen.setDeviceType(ScreenInfo.DeviceType.LOCAL);
            screen.setConnectionStatus(ScreenInfo.ConnectionStatus.CONNECTED);
            
            screens.add(screen);
            screenMap.put(screen.getId(), screen);
        }
        
        // 设置第一个屏幕为活动屏幕
        if (!screens.isEmpty()) {
            activeScreen = screens.get(0);
        }
    }
    
    /**
     * 从网络获取屏幕信息并更新布局
     */
    public void refreshScreensFromNetwork() {
        // 在实际实现中，这里会从网络管理器获取最新的设备信息
        // 并更新屏幕布局
        System.out.println("Refreshing screens from network...");
    }
    
    /**
     * 添加屏幕
     */
    public void addScreen(ScreenInfo screen) {
        screens.add(screen);
        screenMap.put(screen.getId(), screen);
    }
    
    /**
     * 更新屏幕信息
     */
    public void updateScreen(ScreenInfo screen) {
        // 屏幕信息已经在列表中，这里只是确保位置更新已生效
        System.out.println("Updated screen position: " + screen.getId() + 
                          " at (" + screen.getX() + ", " + screen.getY() + ")");
    }
    
    /**
     * 删除屏幕
     */
    public void removeScreen(ScreenInfo screen) {
        screens.remove(screen);
        screenMap.remove(screen.getId());
    }
    
    /**
     * 获取所有屏幕
     */
    public List<ScreenInfo> getAllScreens() {
        return new ArrayList<>(screens);
    }
    
    /**
     * 根据ID获取屏幕
     */
    public ScreenInfo getScreenById(String id) {
        return screenMap.get(id);
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
     * 根据设备信息添加远程屏幕
     */
    public void addRemoteScreens(DeviceInfo deviceInfo) {
        if (deviceInfo == null || deviceInfo.getScreens() == null) {
            return;
        }
        
        List<ScreenInfo> remoteScreens = deviceInfo.getScreens();
        for (ScreenInfo remoteScreen : remoteScreens) {
            // 检查屏幕是否已存在
            ScreenInfo existingScreen = screenMap.get(remoteScreen.getId());
            if (existingScreen != null) {
                // 更新现有屏幕信息
                existingScreen.setX(remoteScreen.getX());
                existingScreen.setY(remoteScreen.getY());
                existingScreen.setWidth(remoteScreen.getWidth());
                existingScreen.setHeight(remoteScreen.getHeight());
                existingScreen.setConnectionStatus(ScreenInfo.ConnectionStatus.CONNECTED);
            } else {
                // 添加新屏幕
                ScreenInfo newScreen = new ScreenInfo();
                newScreen.setId(remoteScreen.getId());
                newScreen.setName(remoteScreen.getName());
                newScreen.setX(remoteScreen.getX());
                newScreen.setY(remoteScreen.getY());
                newScreen.setWidth(remoteScreen.getWidth());
                newScreen.setHeight(remoteScreen.getHeight());
                newScreen.setDeviceType(ScreenInfo.DeviceType.CLIENT);
                newScreen.setConnectionStatus(ScreenInfo.ConnectionStatus.CONNECTED);
                
                screens.add(newScreen);
                screenMap.put(newScreen.getId(), newScreen);
            }
        }
    }
    
    /**
     * 移除远程设备的屏幕
     */
    public void removeRemoteScreens(String deviceId) {
        screens.removeIf(screen -> 
            screen.getId() != null && screen.getId().startsWith(deviceId));
        
        screenMap.entrySet().removeIf(entry -> 
            entry.getKey() != null && entry.getKey().startsWith(deviceId));
    }
}