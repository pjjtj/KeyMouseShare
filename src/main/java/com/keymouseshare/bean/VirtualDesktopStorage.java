package com.keymouseshare.bean;

import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class VirtualDesktopStorage {

    private static final VirtualDesktopStorage INSTANCE = new VirtualDesktopStorage();

    public static VirtualDesktopStorage getInstance() {
        return INSTANCE;
    }

    private ConcurrentMap<String, ScreenInfo> screens = new ConcurrentHashMap<>();
    private Rectangle virtualBounds;
    private Set<VirtualDesktopStorageListener> listeners = new HashSet<>();

    // 动态添加物理屏幕
    public void addScreen(ScreenInfo screen) {
        screens.put(screen.getDeviceIp()+":"+screen.getScreenName(), screen);
        recalculateBounds();
        notifyListeners();
    }

    // 坐标转换服务
    public ScreenCoordinate translate(int globalX, int globalY) {
        return screens.values().parallelStream()
                .filter(s -> s.contains(globalX, globalY))
                .findFirst()
                .map(s -> new ScreenCoordinate(
                        s.getDeviceIp(),
                        s.getScreenName(),
                        globalX - s.getX(),
                        globalY - s.getY()
                )).orElse(null);
    }

    private void recalculateBounds() {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (ScreenInfo screen : screens.values()) {
            minX = Math.min(minX, screen.getX());
            minY = Math.min(minY, screen.getY());
            maxX = Math.max(maxX, screen.getX() + screen.getWidth());
            maxY = Math.max(maxY, screen.getY() + screen.getHeight());
        }
        virtualBounds = new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }
    
    /**
     * 获取所有屏幕信息
     * @return 屏幕信息映射
     */
    public Map<String, ScreenInfo> getScreens() {
        return screens;
    }
    
    /**
     * 添加监听器
     * @param listener 监听器
     */
    public void addListener(VirtualDesktopStorageListener listener) {
        listeners.add(listener);
    }
    
    /**
     * 移除监听器
     * @param listener 监听器
     */
    public void removeListener(VirtualDesktopStorageListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * 通知所有监听器
     */
    private void notifyListeners() {
        for (VirtualDesktopStorageListener listener : listeners) {
            listener.onVirtualDesktopChanged();
        }
    }
}