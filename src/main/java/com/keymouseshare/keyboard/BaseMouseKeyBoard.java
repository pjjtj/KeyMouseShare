package com.keymouseshare.keyboard;

import com.keymouseshare.util.SlidingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.KeyEvent;

public class BaseMouseKeyBoard {

    private static final Logger logger = LoggerFactory.getLogger(BaseMouseKeyBoard.class);

    private Robot robot;

    // 设置一个组合键清空机制，当持续3秒内没有按键被按下，则认为组合键已经结束，清空组合键
    SlidingCache<Integer, Integer> sessionCache = new SlidingCache<>(3000);

    public BaseMouseKeyBoard() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            logger.error("无法创建Robot实例 {}", e.getMessage());
        }
    }

    public void mouseMove(int x, int y) {
        if (robot != null) {
            // 回退到Robot
            robot.mouseMove(x, y);
        }
    }

    public void mousePress(int button, int x, int y) {
        if (robot != null) {
            // 回退到Robot
            if (!sessionCache.isEmpty()) {
                pressCombination();
            }
            robot.mousePress(button);
            robot.delay(50);
        }
    }

    public void mouseRelease(int button, int x, int y) {
        if (robot != null) {
            // 回退到Robot
            robot.mouseRelease(button);
            robot.delay(50);
        }
    }


    public void mouseWheel(int wheelAmount) {
        if (robot != null) {
            // 回退到Robot
            robot.mouseWheel(wheelAmount);
        }
    }

    public void keyPress(int keyCode) {
        if (robot != null) {
            sessionCache.put(keyCode, keyCode);
            if (sessionCache.getKeys().size() > 1) {
                pressCombination();
            } else {
                // 执行普通点击操作
                robot.keyPress(keyCode);
                robot.delay(50);
            }
        }
    }

    public void keyRelease(int keyCode) {
        if (robot != null) {
            robot.keyRelease(keyCode);
            sessionCache.remove(keyCode);
            robot.delay(50);
        }
    }

    /**
     * 按下组合键
     */
    public void pressCombination() {
        if (robot != null) {
            // 按顺序按下所有键
            for (int keyCode : sessionCache.getValues()) {
                robot.keyPress(keyCode);
                robot.delay(50);
            }
        }
    }

    /**
     * 检查是否按下了Ctrl键（兼容左右Ctrl键）
     *
     * @return 如果按下了Ctrl键返回true，否则返回false
     */
    public boolean isCtrlPressed() {
        return sessionCache.get(KeyEvent.VK_CONTROL) != null  || sessionCache.get(KeyEvent.VK_META) != null;
    }

    /**
     * 检查是否按下了Alt键（兼容左右Alt键）
     *
     * @return 如果按下了Alt键返回true，否则返回false
     */
    public boolean isAltPressed() {
        return sessionCache.get(KeyEvent.VK_ALT) != null;
    }

    /**
     * 检查是否按下了Shift键（兼容左右Shift键）
     *
     * @return 如果按下了Shift键返回true，否则返回false
     */
    public boolean isShiftPressed() {
        return sessionCache.get(KeyEvent.VK_SHIFT) != null;
    }
}