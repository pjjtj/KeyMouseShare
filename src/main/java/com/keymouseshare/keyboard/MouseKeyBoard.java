package com.keymouseshare.keyboard;

public interface MouseKeyBoard {

    void mouseMove(int x, int y);

    void mousePress(int button);

    void mouseRelease(int button);

    void mouseClick(int x, int y);

    void mouseDragged(int x, int y);

    void keyPress(char keyChar);

    void keyRelease(char keyChar);

    void initVirtualMouseLocation();

    void startMouseKeyController();

    void stopMouseKeyController();

    boolean isEdgeMode();

    void stopEdgeDetection();
    
    void mouseWheel(int wheelAmount);

}