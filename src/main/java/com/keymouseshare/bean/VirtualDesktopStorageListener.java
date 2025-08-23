package com.keymouseshare.bean;

/**
 * 虚拟桌面存储监听器接口
 */
public interface VirtualDesktopStorageListener {
    /**
     * 当虚拟桌面发生变化时调用
     */
    void onVirtualDesktopChanged();
}