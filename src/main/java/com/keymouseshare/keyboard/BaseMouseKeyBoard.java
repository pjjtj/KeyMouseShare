package com.keymouseshare.keyboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

public class BaseMouseKeyBoard {

    private static final Logger logger = LoggerFactory.getLogger(BaseMouseKeyBoard.class);

    private Robot robot;

    public BaseMouseKeyBoard(){
        try {
            robot = new Robot();
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
            robot.keyPress(keyCode);
        }
    }

    public void keyRelease(int keyCode) {
        if (robot != null) {
            robot.keyRelease(keyCode);
        }
    }
}
