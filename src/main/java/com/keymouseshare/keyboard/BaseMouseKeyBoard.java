package com.keymouseshare.keyboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.LinkedHashSet;
import java.util.Set;

public class BaseMouseKeyBoard {

    private static final Logger logger = LoggerFactory.getLogger(BaseMouseKeyBoard.class);

    private Robot robot;
    private Set<Integer> pressedKeys = new LinkedHashSet<>();

    public BaseMouseKeyBoard() {
        try {
            robot = new Robot();
            robot.delay(5);
        } catch (AWTException e) {
            logger.error("无法创建Robot实例", e);
        }
    }

    public void mouseMove(int x, int y) {
        if (robot != null) {
            // 回退到Robot
            robot.mouseMove(x, y);
        }
    }

    public void mousePress(int button) {
        if (robot != null) {
            // 回退到Robot
            if(!pressedKeys.isEmpty()){
                pressCombination();
            }
            robot.mousePress(button);
        }
    }

    public void mouseRelease(int button) {
        if (robot != null) {
            // 回退到Robot
            robot.mouseRelease(button);
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
            pressedKeys.add(keyCode);
            if (pressedKeys.size() > 1) {
                pressCombination();
            } else {
                // 执行普通点击操作
                robot.keyPress(keyCode);
            }
        }
    }

    public void keyRelease(int keyCode) {
        if (robot != null) {
            robot.keyRelease(keyCode);
            pressedKeys.remove(keyCode);
        }
    }

    /**
     * 按下组合键
     */
    public void pressCombination() {
        if (robot != null) {
            // 按顺序按下所有键
            for (int keyCode : pressedKeys) {
                robot.keyPress(keyCode);
            }
        }
    }

    /**
     * 检查是否按下了Ctrl键（兼容左右Ctrl键）
     *
     * @return 如果按下了Ctrl键返回true，否则返回false
     */
    public boolean isCtrlPressed() {
        return pressedKeys.contains(KeyEvent.VK_CONTROL) || pressedKeys.contains(KeyEvent.VK_META);
    }

    /**
     * 检查是否按下了Alt键（兼容左右Alt键）
     *
     * @return 如果按下了Alt键返回true，否则返回false
     */
    public boolean isAltPressed() {
        return pressedKeys.contains(KeyEvent.VK_ALT);
    }

    /**
     * 检查是否按下了Shift键（兼容左右Shift键）
     *
     * @return 如果按下了Shift键返回true，否则返回false
     */
    public boolean isShiftPressed() {
        return pressedKeys.contains(KeyEvent.VK_SHIFT);
    }
}