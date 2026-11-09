package me.mraxetv.beastlib.scheduler;

import me.mraxetv.beastlib.api.scheduler.SchedulerTaskState;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TrackedSchedulerTaskTest {

    @Test
    public void oneShotTaskCompletesAndNotifiesOnce() {
        AtomicInteger executions = new AtomicInteger();
        AtomicInteger terminalNotifications = new AtomicInteger();
        TrackedSchedulerTask task = task(false, terminalNotifications);

        task.execute(new Runnable() {
            @Override
            public void run() {
                executions.incrementAndGet();
            }
        });

        assertEquals(1, executions.get());
        assertEquals(1, terminalNotifications.get());
        assertEquals(SchedulerTaskState.COMPLETED, task.getState());
        assertTrue(task.isDone());
        assertFalse(task.cancel());
    }

    @Test
    public void repeatingTaskReturnsToScheduledUntilCancelled() {
        AtomicInteger executions = new AtomicInteger();
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger terminalNotifications = new AtomicInteger();
        TrackedSchedulerTask task = task(true, terminalNotifications);
        task.bindCancellation(new Runnable() {
            @Override
            public void run() {
                cancellations.incrementAndGet();
            }
        });

        task.execute(new Runnable() {
            @Override
            public void run() {
                executions.incrementAndGet();
            }
        });
        task.execute(new Runnable() {
            @Override
            public void run() {
                executions.incrementAndGet();
            }
        });

        assertEquals(2, executions.get());
        assertEquals(SchedulerTaskState.SCHEDULED, task.getState());
        assertTrue(task.cancel());
        assertFalse(task.cancel());
        assertEquals(1, cancellations.get());
        assertEquals(1, terminalNotifications.get());
        assertEquals(SchedulerTaskState.CANCELLED, task.getState());
    }

    @Test
    public void cancellationBeforeNativeBindingCancelsNativeTaskOnBind() {
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger terminalNotifications = new AtomicInteger();
        TrackedSchedulerTask task = task(false, terminalNotifications);

        assertTrue(task.cancel());
        task.bindCancellation(new Runnable() {
            @Override
            public void run() {
                cancellations.incrementAndGet();
            }
        });

        assertEquals(1, cancellations.get());
        assertEquals(1, terminalNotifications.get());
        assertEquals(SchedulerTaskState.CANCELLED, task.getState());
    }

    @Test
    public void retiredTaskDoesNotExecute() {
        AtomicInteger executions = new AtomicInteger();
        AtomicInteger terminalNotifications = new AtomicInteger();
        TrackedSchedulerTask task = task(false, terminalNotifications);

        assertTrue(task.retire());
        task.execute(new Runnable() {
            @Override
            public void run() {
                executions.incrementAndGet();
            }
        });

        assertEquals(0, executions.get());
        assertEquals(1, terminalNotifications.get());
        assertEquals(SchedulerTaskState.RETIRED, task.getState());
    }

    @Test
    public void repeatingTaskCanCancelItselfWhileRunning() {
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger terminalNotifications = new AtomicInteger();
        final TrackedSchedulerTask task = task(true, terminalNotifications);
        task.bindCancellation(new Runnable() {
            @Override
            public void run() {
                cancellations.incrementAndGet();
            }
        });

        task.execute(new Runnable() {
            @Override
            public void run() {
                assertTrue(task.cancel());
            }
        });

        assertEquals(1, cancellations.get());
        assertEquals(1, terminalNotifications.get());
        assertEquals(SchedulerTaskState.CANCELLED, task.getState());
    }

    @Test
    public void failingOneShotTaskStillReachesTerminalState() {
        AtomicInteger terminalNotifications = new AtomicInteger();
        TrackedSchedulerTask task = task(false, terminalNotifications);

        try {
            task.execute(new Runnable() {
                @Override
                public void run() {
                    throw new IllegalStateException("expected");
                }
            });
            fail("Expected callback failure to propagate");
        } catch (IllegalStateException expected) {
            assertEquals("expected", expected.getMessage());
        }

        assertEquals(1, terminalNotifications.get());
        assertEquals(SchedulerTaskState.COMPLETED, task.getState());
    }

    private static TrackedSchedulerTask task(
            boolean repeating,
            final AtomicInteger terminalNotifications
    ) {
        return new TrackedSchedulerTask(
                null,
                repeating,
                new TrackedSchedulerTask.TerminalListener() {
                    @Override
                    public void onTerminal(TrackedSchedulerTask task) {
                        terminalNotifications.incrementAndGet();
                    }
                }
        );
    }
}
