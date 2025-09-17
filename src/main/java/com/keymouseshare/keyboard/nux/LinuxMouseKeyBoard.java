package com.keymouseshare.keyboard.nux;

import com.keymouseshare.keyboard.BaseMouseKeyBoard;
import com.keymouseshare.keyboard.MouseKeyBoard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LinuxMouseKeyBoard extends BaseMouseKeyBoard implements MouseKeyBoard {
    private static final Logger logger = LoggerFactory.getLogger(LinuxMouseKeyBoard.class.getName());

    private static final LinuxMouseKeyBoard INSTANCE = new LinuxMouseKeyBoard();

    public static LinuxMouseKeyBoard getInstance() {
        return INSTANCE;
    }

    public LinuxMouseKeyBoard() {
       super();
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

    @Override
    public boolean isChangingScreen() {
        return true;
    }
}
