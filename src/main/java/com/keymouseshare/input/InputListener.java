package com.keymouseshare.input;

/**
 * 输入监听器接口，定义监听输入事件的方法
 */
public interface InputListener {
    
    /**
     * 处理鼠标移动事件
     * @param event 鼠标事件
     */
    void onMouseMove(MouseEvent event);
    
    /**
     * 处理鼠标按键按下事件
     * @param event 鼠标事件
     */
    void onMousePress(MouseEvent event);
    
    /**
     * 处理鼠标按键释放事件
     * @param event 鼠标事件
     */
    void onMouseRelease(MouseEvent event);
    
    /**
     * 处理鼠标滚轮事件
     * @param event 鼠标事件
     */
    void onMouseWheel(MouseEvent event);
    
    /**
     * 处理键盘按键按下事件
     * @param event 键盘事件
     */
    void onKeyPress(KeyEvent event);
    
    /**
     * 处理键盘按键释放事件
     * @param event 键盘事件
     */
    void onKeyRelease(KeyEvent event);
    
    /**
     * 处理文件拖拽开始事件
     * @param event 文件拖拽事件
     */
    void onFileDragStart(FileDragEvent event);
    
    /**
     * 处理文件拖拽结束事件
     * @param event 文件拖拽事件
     */
    void onFileDragEnd(FileDragEvent event);
    
    /**
     * 检查监听器是否正在监听
     * @return true表示正在监听，false表示未监听
     */
    boolean isListening();
}