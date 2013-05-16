/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * added Kelthuzad 2013
 */
package test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.sikuli.script.Region;
import org.sikuli.script.compare.DistanceComparator;
import org.sikuli.script.compare.HorizontalComparator;
import org.sikuli.script.compare.VerticalComparator;

/**
 * Tests the Comparators.
 */
public class ComparatorTest {

    /**
     * Test the {@link HorizontalComparator}.
     */
    @Test
    public void testHoriztonalComparator() {
        Region[] regions = new Region[] {new Region(500,500,100,100), new Region(0,0,100,100), new Region(100,100,100,100)};
        Region[] regions2 = new Region[3];

        System.arraycopy(regions, 0, regions2, 0, regions.length);

        Arrays.sort(regions2, new HorizontalComparator());

        assertEquals(regions2[0], regions[1]);
        assertEquals(regions2[1], regions[2]);
        assertEquals(regions2[2], regions[0]);

    }

    /**
     * Test the {@link VerticalComparator}.
     */
    @Test
    public void testVerticalComparator() {
        Region[] regions = new Region[] {new Region(500,500,100,100), new Region(0,0,100,100), new Region(100,100,100,100)};
        Region[] regions2 = new Region[3];

        System.arraycopy(regions, 0, regions2, 0, regions.length);

        Arrays.sort(regions2, new VerticalComparator());

        assertEquals(regions2[0], regions[1]);
        assertEquals(regions2[1], regions[2]);
        assertEquals(regions2[2], regions[0]);

    }

    /**
     * Test the {@link DistanceComparator}.
     */
    @Test
    public void testDistanceComparator() {
        Region[] regions = new Region[] {new Region(500,500,100,100), new Region(0,0,100,100), new Region(90, 90,100,100)};
        Region[] regions2 = new Region[3];

        System.arraycopy(regions, 0, regions2, 0, regions.length);

        Arrays.sort(regions2, new DistanceComparator(100,100));

        assertEquals(regions2[0], regions[2]);
        assertEquals(regions2[1], regions[1]);
        assertEquals(regions2[2], regions[0]);

    }
}
