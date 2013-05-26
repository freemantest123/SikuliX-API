/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.script;

import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

/**
 * A screen represents a physical monitor with its coordinates and size
 * according to the global point system: the screen areas are grouped
 * around a point (0,0) like in a cartesian system (the top left corner and the
 * points containd in the screen area might have negative x and/or y values)
 * <br >The screens are aranged in an array (index = id) and
 * each screen is always the same object (not possible to create new objects).
 * <br />A screen inherits from class Region, so it can be used as such
 * in all aspects. If you need the region of the screen more than once,
 * you have to create new ones based on the screen.
 *<br />The so called primary screen here is the one with top left (0,0).
 * It is not guaranteed, that the primary screen has id 0, since the
 * sequence of the screens and hence there id depends
 * on the system dependent monitor configuration.
 *
 * @author RaiMan
 */
public class Screen extends Region implements EventObserver, ScreenIF {

    static GraphicsEnvironment genv = null;
    static GraphicsDevice[] gdevs;
    static RobotDesktop[] robots;
    static Screen[] screens;
    protected static int primaryScreen = -1;
    protected static RobotDesktop actionRobot;
    private int curID = 0;
    private GraphicsDevice curGD;
    private Rectangle curROI;
    protected boolean waitPrompt;
    protected OverlayCapturePrompt prompt;
    protected ScreenImage lastScreenImage;

    //<editor-fold defaultstate="collapsed" desc="Initialization">
    private static void initScreens() {
        initScreens(false);
    }

    private static void initScreens(boolean reset) {
        if (genv != null && ! reset) {
            return;
        }
        genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        gdevs = genv.getScreenDevices();
        robots = new RobotDesktop[gdevs.length];
        screens = new Screen[gdevs.length];
        for (int i = 0; i < gdevs.length; i++) {
            screens[i] = new Screen(i, true);
            screens[i].initScreen();
            try {
                robots[i] = new RobotDesktop(screens[i]);
            } catch (AWTException e) {
                Debug.error("Can't initialize Java Robot " + i);
                robots[i] = null;
                screens[i] = null;
            }
            //robots[i].setAutoWaitForIdle(false); //TODO: make sure we don't need this
            robots[i].setAutoDelay(10);
        }
        primaryScreen = 0;
        for (int i = 0; i < getNumberScreens(); i++) {
            if (getBounds(i).x == 0 && getBounds(i).y == 0) {
                primaryScreen = i;
                break;
            }
        }
        try {
            actionRobot = new RobotDesktop(screens[primaryScreen]);
            actionRobot.setAutoDelay(10);
        } catch (AWTException e) {
            Debug.error("Can't initialize Java Robot " + primaryScreen);
            actionRobot = null;
            screens[primaryScreen] = null;
        }
        Debug.log(2, "Screen static initScreens");
    }

    // hack to get an additional internal constructor for the initialization
    private Screen(int id, boolean init) {
        super();
        curID = id;
        setScreen(curID);
    }

    /**
     * Is the screen object at the given id
     *
     * @param id
     */
    public Screen(int id) {
        super();
        initScreens();
        curID = id;
        if (id < 0 || id >= gdevs.length) {
            curID = getPrimaryId();
        }
        initScreen();
    }

    /**
     * Is the screen object having the top left corner as (0,0).
     * If such a screen does not exist it is the screen with id 0.
     */
    public Screen() {
        super();
        initScreens();
        curID = getPrimaryId();
        initScreen();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initScreen(Screen scr) {
        updateSelf();
    }

    private void initScreen() {
        curGD = gdevs[curID];
        if (screens[curID] == null) {
            x = 0;
            y = 0;
            w = -1;
            h = -1;
            curROI = new Rectangle(x, y, w, h);
            Debug.error("Screen cannot be used. No Robot: " + toStringShort());
        } else {
            Rectangle bounds = getBounds();
            x = (int) bounds.getX();
            y = (int) bounds.getY();
            w = (int) bounds.getWidth();
            h = (int) bounds.getHeight();
            curROI = new Rectangle(x, y, w, h);
        }
        setScreen(curID);
    }

    protected Rectangle getCurROI() {
        return curROI;
    }

    protected void setCurROI(Rectangle roi) {
        curROI = new Rectangle(roi.x, roi.y, roi.width, roi.height);
    }

    @Override
    public Screen getScreen() {
        return this;
    }

    @Override
    protected void setScreen(Screen s) {
        // to block the Region method
    }

    /**
     * show the current monitor setup
     */
    public static void showMonitors() {
        initScreens();
        Debug.info("*** monitor configuration [ %s Screen(s)] ***", Screen.getNumberScreens());
        Debug.info("*** Primary is Screen %d", Screen.getPrimaryId());
        for (int i=0; i < gdevs.length; i++) {
            Debug.info("Screen %d: %s", i, Screen.getScreen(i).toStringShort());
        }
        Debug.info("*** end monitor configuration ***");
    }

    /**
     * re-initialize the monitor setup (e.g. when it was changed while running)
     */
    public static void resetMonitors() {
        Debug.error("*** BE AWARE: experimental - might not work ***");
        Debug.error("Re-evaluation of the monitor setup has been requested");
        Debug.error("... Current Region/Screen objects might not be valid any longer");
        Debug.error("... Use existing Region/Screen objects only if you know what you are doing!");
        Debug.error("... When using from Jython script: initSikuli() might be needed!");
        initScreens(true);
        Debug.info("*** new monitor configuration [ %s Screen(s)] ***", Screen.getNumberScreens());
        Debug.info("*** Primary is Screen %d", Screen.getPrimaryId());
        for (int i=0; i < gdevs.length; i++) {
            Debug.info("Screen %d: %s", i, Screen.getScreen(i).toStringShort());
        }
        Debug.error("*** end new monitor configuration ***");
    }

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="getters setters">
    protected boolean useFullscreen() {
        return false;
    }

    private static int getValidID(int id) {
        initScreens();
        if (id < 0 || id >= gdevs.length) {
            return primaryScreen;
        } else {
            return id;
        }
    }

    /**
     *
     * @return number of available screens
     */
    public static int getNumberScreens() {
        initScreens();
        return gdevs.length;
    }

    /**
     *
     * @return the id of the screen at (0,0), if not exists 0
     */
    public static int getPrimaryId() {
        initScreens();
        return primaryScreen;
    }

    /**
     *
     * @return the screen at (0,0), if not exists the one with id 0
     */
    public static Screen getPrimaryScreen() {
        return screens[getPrimaryId()];
    }

    /**
     *
     * @param id of the screen
     * @return the screen with given id, the primary screen if id is invalid
     */
    public static Screen getScreen(int id) {
        return screens[getValidID(id)];
    }

    /**
     *
     * @param id
     * @return the physical coordinate/size <br />as AWT.Rectangle to avoid mix up with getROI
     */
    public static Rectangle getBounds(int id) {
        return gdevs[getValidID(id)].getDefaultConfiguration().getBounds();
    }

    /**
     * each screen has exactly one robot (internally used for screen capturing)
     * <br />available as a convenience for those who know what they are doing.
     * Should not be needed normally.
     *
     * @param id
     * @return the AWT.Robot of the given screen, if id invalid the primary screen
     */
    public static RobotDesktop getRobot(int id) {
        return robots[getValidID(id)];
    }

    /**
     * the one robot, that runs and coordinates all mouse and keyboard activities
     * <br />available as a convenience for those who know what they are doing.
     * Should not be needed normally.
     * @return an AWT.Robot (always same object)
     */
    public RobotDesktop getActionRobot() {
        return actionRobot;
    }

    /**
     *
     * @return
     */
    public int getID() {
        return curID;
    }

    /**
     *
     * @return
     */
    public GraphicsDevice getGraphicsDevice() {
        return curGD;
    }

    /**
     *
     * @return
     */
    @Override
    public RobotDesktop getRobot() {
        return robots[curID];
    }

    @Override
    public Rectangle getBounds() {
        return curGD.getDefaultConfiguration().getBounds();
    }

    /**
     * creates a region on the current screen with the given coordinate/size.
     * The coordinate is translated to the current screen from its
     * relative position on the screen it would have been created normally.
     *
     * @param loc
     * @param width
     * @param height
     * @return the new region
     */
    public Region newRegion(Location loc, int width, int height) {
        return Region.create(loc.copyTo(this), width, height);
    }

    /**
     * creates a location on the current screen with the given point.
     * The coordinate is translated to the current screen from its
     * relative position on the screen it would have been created normally.
     *
     * @param loc
     * @return the new location
     */
    public Location newLocation(Location loc) {
        return (new Location(loc)).copyTo(this);
    }

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Capture - SelectRegion">
    /**
     * create a ScreenImage with the physical bounds of this screen
     *
     * @return the image
     */
    @Override
    public ScreenImage capture() {
        return capture(getCurROI());
    }

    /**
     * create a ScreenImage with given coordinates on this screen.
     * Will be translated to this screen if (x,y) is outside.
     *
     * @param x
     * @param y
     * @param w
     * @param h
     * @return the image
     */
    @Override
    public ScreenImage capture(int x, int y, int w, int h) {
        Rectangle rect = newRegion(new Location(x,y), w, h).getRect();
        return capture(rect);
    }

    /**
     * create a ScreenImage with given rectangle on this screen
     * Will be translated to this screen if top left is outside.
     *
     * @param rect
     * @return the image
     */
    @Override
    public ScreenImage capture(Rectangle rect) {
        rect = newRegion(new Location(rect.x, rect.y), rect.width, rect.height).getRect();
        Rectangle bounds = getBounds();
        rect.x -= bounds.x;
        rect.y -= bounds.y;
        ScreenImage simg = robots[curID].captureScreen(rect);
        simg.x += bounds.x;
        simg.y += bounds.y;
        lastScreenImage = simg;
        Debug.log(2, "Screen.capture: " + rect);
        return simg;
    }

    /**
     * create a ScreenImage with given region on this screen
     * Will be translated to this screen if top left is outside.
     *
     * @param reg
     * @return the image
     */
    @Override
    public ScreenImage capture(Region reg) {
        return capture(newRegion(reg.getTopLeft(), reg.getW(), reg.getH()).getRect());
    }

    /**
     * interactive capture with predefined message: lets the user capture a
     * screen image using the mouse to draw the rectangle
     *
     * @return the image
     */
    public ScreenImage userCapture() {
        return userCapture("Select a region on the screen");
    }

    /**
     * interactive capture with given message: lets the user capture a
     * screen image using the mouse to draw the rectangle
     *
     * @return the image
     */
    public ScreenImage userCapture(final String msg) {
        waitPrompt = true;
        Thread th = new Thread() {
            @Override
            public void run() {
                prompt = new OverlayCapturePrompt(Screen.this, Screen.this);
                prompt.prompt(msg);
            }
        };
        th.start();
        try {
            int count = 0;
            while (waitPrompt) {
                Thread.sleep(100);
                if (count++ > 1000) {
                    return null;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ScreenImage ret = prompt.getSelection();
        lastScreenImage = ret;
        prompt.close();
        return ret;
    }

    /**
     * interactive region create with predefined message: lets the user
     * draw the rectangle using the mouse
     *
     * @return the region
     */
    public Region selectRegion() {
        return selectRegion("Select a region on the screen");
    }

    /**
     * interactive region create with given message: lets the user
     * draw the rectangle using the mouse
     *
     * @return the region
     */
    public Region selectRegion(final String msg) {
        ScreenImage sim = userCapture(msg);
        if (sim == null) {
            return null;
        }
        Rectangle r = sim.getROI();
        return Region.create((int) r.getX(), (int) r.getY(),
                (int) r.getWidth(), (int) r.getHeight());
    }

    /**
     * Internal use only
     * @param s
     */
    @Override
    public void update(EventSubject s) {
        waitPrompt = false;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Visual effects">
    protected void showTarget(Location loc) {
        showTarget(loc, Settings.SlowMotionDelay);
    }

    protected void showTarget(Location loc, double secs) {
        if (Settings.ShowActions) {
            ScreenHighlighter overlay = new ScreenHighlighter(this);
            overlay.showTarget(loc, (float) secs);
        }
    }
    //</editor-fold>

    @Override
    public String toString() {
        Rectangle r = getBounds();
        if (curROI.equals(r)) {
            return String.format("S(%d)[%d,%d %dx%d] E:%s, T:%.1f",
                    curID, (int) r.getX(), (int) r.getY(),
                    (int) r.getWidth(), (int) r.getHeight(),
                    throwException ? "Y" : "N", autoWaitTimeout);
        } else {
            int rx = (int) r.getX();
            int ry = (int) r.getY();
            return String.format("S(%d)[%d,%d %dx%d] ROI[%d,%d, %dx%d] E:%s, T:%.1f",
                    curID, rx, ry,
                    (int) r.getWidth(), (int) r.getHeight(),
                    curROI.x - rx, curROI.y - ry, curROI.width, curROI.height,
                    throwException ? "Y" : "N", autoWaitTimeout);
        }
    }

    /**
     * only a short version of toString()
     *
     * @return like S(0) [0,0, 1440x900]
     */
    public String toStringShort() {
        Rectangle r = getBounds();
        return String.format("S(%d)[%d,%d %dx%d]",
                curID, (int) r.getX(), (int) r.getY(),
                (int) r.getWidth(), (int) r.getHeight());
    }
}
