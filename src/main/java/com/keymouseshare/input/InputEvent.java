package com.keymouseshare.input;

/**
 * 输入事件基类，表示所有类型的输入事件
 */
public class InputEvent {
    // 事件类型
    private EventType type;
    
    // 时间戳
    private long timestamp;
    
    public InputEvent(EventType type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }
    
    public EventType getType() {
        return type;
    }
    
    public void setType(EventType type) {
        this.type = type;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * 事件类型枚举
     */
    public enum EventType {
        MOUSE_MOVE,        // 鼠标移动
        MOUSE_PRESS,       // 鼠标按键按下
        MOUSE_RELEASE,     // 鼠标按键释放
        MOUSE_WHEEL,       // 鼠标滚轮
        KEY_PRESS,         // 键盘按键按下
        KEY_RELEASE,       // 键盘按键释放
        FILE_DRAG_START,   // 文件拖拽开始
        FILE_DRAG_END,     // 文件拖拽结束
        FILE_TRANSFER      // 文件传输
    }
}