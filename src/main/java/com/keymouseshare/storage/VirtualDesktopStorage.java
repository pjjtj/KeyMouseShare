package com.keymouseshare.storage;

import com.keymouseshare.bean.ScreenInfo;
import com.keymouseshare.listener.VirtualDesktopStorageListener;
import javafx.geometry.Rectangle2D;
import javafx.scene.layout.StackPane;

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

    private boolean isApplyVirtualDesktopScreen = false;

    /**
     * 激活屏幕
     */
    private ScreenInfo activeScreen;
    /**
     * 鼠标位置[x,y]
     */
    private int[] mouseLocation = new int[2];

    public synchronized void setActiveScreen(ScreenInfo activeScreen) {
        this.activeScreen = activeScreen;
    }

    public ScreenInfo getActiveScreen() {
        return activeScreen;
    }

    public synchronized void moveMouseLocation(int dx, int dy) {
        // 锁定鼠标在当前虚拟屏幕内，防止鼠标快速移动跳出屏幕
        if(this.mouseLocation[0]+dx<activeScreen.getVx()
                ||this.mouseLocation[0]+dx>activeScreen.getVx()+activeScreen.getWidth()
                ||this.mouseLocation[1]+dy<activeScreen.getVy()
                ||this.mouseLocation[1]+dy>activeScreen.getVy()+activeScreen.getHeight()){
            return;
        }
        this.mouseLocation[0] += dx;
        this.mouseLocation[1] += dy;
        this.mouseLocation =  new int[]{this.mouseLocation[0], this.mouseLocation[1]};
    }

    public synchronized void setMouseLocation(int dx, int dy) {
        this.mouseLocation =  new int[]{dx, dy};
    }

    public int[] getMouseLocation() {
        return mouseLocation;
    }

    public void setApplyVirtualDesktopScreen(boolean applyVirtualDesktopScreen) {
        isApplyVirtualDesktopScreen = applyVirtualDesktopScreen;
    }

    public boolean isApplyVirtualDesktopScreen() {
        return isApplyVirtualDesktopScreen;
    }

    private ConcurrentMap<String, ScreenInfo> screens = new ConcurrentHashMap<>();
    private Rectangle2D virtualBounds;
    private Set<VirtualDesktopStorageListener> listeners = new HashSet<>();

    public void applyScreen(ScreenInfo screen){
        screens.put(screen.getDeviceIp()+screen.getScreenName(), screen);
    }

    // 动态添加物理屏幕
    public void addScreen(ScreenInfo screen) {
        screens.put(screen.getDeviceIp()+screen.getScreenName(), screen);
//        recalculateBounds();
        this.virtualDesktopChanged();
    }

    // 坐标转换服务
//    public ScreenCoordinate translate(int globalX, int globalY) {
//        return screens.values().parallelStream()
//                .filter(s -> s.virtualContains(globalX, globalY))
//                .findFirst()
//                .map(s -> new ScreenCoordinate(
//                        s.getDeviceIp(),
//                        s.getScreenName(),
//                        globalX - s.getVx(),
//                        globalY - s.getVy()
//                )).orElse(null);
//    }

//    private void recalculateBounds() {
//        int minX = Integer.MAX_VALUE;
//        int minY = Integer.MAX_VALUE;
//        int maxX = Integer.MIN_VALUE;
//        int maxY = Integer.MIN_VALUE;
//
//        for (ScreenInfo screen : screens.values()) {
//            minX = Math.min(minX, screen.getVx());
//            minY = Math.min(minY, screen.getVy());
//            maxX = Math.max(maxX, screen.getVx() + screen.getWidth());
//            maxY = Math.max(maxY, screen.getVy() + screen.getHeight());
//        }
//        virtualBounds = new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
//    }


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
    public void virtualDesktopChanged() {
        for (VirtualDesktopStorageListener listener : listeners) {
            listener.onVirtualDesktopChanged();
        }
    }

    public void applyVirtualDesktopScreen(Map<StackPane, String> screenMap, double scale) {
        for (VirtualDesktopStorageListener listener : listeners) {
            listener.onApplyVirtualDesktopScreen(screenMap, scale);
        }
    }
}