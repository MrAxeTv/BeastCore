package me.mraxetv.beastlib.client;

import me.mraxetv.beastlib.api.client.ClientChunkSnapshot;
import me.mraxetv.beastlib.api.event.player.PlayerUnknownEntityUseEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ClientEventDispatcherTest {
    @Test
    public void eachBoundaryStaysBehindItsOwnMovementWork() {
        ArrayDeque<Runnable> nativeQueue = new ArrayDeque<Runnable>();
        List<String> calls = new ArrayList<String>();
        ClientEventDispatcher dispatcher = new ClientEventDispatcher(8, nativeQueue::add, () -> true,
                error -> { throw new AssertionError(error); });
        nativeQueue.add(() -> calls.add("movement1"));
        dispatcher.submit(() -> calls.add("tick1"));
        nativeQueue.add(() -> calls.add("movement2"));
        dispatcher.submit(() -> calls.add("tick2"));
        assertEquals(4, nativeQueue.size());
        while (!nativeQueue.isEmpty()) nativeQueue.remove().run();
        assertEquals(Arrays.asList("movement1", "tick1", "movement2", "tick2"), calls);
    }

    @Test
    public void overflowStopsStreamAndDoesNotDeliverPartialQueuedFrames() {
        ArrayDeque<Runnable> nativeQueue = new ArrayDeque<Runnable>();
        AtomicBoolean active = new AtomicBoolean(true);
        AtomicInteger failures = new AtomicInteger(), calls = new AtomicInteger();
        ClientEventDispatcher dispatcher = new ClientEventDispatcher(2, nativeQueue::add, active::get,
                error -> { failures.incrementAndGet(); active.set(false); });
        dispatcher.submit(calls::incrementAndGet);
        dispatcher.submit(calls::incrementAndGet);
        dispatcher.submit(calls::incrementAndGet);
        dispatcher.submit(calls::incrementAndGet);
        while (!nativeQueue.isEmpty()) nativeQueue.remove().run();
        assertEquals(1, failures.get());
        assertEquals(0, calls.get());
    }

    @Test
    public void executorRejectionFailsClosedAndConnectionRetirementDropsQueuedWork() {
        AtomicInteger failures = new AtomicInteger();
        ClientEventDispatcher rejected = new ClientEventDispatcher(1,
                task -> { throw new RejectedExecutionException(); }, () -> true, error -> failures.incrementAndGet());
        rejected.submit(() -> fail("rejected callback ran"));
        rejected.submit(() -> fail("rejected callback ran"));
        assertEquals(2, failures.get());

        ArrayDeque<Runnable> nativeQueue = new ArrayDeque<Runnable>();
        AtomicBoolean active = new AtomicBoolean(true);
        ClientEventDispatcher retiring = new ClientEventDispatcher(1, nativeQueue::add, active::get,
                error -> fail(error.toString()));
        retiring.submit(() -> fail("retired connection callback ran"));
        active.set(false);
        nativeQueue.remove().run();
    }

    @Test
    public void snapshotsKeepCompleteSignedKeysAndCannotBeMutated() {
        Set<Long> keys = new LinkedHashSet<Long>();
        for (int x : new int[]{Integer.MIN_VALUE, -1, 0, Integer.MAX_VALUE})
            for (int z : new int[]{Integer.MIN_VALUE, -1, 0, Integer.MAX_VALUE}) {
                long key = ClientChunkSnapshot.chunkKey(x, z);
                assertEquals(x, (int) key);
                assertEquals(z, (int) (key >> 32));
                keys.add(key);
            }
        ClientChunkSnapshot snapshot = new ClientChunkSnapshot(UUID.randomUUID(), keys);
        keys.clear();
        assertEquals(16, snapshot.getChunkKeys().size());
        try { snapshot.getChunkKeys().clear(); fail("mutable snapshot"); }
        catch (UnsupportedOperationException expected) { }
    }

    @Test
    public void interactionPositionsAreDefensiveAndAttackHandMayBeAbsent() {
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> null);
        Vector original = new Vector(1, 2, 3);
        PlayerUnknownEntityUseEvent event = new PlayerUnknownEntityUseEvent(player, -19, false, EquipmentSlot.HAND, original);
        original.setX(90);
        event.getClickedPosition().setY(91);
        assertEquals(new Vector(1, 2, 3), event.getClickedPosition());
        PlayerUnknownEntityUseEvent attack = new PlayerUnknownEntityUseEvent(player, Integer.MAX_VALUE, true, null, null);
        assertTrue(attack.isAttack());
        assertNull(attack.getHand());
        assertNull(attack.getClickedPosition());
    }
}
