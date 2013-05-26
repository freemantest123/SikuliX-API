/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.script;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class RobotDesktop extends Robot implements RobotIF {

  final static int MAX_DELAY = 60000;
  private static int heldButtons = 0;
  private static String heldKeys = "";
  private static ArrayList<Integer> heldKeyCodes = new ArrayList<Integer>();
  private Screen scr = null;

  @Override
  public Screen getScreen() {
      return scr;
  }

  public RobotDesktop(Screen screen) throws AWTException {
    super(screen.getGraphicsDevice());
    scr = screen;
  }

  @Override
  public void smoothMove(Location dest) {
    smoothMove(Screen.atMouse(), dest, (long) (Settings.MoveMouseDelay * 1000L));
  }

  @Override
  public void smoothMove(Location src, Location dest, long ms) {
    if (ms == 0) {
      Debug.log(2, "smoothMove: " + src.toString() + "---" + dest.toString());
      mouseMove(dest.x, dest.y);
      return;
    }

    OverlayAnimator aniX = new TimeBasedAnimator(
            new OutQuarticEase(src.x, dest.x, ms));
    OverlayAnimator aniY = new TimeBasedAnimator(
            new OutQuarticEase(src.y, dest.y, ms));
    while (aniX.running()) {
      float x = aniX.step();
      float y = aniY.step();
      mouseMove((int) x, (int) y);
      delay(50);
    }
  }

  @Override
  public void mouseDown(int buttons) {
    if (heldButtons != 0) {
      Debug.error("mouseDown: buttons still pressed - using all", buttons, heldButtons);
      heldButtons |= buttons;
    } else {
      heldButtons = buttons;
    }
    mousePress(heldButtons);
    waitForIdle();
  }

  @Override
  public void mouseUp(int buttons) {
    if (buttons == 0) {
      mouseRelease(heldButtons);
      heldButtons = 0;
    } else {
      mouseRelease(buttons);
      heldButtons &= ~buttons;
    }
    waitForIdle();
  }

  @Override
  public void delay(int ms) {
    if (ms < 0) {
      return;
    }
    while (ms > MAX_DELAY) {
      super.delay(MAX_DELAY);
      ms -= MAX_DELAY;
    }
    super.delay(ms);
  }

  @Override
  public ScreenImage captureScreen(Rectangle rect) {
    BufferedImage img = createScreenCapture(rect);
    Debug.log(6, "DesktoRobot.captureScreen img: " + img);
    return new ScreenImage(rect, img);
  }

  @Override
  public Color getColorAt(int x, int y) {
    return getPixelColor(x, y);
  }

  @Override
  public void pressModifiers(int modifiers) {
    if ((modifiers & KeyModifier.SHIFT) != 0) {
      keyPress(KeyEvent.VK_SHIFT);
    }
    if ((modifiers & KeyModifier.CTRL) != 0) {
      keyPress(KeyEvent.VK_CONTROL);
    }
    if ((modifiers & KeyModifier.ALT) != 0) {
      keyPress(KeyEvent.VK_ALT);
    }
    if ((modifiers & KeyModifier.META) != 0) {
      if (Settings.isWindows()) {
        keyPress(KeyEvent.VK_WINDOWS);
      } else {
        keyPress(KeyEvent.VK_META);
      }
    }
  }

  @Override
  public void releaseModifiers(int modifiers) {
    if ((modifiers & KeyModifier.SHIFT) != 0) {
      keyRelease(KeyEvent.VK_SHIFT);
    }
    if ((modifiers & KeyModifier.CTRL) != 0) {
      keyRelease(KeyEvent.VK_CONTROL);
    }
    if ((modifiers & KeyModifier.ALT) != 0) {
      keyRelease(KeyEvent.VK_ALT);
    }
    if ((modifiers & KeyModifier.META) != 0) {
      if (Settings.isWindows()) {
        keyRelease(KeyEvent.VK_WINDOWS);
      } else {
        keyRelease(KeyEvent.VK_META);
      }
    }
  }

  @Override
  public void keyDown(String keys) {
    if (keys != null && !"".equals(keys)) {
      for (int i = 0; i < keys.length(); i++) {
        if (heldKeys.indexOf(keys.charAt(i)) == -1) {
          Debug.log(5, "press: " + keys.charAt(i));
          typeChar(keys.charAt(i), RobotIF.KeyMode.PRESS_ONLY);
          heldKeys += keys.charAt(i);
        }
      }
      waitForIdle();
    }
  }

  @Override
  public void keyDown(int code) {
    if (!heldKeyCodes.contains(code)) {
      keyPress(code);
      heldKeyCodes.add(code);
    }
    waitForIdle();
  }

  @Override
  public void keyUp(String keys) {
    if (keys != null && !"".equals(keys)) {
      for (int i = 0; i < keys.length(); i++) {
        int pos;
        if ((pos = heldKeys.indexOf(keys.charAt(i))) != -1) {
          Debug.log(5, "release: " + keys.charAt(i));
          typeChar(keys.charAt(i), RobotIF.KeyMode.RELEASE_ONLY);
          heldKeys = heldKeys.substring(0, pos)
                  + heldKeys.substring(pos + 1);
        }
      }
      waitForIdle();
    }
  }

  @Override
  public void keyUp(int code) {
    if (heldKeyCodes.contains(code)) {
      keyRelease(code);
      heldKeyCodes.remove((Object) code);
    }
    waitForIdle();
  }

  @Override
  public void keyUp() {
    keyUp(heldKeys);
    for (int code : heldKeyCodes) {
      keyUp(code);
    }
  }

  private void doType(KeyMode mode, int... keyCodes) {
    if (mode == KeyMode.PRESS_ONLY) {
      for (int i = 0; i < keyCodes.length; i++) {
        keyPress(keyCodes[i]);
      }
    } else if (mode == KeyMode.RELEASE_ONLY) {
      for (int i = 0; i < keyCodes.length; i++) {
        keyRelease(keyCodes[i]);
      }
    } else {
      for (int i = 0; i < keyCodes.length; i++) {
        keyPress(keyCodes[i]);
      }
      for (int i = 0; i < keyCodes.length; i++) {
        keyRelease(keyCodes[i]);
      }
    }
  }

  @Override
  public void typeChar(char character, KeyMode mode) {
    doType(mode, Key.toJavaKeyCode(character));
  }

  @Override
  public void cleanup() {
    HotkeyManager.getInstance().cleanUp();
    keyUp();
  }


}
