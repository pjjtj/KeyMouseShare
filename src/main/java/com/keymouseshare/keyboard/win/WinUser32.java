package com.keymouseshare.keyboard.win;
import com.sun.jna.*;
import com.sun.jna.win32.*;
import com.sun.jna.platform.win32.*;

public interface WinUser32 extends StdCallLibrary {
    WinUser32 INSTANCE = Native.load("user32", WinUser32.class, W32APIOptions.DEFAULT_OPTIONS);

    WinUser.HHOOK SetWindowsHookEx(int idHook, WinHookManager.LowLevelProc lpfn, WinDef.HMODULE hMod, int dwThreadId);
    WinDef.LRESULT CallNextHookEx(WinUser.HHOOK hhk, int nCode, WinDef.WPARAM wParam, WinDef.LPARAM lParam);
    boolean UnhookWindowsHookEx(WinUser.HHOOK hhk);
    boolean GetMessage(WinUser.MSG lpMsg, WinDef.HWND hWnd, int wMsgFilterMin, int wMsgFilterMax);
    boolean TranslateMessage(WinUser.MSG lpMsg);
    WinDef.LRESULT DispatchMessage(WinUser.MSG lpMsg);

    int WH_MOUSE_LL = 14;
    int WH_KEYBOARD_LL = 13;
}