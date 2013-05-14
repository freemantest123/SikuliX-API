/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.system;

import java.awt.Window;
import org.sikuli.script.Region;

public interface OSUtil {
  // Windows: returns PID, 0 if fails
  // Others: return 0 if succeeds, -1 if fails

  public int openApp(String appName);

  // Windows: returns PID, 0 if fails
  // Others: return 0 if succeeds, -1 if fails
  public int switchApp(String appName);

  public int switchApp(String appName, int winNum);

  //internal use
  public int switchApp(int pid, int num);

  // returns 0 if succeeds, -1 if fails
  public int closeApp(String appName);

  //internal use
  public int closeApp(int pid);

  public Region getWindow(String appName);

  public Region getWindow(String appName, int winNum);

  Region getWindow(int pid);

  Region getWindow(int pid, int winNum);

  public Region getFocusedWindow();

  public void bringWindowToFront(Window win, boolean ignoreMouse);
}
