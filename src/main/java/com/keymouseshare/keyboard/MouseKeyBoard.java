package com.keymouseshare.keyboard;

public interface MouseKeyBoard {

    public void mouseMove(int x, int y);

    public void mousePress(int button);

    public void mouseRelease(int button);

    public void mouseClick(int x, int y);

    public void mouseDragged();

    public void keyPress(int keyCode);

    public void keyRelease(int keyCode);

    public void startIntercept();

    public void stopIntercept();

}
