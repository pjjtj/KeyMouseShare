package com.keymouseshare.bean;

import com.keymouseshare.listener.VirtualDesktopStorageListener;
import javafx.geometry.Rectangle2D;

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
    private Rectangle2D virtualBounds;
    private Set<VirtualDesktopStorageListener> listeners = new HashSet<>();

    public void applyScreen(ScreenInfo screen){
        screens.put(screen.getDeviceIp()+screen.getScreenName(), screen);
        recalculateBounds();
    }

    // 动态添加物理屏幕
    public void addScreen(ScreenInfo screen) {
        screens.put(screen.getDeviceIp()+screen.getScreenName(), screen);
        recalculateBounds();
        notifyListeners();
    }

    // 坐标转换服务
    public ScreenCoordinate translate(double globalX, double globalY) {
        return screens.values().parallelStream()
                .filter(s -> s.virtualContains(globalX, globalY))
                .findFirst()
                .map(s -> new ScreenCoordinate(
                        s.getDeviceIp(),
                        s.getScreenName(),
                        globalX - s.getVx(),
                        globalY - s.getVy()
                )).orElse(null);
    }

    private void recalculateBounds() {
        double minX = Integer.MAX_VALUE;
        double minY = Integer.MAX_VALUE;
        double maxX = Integer.MIN_VALUE;
        double maxY = Integer.MIN_VALUE;

        for (ScreenInfo screen : screens.values()) {
            minX = Math.min(minX, screen.getVx());
            minY = Math.min(minY, screen.getVy());
            maxX = Math.max(maxX, screen.getVx() + screen.getWidth());
            maxY = Math.max(maxY, screen.getVy() + screen.getHeight());
        }
        virtualBounds = new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
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
    public void notifyListeners() {
        for (VirtualDesktopStorageListener listener : listeners) {
            listener.onVirtualDesktopChanged();
        }
    }
}