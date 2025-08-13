package com.keymouseshare.input;

/**
 * 鼠标事件类，表示鼠标相关的操作
 */
public class MouseEvent extends InputEvent {
    // 鼠标X坐标
    private double x;
    
    // 鼠标Y坐标
    private double y;
    
    // 鼠标按键
    private MouseButton button;
    
    // 滚轮值
    private int wheelAmount;
    
    public MouseEvent(EventType type) {
        super(type);
    }
    
    public double getX() {
        return x;
    }
    
    public void setX(double x) {
        this.x = x;
    }
    
    public double getY() {
        return y;
    }
    
    public void setY(double y) {
        this.y = y;
    }
    
    public MouseButton getButton() {
        return button;
    }
    
    public void setButton(MouseButton button) {
        this.button = button;
    }
    
    public int getWheelAmount() {
        return wheelAmount;
    }
    
    public void setWheelAmount(int wheelAmount) {
        this.wheelAmount = wheelAmount;
    }
    
    /**
     * 鼠标按键枚举
     */
    public enum MouseButton {
        LEFT,    // 左键
        RIGHT,   // 右键
        MIDDLE   // 中键
    }
}