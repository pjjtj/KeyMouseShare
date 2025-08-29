package com.keymouseshare.keyboard;

public interface MouseKeyBoard {

    void mouseMove(int x, int y);

    void mousePress(int button);

    void mouseRelease(int button);

    void mouseClick(int x, int y);

    void mouseDragged();

    void keyPress(int keyCode);

    void keyRelease(int keyCode);

    void initVirtualMouseLocation();

    void startMouseKeyController();

    void stopMouseKeyController();

    void stopEdgeDetection();
}
