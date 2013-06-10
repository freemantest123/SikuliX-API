package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import org.junit.Test;
import org.sikuli.script.Match;
import org.sikuli.script.Pattern;
import org.sikuli.script.Region;
import org.sikuli.script.Screen;
import org.sikuli.script.ScreenImage;

public class ScreenCaptureTest {

    @Test
    public void testScreenCapture() throws Exception {
        Screen s = new Screen(0);
        ScreenImage img = s.capture(100,100,100,100);
        System.out.println(img.getFile());
        Match m = s.exists(new Pattern(img.getFile()));
        System.out.println(m);
        assertNotNull(m);

        // screen 0 is always primary screen
        System.out.println(Screen.getPrimaryScreen().toStringShort());
        assertEquals(Screen.getPrimaryScreen().toStringShort(), (new Screen(0)).toStringShort());

        Screen primary = Screen.getPrimaryScreen();

        Region prReg1 = new Region(0,0,10,10);

        if (Screen.getNumberScreens() > 1) {
            Screen secondary = Screen.getScreen(1);
            assertNotSame(primary, secondary);
        }
    }
}
