package com.keymouseshare.input;

/**
 * 输入监听管理器接口
 */
public interface InputListenerManager {
    
    /**
     * 启动输入监听
     */
    void startListening();
    
    /**
     * 停止输入监听
     */
    void stopListening();
    
    /**
     * 设置事件监听器
     * @param listener 事件监听器
     */
    void setEventListener(InputListener listener);
    
    /**
     * 检查是否正在监听输入事件
     * @return true表示正在监听，false表示未监听
     */
    boolean isListening();
}