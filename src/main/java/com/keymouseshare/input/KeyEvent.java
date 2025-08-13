package com.keymouseshare.input;

/**
 * 键盘事件类，表示键盘相关的操作
 */
public class KeyEvent extends InputEvent {
    // 键码
    private int keyCode;
    
    // 字符
    private char keyChar;
    
    // 是否是功能键
    private boolean isFunctionKey;
    
    public KeyEvent(EventType type) {
        super(type);
    }
    
    public int getKeyCode() {
        return keyCode;
    }
    
    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }
    
    public char getKeyChar() {
        return keyChar;
    }
    
    public void setKeyChar(char keyChar) {
        this.keyChar = keyChar;
    }
    
    public boolean isFunctionKey() {
        return isFunctionKey;
    }
    
    public void setFunctionKey(boolean functionKey) {
        isFunctionKey = functionKey;
    }
}