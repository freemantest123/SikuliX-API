/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.script;

import java.awt.Color;
import java.awt.Rectangle;

public interface RobotIF {
   enum KeyMode {
      PRESS_ONLY, RELEASE_ONLY, PRESS_RELEASE
   };
   void keyDown(String keys);
   void keyUp(String keys);
   void keyDown(int code);
   void keyUp(int code);
   void keyUp();
   void pressModifiers(int modifiers);
   void releaseModifiers(int modifiers);
   void typeChar(char character, KeyMode mode);
   void mouseMove(int x, int y);
   void mouseDown(int buttons);
   void mouseUp(int buttons);
   void smoothMove(Location dest);
   void smoothMove(Location src, Location dest, long ms);
   void mouseWheel(int wheelAmt);
   ScreenImage captureScreen(Rectangle screenRect);
   void waitForIdle();
   void delay(int ms);
   void setAutoDelay(int ms);
   Color getColorAt(int x, int y);
   void cleanup();

   /**
    *  Return the underlying device object (if any).
    */
   Screen getScreen();
}

