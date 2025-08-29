package com.keymouseshare.keyboard;

import com.sun.jna.Platform;

public interface MouseKeyBoardFactory {

    static MouseKeyBoard getFactory() {
        if (Platform.isWindows()) {
            return WindowMouseKeyBoard.getInstance();
        } else if (Platform.isMac()) {
            return MacMouseKeyBoard.getInstance();
        } else if (Platform.isLinux()) {
            return LinuxMouseKeyBoard.getInstance();
        }
        throw new UnsupportedOperationException("Unsupported platform");
    }
}
