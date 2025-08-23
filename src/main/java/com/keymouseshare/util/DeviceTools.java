package com.keymouseshare.util;

import com.keymouseshare.bean.ScreenInfo;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DeviceTools {

    private static final Logger logger = Logger.getLogger(DeviceTools.class.getName());
    /**
     * 获取本机屏幕信息
     *
     * @return 屏幕信息列表
     */
    public static List<ScreenInfo> getLocalScreens() {
        List<ScreenInfo> screens = new ArrayList<>();

        // 如果JavaFX不可用，则尝试使用AWT GraphicsEnvironment获取屏幕信息
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] gds = ge.getScreenDevices();

            if (gds.length > 0) {
                for (int i = 0; i < gds.length; i++) {
                    GraphicsDevice gd = gds[i];
                    GraphicsConfiguration gc = gd.getDefaultConfiguration();
                    Rectangle bounds = gc.getBounds();
                    String screenName = gd.getIDstring();
                    // 获取实际的屏幕尺寸，考虑高DPI缩放
                    int width = bounds.width;
                    int height = bounds.height;
                    
                    // 尝试获取真实的像素尺寸（不受缩放影响）
                    try {
                        // 使用Toolkit获取真实的屏幕分辨率
                        Toolkit toolkit = Toolkit.getDefaultToolkit();
                        Dimension screenSize = toolkit.getScreenSize();
                        
                        // 如果Toolkit返回的尺寸与GraphicsDevice不同，可能是由于缩放
                        if (screenSize.width >= width && screenSize.height >= height) {
                            width = screenSize.width;
                            height = screenSize.height;
                        }
                    } catch (Exception e) {
                        logger.warning("Failed to get screen size using Toolkit: " + e.getMessage());
                    }
                    
                    ScreenInfo screenInfo = new ScreenInfo(NetUtil.getLocalIpAddress(), screenName, width, height, bounds.x, bounds.y);
                    screens.add(screenInfo);
                }
                return screens;
            }
        } catch (Exception e) {
            logger.warning("Failed to get screen info using GraphicsEnvironment: " + e.getMessage());
        }

        // 如果上述方法都失败了，使用默认值
        if (screens.isEmpty()) {
            ScreenInfo screenInfo = new ScreenInfo(NetUtil.getLocalIpAddress(), "ScreenA", 1920, 1080, 0, 0); // 默认1080p屏幕
            screens.add(screenInfo);
        }

        return screens;
    }
}