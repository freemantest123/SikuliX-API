/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.system;

import java.awt.Window;
import org.sikuli.script.Debug;
import org.sikuli.script.Region;

public class DummyOSUtil implements OSUtil {

   public int switchApp(String appName){
      Debug.error("Your OS doesn't support switchApp");
      return -1;
   }

   public int switchApp(String appName, int winNum){
      Debug.error("Your OS doesn't support switchApp");
      return -1;
   }

   public int switchApp(int pid, int num){
      Debug.error("Your OS doesn't support switchApp");
      return -1;
   }

   public int openApp(String appName){
      Debug.error("Your OS doesn't support openApp");
      return -1;
   }


   public int closeApp(String appName){
      Debug.error("Your OS doesn't support closeApp");
      return -1;
   }

   public int closeApp(int pid){
      Debug.error("Your OS doesn't support closeApp");
      return -1;

   }

   public Region getWindow(String appName){
      Debug.error("Your OS doesn't support getWindow");
      return null;
   }

   public Region getWindow(String appName, int winNum){
      Debug.error("Your OS doesn't support getWindow");
      return null;
   }

   public Region getWindow(int pid){
      Debug.error("Your OS doesn't support getWindow");
      return null;
   }

   public Region getWindow(int pid, int winNum){
      Debug.error("Your OS doesn't support getWindow");
      return null;
   }


   public Region getFocusedWindow(){
      Debug.error("Your OS doesn't support getFocusedWindow");
      return null;
   }
   public void setWindowOpacity(Window win, float alpha){
   }
   public void setWindowOpaque(Window win, boolean opaque){
   }
   public void bringWindowToFront(Window win, boolean ignoreMouse){}
}


