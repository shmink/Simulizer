package simulizer.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javafx.application.Platform;

/**
 * Utility functions for dealing with threads
 *
 * @author mbway
 */
public class ThreadUtils {

	public static class Blocker {
		private volatile boolean paused;

		public Blocker() {
            paused = false;
		}

		public boolean isPaused() {
			return paused;
		}

		public void waitIfPaused() throws InterruptedException {
			// don't synchronize until you have to. should be faster in the un-paused case
			if(paused) {
				synchronized (this) {
                    while (paused) {
                        wait();
                    }
				}
			}
		}

		public void pause() {
            paused = true;
		}

		public synchronized void resume() {
			paused = false;
			notifyAll();
		}
	}

	/**
	 * like Platform.runLater but waits until the thread has finished
	 * based on: http://www.guigarage.com/2013/01/invokeandwait-for-javafx/
	 * @param r the runnable to run in a JavaFX thread
	 */
	public static void platformRunAndWait(final Runnable r) throws Throwable {
		if(Platform.isFxApplicationThread()) {
			try {
				r.run();
			} catch(Exception e) {
				throw new ExecutionException(e);
			}
		} else {
			final Lock lock = new ReentrantLock();
			final Condition condition = lock.newCondition();
            final boolean[] done = {false};
			// to get around the requirement for final
			final Throwable[] ex = { null };
			lock.lock();
			try {

				Platform.runLater(() -> {
					lock.lock();
					try {
						r.run();
					} catch(Throwable e) {
						ex[0] = e;
					} finally {
						try {
							done[0] = true;
							condition.signal();
						} finally {
							lock.unlock();
						}
					}
				});

				while(!done[0])
                    condition.await();

				if(ex[0] != null) {
					// re-throw exception from the runLater thread
					throw ex[0];
				}
			} finally {
				lock.unlock();
			}
		}
	}

	public static void runLater(final Runnable r) {
		Thread t = new Thread(r, "ThreadUtils.runLater-Thread");
		t.setDaemon(true);
		t.start();
	}

	public static void sleepQuiet(long l) {
		try {
			Thread.sleep(l);
		} catch (InterruptedException ignored) { }
	}

	/**
     * Given the name of the thread pool or other executor. This factory gives
     * descriptive names to the threads in that pool
	 */
	public static class NamedThreadFactory implements ThreadFactory {
		private final String poolName;
		private int threadID;

		public NamedThreadFactory(String poolName) {
			this.poolName = poolName;
			threadID = 0;
		}

		@SuppressWarnings({"NullableProblems", "ConstantConditions"})
		@Override
		public synchronized Thread newThread(Runnable runnable) {
			if(runnable == null) runnable = () -> {};

			Thread t = new Thread(runnable, "Executor(" + poolName + "): Thread(" + (threadID++) + ")");
			t.setDaemon(true);
			return t;
		}
	}

	public static class NamedTaggedThreadFactory extends NamedThreadFactory {
		private Set<Thread> threads;

		public NamedTaggedThreadFactory(String poolName) {
			super(poolName);
			threads = new HashSet<>();
		}

		public synchronized void killThreads() {
			threads.forEach(Thread::interrupt);
		}

		@SuppressWarnings("NullableProblems")
		@Override
		public synchronized Thread newThread(Runnable runnable) {
			Thread t = super.newThread(runnable);
			threads.add(t);
			return t;
		}

		public boolean runningOnThreadFromFactory() {
			return threads.contains(Thread.currentThread());
		}

	}
}
