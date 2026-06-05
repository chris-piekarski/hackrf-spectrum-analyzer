package jspectrumanalyzer.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class GraphicsToolkitTest {

    @Test
    void testCreateAcceleratedImageOpaque() {
        BufferedImage img = GraphicsToolkit.createAcceleratedImageOpaque(100, 50);
        assertNotNull(img);
        assertEquals(100, img.getWidth());
        assertEquals(50, img.getHeight());
        // In headless, it should still create a compatible image
        assertTrue(img.getType() == BufferedImage.TYPE_INT_RGB || img.getType() == BufferedImage.TYPE_INT_ARGB);
    }

    @Test
    void testCreateAcceleratedImageTransparent() {
        BufferedImage img = GraphicsToolkit.createAcceleratedImageTransparent(80, 30);
        assertNotNull(img);
        assertEquals(80, img.getWidth());
        assertEquals(30, img.getHeight());
    }

    @Test
    void testInvalidSize() {
        // Should still return something, or handle gracefully
        BufferedImage img = GraphicsToolkit.createAcceleratedImageOpaque(0, 0);
        // Graphics config may return null or throw, but in practice for tests:
        // We just ensure no crash in normal use; for 0 it may be degenerate
        if (img != null) {
            assertEquals(0, img.getWidth());
        }
    }
}
