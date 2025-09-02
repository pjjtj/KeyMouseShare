package com.keymouseshare.bean;

/**
 * @Description TODO
 * @Author pengjiajia
 * @Date 2025/8/18 17:37
 **/
public class MoveTargetScreenInfo {
    private String direction;
    private ScreenInfo screenInfo;

    public MoveTargetScreenInfo(String direction, ScreenInfo screenInfo) {
        this.direction = direction;
        this.screenInfo = screenInfo;
    }


    public String getDirection() {
        return direction;
    }
    public ScreenInfo getScreenInfo() {
        return screenInfo;
    }
}
