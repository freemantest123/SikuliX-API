/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.script;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * A Region always lies completely inside its parent screen
 *
 * @author RaiMan
 */
public class Region {

    final static float DEFAULT_HIGHLIGHT_TIME = Settings.DefaultHighlightTime;
    static final int PADDING = Settings.DefaultPadding;

    private Screen scr;
    private ScreenHighlighter overlay = null;

    public int x, y;
    /**
     * current width/height - might be cropped to screen
     */
    public int w, h;
    /**
     * width/height is remembered when region is cropped the 1st time and reused later
     */
    protected int vWidth = -1;
    protected int vHeight = -1;

    private FindFailedResponse findFailedResponse =
            Settings.defaultFindFailedResponse;
    protected boolean throwException = Settings.ThrowException;
    protected double autoWaitTimeout = Settings.AutoWaitTimeout;
    private boolean observing = false;
    private SikuliEventManager evtMgr = null;
    private Match lastMatch = null;
    private Iterator<Match> lastMatches;

    @Override
    public String toString() {
        return String.format("R[%d,%d %dx%d]@%s E:%s, T:%.1f",
                x, y, w, h, (getScreen()== null ? "Screen???" : getScreen().toStringShort()),
                throwException ? "Y" : "N", autoWaitTimeout);
    }

    //<editor-fold defaultstate="collapsed" desc="Initialization">

    private Region initialize(int X, int Y, int W, int H, Screen parentScreen) {
        x = X;
        y = Y;
        w = W;
        h = H;
        if (parentScreen != null) {
            setScreen(parentScreen);
        }
        initScreen(getScreen());
        return this;
    }

    protected int getVW() {
        return (vWidth < 0) ? w : vWidth;
    }

    protected int getVH() {
        return (vHeight < 0) ? h : vHeight;
    }

    private void initScreen(Screen scr) {
        if (!(this instanceof Screen)) {
            Rectangle rect = new Rectangle(x, y, getVW(), getVH());
            setScreen(null);
            for (int i = 0; i < Screen.getNumberScreens(); i++) {
                Rectangle sb = Screen.getBounds(i);
                if (sb.contains(rect.getLocation())) {
                    setScreen(Screen.getScreen(i));
                    break;
                }
            }
            Screen scrNew = getScreen();
            if (scrNew == null) {
                w = getVW();
                h = getVH();
                if (scr == null) {
                    Debug.error("Region (%s, %s) outside any screen - subsequent actions might not work as expected", w, h);
                } else {
                    scrNew = scr;
                }
            }
            if (scrNew != null) {
                setScreen(scrNew);
                rect = scrNew.getBounds().intersection(rect);
                if (rect.width < w || rect.height < h) {
                    Debug.log(1, "%s cropped to screen", this);
                    if (vWidth < 0) {
                        vWidth = w;
                        vHeight = h;
                    }
                }
                x = (int) rect.getX();
                y = (int) rect.getY();
                w = (int) rect.getWidth();
                h = (int) rect.getHeight();
                return; // to position a breakpoint here
            }
        }
        updateSelf();
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Constructors to be used with Jython">
    /**
     * Create a region with the provided coordinate / size
     *
     * @param X X position
     * @param Y Y position
     * @param W width
     * @param H heigth
     */
    public Region(int X, int Y, int W, int H) {
        initialize(X, Y, W, H, null);
    }

    /**
     * Create a region from a Rectangle
     *
     * @param r the Rectangle
     */
    public Region(Rectangle r) {
        initialize(r.x, r.y, r.width, r.height, null);
    }

    /**
     * Create a new region from another region<br />including the region's
     * settings
     *
     * @param r the region
     */
    public Region(Region r) {
        autoWaitTimeout = r.autoWaitTimeout;
        findFailedResponse = r.findFailedResponse;
        throwException = r.throwException;
        initialize(r.x, r.y, r.w, r.h, r.getScreen());
    }

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Quasi-Constructors to be used in Java">
    /**
     * internal use only, used for new Screen objects to get the Region behavior
     */
    protected Region() {
    }

    /**
     * Create a region with the provided top left corner and size
     *
     * @param X top left X position
     * @param Y top left Y position
     * @param W width
     * @param H heigth
     * @return
     */
    public static Region create(int X, int Y, int W, int H) {
        return Region.create(X, Y, W, H, null);
    }

    /**
     * Create a region with the provided top left corner and size
     *
     * @param X top left X position
     * @param Y top left Y position
     * @param W width
     * @param H heigth
     * @param scr the source screen
     * @return the created region
     */
    private static Region create(int X, int Y, int W, int H, Screen scr) {
        Region reg = new Region();
        reg = reg.initialize(X, Y, W, H, scr);
        StackTraceElement[] callstack = Thread.currentThread().getStackTrace();
        String showMe = callstack[callstack.length-1].toString();
        return reg;
    }

    /**
     * Create a region with the provided top left corner and size
     *
     * @param loc top left corner
     * @param w width
     * @param h height
     * @return
     */
    public static Region create(Location loc, int w, int h) {
        return Region.create(loc.x, loc.y, w, h, null);
    }

    /**
     * create a region with a corner at the given point<br />as specified with x
     * y<br /> 0 0 top left<br /> 0 1 bottom left<br /> 1 0 top right<br /> 1 1
     * bottom right<br />
     *
     * @param loc the refence point
     * @param x ==0 is left side !=0 is right side
     * @param y ==0 is top side !=0 is bottom side
     * @param w the width
     * @param h the height
     * @return the new region
     */
    public static Region create(Location loc, int x, int y, int w, int h) {
        int X;
        int Y;
        int W = w;
        int H = h;
        if (x == 0) {
            if (y == 0) {
                X = loc.x;
                Y = loc.y;
            } else {
                X = loc.x;
                Y = loc.y - h;
            }
        } else {
            if (y == 0) {
                X = loc.x - w;
                Y = loc.y;
            } else {
                X = loc.x - w;
                Y = loc.y - h;
            }
        }
        return Region.create(X, Y, W, H);
    }

    /**
     * create a region with a corner at the given point<br />as specified with x
     * y<br /> 0 0 top left<br /> 0 1 bottom left<br /> 1 0 top right<br /> 1 1
     * bottom right<br />same as the corresponding create method,
     * here to be naming compatible with class Location
     *
     * @param loc the refence point
     * @param x ==0 is left side !=0 is right side
     * @param y ==0 is top side !=0 is bottom side
     * @param w the width
     * @param h the height
     * @return the new region
     */
    public static Region grow(Location loc, int x, int y, int w, int h) {
        return Region.create(loc, x, y, w, h);
    }

    /**
     * Create a region from a Rectangle
     *
     * @param r the Rectangle
     * @return
     */
    public static Region create(Rectangle r) {
        return Region.create(r.x, r.y, r.width, r.height, null);
    }

    protected static Region create(Rectangle r, Screen parentScreen) {
        return Region.create(r.x, r.y, r.width, r.height, parentScreen);
    }

    /**
     * Create a region from another region<br />including the region's settings
     *
     * @param r the region
     * @return
     */
    public static Region create(Region r) {
        Region reg = Region.create(r.x, r.y, r.w, r.h, null);
        reg.autoWaitTimeout = r.autoWaitTimeout;
        reg.findFailedResponse = r.findFailedResponse;
        reg.throwException = r.throwException;
        return reg;
    }

    /**
     * create a region with the given point as center and the given size
     *
     * @param loc the center point
     * @param w the width
     * @param h the height
     * @return the new region
     */
    public static Region grow(Location loc, int w, int h) {
        int X = loc.x - w/2;
        int Y = loc.y - h/2;
        return Region.create(X, Y, w, h);
    }

    /**
     * create a minimal symetric region at given point as center with size 3x3
     *
     * @param loc the center point
     * @return the new region
     */
    public static Region grow(Location loc) {
        return grow(loc, 3, 3);
    }

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="handle coordinates">
    /**
     * check if current region contains given point
     *
     * @param point
     * @return true/false
     */
    public boolean contains(Location point) {
        return getRect().contains(point.x, point.y);
    }

    /**
     * check if mouse pointer is inside current region
     *
     * @return true/false
     */
    public boolean containsMouse() {
        return contains(atMouse());
    }

    /**
     * new region with same offset to current screen's top left on primary screen
     *
     * @return new region
     */
    public Region copyTo() {
        return copyTo(Screen.getPrimaryScreen());
    }

    /**
     * new region with same offset to current screen's top left on given screen
     *
     * @param scrID number of screen
     * @return new region
     */
    public Region copyTo(int scrID) {
        return copyTo(Screen.getScreen(scrID));
    }

    /**
     * new region with same offset to current screen's top left on given screen
     *
     * @param screen new parent screen
     * @return new region
     */
    public Region copyTo(Screen screen) {
        Location o = new Location(getScreen().getBounds().getLocation());
        Location n = new Location(screen.getBounds().getLocation());
        return Region.create(n.x + x - o.x, n.y + y - o.y, getVW(), getVH());
    }

    /**
     * new region with same offset to current screen's top left <br />on the given
     * region's screen <br />mainly to support Jython Screen objects
     *
     * @param screen new parent screen
     * @return new region
     */
    public Region copyTo(Region reg) {
        return copyTo(reg.getScreen());
    }

    /**
     * used in SikuliEventManager.callChangeObserver, Finder.next to adjust region
     * relative coordinates of matches to screen coordinates
     *
     * @param m
     * @return
     */
    protected Match toGlobalCoord(Match m) {
        m.x += x;
        m.y += y;
        return m;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="handle Settings">
    /**
     * true - (initial setting) should throw exception FindFailed if find
     * unsuccessful in this region<br /> false - do not abort script on FindFailed
     * (might leed to null pointer exceptions later)
     *
     * @param flag true/false
     */
    public void setThrowException(boolean flag) {
        throwException = flag;
        if (throwException) {
            findFailedResponse = FindFailedResponse.ABORT;
        } else {
            findFailedResponse = FindFailedResponse.SKIP;
        }
    }

    /**
     * current setting for this region (see setThrowException)
     *
     * @return true/false
     */
    public boolean getThrowException() {
        return throwException;
    }

    /**
     * the time in seconds a find operation should wait for the appearence of the
     * target in this region<br /> initial value 3 secs
     *
     * @param sec
     */
    public void setAutoWaitTimeout(double sec) {
        autoWaitTimeout = sec;
    }

    /**
     * current setting for this region (see setAutoWaitTimeout)
     *
     * @return value of seconds
     */
    public double getAutoWaitTimeout() {
        return autoWaitTimeout;
    }

    /**
     * FindFailedResponse.<br /> ABORT - (initial value) abort script on
     * FindFailed (= setThrowException(true) )<br /> SKIP - ignore FindFailed
     * (same as setThrowException(false) )<br /> PROMPT - display prompt on
     * FindFailed to let user decide how to proceed<br /> RETRY - continue to wait
     * for appearence on FindFailed (caution: endless loop)
     *
     * @param response FindFailedResponse.XXX
     */
    public void setFindFailedResponse(FindFailedResponse response) {
        findFailedResponse = response;
    }

    /**
     *
     * @return the current setting (see setFindFailedResponse)
     */
    public FindFailedResponse getFindFailedResponse() {
        return findFailedResponse;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="getters / setters / modificators">
    /**
     *
     * @return the Screen object containing the region
     */
    public Screen getScreen() {
        return scr;
    }

    /**
     *
     * @return the screen, that contains the top left corner of the region.
     * Returns primary screen if outside of any screen.
     */
    public Screen getScreenContaining() {
        for (int i = 0; i < Screen.getNumberScreens(); i++) {
            Rectangle sb = Screen.getBounds(i);
            if (sb.contains(this.getTopLeft())) {
                return Screen.getScreen(i);
            }
        }
        return Screen.getPrimaryScreen();
    }

    /**
     *
     * @param is the containing screen object
     */
    protected void setScreen(Screen is) {
        scr = is;
    }

    /**
     * internal use from Screen initialization to act like Region
     * @param id the containing screen object's id
     */
    protected void setScreen(int id) {
        scr = Screen.getScreen(id);
    }

    /**
     * synonym for showMonitors
     */
    public void showScreens() {
        Screen.showMonitors();
    }

    /**
     * synonym for resetMonitors
     */
    public void resetScreens() {
        Screen.resetMonitors();
    }

    // ************************************************
    /**
     *
     * @return the center pixel location of the region
     */
    public Location getCenter() {
        return new Location(x + w / 2, y + h / 2);
    }

    /**
     * conveneince method
     *
     * @return the region's center or match's TargetOffset
     */
    public Location getTarget() {
        return getCenter();
    }

    /**
     * Moves the region to the area, whose center is the given location
     *
     * @param loc
     * @return the region itself
     */
    public Region setCenter(Location loc) {
        Location c = getCenter();
        x = x - c.x + loc.x;
        y = y - c.y + loc.y;
        initScreen(loc.getScreen());
        return this;
    }

    /**
     *
     * @return top left corner Location
     */
    public Location getTopLeft() {
        return new Location(x, y);
    }

    /**
     * Moves the region to the area, whose top left corner is the given location
     *
     * @param loc
     * @return the region itself
     */
    public Region setTopLeft(Location loc) {
        return setLocation(loc);
    }

    /**
     *
     * @return top right corner Location
     */
    public Location getTopRight() {
        return new Location(x + w, y);
    }

    /**
     * Moves the region to the area, whose top right corner is the given location
     *
     * @param loc
     * @return the region itself
     */
    public Region setTopRight(Location loc) {
        Location c = getTopRight();
        x = x - c.x + loc.x;
        y = y - c.y + loc.y;
        initScreen(getScreen());
        return this;
    }

    /**
     *
     * @return bottom left corner Location
     */
    public Location getBottomLeft() {
        return new Location(x, y + h);
    }

    /**
     * Moves the region to the area, whose bottom left corner is the given
     * location
     *
     * @param loc
     * @return the region itself
     */
    public Region setBottomLeft(Location loc) {
        Location c = getBottomLeft();
        x = x - c.x + loc.x;
        y = y - c.y + loc.y;
        initScreen(getScreen());
        return this;
    }

    /**
     *
     * @return bottom right corner Location
     */
    public Location getBottomRight() {
        return new Location(x + w, y + h);
    }

    /**
     * Moves the region to the area, whose bottom right corner is the given
     * location
     *
     * @param loc
     * @return the region itself
     */
    public Region setBottomRight(Location loc) {
        Location c = getBottomRight();
        x = x - c.x + loc.x;
        y = y - c.y + loc.y;
        initScreen(null);
        return this;
    }

    // ************************************************
    /**
     *
     * @return x of top left corner
     */
    public int getX() {
        return x;
    }

    /**
     *
     * @return y of top left corner
     */
    public int getY() {
        return y;
    }

    /**
     *
     * @return width of region
     */
    public int getW() {
        return w;
    }

    /**
     *
     * @return height of region
     */
    public int getH() {
        return h;
    }

    /**
     *
     * @param X new x position of top left corner
     */
    public void setX(int X) {
        x = X;
    }

    /**
     *
     * @param Y new y position of top left corner
     */
    public void setY(int Y) {
        y = Y;
    }

    /**
     *
     * @param W new width
     */
    public void setW(int W) {
        w = W;
    }

    /**
     *
     * @param H new height
     */
    public void setH(int H) {
        h = H;
    }

    // ************************************************
    /**
     *
     * @param W new width
     * @param H new height
     * @return the region itself
     */
    public Region setSize(int W, int H) {
        w = W;
        h = H;
        initScreen(null);
        return this;
    }

    /**
     *
     * @return the AWT Rectangle of the region
     */
    public Rectangle getRect() {
        return new Rectangle(x, y, w, h);
    }

    /**
     * set the regions position/size<br />this might move the region even to
     * another screen
     *
     * @param r the AWT Rectagle to use for position/size
     * @return the region itself
     */
    public Region setRect(Rectangle r) {
        setRect(r.x, r.y, r.width, r.height);
        return this;
    }

    /**
     * set the regions position/size<br />this might move the region even to
     * another screen
     *
     * @param X new x of top left corner
     * @param Y new y of top left corner
     * @param W new width
     * @param H new height
     * @return the region itself
     */
    public Region setRect(int X, int Y, int W, int H) {
        x = X;
        y = Y;
        w = W;
        h = H;
        initScreen(getScreen());
        return this;
    }

    /**
     * set the regions position/size<br />this might move the region even to
     * another screen
     *
     * @param r the region to use for position/size
     * @return the region itself
     */
    public Region setRect(Region r) {
        setRect(r.x, r.y, r.w, r.h);
        return this;
    }

    // ****************************************************
    /**
     * returns the region being the current ROI of the containing screen
     *
     * Because of the wanted side effect for the containing screen,
     * this should only be used with screen objects.
     * For Region objects use getRect() instead.
     *
     * @return
     */
    public Region getROI() {
        return Region.create(getScreen().getCurROI());
    }

    /**
     * resets this region to the given location, size <br />sets the ROI of the
     * containing screen to this modified region <br />this might move the region
     * even to another screen
     *
     * Because of the wanted side effect for the containing screen,
     * this should only be used with screen objects.
     * For Region objects use setRect() instead.
     *
     * @param X
     * @param Y
     * @param W
     * @param H
     */
    public void setROI(int X, int Y, int W, int H) {
        x = X;
        y = Y;
        w = W;
        h = H;
        initScreen(getScreen());
        getScreen().setCurROI(new Rectangle(X, Y, W, H));
    }

    /**
     * resets this region to the given rectangle <br />sets the ROI of the
     * containing screen to this modified region<br />this might move the region
     * even to another screen
     *
     * Because of the wanted side effect for the containing screen,
     * this should only be used with screen objects.
     * For Region objects use setRect() instead.
     *
     * @param roi
     */
    public void setROI(Rectangle roi) {
        x = (int) roi.getX();
        y = (int) roi.getY();
        w = (int) roi.getWidth();
        h = (int) roi.getHeight();
        initScreen(getScreen());
        getScreen().setCurROI(roi);
    }

    /**
     * resets this region to the given region <br />sets the ROI of the containing
     * screen to this modified region<br />this might move the region even to
     * another screen
     *
     * Because of the wanted side effect for the containing screen,
     * this should only be used with screen objects.
     * For Region objects use setRect() instead.
     *
     * @param reg
     */
    public void setROI(Region reg) {
        x = reg.x;
        y = reg.y;
        w = reg.w;
        h = reg.h;
        initScreen(getScreen());
        getScreen().setCurROI(reg.getRect());
    }

    /**
     * resets the ROI of the containing screen
     * to the physical bounds of the screen again
     * the region itself is not touched
     *
     * Because of the wanted side effect for the containing screen,
     * this should only be used with screen objects.
     * For Region objects use setRect() instead.
     *
     * @param reg
     */
    public void resetROI() {
        if ((this instanceof Screen)) {
            Rectangle roi = getScreen().getBounds();
            x = (int) roi.getX();
            y = (int) roi.getY();
            w = (int) roi.getWidth();
            h = (int) roi.getHeight();
            getScreen().setCurROI(roi);
        }
    }

    // ****************************************************
    /**
     *
     * @return the region itself
     * @deprecated only for bachward compatibility
     */
    @Deprecated
    public Region inside() {
        return this;
    }

    /**
     * set the regions position<br />this might move the region even to another
     * screen
     *
     * @param loc new top left corner
     * @return the region itself
     * @deprecated to be like AWT Rectangle API use setLocation()
     */
    @Deprecated
    public Region moveTo(Location loc) {
        return setLocation(loc);
    }

    /**
     * set the regions position<br />this might move the region even to another
     * screen
     *
     * @param loc new top left corner
     * @return the region itself
     */
    public Region setLocation(Location loc) {
        x = loc.x;
        y = loc.y;
        initScreen(getScreen());
        return this;
    }

    /**
     * set the regions position/size<br />this might move the region even to
     * another screen
     *
     * @param r
     * @return the region itself
     * @deprecated to be like AWT Rectangle API use setRect() instead
     */
    @Deprecated
    public Region morphTo(Region r) {
        return setRect(r);
    }

    /**
     * resize the region using the given padding values<br />might be negative
     *
     * @param l padding on left side
     * @param r padding on right side
     * @param t padding at top side
     * @param b padding at bottom side
     * @return the region itself
     */
    public Region add(int l, int r, int t, int b) {
        x = x - l;
        y = y - t;
        w = w + l + r;
        h = h + t + b;
        initScreen(getScreen());
        return this;
    }

    /**
     * extend the region, so it contains the given region<br />but only the part
     * inside the current screen
     *
     * @param r the region to include
     * @return the region itself
     */
    public Region add(Region r) {
        Rectangle rect = getRect();
        rect.add(r.getRect());
        setRect(rect);
        initScreen(getScreen());
        return this;
    }

    /**
     * extend the region, so it contains the given point<br />but only the part
     * inside the current screen
     *
     * @param loc the point to include
     * @return the region itself
     */
    public Region add(Location loc) {
        Rectangle rect = getRect();
        rect.add(loc.x, loc.y);
        setRect(rect);
        initScreen(getScreen());
        return this;
    }

    // ************************************************
    /**
     * a find operation saves its match on success in the used region object<br
     * />unchanged if not successful
     *
     * @return the Match object from last successful find in this region
     */
    public Match getLastMatch() {
        return lastMatch;
    }

    // ************************************************
    /**
     * a findAll operation saves its matches on success in the used region
     * object<br />unchanged if not successful
     *
     * @return a Match-Iterator of matches from last successful findAll in this
     * region
     */
    public Iterator<Match> getLastMatches() {
        return lastMatches;
    }

    // ************************************************
    /**
     * get the last image taken on this regions screen
     * @return
     */
    public ScreenImage getLastScreenImage() {
        return getScreen().lastScreenImage;
    }

    /**
     * stores the lastScreenImage in the current bundle path with a created unique name
     *
     * @return the absolute file name
     */
    public String getLastScreenImageFile() throws IOException {
        return getLastScreenImageFile(Settings.BundlePath, null);
    }

    /**
     * stores the lastScreenImage in the current bundle path with the given name
     *
     * @param name file name (.png is added if not there)
     * @return the absolute file name
     */
    public String getLastScreenImageFile(String name) throws IOException {
        return getScreen().lastScreenImage.getFile(Settings.BundlePath, name);
    }

    /**
     * stores the lastScreenImage in the given path with the given name
     *
     * @param name file name (.png is added if not there)
     * @return the absolute file name
     */
    public String getLastScreenImageFile(String path, String name) throws IOException {
        return getScreen().lastScreenImage.getFile(path, name);
    }

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="spatial operators - new regions">
    /**
     * check if current region contains given region
     *
     * @param region
     * @return true/false
     */
    public boolean contains(Region region) {
        return getRect().contains(region.getRect());
    }

    /**
     * create region with same size at top left corner offset
     *
     * @param loc
     * @return the new region
     */
    public Region offset(Location loc) {
        return Region.create(x + loc.x, y + loc.y, getVW(), getVH());
    }

    /**
     * create a region enlarged 50 pixels on each side
     *
     * @return the new region
     * @deprecated to be like AWT Rectangle API use grow() instead
     */
    @Deprecated
    public Region nearby() {
        return grow(PADDING, PADDING);
    }

    /**
     * create a region enlarged range pixels on each side
     *
     * @param range
     * @return the new region
     * @deprecated to be like AWT Rectangle API use grow() instaed
     */
    @Deprecated
    public Region nearby(int range) {
        return grow(range, range);
    }

    /**
     * create a region enlarged range pixels on each side
     *
     * @param range
     * @return the new region
     */
    public Region grow(int range) {
        return grow(range, range);
    }

    /**
     * create a region enlarged w pixels on left and right side<br /> and h pixels
     * at top and bottom
     *
     * @param w
     * @param h
     * @return the new region
     */
    public Region grow(int w, int h) {
        Rectangle r = getRect();
        r.setSize(getVW(), getVH());
        r.grow(w, h);
        return Region.create(r.x, r.y, r.width, r.height, scr);
    }

    /**
     * create a region enlarged l pixels on left and r pixels right side<br /> and
     * t pixels at top side and b pixels at bottom side
     *
     * @param l
     * @param b
     * @param r
     * @param t
     * @return the new region
     */
    public Region grow(int l, int r, int t, int b) {
        Rectangle re = getRect();
        int _x = x - l;
        int _y = y - b;
        int _w = getVW() + l + r;
        int _h = getVH() + t + b;
        return Region.create(_x, _y, _w, _h);
    }

    /**
     *
     * @return point middle on right edge
     */
    public Location rightAt() {
        return rightAt(0);
    }

    /**
     * positive offset goes to the right <br />might be off current screen
     *
     * @return point with given offset horizontally to middle point on right edge
     */
    public Location rightAt(int offset) {
        return new Location(x + w + offset, y + h / 2);
    }

    /**
     * create a region right of the right side with same height<br /> the new
     * region extends to the right screen border<br /> use grow() to include the
     * current region
     *
     * @return the new region
     */
    public Region right() {
        return right(9999999);
    }

    /**
     * create a region right of the right side with same height and given width<br
     * /> use grow() to include the current region
     *
     * @param width
     * @return the new region
     */
    public Region right(int width) {
        int _x = x + w;
        int _y = y;
        int _w = width;
        int _h = h;
        return Region.create(_x, _y, _w, _h);
    }

    /**
     *
     * @return point middle on left edge
     */
    public Location leftAt() {
        return leftAt(0);
    }

    /**
     * negative offset goes to the left <br />might be off current screen
     *
     * @return point with given offset horizontally to middle point on left edge
     */
    public Location leftAt(int offset) {
        return new Location(x + offset, y + h / 2);
    }

    /**
     * create a region left of the left side with same height<br /> the new region
     * extends to the left screen border<br /> use grow() to include the current
     * region
     *
     * @return the new region
     */
    public Region left() {
        return left(9999999);
    }

    /**
     * create a region left of the left side with same height and given width<br
     * /> use grow() to include the current region
     *
     * @param width
     * @return the new region
     */
    public Region left(int width) {
        Rectangle bounds = getScreen().getBounds();
        int _x = x - width < bounds.x ? bounds.x : x - width;
        int _y = y;
        int _w = x - _x;
        int _h = h;
        return Region.create(_x, _y, _w, _h);
    }

    /**
     *
     * @return point middle on top edge
     */
    public Location aboveAt() {
        return aboveAt(0);
    }

    /**
     * negative offset goes towards top of screen <br />might be off current
     * screen
     *
     * @return point with given offset vertically to middle point on top edge
     */
    public Location aboveAt(int offset) {
        return new Location(x + w / 2, y + offset);
    }

    /**
     * create a region above the top side with same width<br /> the new region
     * extends to the top screen border<br /> use grow() to include the current
     * region
     *
     * @return the new region
     */
    public Region above() {
        return above(9999999);
    }

    /**
     * create a region above the top side with same width and given height<br />
     * use grow() to include the current region
     *
     * @param height
     * @return the new region
     */
    public Region above(int height) {
        Rectangle bounds = getScreen().getBounds();
        int _x = x;
        int _y = y - height < bounds.y ? bounds.y : y - height;
        int _w = w;
        int _h = y - _y;
        return Region.create(_x, _y, _w, _h);
    }

    /**
     *
     * @return point middle on bottom edge
     */
    public Location belowAt() {
        return belowAt(0);
    }

    /**
     * positive offset goes towards bottom of screen <br />might be off current
     * screen
     *
     * @return point with given offset vertically to middle point on bottom edge
     */
    public Location belowAt(int offset) {
        return new Location(x + w / 2, y + h - offset);
    }

    /**
     * create a region below the bottom side with same width<br /> the new region
     * extends to the bottom screen border<br /> use grow() to include the current
     * region
     *
     * @return the new region
     */
    public Region below() {
        return below(999999);
    }

    /**
     * create a region below the bottom side with same width and given height<br
     * /> use grow() to include the current region
     *
     * @param height
     * @return the new region
     */
    public Region below(int height) {
        int _x = x;
        int _y = y + h;
        int _w = w;
        int _h = height;
        return Region.create(_x, _y, _w, _h);
    }

    /**
     * create a new region containing both regions<br /> the region is cropped to
     * fit into the current screen<br /> like AWT Rectangle API
     *
     * @param ur region to unite with
     * @return the new region
     */
    public Region union(Region ur) {
        Rectangle r = getRect();
        r.setSize(getVW(), getVH());
        r = r.union(ur.getRect());
        return Region.create(r.x, r.y, r.width, r.height);
    }

    /**
     * create a region that is the intersection of the given regions
     *
     * @param ir the region to intersect with like AWT Rectangle API
     * @return the new region
     */
    public Region intersection(Region ir) {
        Rectangle r = getRect();
        r.setSize(getVW(), getVH());
        r = r.intersection(ir.getRect());
        return Region.create(r.x, r.y, r.width, r.height);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="highlight">
    protected void updateSelf() {
        if (overlay != null) {
            highlight(false);
            highlight(true);
        }
    }

    /**
     * Toggle the regions Highlight visibility (currently red frame)
     *
     * @return the region itself
     */
    public Region highlight() {
        if (!(getScreen()instanceof Screen)) {
            Debug.error("highlight only works on the physical desktop screens.");
            return this;
        }
        if (overlay == null) {
            highlight(true);
        } else {
            highlight(false);
        }
        return this;
    }

    private void highlight(boolean toEnable) {
        Debug.history("toggle highlight " + toString() + ": " + toEnable);
        if (toEnable) {
            overlay = new ScreenHighlighter(getScreen());
            overlay.highlight(this);
        } else {
            if (overlay != null) {
                overlay.close();
                overlay = null;
            }
        }
    }

    /**
     * show the regions Highlight for the given time in seconds (currently red
     * frame) if 0 - use the global Settings.SlowMotionDelay
     *
     * @param secs time in seconds
     * @return the region itself
     */
    public Region highlight(float secs) {
        if (secs < 0.1) {
            return highlight((int) secs);
        }
        Debug.history("highlight " + toString() + " for " + secs + " secs");
        if (!(getScreen()instanceof Screen)) {
            Debug.error("highlight only work on the physical desktop screens.");
            return this;
        }
        ScreenHighlighter _overlay = new ScreenHighlighter(getScreen());
        _overlay.highlight(this, secs);
        return this;
    }

    /**
     * hack to implement the getLastMatch() convenience
     * 0 means same as highlight()
     * &lt.0 same as highlight(secs)
     * if available the last match is highlighted
     *
     * @param secs
     * @return
     */
    public Region highlight(int secs) {
        if (secs > 0) {
            return highlight((float) secs);
        } else {
            if (lastMatch != null) {
                if (secs < 0) {
                    return lastMatch.highlight((float) -secs);
                } else {
                    return lastMatch.highlight(Settings.DefaultHighlightTime);
                }
            } else {
                return this;
            }
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="find public methods">
    //WARNING: wait(long timeout) is taken by Java Object as final
    public void wait(double timeout) {
        try {
            Thread.sleep((long) (timeout * 1000L));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private <PatternOrString> String getImageFilename(PatternOrString target) {
        String imageFileName = null;
        if (target instanceof Pattern) {
            imageFileName = ((Pattern) target).getFilename();
        } else if (target instanceof String) {
            imageFileName = (String) target;
        }
        try {
            return ImageLocator.locate(imageFileName);
        } catch (IOException ex) {
            return "*** not known ***";
        }
    }

    /**
     * return false to skip return true to try again throw FindFailed to abort
     */
    private <PatternOrString> boolean handleFindFailed(PatternOrString target) throws FindFailed {
        FindFailedResponse response;
        if (findFailedResponse == FindFailedResponse.PROMPT) {
            FindFailedDialog fd = new FindFailedDialog(target);
            fd.setVisible(true);
            response = fd.getResponse();
            fd.dispose();
            wait(0.5);
        } else {
            response = findFailedResponse;
        }
        if (response == FindFailedResponse.SKIP) {
            return false;
        } else if (response == FindFailedResponse.RETRY) {
            return true;
        } else if (response == FindFailedResponse.ABORT) {
            throw new FindFailed("can not find " + target + " on the screen.");
        }
        return false;
    }

    /**
     * Match find( Pattern/String ) finds the given pattern on the
     * screen and returns the best match. If AutoWaitTimeout is set, this is
     * equivalent to wait().
     *
     * @param target A search criteria
     * @return If found, the element. null otherwise
     * @throws FindFailed if the Find operation failed
     */
    public <PatternOrString> Match find(PatternOrString target) throws FindFailed {
        if (autoWaitTimeout > 0) {
            return wait(target, autoWaitTimeout);
        }
        while (true) {
            try {
                lastMatch = doFind(target, null);
            } catch (Exception e) {
                throw new FindFailed(e.getMessage());
            }
            if (lastMatch != null) {
                lastMatch.setImage(getImageFilename(target));
                return lastMatch;
            }
            if (!handleFindFailed(target)) {
                return null;
            }
        }
    }

    /**
     * Iterator<Match> findAll( Pattern/String/PatternClass ) finds the given
     * pattern on the screen and returns the best match. If AutoWaitTimeout is
     * set, this is equivalent to wait().
     *
     * @param target A search criteria
     * @return All elements matching
     * @throws FindFailed if the Find operation failed
     */
    public <PatternOrString> Iterator<Match> findAll(PatternOrString target) throws FindFailed {
        while (true) {
            try {
                if (autoWaitTimeout > 0) {
                    RepeatableFindAll rf = new RepeatableFindAll(target);
                    rf.repeat(autoWaitTimeout);
                    lastMatches = rf.getMatches();
                } else {
                    lastMatches = doFindAll(target, null);
                }
            } catch (Exception e) {
                throw new FindFailed(e.getMessage());
            }
            if (lastMatches != null) {
                return lastMatches;
            }
            if (!handleFindFailed(target)) {
                return null;
            }
        }
    }

    public <PatternOrString> Match wait(PatternOrString target) throws FindFailed {
        return wait(target, autoWaitTimeout);
    }

    /**
     * Match wait(Pattern/String/PatternClass target, timeout-sec) waits until
     * target appears or timeout (in second) is passed
     *
     * @param target A search criteria
     * @param timeout Timeout in seconds
     * @return All elements matching
     * @throws FindFailed if the Find operation failed
     */
    public <PatternOrString> Match wait(PatternOrString target, double timeout) throws FindFailed {
        RepeatableFind rf;
        while (true) {
            try {
                Debug.log(2, "waiting for " + target + " to appear");
                rf = new RepeatableFind(target);
                rf.repeat(timeout);
                lastMatch = rf.getMatch();
                if (lastMatch != null) {
                    lastMatch.setImage(rf._imagefilename);
                }
            } catch (Exception e) {
                throw new FindFailed(e.getMessage());
            }

            if (lastMatch != null) {
                lastMatch.setImage(rf._imagefilename);
                Debug.log(2, "" + target + " has appeared.");
                break;
            }

            Debug.log(2, "" + target + " has not appeared.");

            if (!handleFindFailed(target)) {
                return null;
            }
        }

        return lastMatch;
    }

    /**
     * Check if target exists (with the default autoWaitTimeout)
     *
     * @param target Pattern or String
     * @return the match (null if not found or image file missing)
     */
    public <PatternOrString> Match exists(PatternOrString target)  {
        return exists(target, autoWaitTimeout);
    }

    /**
     * Check if target exists with a specified timeout
     *
     * @param target Pattern or String
     * @param timeout Timeout in seconds
     * @return the match (null if not found or image file missing)
     */
    public <PatternOrString> Match exists(PatternOrString target, double timeout) {
        try {
            RepeatableFind rf = new RepeatableFind(target);
            if (rf.repeat(timeout)) {
                lastMatch = rf.getMatch();
                lastMatch.setImage(getImageFilename(target));
                return lastMatch;
            }
        } catch (Exception ex) {
            Debug.error("Region.exists: seems that imagefile could not be found on disk", target);
        }
        return null;
    }

    //TODO implement findText + check text target already here (find(String))
    /**
     *
     * @param text
     * @param timeout
     * @return
     */
    public Match findText(String text, double timeout) throws FindFailed {
        throw new FindFailed("Region.findText: not yet implemented");
    }

    /**
     *
     * @param text
     * @return
     */
    public Match findText(String text) throws FindFailed {
        return findText(text, autoWaitTimeout);
    }

    /**
     *
     * @param text
     * @param timeout
     * @return
     */
    public Match findAllText(String text, double timeout) throws FindFailed {
        throw new FindFailed("Region.findText: not yet implemented");
    }

    /**
     *
     * @param text
     * @return
     */
    public Match findAllText(String text) throws FindFailed {
        return findText(text, autoWaitTimeout);
    }

    /**
     * boolean waitVanish(Pattern/String/PatternClass target, timeout-sec) waits
     * until target vanishes or timeout (in second) is passed
     *
     * @return true if the target vanishes, otherwise returns false.
     */
    public <PatternOrString> boolean waitVanish(PatternOrString target) {
        return waitVanish(target, autoWaitTimeout);
    }

    /**
     * boolean waitVanish(Pattern/String/PatternClass target, timeout-sec) waits
     * until target vanishes or timeout (in second) is passed
     *
     * @return true if target vanishes, false otherwise and if imagefile is missing.
     */
    public <PatternOrString> boolean waitVanish(PatternOrString target, double timeout) {
        try {
            Debug.log(2, "waiting for " + target + " to vanish");
            RepeatableVanish r = new RepeatableVanish(target);
            if (r.repeat(timeout)) {
                // target has vanished before timeout
                Debug.log(2, "" + target + " has vanished");
                return true;
            } else {
                // target has not vanished before timeout
                Debug.log(2, "" + target + " has not vanished before timeout");
                return false;
            }

        } catch (Exception e) {
            Debug.error("Region.waitVanish: seems that imagefile could not be found on disk", target);
        }
        return false;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="find internal methods">
    /**
     * Match findNow( Pattern/String/PatternClass ) finds the given pattern on the
     * screen and returns the best match without waiting.
     */
    private <PatternOrString> Match doFind(PatternOrString ptn, RepeatableFind repeating) throws IOException {
        Finder f;
        ScreenImage simg = getScreen().capture(x, y, w, h);
        if (repeating != null && repeating._finder != null) {
            f = repeating._finder;
            f.setScreenImage(simg);
            f.setRepeating();
            f.findRepeat();
        } else {
            f = new Finder(simg, this);
            String text;
            if (ptn instanceof String) {
                text = f.find((String) ptn);
                if (null == text) {
                    throw new IOException("ImageFile " + ptn + " not found on disk");
                } else if ((((String) ptn) + "???").equals(text)) {
                    throw new IOException("Text search currently switched off");
                }
            } else {
                text = ((Pattern) ptn).getFilename();
                if (null == f.find((Pattern) ptn)) {
                    throw new IOException("ImageFile " + text
                            + " not found on disk");
                }
            }
            if (repeating != null) {
                repeating._finder = f;
                repeating._imagefilename = text;
            }
        }
        if (f.hasNext()) {
            return f.next();
        }
        return null;
    }

    /**
     * Match findAllNow( Pattern/String/PatternClass ) finds the given pattern on
     * the screen and returns the best match without waiting.
     */
    private <PatternOrString> Iterator<Match> doFindAll(PatternOrString ptn, RepeatableFindAll repeating) throws IOException {
        Finder f;
        ScreenImage simg = getScreen().capture(x, y, w, h);
        if (repeating != null && repeating._finder != null) {
            f = repeating._finder;
            f.setScreenImage(simg);
            f.setRepeating();
            f.findAllRepeat();
        } else {
            f = new Finder(simg, this);
            if (ptn instanceof String) {
                if (null == f.findAll((String) ptn)) {
                    throw new IOException();
                }
            } else {
                if (null == f.findAll((Pattern) ptn)) {
                    throw new IOException("ImageFile " + ((Pattern) ptn).getFilename()
                            + " not found on disk");
                }
            }
        }
        if (f.hasNext()) {
            return f;
        }
        return null;
    }

    // Repeatable Find ////////////////////////////////
    private abstract class Repeatable {

        abstract void run() throws Exception;

        abstract boolean ifSuccessful();

        // return TRUE if successful before timeout
        // return FALSE if otherwise
        // throws Exception if any unexpected error occurs
        boolean repeat(double timeout) throws Exception {

            int MaxTimePerScan = (int) (1000.0 / Settings.WaitScanRate);
            int MaxTimePerScanSecs = MaxTimePerScan/1000;
            long begin_t = (new Date()).getTime();
            do {
                long before_find = (new Date()).getTime();

                run();
                if (ifSuccessful()) {
                    return true;
                } else if (timeout < MaxTimePerScanSecs) {
                    // instant return on first search failed if timeout very small or 0
                    return false;
                }

                long after_find = (new Date()).getTime();
                if (after_find - before_find < MaxTimePerScan) {
                    getScreen().getActionRobot().delay((int) (MaxTimePerScan - (after_find - before_find)));
                } else {
                    getScreen().getActionRobot().delay(10);
                }
            } while (begin_t + timeout * 1000 > (new Date()).getTime());

            return false;
        }
    }

    private class RepeatableFind extends Repeatable {

        Object _target;
        Match _match = null;
        Finder _finder = null;
        String _imagefilename = null;

        public <PatternOrString> RepeatableFind(PatternOrString target) {
            _target = target;
        }

        public Match getMatch() {
            if (_finder != null) {
                _finder.destroy();
            }
            return (_match == null ) ? _match : new Match(_match);
        }

        @Override
        public void run() throws IOException {
            _match = doFind(_target, this);
        }

        @Override
        boolean ifSuccessful() {
            return _match != null;
        }
    }

    private class RepeatableVanish extends RepeatableFind {

        public <PatternOrString> RepeatableVanish(PatternOrString target) {
            super(target);
        }

        @Override
        boolean ifSuccessful() {
            return _match == null;
        }
    }

    private class RepeatableFindAll extends Repeatable {

        Object _target;
        Iterator<Match> _matches = null;
        Finder _finder = null;

        public <PatternOrString> RepeatableFindAll(PatternOrString target) {
            _target = target;
        }

        public Iterator<Match> getMatches() {
            return _matches;
        }

        @Override
        public void run() throws IOException {
            _matches = doFindAll(_target, this);
        }

        @Override
        boolean ifSuccessful() {
            return _matches != null;
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Find internal -- obsolete??">
    /**
     *
     * @param ptn
     * @return the match if successful
     * @throws FindFailed
     * @deprecated should not be used anymore - use find() instead
     */
    @Deprecated
    public <PatternOrString> Match findNow(PatternOrString ptn) throws FindFailed {
        ScreenImage simg = getScreen().capture(x, y, w, h);
        Finder f = new Finder(simg, this);
        Match ret = null;
        if (ptn instanceof String) {
            if (null == f.find((String) ptn)) {
                throw new FindFailed("ImageFile not found");
            }
        } else {
            if (null == f.find((Pattern) ptn)) {
                throw new FindFailed("ImageFile " + ((Pattern) ptn).getFilename()
                        + " not found on disk");
            }
        }
        if (f.hasNext()) {
            ret = f.next();
        }
        f.destroy();
        return ret;
    }

    /**
     *
     * @param ptn
     * @return the itreator of matches if successful
     * @throws FindFailed
     * @deprecated should not be used anymore - use findAll() instead
     */
    @Deprecated
    public <PatternOrString> Iterator<Match> findAllNow(PatternOrString ptn)
            throws FindFailed {
        ScreenImage simg = getScreen().capture(x, y, w, h);
        Finder f = new Finder(simg, this);
        if (ptn instanceof String) {
            if (null == f.findAll((String) ptn)) {
                throw new FindFailed("ImageFile not found");
            }
        } else {
            if (null == f.findAll((Pattern) ptn)) {
                throw new FindFailed("ImageFile " + ((Pattern) ptn).getFilename()
                        + " not found on disk");
            }
        }
        if (f.hasNext()) {
            return f;
        }
        f.destroy();
        return null;
    }

    /**
     *
     * @param target
     * @param timeout
     * @return the itreator of matches if successful
     * @throws FindFailed
     * @deprecated does not really make sense - use findAll() instead
     */
    @Deprecated
    public <PatternOrString> Iterator<Match> waitAll(PatternOrString target, double timeout)
            throws FindFailed {

        while (true) {
            try {

                RepeatableFindAll rf = new RepeatableFindAll(target);
                rf.repeat(timeout);
                lastMatches = rf.getMatches();

            } catch (Exception e) {
                throw new FindFailed(e.getMessage());
            }

            if (lastMatches != null) {
                break;
            }

            if (!handleFindFailed(target)) {
                return null;
            }
        }

        return lastMatches;
    }

    private <PatternStringRegionMatch> Region
    getRegionFromTarget(PatternStringRegionMatch target)
            throws FindFailed {
        if (target instanceof Pattern || target instanceof String) {
            Match m = find(target);
            if (m != null) {
                return m;
            }
            return null;
        }
        if (target instanceof Region) {
            return (Region) target;
        }
        return null;
    }

    private <PatternStringRegionMatchLocation> Location
    getLocationFromTarget(PatternStringRegionMatchLocation target)
            throws FindFailed {
        if (target instanceof Pattern || target instanceof String) {
            Match m = find(target);
            if (m != null) {
                return m.getTarget();
            }
            return null;
        }
        if (target instanceof Match) {
            return ((Match) target).getTarget();
        }
        if (target instanceof Region) {
            return ((Region) target).getCenter();
        }
        if (target instanceof Location) {
            return (Location) target;
        }
        return null;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Observer">
    private SikuliEventManager getEventManager() {
        if (evtMgr == null) {
            evtMgr = new SikuliEventManager(this);
        }
        return evtMgr;
    }

    public <PatternOrString> void onAppear(PatternOrString target, Object observer) {
        getEventManager().addAppearObserver(target, (SikuliEventObserver) observer);
    }

    public <PatternOrString> void onVanish(PatternOrString target, Object observer) {
        getEventManager().addVanishObserver(target, (SikuliEventObserver) observer);
    }

    public void onChange(int threshold, Object observer) {
        getEventManager().addChangeObserver(threshold, (SikuliEventObserver) observer);
    }

    public void onChange(SikuliEventObserver observer) {
        getEventManager().addChangeObserver(Settings.ObserveMinChangedPixels, observer);
    }

    public void observe() {
        observe(Float.POSITIVE_INFINITY);
    }

    public void observeInBackground(final double secs) {
        Thread th = new Thread() {
            @Override
            public void run() {
                observe(secs);
            }
        };
        th.start();
    }

    public void stopObserver() {
        observing = false;
    }

    public void observe(double secs) {
        if (evtMgr == null) {
            Debug.error("observe(): Nothing to observe (Region might be invalid)");
            return;
        }
        int MaxTimePerScan = (int) (1000.0 / Settings.ObserveScanRate);
        long begin_t = (new Date()).getTime();
        observing = true;
        evtMgr.initialize();
        while (observing && begin_t + secs * 1000 > (new Date()).getTime()) {
            long before_find = (new Date()).getTime();
            ScreenImage simg = getScreen().capture(x, y, w, h);
            if (! evtMgr.update(simg)) {
                break;
            }
            long after_find = (new Date()).getTime();
            try {
                if (after_find - before_find < MaxTimePerScan) {
                    Thread.sleep((int) (MaxTimePerScan - (after_find - before_find)));
                }
            } catch (Exception e) {
            }
        }
        stopObserver(); 
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Mouse actions - clicking">
    // returns target offset of lastmatch if exists
    //Region.center / Match.targetOffset otherwise
    private Location checkMatch() {
        if (lastMatch != null) {
            return lastMatch.getTarget();
        }
        return getTarget();
    }

    /**
     * move the mouse pointer to region's last successful match <br />use center
     * if no lastMatch <br />if region is a match: move to targetOffset <br />same
     * as mouseMove
     *
     * @return 1 if possible, 0 otherwise
     */
    public int hover() {
        try { // needed to cut throw chain for FindFailed
            return hover(checkMatch());
        } catch (FindFailed ex) {
        }
        return 0;
    }

    /**
     * move the mouse pointer to the given target location<br /> same as
     * mouseMove<br /> Pattern or Filename - do a find before and use the match<br
     * /> Region - position at center<br /> Match - position at match's
     * targetOffset<br /> Location - position at that point<br />
     *
     * @param <PatternFilenameRegionMatchLocation> target
     * @return 1 if possible, 0 otherwise
     * @throws FindFailed for Pattern or Filename
     */
    public <PatternFilenameRegionMatchLocation> int hover(PatternFilenameRegionMatchLocation target)
            throws FindFailed {
        return mouseMove(target);
    }

    /**
     * left click at the region's last successful match <br />use center if no
     * lastMatch <br />if region is a match: click targetOffset
     *
     * @return 1 if possible, 0 otherwise
     */
    public int click() {
        try { // needed to cut throw chain for FindFailed
            return click(checkMatch(), 0);
        } catch (FindFailed ex) {
        }
        return 0;
    }

    /**
     * left click at the given target location<br /> Pattern or Filename - do a
     * find before and use the match<br /> Region - position at center<br /> Match
     * - position at match's targetOffset<br /> Location - position at that
     * point<br />
     *
     * @param <PatternFilenameRegionMatchLocation> target
     * @return 1 if possible, 0 otherwise
     * @throws FindFailed for Pattern or Filename
     */
    public <PatternFilenameRegionMatchLocation> int click(PatternFilenameRegionMatchLocation target)
            throws FindFailed {
        return click(target, 0);
    }

    /**
     * left click at the given target location<br /> holding down the given
     * modifier keys<br /> Pattern or Filename - do a find before and use the
     * match<br /> Region - position at center<br /> Match - position at match's
     * targetOffset<br /> Location - position at that point<br />
     *
     * @param <PatternFilenameRegionMatchLocation> target
     * @param modifiers the value of the resulting bitmask (see KeyModifier)
     * @return 1 if possible, 0 otherwise
     * @throws FindFailed for Pattern or Filename
     */
    public <PatternFilenameRegionMatchLocation> int click(PatternFilenameRegionMatchLocation target, int modifiers)
            throws FindFailed {
        Location loc = getLocationFromTarget(target);
        int ret = _click(loc, InputEvent.BUTTON1_MASK, modifiers, false);

        //TODO      SikuliActionManager.getInstance().clickTarget(this, target, _lastScreenImage, _lastMatch);
        return ret;
    }

    /**
     * double click at the region's last successful match <br />use center if no
     * lastMatch <br />if region is a match: click targetOffset
     *
     * @return 1 if possible, 0 otherwise
     */
    public int doubleClick() {
        try { // needed to cut throw chain for FindFailed
            return doubleClick(checkMatch(), 0);
        } catch (FindFailed ex) {
        }
        return 0;
    }

    /**
     * double click at the given target location<br /> Pattern or Filename - do a
     * find before and use the match<br /> Region - position at center<br /> Match
     * - position at match's targetOffset<br /> Location - position at that
     * point<br />
     *
     * @param <PatternFilenameRegionMatchLocation> target
     * @return 1 if possible, 0 otherwise
     * @throws FindFailed for Pattern or Filename
     */
    public <PatternFilenameRegionMatchLocation> int doubleClick(PatternFilenameRegionMatchLocation target)
            throws FindFailed {
        return doubleClick(target, 0);
    }

    /**
     * double click at the given target location<br /> holding down the given
     * modifier keys<br /> Pattern or Filename - do a find before and use the
     * match<br /> Region - position at center<br /> Match - position at match's
     * targetOffset<br /> Location - position at that point<br />
     *
     * @param <PatternFilenameRegionMatchLocation> target
     * @param modifiers the value of the resulting bitmask (see KeyModifier)
     * @return 1 if possible, 0 otherwise
     * @throws FindFailed for Pattern or Filename
     */
    public <PatternFilenameRegionMatchLocation> int doubleClick(PatternFilenameRegionMatchLocation target, int modifiers)
            throws FindFailed {
        Location loc = getLocationFromTarget(target);
        int ret = _click(loc, InputEvent.BUTTON1_MASK, modifiers, true);

        //TODO      SikuliActionManager.getInstance().doubleClickTarget(this, target, _lastScreenImage, _lastMatch);
        return ret;
    }

    /**
     * right click at the region's last successful match <br />use center if no
     * lastMatch <br />if region is a match: click targetOffset
     *
     * @return 1 if possible, 0 otherwise
     */
    public int rightClick() {
        try { // needed to cut throw chain for FindFailed
            return rightClick(checkMatch(), 0);
        } catch (FindFailed ex) {
        }
        return 0;
    }

    /**
     * right click at the given target location<br /> Pattern or Filename - do a
     * find before and use the match<br /> Region - position at center<br /> Match
     * - position at match's targetOffset<br /> Location - position at that
     * point<br />
     *
     * @param <PatternFilenameRegionMatchLocation> target
     * @return 1 if possible, 0 otherwise
     * @throws FindFailed for Pattern or Filename
     */
    public <PatternFilenameRegionMatchLocation> int rightClick(PatternFilenameRegionMatchLocation target)
            throws FindFailed {
        return rightClick(target, 0);
    }

    /**
     * right click at the given target location<br /> holding down the given
     * modifier keys<br /> Pattern or Filename - do a find before and use the
     * match<br /> Region - position at center<br /> Match - position at match's
     * targetOffset<br /> Location - position at that point<br />
     *
     * @param <PatternFilenameRegionMatchLocation> target
     * @param modifiers the value of the resulting bitmask (see KeyModifier)
     * @return 1 if possible, 0 otherwise
     * @throws FindFailed for Pattern or Filename
     */
    public <PatternFilenameRegionMatchLocation> int rightClick(PatternFilenameRegionMatchLocation target, int modifiers)
            throws FindFailed {
        Location loc = getLocationFromTarget(target);
        int ret = _click(loc, InputEvent.BUTTON3_MASK, modifiers, false);

        //TODO      SikuliActionManager.getInstance().rightClickTarget(this, target, _lastScreenImage, _lastMatch);
        return ret;
    }

    private int _click(Location loc, int buttons, int modifiers,
            boolean dblClick) {
        if (loc == null) {
            return 0;
        }
        Debug.history(getClickMsg(loc, buttons, modifiers, dblClick));
        getScreen().showTarget(loc);
        RobotDesktop r = getScreen().getActionRobot();
        r.pressModifiers(modifiers);
        r.smoothMove(loc);
        r.mouseDown(buttons);
        r.mouseUp(buttons);
        if (dblClick) {
            r.mouseDown(buttons);
            r.mouseUp(buttons);
        }
        r.releaseModifiers(modifiers);
        r.waitForIdle();
        return 1;
    }

    private String getClickMsg(Location loc, int buttons, int modifiers,
            boolean dblClick) {
        String msg = "";
        if (modifiers != 0) {
            msg += KeyEvent.getKeyModifiersText(modifiers) + "+";
        }
        if (buttons == InputEvent.BUTTON1_MASK && !dblClick) {
            msg += "CLICK";
        }
        if (buttons == InputEvent.BUTTON1_MASK && dblClick) {
            msg += "DOUBLE CLICK";
        }
        if (buttons == InputEvent.BUTTON3_MASK) {
            msg += "RIGHT CLICK";
        } else if (buttons == InputEvent.BUTTON2_MASK) {
            msg += "MID CLICK";
        }
        msg += " on " + loc;
        return msg;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Mouse actions - drag & drop">
    /**
     * Drag from region's last match and drop at given target <br />applying
     * Settings.DelayAfterDrag and DelayBeforeDrop <br /> using left mouse button
     *
     * @param <PatternFilenameRegionMatchLocation> target destination position
     * @return 1 if possible, 0 otherwise
     * @throws FindFailed if the Find operation failed
     */
    public <PatternFilenameRegionMatchLocation> int dragDrop(PatternFilenameRegionMatchLocation target)
            throws FindFailed {
        return dragDrop(lastMatch, target);
    }

    /**
     * Drag from a position and drop to another using left mouse button<br
     * />applying Settings.DelayAfterDrag and DelayBeforeDrop
     *
     * @param <PatternFilenameRegionMatchLocation> t1 source position
     * @param <PatternFilenameRegionMatchLocation> t2 destination position
     * @return 1 if possible, 0 otherwise
     * @throws FindFailed if the Find operation failed
     */
    public <PatternFilenameRegionMatchLocation> int dragDrop(PatternFilenameRegionMatchLocation t1,
            PatternFilenameRegionMatchLocation t2)
                    throws FindFailed {
        Location loc1 = getLocationFromTarget(t1);
        Location loc2 = getLocationFromTarget(t2);
        if (loc1 != null && loc2 != null) {
            getScreen().showTarget(loc1);
            RobotDesktop r = getScreen().getActionRobot();
            r.smoothMove(loc1);
            r.mouseDown(InputEvent.BUTTON1_MASK);
            r.delay((int) (Settings.DelayAfterDrag * 1000));
            getScreen().showTarget(loc2);
            r.smoothMove(loc2);
            r.delay((int) (Settings.DelayBeforeDrop * 1000));
            r.mouseUp(InputEvent.BUTTON1_MASK);
            return 1;
        }
        return 0;
    }

    /**
     * Prepare a drag action: move mouse to given target <br />press and hold left
     * mouse button <br />wait Settings.DelayAfterDrag
     *
     * @param <PatternFilenameRegionMatchLocation> target
     * @return 1 if possible, 0 otherwise
     * @throws FindFailed
     */
    public <PatternFilenameRegionMatchLocation> int drag(PatternFilenameRegionMatchLocation target)
            throws FindFailed {
        Location loc = getLocationFromTarget(target);
        if (loc != null) {
            RobotDesktop r = getScreen().getActionRobot();
            getScreen().showTarget(loc);
            r.smoothMove(loc);
            r.mouseDown(InputEvent.BUTTON1_MASK);
            r.delay((int) (Settings.DelayAfterDrag * 1000));
            r.waitForIdle();
            return 1;
        }
        return 0;
    }

    /**
     * finalize a drag action with a drop: move mouse to given target <br />wait
     * Settings.DelayBeforeDrop <br />release the left mouse button
     *
     * @param <PatternFilenameRegionMatchLocation> target
     * @return 1 if possible, 0 otherwise
     * @throws FindFailed
     */
    public <PatternFilenameRegionMatchLocation> int dropAt(PatternFilenameRegionMatchLocation target)
            throws FindFailed {
        Location loc = getLocationFromTarget(target);
        if (loc != null) {
            getScreen().showTarget(loc);
            RobotDesktop r = getScreen().getActionRobot();
            r.smoothMove(loc);
            r.delay((int) (Settings.DelayBeforeDrop * 1000));
            r.mouseUp(InputEvent.BUTTON1_MASK);
            r.waitForIdle();
            return 1;
        }
        return 0;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Mouse actions - low level + Wheel">
    /**
     * press and hold the specified buttons - use + to combine Button.LEFT left
     * mouse button Button.MIDDLE middle mouse button Button.RIGHT right mouse
     * button
     *
     * @param buttons
     */
    public void mouseDown(int buttons) {
        getScreen().getActionRobot().mouseDown(buttons);
    }

    /**
     * release all currently held buttons
     */
    public void mouseUp() {
        mouseUp(0);
    }

    /**
     * release the specified mouse buttons (see mouseDown) if buttons==0, all
     * currently held buttons are released
     *
     * @param buttons
     */
    public void mouseUp(int buttons) {
        getScreen().getActionRobot().mouseUp(buttons);
    }

    /**
     * move the mouse pointer to the region's last successful match<br />same as
     * hover<br />
     *
     * @return 1 if possible, 0 otherwise
     */
    public int mouseMove() {
        if (lastMatch != null) {
            try {
                return mouseMove(lastMatch);
            } catch (FindFailed ex) {
                return 0;
            }
        } else {
            return 0;
        }
    }

    /**
     * move the mouse pointer to the given target location<br /> same as hover<br
     * /> Pattern or Filename - do a find before and use the match<br /> Region -
     * position at center<br /> Match - position at match's targetOffset<br />
     * Location - position at that point<br />
     *
     * @param <PatternFilenameRegionMatchLocation> target
     * @return 1 if possible, 0 otherwise
     * @throws FindFailed for Pattern or Filename
     */
    public <PatternFilenameRegionMatchLocation> int mouseMove(PatternFilenameRegionMatchLocation target)
            throws FindFailed {
        Location loc = getLocationFromTarget(target);
        if (loc != null) {
            getScreen().showTarget(loc);
            RobotDesktop r = getScreen().getActionRobot();
            r.smoothMove(loc);
            r.waitForIdle();
            return 1;
        }
        return 0;
    }

    /**
     * Move the wheel at the current mouse position<br /> the given steps in the
     * given direction: <br />Button.WHEEL_DOWN, Button.WHEEL_UP
     *
     * @param direction to move the wheel
     * @param steps the number of steps
     * @return 1 in any case
     */
    public int wheel(int direction, int steps) {
        for (int i = 0; i < steps; i++) {
            RobotDesktop r = getScreen().getActionRobot();
            r.mouseWheel(direction);
            r.delay(50);
        }
        return 1;
    }

    /**
     * move the mouse pointer to the given target location<br /> and move the
     * wheel the given steps in the given direction: <br />Button.WHEEL_DOWN,
     * Button.WHEEL_UP
     *
     * @param <PatternFilenameRegionMatchLocation> target
     * @param direction to move the wheel
     * @param steps the number of steps
     * @return 1 if possible, 0 otherwise
     * @throws FindFailed if the Find operation failed
     */
    public <PatternFilenameRegionMatchLocation> int wheel(PatternFilenameRegionMatchLocation target, int direction, int steps)
            throws FindFailed {
        if (target == null || mouseMove(target) != 0) {
            return wheel(direction, steps);
        }
        return 0;
    }

    /**
     *
     * @return the current mouse pointer Location
     */
    public static Location atMouse() {
        Point loc = MouseInfo.getPointerInfo().getLocation();
        return new Location(loc.x, loc.y);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Keyboard actions + paste">
    /**
     * press and hold the given key use a constant from java.awt.event.KeyEvent
     * which might be special in the current machine/system environment
     *
     * @param keycode
     */
    public void keyDown(int keycode) {
        getScreen().getActionRobot().keyDown(keycode);
    }

    /**
     * press and hold the given keys including modifier keys <br />use the key
     * constants defined in class Key, <br />which only provides a subset of a
     * US-QWERTY PC keyboard layout <br />might be mixed with simple characters
     * <br />use + to concatenate Key constants
     *
     * @param keys
     */
    public void keyDown(String keys) {
        getScreen().getActionRobot().keyDown(keys);
    }

    /**
     * release all currently pressed keys
     */
    public void keyUp() {
        getScreen().getActionRobot().keyUp(); 
    }

    /**
     * release the given keys (see keyDown(keycode) )
     *
     * @param keycode
     */
    public void keyUp(int keycode) {
        getScreen().getActionRobot().keyUp(keycode);
    }

    /**
     * release the given keys (see keyDown(keys) )
     *
     * @param keys
     */
    public void keyUp(String keys) {
        getScreen().getActionRobot().keyUp(keys);
    }

    /**
     * enters the given text one character/key after another using keyDown/keyUp
     * <br />about the usable Key constants see keyDown(keys) <br />Class Key only
     * provides a subset of a US-QWERTY PC keyboard layout<br />the text is
     * entered at the current position of the focus/carret
     *
     * @param text containing characters and/or Key constants
     * @return 1 if possible, 0 otherwise
     */
    public int type(String text) {
        try {
            return keyin(null, text, 0);
        } catch (FindFailed ex) {
            return 0;
        }
    }

    /**
     * enters the given text one character/key after another using
     * keyDown/keyUp<br />while holding down the given modifier keys <br />about
     * the usable Key constants see keyDown(keys) <br />Class Key only provides a
     * subset of a US-QWERTY PC keyboard layout<br />the text is entered at the
     * current position of the focus/carret
     *
     * @param text containing characters and/or Key constants
     * @param modifiers constants according to class KeyModifiers
     * @return 1 if possible, 0 otherwise
     */
    public int type(String text, int modifiers) {
        try {
            return keyin(null, text, modifiers);
        } catch (FindFailed findFailed) {
            return 0;
        }
    }

    /**
     * enters the given text one character/key after another using

     * keyDown/keyUp<br />while holding down the given modifier keys <br />about
     * the usable Key constants see keyDown(keys) <br />Class Key only provides a
     * subset of a US-QWERTY PC keyboard layout<br />the text is entered at the
     * current position of the focus/carret
     *

     * @param text containing characters and/or Key constants
     * @param modifiers constants according to class Key - combine using +
     * @return 1 if possible, 0 otherwise
     */
    public int type(String text, String modifiers) {
        String target = null;
        int modifiersNew = Key.convertModifiers(modifiers);
        if (modifiersNew == 0) {
            target = text;
            text = modifiers;
        }
        try {
            return keyin(target, text, modifiersNew);
        } catch (FindFailed findFailed) {
            return 0;
        }
    }

    /**
     * first does a click(target) at the given target position to gain
     * focus/carret <br />enters the given text one character/key after another
     * using keyDown/keyUp <br />about the usable Key constants see keyDown(keys)
     * <br />Class Key only provides a subset of a US-QWERTY PC keyboard layout
     *
     * @param <PatternFilenameRegionMatchLocation> target
     * @param text containing characters and/or Key constants
     * @return 1 if possible, 0 otherwise
     * @throws FindFailed
     */
    public <PatternFilenameRegionMatchLocation> int type(PatternFilenameRegionMatchLocation target, String text)
            throws FindFailed {
        return keyin(target, text, 0);
    }

    /**
     * first does a click(target) at the given target position to gain
     * focus/carret <br />enters the given text one character/key after another
     * using keyDown/keyUp <br />while holding down the given modifier keys<br
     * />about the usable Key constants see keyDown(keys) <br />Class Key only
     * provides a subset of a US-QWERTY PC keyboard layout
     *
     * @param <PatternFilenameRegionMatchLocation> target
     * @param text containing characters and/or Key constants
     * @param modifiers constants according to class KeyModifiers
     * @return 1 if possible, 0 otherwise
     */
    public <PatternFilenameRegionMatchLocation> int type(PatternFilenameRegionMatchLocation target, String text, int modifiers)
            throws FindFailed {
        return keyin(target, text, modifiers);
    }

    /**
     * first does a click(target) at the given target position to gain
     * focus/carret <br />enters the given text one character/key after another
     * using keyDown/keyUp <br />while holding down the given modifier keys<br
     * />about the usable Key constants see keyDown(keys) <br />Class Key only
     * provides a subset of a US-QWERTY PC keyboard layout
     *
     * @param <PatternFilenameRegionMatchLocation> target
     * @param text containing characters and/or Key constants
     * @param modifiers constants according to class Key - combine using +
     * @return 1 if possible, 0 otherwise
     */
    public <PatternFilenameRegionMatchLocation> int type(PatternFilenameRegionMatchLocation target, String text, String modifiers)
            throws FindFailed {
        int modifiersNew = Key.convertModifiers(modifiers);
        return keyin(target, text, modifiersNew);
    }

    private <PatternFilenameRegionMatchLocation> int keyin(PatternFilenameRegionMatchLocation target, String text, int modifiers)
            throws FindFailed {
        if (target != null && 0 == click(target, 0)) {
            return 0;
        }
        if (text != null && !"".equals(text)) {
            String showText = "";
            for (int i = 0; i < text.length(); i++) {
                showText += Key.toJavaKeyCodeText(text.charAt(i));
            }
            Debug.history(
                    (modifiers != 0 ? KeyEvent.getKeyModifiersText(modifiers) + "+" : "")
                    + "TYPE \"" + showText + "\"");
            RobotDesktop r = getScreen().getActionRobot();
            for (int i = 0; i < text.length(); i++) {
                r.pressModifiers(modifiers);
//TODO allow symbolic keys as #NAME. (CUT, COPY, PASTE, (select) ALL, ...)
                r.typeChar(text.charAt(i), RobotIF.KeyMode.PRESS_RELEASE);
                r.releaseModifiers(modifiers);
                r.delay(20);
            }
            r.waitForIdle();
            return 1;
        }
        return 0;
    }

    /**
     * pastes the text at the current position of the focus/carret <br />using the
     * clipboard and strg/ctrl/cmd-v (paste keyboard shortcut)
     *
     * @param text a string, which might contain unicode characters
     * @return 0 if possible, 1 otherwise
     */
    public int paste(String text) {
        try {
            return paste(null, text);
        } catch (FindFailed ex) {
            return 1;
        }
    }

    /**
     * first does a click(target) at the given target position to gain
     * focus/carret <br /> and then pastes the text <br /> using the clipboard and
     * strg/ctrl/cmd-v (paste keyboard shortcut)
     *
     * @param <PatternFilenameRegionMatchLocation> target
     * @param text a string, which might contain unicode characters
     * @return 0 if possible, 1 otherwise
     * @throws FindFailed
     */
    public <PatternFilenameRegionMatchLocation> int paste(PatternFilenameRegionMatchLocation target, String text)
            throws FindFailed {
        click(target, 0);
        if (text != null) {
            App.setClipboard(text);
            int mod = Key.getHotkeyModifier();
            RobotDesktop r = getScreen().getActionRobot();
            r.keyDown(mod);
            r.keyDown(KeyEvent.VK_V);
            r.keyUp(KeyEvent.VK_V);
            r.keyUp(mod);
            return 1;
        }
        return 0;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="OCR - read text from Screen">
    /**
     * STILL EXPERIMENTAL: tries to read the text in this region<br />
     * might contain misread characters, NL characters and other stuff, when
     * interpreting contained grafics as text<br />
     * Best results: one line of text with no grafics in the line
     *
     * @return the text read (utf8 encoded)
     */
    public String text() {
        if (Settings.OcrTextRead) {
            ScreenImage simg = getScreen().capture(x, y, w, h);
            TextRecognizer tr = TextRecognizer.getInstance();
            if (tr == null) {
                Debug.error("Region.text: text recognition is now switched off");
                return "--- no text ---";
            } else {
                String textRead = tr.recognize(simg);
                Debug.log(2, "Region.text: #(" + textRead + ")#");
                return textRead;
            }
        } else {
            Debug.error("Region.text: text recognition is currently switched off");
            return "--- no text ---";
        }
    }

    /**
     * VERY EXPERIMENTAL: returns a list of matches, that represent single words,
     * that have been found in this region<br />
     * the match's x,y,w,h the region of the word<br />
     * Match.getText() returns the word (utf8) at this match<br />
     * Match.getScore() returns a value between 0 ... 1, that represents
     * some OCR-confidence value<br />
     * (the higher, the better the OCR engine thinks the result is)
     *
     * @return a list of matches
     */
    public List<Match> listText() {
        if (Settings.OcrTextRead) {
            ScreenImage simg = getScreen().capture(x, y, w, h);
            TextRecognizer tr = TextRecognizer.getInstance();
            if (tr == null) {
                Debug.error("Region.text: text recognition is now switched off");
                return null;
            } else {
                Debug.log(2, "Region.listText");
                return tr.listText(simg, this);
            }
        } else {
            Debug.error("Region.text: text recognition is currently switched off");
            return null;
        }
    }
    //</editor-fold>
}
