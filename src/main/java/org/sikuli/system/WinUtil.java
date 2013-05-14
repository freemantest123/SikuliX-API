/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.system;

import java.awt.Rectangle;
import java.awt.Window;
import org.sikuli.script.Debug;
import org.sikuli.script.FileManager;
import org.sikuli.script.Region;

public class WinUtil implements OSUtil {

  static {
    FileManager.loadLibrary("WinUtil");
  }

  @Override
  public int switchApp(String appName) {
    return switchApp(appName, 0);
  }

  @Override
  public native int switchApp(String appName, int num);

  @Override
  public native int switchApp(int pid, int num);

  @Override
  public native int openApp(String appName);

  @Override
  public native int closeApp(String appName);

  @Override
  public native int closeApp(int pid);

  @Override
  public Region getWindow(String appName) {
    return getWindow(appName, 0);
  }

  @Override
  public Region getWindow(int pid) {
    return getWindow(pid, 0);
  }

  @Override
  public Region getWindow(String appName, int winNum) {
    long hwnd = getHwnd(appName, winNum);
    return _getWindow(hwnd, winNum);
  }

  @Override
  public Region getWindow(int pid, int winNum) {
    long hwnd = getHwnd(pid, winNum);
    return _getWindow(hwnd, winNum);
  }

  @Override
  public Region getFocusedWindow() {
    Rectangle rect = getFocusedRegion();
    if (rect != null) {
      return Region.create(rect);
    }
    return null;
  }

  @Override
  public native void bringWindowToFront(Window win, boolean ignoreMouse);

  private static native long getHwnd(String appName, int winNum);

  private static native long getHwnd(int pid, int winNum);

  private static native Rectangle getRegion(long hwnd, int winNum);

  private static native Rectangle getFocusedRegion();

  private Region _getWindow(long hwnd, int winNum) {
    Rectangle rect = getRegion(hwnd, winNum);
    Debug.log("getWindow: " + rect);
    if (rect != null) {
      return Region.create(rect);
    }
    return null;
  }
}
