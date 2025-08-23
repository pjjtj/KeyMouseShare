package com.keymouseshare.bean;

public class ScreenCoordinate {
    public String ip;
    public String screenName;
    public double x;
    public double y;


    public ScreenCoordinate() {
    }

    public ScreenCoordinate(String ip, String screenName, double x, double y) {
        this.ip = ip;
        this.screenName = screenName;
        this.x = x;
        this.y = y;
    }
}
