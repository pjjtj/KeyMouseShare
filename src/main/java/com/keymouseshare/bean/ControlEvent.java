package com.keymouseshare.bean;

/**
 * 控制事件类
 */
public class ControlEvent {
    private String deviceIp;
    private String screenName;
    private String type;
    private int x;
    private int y;
    private int button;
    private int keyCode;
    private String data;

    public ControlEvent() {
    }

    public ControlEvent(String type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public ControlEvent(String deviceIp,String type, int x, int y) {
        this.deviceIp = deviceIp;
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public ControlEvent(String deviceIp,String type, int button) {
        this.deviceIp = deviceIp;
        this.type = type;
        this.button = button;
    }

    public ControlEvent(String deviceIp,String type, int keyCode, String data) {
        this.deviceIp = deviceIp;
        this.type = type;
        this.keyCode = keyCode;
        this.data = data;
    }

    // Getters and setters
    public String getDeviceIp() {
        return deviceIp;
    }

    public void setDeviceIp(String deviceIp) {
        this.deviceIp = deviceIp;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getButton() {
        return button;
    }

    public void setButton(int button) {
        this.button = button;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}