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
    private int dx; // 本地屏幕坐标
    private int dy; // 本地屏幕坐标
    private int vx; // 虚拟桌面屏幕坐标
    private int vy; // 虚拟桌面屏幕坐标
    private int mx; // 屏幕配置坐标
    private int my; // 屏幕配置坐标

    public ScreenInfo() {}


    public ScreenInfo(String deviceIp, String screenName, int width, int height, int dx, int dy) {
        this.deviceIp = deviceIp;
        this.screenName = screenName;
        this.width = width;
        this.height = height;
        this.dx = dx;
        this.dy = dy;
    }


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

    public int getDx() { return dx; }

    public void setDx(int dx) { this.dx = dx; }

    public int getDy() { return dy; }

    public void setDy(int dy) { this.dy = dy; }

    public int getVx() { return vx; }

    public void setVx(int vx) { this.vx = vx; }

    public int getVy() { return vy; }

    public void setVy(int vy) { this.vy = vy; }

    public void setMx(int mx) {
        this.mx = mx;
    }

    public int getMx() {
        return mx;
    }

    public void setMy(int my) {
        this.my = my;
    }

    public int getMy() {
        return my;
    }

    public boolean localContains(int globalX, int globalY) {
        return globalX >= dx && globalX < dx + width && globalY >= dy && globalY < dy + height;
    }
    public boolean virtualContains(int globalX, int globalY) {
        return globalX >= vx && globalX < vx + width && globalY >= vy && globalY < vy + height;
    }
}
