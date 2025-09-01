package com.keymouseshare.keyboard;


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
