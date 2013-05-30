/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.script;

import java.awt.Color;
import java.awt.Point;

/**
 * A point like AWT.Point using global coordinates, hence modifications might move location out of
 * any screen (not checked as is done with region)
 *
 */
public class Location extends Point {

  /**
   * to allow calculated x and y that might not be integers
   * @param x
   * @param y
   * truncated to the integer part
   */
  public Location(float x, float y) {
    super((int) x, (int) y);
  }

  /**
   *
   * @param x
   * @param y
   */
  public Location(int x, int y) {
    super(x, y);
  }

  /**
   *
   * @param loc
   */
  public Location(Location loc) {
    super(loc.x, loc.y);
  }

  /**
   *
   * @param point
   */
  public Location(Point point) {
    super(point);
  }

   /**
    * Returns null, if outside of any screen<br />
    * subsequent actions will crash
    *
    * @return the screen, that contains the given point.<br />
    */
  public Screen getScreen() {
    for (int i = 0; i < Screen.getNumberScreens(); i++) {
      if (Screen.getScreen(i).contains(this)) {
        return Screen.getScreen(i);
      }
    }
    Debug.error("Location: outside any screen (%s, %s) - subsequent actions might not work as expected", x, y);
    return null;
  }

// TODO Location.getColor() implement more support and make it useable
  /**
   * Get the color at the given Point for details: see java.awt.Robot and ...Color
   *
   * @return The Color of the Point
   */
  public Color getColor() {
    if (getScreen() == null) {
      return null;
    }
    return getScreen().getRobot().getColorAt(x, y);
  }

  /**
   * the offset of given point as (x,y) relative to this point
   *
   * @param loc1
   * @param loc2
   * @return relative offset
   */
  public Location getOffset(Location loc) {
    return (new Location(loc.x - x, loc.y - y));
  }

  /**
   * create a region with this point as center and the given size
   *
   * @param w the width
   * @param h the height
   * @return the new region
   */
  public Region grow(int w, int h) {
    return Region.grow(this, w, h);
  }

  /**
   * create a region with this point as center and the given size
   *
   * @param wh the width and height
   * @return the new region
   */
  public Region grow(int wh) {
    return grow(wh, wh);
  }

  /**
   * create a region with a corner at this point<br />as specified with x y<br /> 0 0 top left<br />
   * 0 1 bottom left<br /> 1 0 top right<br /> 1 1 bottom right<br />
   *
   * @param CREATE_X_DIRECTION == 0 is left side !=0 is right side, see {@link Region#CREATE_X_DIRECTION_LEFT}, {@link Region#CREATE_X_DIRECTION_RIGHT}
   * @param CREATE_Y_DIRECTION == 0 is top side !=0 is bottom side, see {@link Region#CREATE_Y_DIRECTION_TOP}, {@link Region#CREATE_Y_DIRECTION_BOTTOM}
   * @param w the width
   * @param h the height
   * @return the new region
   */
  public Region grow(int CREATE_X_DIRECTION, int CREATE_Y_DIRECTION, int w, int h) {
    return Region.create(this, CREATE_X_DIRECTION, CREATE_Y_DIRECTION, w, h);
  }

  /**
   * moves the point the given amounts in the x and y direction, might be negative <br />might move
   * point outside of any screen, not checked
   *
   * @param dx
   * @param dy
   * @return the location itself modified
   */
  public Location moveFor(int dx, int dy) {
    super.translate(dx, dy);
    return this;
  }

  /**
   * changes the locations x and y value to the given values (moves it) <br />might move point
   * outside of any screen, not checked
   *
   * @param X
   * @param Y
   * @return the location itself modified
   */
  public Location moveTo(int X, int Y) {
    super.move(X, Y);
    return this;
  }

  /**
   * creates a point at the given offset, might be negative <br />might create a point outside of
   * any screen, not checked
   *
   * @param dx
   * @param dy
   * @return new location
   */
  public Location offset(int dx, int dy) {
    return new Location(x + dx, y + dy);
  }

  /**
   * creates a point at the given offset to the left, might be negative <br />might create a point
   * outside of any screen, not checked
   *
   * @param dx
   * @return new location
   */
  public Location left(int dx) {
    return new Location(x - dx, y);
  }

  /**
   * creates a point at the given offset to the right, might be negative <br />might create a point
   * outside of any screen, not checked
   *
   * @param dx
   * @return new location
   */
  public Location right(int dx) {
    return new Location(x + dx, y);
  }

  /**
   * creates a point at the given offset above, might be negative <br />might create a point outside
   * of any screen, not checked
   *
   * @param dy
   * @return new location
   */
  public Location above(int dy) {
    return new Location(x, y - dy);
  }

  /**
   * creates a point at the given offset below, might be negative <br />might create a point outside
   * of any screen, not checked
   *
   * @param dy
   * @return new location
   */
  public Location below(int dy) {
    return new Location(x, y + dy);
  }

  /**
   * new point with same offset to current screen's top left on given screen
   *
   * @param scrID number of screen
   * @return new location
   */
  public Location copyTo(int scrID) {
    return copyTo(Screen.getScreen(scrID));
  }

  /**
   * New point with same offset to current screen's top left on given screen
   *
   * @param screen new parent screen
   * @return new location
   */
  public Location copyTo(Screen screen) {
    Location o = new Location(getScreen().getBounds().getLocation());
    Location n = new Location(screen.getBounds().getLocation());
    return new Location(n.x + x - o.x, n.y + y - o.y);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "L(" + x + "," + y + ")@" + getScreen().toStringShort();
  }
}
