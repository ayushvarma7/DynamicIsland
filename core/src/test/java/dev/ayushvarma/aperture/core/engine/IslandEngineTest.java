package dev.ayushvarma.aperture.core.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class IslandEngineTest {
    private long now;
    private IslandEngine engine;
    private final List<IslandState> events = new ArrayList<>();

    @Before
    public void setUp() {
        now = 1_000_000;
        engine = new IslandEngine(() -> now);
        engine.addListener(events::add);
    }

    private IslandActivity act(String id, ActivityKind kind) {
        return IslandActivity.builder(id, "pkg." + id, kind).title(id).updatedAt(now).createdAt(now).build();
    }

    private IslandActivity media(String id, boolean playing) {
        return IslandActivity.builder(id, "pkg." + id, ActivityKind.MEDIA).title(id).playing(playing).updatedAt(now).build();
    }

    @Test
    public void highestPriorityOwnsCompactNextTwoAreMinimal() {
        engine.upsert(media("music", true));
        engine.upsert(act("dl", ActivityKind.DOWNLOAD));
        engine.upsert(act("call", ActivityKind.ONGOING_CALL));
        engine.upsert(act("rec", ActivityKind.RECORDING));
        IslandState s = engine.state();
        assertEquals("call", s.compact.id);
        assertEquals(2, s.minimal.size());
        assertEquals("music", s.minimal.get(0).id);
        assertEquals("rec", s.minimal.get(1).id);
    }

    @Test
    public void pausedMediaDropsToLowestPriority() {
        engine.upsert(media("music", false));
        engine.upsert(act("dl", ActivityKind.DOWNLOAD));
        assertEquals("dl", engine.state().compact.id);
        engine.upsert(media("music", true));
        assertEquals("music", engine.state().compact.id);
    }

    @Test
    public void transientPreemptsThenRestores() {
        engine.upsert(media("music", true));
        assertTrue(engine.postTransient(act("msg", ActivityKind.NOTIFICATION), 4000));
        assertEquals("msg", engine.state().transientActivity.id);
        assertEquals("music", engine.state().compact.id);
        assertEquals(now + 4000, engine.nextDeadline());
        now += 4000;
        engine.tick();
        assertNull(engine.state().transientActivity);
    }

    @Test
    public void transientQueueIsFifoMaxFiveAndCoalesces() {
        for (int i = 0; i < 7; i++) {
            engine.postTransient(IslandActivity.builder("n" + i, "p", ActivityKind.NOTIFICATION).title("t" + i).build(), 1000);
        }
        // n0 shows immediately; the queue kept the newest five of the remaining six.
        assertEquals("n0", engine.state().transientActivity.id);
        List<String> order = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            now += 1000;
            engine.tick();
            if (engine.state().transientActivity != null) order.add(engine.state().transientActivity.id);
        }
        assertEquals(List.of("n2", "n3", "n4", "n5", "n6"), order);

        now += 10_000;
        engine.postTransient(IslandActivity.builder("charge", "sys", ActivityKind.SYSTEM).title("45%").build(), 3000);
        engine.postTransient(IslandActivity.builder("charge", "sys", ActivityKind.SYSTEM).title("46%").build(), 3000);
        assertEquals("46%", engine.state().transientActivity.title.toString());
    }

    @Test
    public void duplicateWithinTwoSecondsIsRejected() {
        IslandActivity a = IslandActivity.builder("k", "p", ActivityKind.NOTIFICATION).title("hi").text("x").build();
        assertTrue(engine.postTransient(a, 4000));
        now += 1500;
        IslandActivity again = IslandActivity.builder("k2", "p", ActivityKind.NOTIFICATION).title("hi").text("x").build();
        assertFalse(engine.postTransient(again, 4000));
        now += 600;
        assertTrue(engine.postTransient(again, 4000));
    }

    @Test
    public void cyclePinsForThirtyMinutesUnlessUrgent() {
        engine.upsert(act("call", ActivityKind.ONGOING_CALL));
        engine.upsert(media("music", true));
        engine.cycle(1);
        assertEquals("music", engine.state().compact.id);
        now += IslandEngine.PIN_MS;
        engine.tick();
        assertEquals("call", engine.state().compact.id);

        engine.cycle(1);
        assertEquals("music", engine.state().compact.id);
        engine.upsert(act("ring", ActivityKind.INCOMING_CALL));
        assertEquals("ring", engine.state().compact.id);
    }

    @Test
    public void timerBecomesUrgentInLastTenSeconds() {
        engine.upsert(media("music", true));
        IslandActivity timer = IslandActivity.builder("t", "clock", ActivityKind.TIMER)
                .chronometer(now + 30_000, true).updatedAt(now).build();
        engine.upsert(timer);
        assertEquals("music", engine.state().compact.id);
        assertEquals(now + 20_000, engine.nextDeadline());
        now += 20_000;
        engine.tick();
        assertSame(timer, engine.state().compact);
    }

    @Test
    public void staleActivitiesExpireAfterEightHours() {
        engine.upsert(act("dl", ActivityKind.DOWNLOAD));
        now += IslandEngine.STALE_MS;
        engine.tick();
        assertTrue(engine.state().isIdle());
    }

    @Test
    public void removeAndListenerFiresOnlyOnChange() {
        engine.upsert(act("dl", ActivityKind.DOWNLOAD));
        int before = events.size();
        engine.tick();
        assertEquals(before, events.size());
        engine.remove("dl");
        assertTrue(engine.state().isIdle());
        assertEquals(before + 1, events.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void transientKindsCannotBePersistent() {
        engine.upsert(act("n", ActivityKind.NOTIFICATION));
    }
}
