package com.keymouseshare.keyboard.nux;

import com.keymouseshare.keyboard.MouseKeyBoard;
import com.keymouseshare.util.KeyBoardUtils;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.keymouseshare.util.KeyBoardUtils.getButtonMask;

public class LinuxMouseKeyBoard implements MouseKeyBoard {
    private static final Logger logger = Logger.getLogger(LinuxMouseKeyBoard.class.getName());

    private static final LinuxMouseKeyBoard INSTANCE = new LinuxMouseKeyBoard();

    public static LinuxMouseKeyBoard getInstance() {
        return INSTANCE;
    }

    private Robot robot;

    public LinuxMouseKeyBoard() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            logger.log(Level.SEVERE, "无法创建Robot实例", e);
        }
    }

    @Override
    public void mouseMove(int x, int y) {
        if (robot != null) {
            robot.mouseMove(x, y);
        }
    }

    @Override
    public void mousePress(int button) {
        if (robot != null) {
            int buttonMask = getButtonMask(button);
            robot.mousePress(buttonMask);
        }
    }

    @Override
    public void mouseRelease(int button) {
        if (robot != null) {
            int buttonMask = getButtonMask(button);
            robot.mouseRelease(buttonMask);
        }
    }

    @Override
    public void mouseClick(int x, int y) {
        if (robot != null) {
            robot.mouseMove(x, y);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        }
    }

    @Override
    public void mouseDragged(int x,int y) {
        if (robot != null) {
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseMove(x, y);
        }
    }

    @Override
    public void keyPress(char keyChar) {
        if (robot != null) {
            //TDDO 需要修改
            robot.keyPress(KeyBoardUtils.keyCharToMacKeyCode(keyChar));
        }
    }

    @Override
    public void keyRelease(char keyChar) {
        if (robot != null) {
            //TDDO 需要修改
            robot.keyRelease(KeyBoardUtils.keyCharToMacKeyCode(keyChar));
        }
    }

    @Override
    public void mouseWheel(int wheelAmount) {
        if (robot != null) {
            // 回退到Robot
            robot.mouseWheel(wheelAmount);
        }
    }

    @Override
    public void initVirtualMouseLocation() {

    }

    @Override
    public void startMouseKeyController() {

    }

    @Override
    public void stopMouseKeyController() {

    }

    @Override
    public void stopEdgeDetection() {

    }

    @Override
    public boolean isEdgeMode() {
        return false;
    }
}
