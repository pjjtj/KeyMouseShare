package com.keymouseshare.util;

import com.keymouseshare.bean.ScreenInfo;

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

        // 首先尝试使用AWT Toolkit获取真实的屏幕分辨率
        try {
            // 在高DPI屏幕上获取真实的屏幕分辨率
            java.awt.Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
            // 设置属性以获取真实的像素尺寸
            toolkit.setDynamicLayout(false);

            // 获取屏幕尺寸
            java.awt.Dimension screenSize = toolkit.getScreenSize();

            if (screenSize.width > 0 && screenSize.height > 0) {
                // 只有一个屏幕的情况
                screens.add(new ScreenInfo("ScreenA", screenSize.width, screenSize.height));
                return screens;
            }
        } catch (Exception e) {
            logger.warning("Failed to get screen info using Toolkit: " + e.getMessage());
        }

        // 使用JavaFX的Screen类获取屏幕信息
        try {
            javafx.stage.Screen.getPrimary(); // 确保Toolkit已初始化
            java.util.List<javafx.stage.Screen> javafxScreens = javafx.stage.Screen.getScreens();

            for (int i = 0; i < javafxScreens.size(); i++) {
                javafx.stage.Screen fxScreen = javafxScreens.get(i);
                javafx.geometry.Rectangle2D bounds = fxScreen.getBounds();
                javafx.geometry.Rectangle2D visualBounds = fxScreen.getVisualBounds();

                String screenName = "Screen" + (char) ('A' + i);
                // 使用完整边界来获取实际的屏幕尺寸（包括系统UI区域）
                screens.add(new ScreenInfo(screenName, (int) bounds.getWidth(), (int) bounds.getHeight()));
            }
        } catch (Exception e) {
            logger.warning("Failed to get screen info using JavaFX: " + e.getMessage());
        }

        // 如果上述方法都失败了，使用默认值
        if (screens.isEmpty()) {
            screens.add(new ScreenInfo("ScreenA", 1920, 1080)); // 默认1080p屏幕
        }

        return screens;
    }
}
