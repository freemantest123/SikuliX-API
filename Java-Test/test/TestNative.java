package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sikuli.script.natives.FindInput;
import org.sikuli.script.natives.FindResults;
import org.sikuli.script.natives.Vision;

public class TestNative {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Simple test to check if the VisionProxy is loaded and find() is basically working.
     * If this test is running, the native VisionProxy lib is correctly imported.
     */
    @Test
    public void testVisionProxyJNI() {
        FindResults results = Vision.find(new FindInput());
        assertNotNull(results);
        assertEquals((long)0, results.size());
    }

}
