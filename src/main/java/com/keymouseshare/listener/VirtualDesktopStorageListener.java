package com.keymouseshare.listener;

import javafx.scene.layout.StackPane;

import java.util.Map;

/**
 * 虚拟桌面存储监听器接口
 */
public interface VirtualDesktopStorageListener {
    /**
     * 当虚拟桌面发生变化时调用
     */
    void onVirtualDesktopChanged();

    void onApplyVirtualDesktopScreen(Map<StackPane, String>  screenMap, double scale);

    void onEnterEdgeMode();

    void onExitEdgeMode();
}