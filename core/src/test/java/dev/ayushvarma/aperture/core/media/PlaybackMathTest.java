package dev.ayushvarma.aperture.core.media;

import static org.junit.Assert.assertEquals;

import dev.ayushvarma.aperture.core.format.TimeText;

import org.junit.Test;

public class PlaybackMathTest {
    @Test
    public void extrapolatesWhilePlayingAndClamps() {
        assertEquals(15_000, PlaybackMath.position(10_000, 1_000, 1f, true, 6_000, 200_000));
        assertEquals(20_000, PlaybackMath.position(10_000, 1_000, 2f, true, 6_000, 200_000));
        assertEquals(10_000, PlaybackMath.position(10_000, 1_000, 1f, false, 6_000, 200_000));
        assertEquals(200_000, PlaybackMath.position(199_000, 1_000, 1f, true, 60_000, 200_000));
    }

    @Test
    public void missingDurationHidesBar() {
        assertEquals(-1f, PlaybackMath.fraction(5_000, 0), 0f);
        assertEquals(0.5f, PlaybackMath.fraction(5_000, 10_000), 0f);
    }

    @Test
    public void timeText() {
        assertEquals("0:07", TimeText.clock(7_400));
        assertEquals("1:02:03", TimeText.clock(3_723_000));
        assertEquals("0:01", TimeText.countdown(1));
        assertEquals("0:00", TimeText.countdown(-5));
    }
}
