package com.keymouseshare.bean;

public class ScreenCoordinate {
    public String ip;
    public String screenName;
    public int x;
    public int y;


    public ScreenCoordinate() {
    }

    public ScreenCoordinate(String ip, String screenName, int x, int y) {
        this.ip = ip;
        this.screenName = screenName;
        this.x = x;
        this.y = y;
    }
}
