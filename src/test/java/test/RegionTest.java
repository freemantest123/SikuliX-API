/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * added Kelthuzad 2013
 */
package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.awt.Rectangle;

import org.junit.Test;
import org.sikuli.script.Location;
import org.sikuli.script.Region;
import org.sikuli.script.Screen;
import org.sikuli.script.Settings;

/**
 * Tests the Comparators.
 */
public class RegionTest {

    /**
     * Test the Region initialization.
     */
    @Test
    public void testRegionConstructors() {
        // Constructor with x,y,w,h
        Region r1 = new Region(10, 20, 100, 200);
        assertEquals(10, r1.getX());
        assertEquals(10, r1.x);
        assertEquals(20, r1.getY());
        assertEquals(20, r1.y);
        assertEquals(100, r1.getW());
        assertEquals(100, r1.w);
        assertEquals(200, r1.getH());
        assertEquals(200, r1.h);
        assertEquals(Settings.AutoWaitTimeout, r1.getAutoWaitTimeout(), 0.0001);
        assertEquals(Screen.getPrimaryScreen(), r1.getScreen());

        // Constructor with another Region

        r1.setAutoWaitTimeout(15.d); // set timeout to other than default

        Region r2 = new Region(r1);
        assertNotSame(r1, r2); // the two regions must not be the same
        assertEquals(r1.getX(), r2.getX());
        assertEquals(r1.getY(), r2.getY());
        assertEquals(r1.getW(), r2.getW());
        assertEquals(r1.getH(), r2.getH());
        assertEquals(r1.getScreen(), r2.getScreen());
        assertEquals(r1.getAutoWaitTimeout(), r2.getAutoWaitTimeout(), 0.0001);

        // Constructor from Rectangle
        Region r3 = new Region(new Rectangle(10, 20, 100, 200));
        assertEquals(10, r3.getX());
        assertEquals(20, r3.getY());
        assertEquals(100, r3.getW());
        assertEquals(200, r3.getH());
        assertEquals(Settings.AutoWaitTimeout, r3.getAutoWaitTimeout(), 0.0001);
        assertEquals(Screen.getPrimaryScreen(), r3.getScreen());
    }

    /**
     * Test the Region.create() method
     */
    @Test
    public void testRegionCreate() {
        // creation from x,y,w,h
        Region r1 = Region.create(10, 20, 100, 200);
        assertEquals(10, r1.getX());
        assertEquals(10, r1.x);
        assertEquals(20, r1.getY());
        assertEquals(20, r1.y);
        assertEquals(100, r1.getW());
        assertEquals(100, r1.w);
        assertEquals(200, r1.getH());
        assertEquals(200, r1.h);
        assertEquals(Settings.AutoWaitTimeout, r1.getAutoWaitTimeout(), 0.0001);
        assertEquals(Screen.getPrimaryScreen(), r1.getScreen());

        // creation from another Region

        r1.setAutoWaitTimeout(15.d);

        Region r2 = Region.create(r1);
        assertNotSame(r1, r2); // the two regions must not be the same
        assertEquals(r1.getX(), r2.getX());
        assertEquals(r1.getY(), r2.getY());
        assertEquals(r1.getW(), r2.getW());
        assertEquals(r1.getH(), r2.getH());
        assertEquals(r1.getScreen(), r2.getScreen());
        assertEquals(r1.getAutoWaitTimeout(), r2.getAutoWaitTimeout(), 0.0001);

        // creation from Rectangle
        Region r3 = Region.create(new Rectangle(10, 20, 100, 200));
        assertEquals(10, r3.getX());
        assertEquals(20, r3.getY());
        assertEquals(100, r3.getW());
        assertEquals(200, r3.getH());
        assertEquals(Settings.AutoWaitTimeout, r3.getAutoWaitTimeout(), 0.0001);
        assertEquals(Screen.getPrimaryScreen(), r3.getScreen());

        // creation from location and w,h
        Location loc = new Location(10, 20);
        Region r4 = Region.create(loc, 100, 200);
        assertEquals(loc.getX(), r4.getX(), 0.0001);
        assertEquals(loc.getY(), r4.getY(), 0.0001);
        assertEquals(100, r4.getW());
        assertEquals(200, r4.getH());
        assertEquals(Settings.AutoWaitTimeout, r4.getAutoWaitTimeout(), 0.0001);
        assertEquals(Screen.getPrimaryScreen(), r4.getScreen());

        // creation with location and direction
        loc = new Location(100, 200);

        Region r5 = Region.create(loc, Region.CREATE_X_DIRECTION_LEFT, Region.CREATE_Y_DIRECTION_TOP, 100, 200);
        assertEquals(loc.getX(), r5.getX(), 0.0001);
        assertEquals(loc.getY(), r5.getY(), 0.0001);

        Region r6 = Region.create(loc, Region.CREATE_X_DIRECTION_LEFT, Region.CREATE_Y_DIRECTION_BOTTOM, 10, 20);
        assertEquals(loc.getX(), r6.getX(), 0.0001);
        assertEquals(loc.getY() - 20, r6.getY(), 0.0001);

        Region r7 = Region.create(loc, Region.CREATE_X_DIRECTION_RIGHT, Region.CREATE_Y_DIRECTION_TOP, 10, 20);
        assertEquals(loc.getX() - 10, r7.getX(), 0.0001);
        assertEquals(loc.getY(), r7.getY(), 0.0001);

        Region r8 = Region.create(loc, Region.CREATE_X_DIRECTION_RIGHT, Region.CREATE_Y_DIRECTION_BOTTOM, 10, 20);
        assertEquals(loc.getX() - 10, r8.getX(), 0.0001);
        assertEquals(loc.getY() - 20, r8.getY(), 0.0001);
    }

    /**
     * Test Region creation with above/below/right/left
     */
    @Test
    public void testRelativeCreation() {
        Region r1 = Region.create(100, 200, 300, 400);
        Screen s = r1.getScreen();

        Region r2 = r1.above();
        assertEquals(r1.getX(), r2.getX());
        assertEquals(s.getY(), r2.getY());
        assertEquals(r1.getW(), r2.getW());
        assertEquals(r1.getY()-s.getY(), r2.getH());

        r2 = r1.below();
        assertEquals(r1.getX(), r2.getX());
        assertEquals(r1.getY()+r1.getH(), r2.getY());
        assertEquals(r1.getW(), r2.getW());
        assertEquals((s.getY()+s.getH())-(r1.getY()+r1.getH()), r2.getH());

        r2 = r1.left();
        assertEquals(s.getX(), r2.getX());
        assertEquals(r1.getY(), r2.getY());
        assertEquals(r1.getX()-s.getX(), r2.getW());
        assertEquals(r1.getH(), r2.getH());

        r2 = r1.right();
        assertEquals(r1.getX()+r1.getW(), r2.getX());
        assertEquals(r1.getY(), r2.getY());
        assertEquals((s.getX()+s.getW())-(r1.getX()+r1.getW()), r2.getW());
        assertEquals(r1.getH(), r2.getH());
    }

    /**
     * Tests if the Regions are cropped if the extend over top left:
     * The Region should be cropped so it is completely inside its screen.
     * This test checks if a Region that is out of its Screen on the top left corner is cropped to the Screen dimensions.

    @Test
    public void testRegionCropTopLeft() {
        Screen s = Screen.getPrimaryScreen();

        Region r1 = new Region(s.getX() - 10, s.getY() - 20, 100, 200);
        assertEquals(s.getX(), r1.getX());
        assertEquals(s.getY(), r1.getY());
        assertEquals(90, r1.getW());
        assertEquals(180, r1.getH());
    }*/

    /**
     * Tests if the Regions are cropped if the extend over top right:
     * The Region should be cropped so it is completely inside its screen.
     * This test checks if a Region that is out of its Screen on the top right corner is cropped to the Screen dimensions.

    @Test
    public void testRegionCropTopRight() {
        Screen s = Screen.getPrimaryScreen();

        Region r1 = new Region(s.getX() + s.getW() - 10, s.getY() - 20, 100, 200);
        assertEquals(s.getX()+s.getW()-10, r1.getX());
        assertEquals(s.getY(), r1.getY());
        assertEquals(10, r1.getW());
        assertEquals(180, r1.getH());
    }*/

    /**
     * Tests if the Regions are cropped if the extend over bottom left:
     * The Region should be cropped so it is completely inside its screen.
     * This test checks if a Region that is out of its Screen on the bottom left corner is cropped to the Screen dimensions.

    @Test
    public void testRegionCropBottomLeft() {
        Screen s = Screen.getPrimaryScreen();

        Region r1 = new Region(s.getX() - 10, s.getY() + s.getH() - 20, 100, 200);
        assertEquals(s.getX(), r1.getX());
        assertEquals(s.getY() + s.getH() - 20, r1.getY());
        assertEquals(90, r1.getW());
        assertEquals(20, r1.getH());
    }*/

    /**
     * Tests if the Regions are cropped if the extend over bottom right:
     * The Region should be cropped so it is completely inside its screen.
     * This test checks if a Region that is out of its Screen on the bottom right corner is cropped to the Screen dimensions.

    @Test
    public void testRegionCropBottomRight() {
        Screen s = Screen.getPrimaryScreen();

        Region r1 = new Region(s.getX() + s.getW() - 10, s.getY() + s.getH() - 20, 100, 200);
        assertEquals(s.getX() + s.getW() - 10, r1.getX());
        assertEquals(s.getY() + s.getH() - 20, r1.getY());
        assertEquals(10, r1.getW());
        assertEquals(20, r1.getH());
    }*/
}
