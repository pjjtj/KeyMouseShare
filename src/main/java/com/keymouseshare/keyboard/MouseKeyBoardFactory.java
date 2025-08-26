package com.keymouseshare.keyboard;

import com.sun.jna.Platform;

public interface MouseKeyBoardFactory {

    static MouseKeyBoard getFactory() {
        if (Platform.isWindows()) {
            return new WindowMouseKeyBoard();
        } else if (Platform.isMac()) {
            return new MacMouseKeyBoard();
        } else if (Platform.isLinux()) {
            return new LinuxMouseKeyBoard();
        }
        throw new UnsupportedOperationException("Unsupported platform");
    }
}
