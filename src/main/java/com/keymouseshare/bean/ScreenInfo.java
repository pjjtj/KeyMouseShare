package com.keymouseshare.bean;

/**
 * @Description TODO
 * @Author pengjiajia
 * @Date 2025/8/18 17:37
 **/
public class ScreenInfo {
    private String deviceIp;
    private String screenName;
    private double width;
    private double height;
    private double dx; // 本地屏幕坐标
    private double dy; // 本地屏幕坐标
    private double vx; // 虚拟桌面屏幕坐标
    private double vy; // 虚拟桌面屏幕坐标
    private double mx; // 屏幕配置坐标
    private double my; // 屏幕配置坐标

    public ScreenInfo() {}


    public ScreenInfo(String deviceIp, String screenName, double width, double height, double dx, double dy) {
        this.deviceIp = deviceIp;
        this.screenName = screenName;
        this.width = width;
        this.height = height;
        this.dx = dx;
        this.dy = dy;
    }


    public ScreenInfo(String deviceIp, String screenName, double width, double height) {
        this.deviceIp = deviceIp;
        this.screenName = screenName;
        this.width = width;
        this.height = height;
    }


    // Getters and setters
    public String getScreenName() { return screenName; }
    public void setScreenName(String screenName) { this.screenName = screenName; }

    public double getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public double getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public String getDeviceIp() { return deviceIp; }
    public void setDeviceIp(String deviceIp) { this.deviceIp = deviceIp; }

    public double getDx() { return dx; }

    public void setDx(double dx) { this.dx = dx; }

    public double getDy() { return dy; }

    public void setDy(double dy) { this.dy = dy; }

    public double getVx() { return vx; }

    public void setVx(double vx) { this.vx = vx; }

    public double getVy() { return vy; }

    public void setVy(double vy) { this.vy = vy; }

    public void setMx(double mx) {
        this.mx = mx;
    }

    public double getMx() {
        return mx;
    }

    public void setMy(double my) {
        this.my = my;
    }

    public double getMy() {
        return my;
    }

    public boolean localContains(double globalX, double globalY) {
        return globalX >= dx && globalX < dx + width && globalY >= dy && globalY < dy + height;
    }
    public boolean virtualContains(double globalX, double globalY) {
        return globalX >= vx && globalX < vx + width && globalY >= vy && globalY < vy + height;
    }
}
