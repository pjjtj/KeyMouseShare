package com.keymouseshare.bean;

/**
 * @Description TODO
 * @Author pengjiajia
 * @Date 2025/8/18 17:37
 **/
public class ScreenInfo {
    private String screenName;
    private int width;
    private int height;

    public ScreenInfo() {}

    public ScreenInfo(String screenName, int width, int height) {
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
}
