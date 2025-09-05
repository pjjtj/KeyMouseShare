package com.keymouseshare.keyboard;

public interface MouseKeyBoard {

    void mouseMove(int x, int y);

    void mousePress(int button,int x, int y);

    void mouseRelease(int button, int x, int y);

    void mouseWheel(int wheelAmount);


    void keyPress(int keyCode);

    void keyRelease(int keyCode);



    void initVirtualMouseLocation();

    void startMouseKeyController();

    void stopMouseKeyController();

    boolean isEdgeMode();

    void stopEdgeDetection();
    


}