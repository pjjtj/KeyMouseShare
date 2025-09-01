package com.keymouseshare.keyboard;


import com.keymouseshare.keyboard.mac.MacMouseKeyBoard;
import com.keymouseshare.keyboard.nux.LinuxMouseKeyBoard;
import com.keymouseshare.keyboard.win.WindowMouseKeyBoard;

public interface MouseKeyBoardFactory {

    static MouseKeyBoard getFactory() {
        String OS = System.getProperty("os.name").toLowerCase();
        if (OS.contains("win")) {
            return WindowMouseKeyBoard.getInstance();
        } else if (OS.contains("mac")) {
            return MacMouseKeyBoard.getInstance();
        } else if (OS.contains("nux")) {
            return LinuxMouseKeyBoard.getInstance();
        }
        throw new UnsupportedOperationException("Unsupported platform");
    }
}
