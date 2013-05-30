/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.script;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class ScreenUnion extends Screen {
   private Rectangle _bounds;

   public ScreenUnion(){
      super();
   }

   public int getIdFromPoint(int x, int y){
      Debug.log(5, "union bound: " + getBounds() );
      Debug.log(5, "x, y: " + x + "," + y);
      x += getBounds().x;
      y += getBounds().y;
      Debug.log(5, "new x, y: " + x + "," + y);
      for(int i=0;i<getNumberScreens();i++)
         if(Screen.getBounds(i).contains(x, y)){
            return i;
         }
      return 0;
   }

  @Override
   public Rectangle getBounds(){
      if(_bounds == null){
         _bounds = new Rectangle();
         for (int i=0; i < Screen.getNumberScreens(); i++) {
            _bounds = _bounds.union(Screen.getBounds(i));
         }
      }
      return _bounds;
   }


  @Override
   public ScreenImage capture(Rectangle rect) {
      Debug.log(5, "capture: " + rect);

      BufferedImage ret = new BufferedImage( rect.width, rect.height, BufferedImage.TYPE_INT_RGB );
      Graphics2D g2d = ret.createGraphics();
      for (int i=0; i < Screen.getNumberScreens(); i++) {
         Rectangle scrBound = Screen.getBounds(i);
         if(scrBound.intersects(rect)){
            Rectangle inter = scrBound.intersection(rect);
            Debug.log(5, "scrBound: " + scrBound + ", inter: " +inter);
            int ix = inter.x, iy = inter.y;
            inter.x-=scrBound.x;
            inter.y-=scrBound.y;
            ScreenImage img = Screen.getScreen(i).getRobot().captureScreen(inter);
            g2d.drawImage(img.getImage(), ix-rect.x, iy-rect.y, null);
         }
      }
      g2d.dispose();
      return new ScreenImage(rect, ret);
   }

  @Override
   public boolean useFullscreen(){
      return false;
   }

}
