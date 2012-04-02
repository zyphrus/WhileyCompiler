package wyjc.runtime.concurrency;

import java.lang.reflect.InvocationTargetException;

/**
 * A lightweight thread. Uses a <code>Scheduler</code> and yielding to
 * simulate threaded behaviour without the overhead associated with creating a
 * real thread, allowing large amounts to be created and used.
 * 
 * If <code>Strand</code> is instantiated, it will perform exactly like an
 * actor, except it has no internal state.
 * 
 * Strands are also usable outside of the ordinary Whiley runtime. The
 * <code>sendAsync</code> method will work as expected, and the
 * <code>sendSync</code> method will cause the calling thread to block until
 * the responding method exits. Calling <code>wait</code> on a strand will
 * block until the strand idles.
 * 
 * @author Timothy Jones
 */
public class Strand extends Messager {

	private final Scheduler scheduler;

	/**
	 * Whether the messager should resume immediately upon completing its
	 * yielding process. This is used mainly to react to a premature resumption,
	 * when the messager is asked to resume before being ready. In this case,
	 * <code>shouldResumeImmediately</code> will cause this actor to immediately
	 * place itself back in the scheduler.
	 */
	private boolean shouldResumeImmediately = false;

	/**
	 * Whether the messager is ready to resume. This is important if a message
	 * is sent synchronously and the receiver attempts to resume this messager
	 * before it has completed yielding, in which case the messager will enter
	 * an inconsistent state. Obviously, all messagers are ready to begin with.
	 */
	private boolean isReadyToResume = true;

	private long wakeAt = -1;

	/**
	 * @param scheduler The scheduler to use for concurrency
	 */
	public Strand(Scheduler scheduler) {
		super(scheduler);

		this.scheduler = scheduler;
	}

	/**
	 * Retrieves the strand controlling the current thread. Note that the
	 * behaviour of this method differs from that of the Scheduler, which only
	 * retrieves the strand if the strand belongs to that scheduler. This method
	 * will work for any controlling strand. It will still return null if the
	 * thread is not in any scheduler, however.
	 * 
	 * @return The strand controlling the current thread, or null
	 */
	public static Strand getCurrentStrand() {
		Thread thread = Thread.currentThread();
		if (thread instanceof Scheduler.SchedulerThread) {
			return ((Scheduler.SchedulerThread) thread).getCurrentStrand();
		}

		return null;
	}

	@Override
	protected void controlThisThread() {
		scheduler.setCurrentStrand(this);
	}

	@Override
	protected void scheduleResume() {
		synchronized (this) {
			if (!isReadyToResume) {
				shouldResumeImmediately = true;
				return;
			}

			isReadyToResume = shouldResumeImmediately = false;
		}

		scheduler.scheduleResume(this);
	}

	/**
	 * Causes the strand to yield control of the thread and schedule itself back
	 * into the thread pool. This will not work outside of the scheduler, and is
	 * not synchronized. Only call from strand-local code.
	 */
	public void yield() {
		if (!(Thread.currentThread() instanceof Scheduler.SchedulerThread)) {
			throw new UnsupportedOperationException(
					"Cannot yield from outside of the scheduler.");
		}

		if (isYielded()) {
			unyield();
		} else {
			shouldResumeImmediately = true;
			yield(0);
		}
	}

	/**
	 * Causes the strand to yield control of the thread for at least the given
	 * amount of time. It will continually schedule resumptions until the
	 * correct amount of time has passed. This will not work outside of the
	 * scheduler, and is not synchronized. Only call from strand-local code.
	 * 
	 * @param milliseconds The number of milliseconds to sleep for
	 */
	public void sleep(long milliseconds) throws InterruptedException {
		if (milliseconds <= 0) {
			return;
		}

		if (isYielded()) {
			unyield();
		} else {
			wakeAt = System.currentTimeMillis() + milliseconds;
			yield();
		}
	}

	@Override
	protected void resume() {
		if (wakeAt != -1) {
			if (System.currentTimeMillis() < wakeAt) {
				isReadyToResume = true;
				scheduleResume();
				return;
			} else {
				wakeAt = -1;
			}
		}

		Object result;
		
		try {
			result = invokeCurrentMethod();
		} catch (IllegalArgumentException iax) {
			// Not possible - caught by the language compiler.
			System.err.println("Warning - illegal arguments in actor resumption.");
			return;
		} catch (IllegalAccessException iax) {
			// Not possible - all message invocations are on public methods.
			System.err.println("Warning - illegal access in actor resumption.");
			return;
		} catch (InvocationTargetException itx) {
			// Asynchronous messages aren't caught, so errors bottom out here.
			if (!isCurrentMessageSynchronous()) {
				System.err.println(this + " failed in a message to "
						+ getCurrentMessageMethodName() + " because "
						+ itx.getCause().getMessage());
			}

			// Fails the message and moves on to the next one.
			failCurrentMessage(itx.getCause());
			return;
		}
		
		if (!isYielded()) {
			isReadyToResume = true;
			// Completes the message and moves on to the next one.
			completeCurrentMessage(result);
		} else {
			synchronized (this) {
				isReadyToResume = true;
				if (shouldResumeImmediately) {
					// Readies the actor for another resumption.
					scheduleResume();
				}
			}
		}
	}

	@Override
	public String toString() {
		return "strand@" + System.identityHashCode(this);
	}

}
