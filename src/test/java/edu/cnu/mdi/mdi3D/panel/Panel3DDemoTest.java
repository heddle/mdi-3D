package edu.cnu.mdi.mdi3D.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Dimension;
import java.awt.Rectangle;

import org.junit.jupiter.api.Test;

class Panel3DDemoTest {

    @Test
    void initialWindowSizeTracksUsableScreenBounds() {
        assertEquals(new Dimension(1040, 675),
                Panel3DDemo.demoWindowSize(new Rectangle(0, 0, 1600, 900)));
        assertEquals(new Dimension(1, 1), Panel3DDemo.demoWindowSize(null));
    }
}
