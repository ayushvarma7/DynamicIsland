package dev.ayushvarma.aperture.core.geometry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GeometryCalculatorTest {
    private static final float D = 2.625f;

    /** Pixel 8 values measured in Phase 0 (docs/RESEARCH.md section 4.2). */
    private static DisplayInfo pixel8() {
        return new DisplayInfo(1080, 2400, D, 132, 104, 810, 0,
                CutoutInfo.fromBounds(503.5f, 29.5f, 576f, 102f));
    }

    @Test
    public void pixel8MatchesSection72Formulas() {
        IslandGeometry g = GeometryCalculator.compute(pixel8(), Calibration.DEFAULT);
        assertEquals(IslandGeometry.Layout.SYMMETRIC, g.layout);
        assertEquals(539.75f, g.centerX, 0.01f);
        assertEquals(65.75f, g.centerY, 0.01f);
        assertEquals(36.25f, g.cutoutRadius, 0.01f);
        assertEquals(88.25f, g.compactHeight, 0.01f);
        assertEquals(124f, g.leadingWidth, 0.01f);
        assertEquals(336.25f, g.compactWidth(), 0.01f);
        assertEquals(1017f, g.expandedWidth, 0.01f);
        assertEquals(72.5f, g.expandedCorner, 0.01f);
        assertEquals(21.625f, g.expandedTop, 0.01f);
        assertEquals(46.75f, g.keepOutRadius, 0.01f);
        assertTrue(g.visible);
    }

    @Test
    public void compactNeverEntersPrivacyZone() {
        DisplayInfo d = new DisplayInfo(1080, 2400, D, 132, 104, 700, 0,
                CutoutInfo.fromBounds(503.5f, 29.5f, 576f, 102f));
        IslandGeometry g = GeometryCalculator.compute(d, new Calibration(0, 0, 0, 3, 1.3f));
        float right = g.compactLeft() + g.compactWidth();
        assertTrue("right edge " + right, right <= 700 - 8 * D + 0.01f);
        assertEquals(g.leadingWidth, g.trailingWidth, 0.01f);
    }

    @Test
    public void offCenterHolePutsBothSlotsTowardCenter() {
        DisplayInfo d = new DisplayInfo(1080, 2400, D, 136, 0, 0, 0, CutoutInfo.fromBounds(40, 39, 128, 127));
        IslandGeometry g = GeometryCalculator.compute(d, Calibration.DEFAULT);
        assertEquals(IslandGeometry.Layout.OFFSET, g.layout);
        assertEquals(0f, g.leadingWidth, 0f);
        assertTrue(g.trailingWidth > 0);
    }

    @Test
    public void noCutoutGivesVirtualIslandInStatusBar() {
        DisplayInfo d = new DisplayInfo(1600, 2560, 2f, 48, 0, 0, 0, CutoutInfo.none());
        IslandGeometry g = GeometryCalculator.compute(d, Calibration.DEFAULT);
        assertEquals(IslandGeometry.Layout.VIRTUAL, g.layout);
        assertEquals(800f, g.centerX, 0f);
        assertEquals(24f, g.centerY, 0f);
        assertEquals(48 - 12f, g.compactHeight, 0.01f);
        assertEquals(36 * 2f - 24f, g.expandedCorner, 0.01f); // fallback corner 36 dp minus 12 dp margin
    }

    @Test
    public void calibrationNudgesAndLandscapeHides() {
        IslandGeometry g = GeometryCalculator.compute(pixel8(), new Calibration(1f, -0.5f, 0.5f, 3f, 1f));
        assertEquals(539.75f + D, g.centerX, 0.01f);
        assertEquals(65.75f - 0.5f * D, g.centerY, 0.01f);
        assertEquals(36.25f + 0.5f * D, g.cutoutRadius, 0.01f);
        DisplayInfo land = new DisplayInfo(2400, 1080, D, 74, 104, 0, 0, CutoutInfo.fromBounds(29.5f, 503.5f, 102f, 576f));
        assertFalse(GeometryCalculator.compute(land, Calibration.DEFAULT).visible);
    }

    @Test
    public void waterfallWidensMargin() {
        DisplayInfo d = new DisplayInfo(1080, 2400, D, 132, 104, 0, 20, CutoutInfo.fromBounds(503.5f, 29.5f, 576f, 102f));
        assertEquals(1080 - 2 * (12 * D + 20), GeometryCalculator.compute(d, Calibration.DEFAULT).expandedWidth, 0.01f);
    }
}
