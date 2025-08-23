package com.keymouseshare.bean;

/**
 * @Description TODO
 * @Author pengjiajia
 * @Date 2025/8/18 17:37
 **/
public class ScreenInfo {
    private String deviceIp;
    private String screenName;
    private int width;
    private int height;
    private int x;
    private int y;

    public ScreenInfo() {}

    public ScreenInfo(String deviceIp, String screenName, int width, int height) {
        this.deviceIp = deviceIp;
        this.screenName = screenName;
        this.width = width;
        this.height = height;
    }


    // Getters and setters
    public String getScreenName() { return screenName; }
    public void setScreenName(String screenName) { this.screenName = screenName; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public String getDeviceIp() { return deviceIp; }
    public void setDeviceIp(String deviceIp) { this.deviceIp = deviceIp; }

    public int getX(){
        return x;
    }

    public void setX(int x){
        this.x = x;
    }

    public int getY(){
        return y;
    }

    public void setY(int y){
        this.y = y;
    }

    public boolean contains(int globalX, int globalY) {
        return globalX >= x && globalX < x + width && globalY >= y && globalY < y + height;
    }
}
