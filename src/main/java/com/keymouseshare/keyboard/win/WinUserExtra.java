package com.keymouseshare.keyboard.win;

public interface WinUserExtra {
    // 鼠标消息（JNA WinUser 没提供的部分）
    int WM_MOUSEMOVE      = 0x0200;
    int WM_LBUTTONDOWN    = 0x0201;
    int WM_LBUTTONUP      = 0x0202;
    int WM_LBUTTONDBLCLK  = 0x0203;
    int WM_RBUTTONDOWN    = 0x0204;
    int WM_RBUTTONUP      = 0x0205;
    int WM_RBUTTONDBLCLK  = 0x0206;
    int WM_MBUTTONDOWN    = 0x0207;
    int WM_MBUTTONUP      = 0x0208;
    int WM_MBUTTONDBLCLK  = 0x0209;
    int WM_MOUSEWHEEL     = 0x020A;
    int WM_XBUTTONDOWN    = 0x020B;
    int WM_XBUTTONUP      = 0x020C;
    int WM_XBUTTONDBLCLK  = 0x020D;
    int WM_MOUSEHWHEEL    = 0x020E;
}
