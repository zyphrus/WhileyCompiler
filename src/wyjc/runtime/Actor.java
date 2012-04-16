// Copyright (c) 2011, David J. Pearce (djp@ecs.vuw.ac.nz)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
// * Neither the name of the <organization> nor the
// names of its contributors may be used to endorse or promote products
// derived from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL DAVID J. PEARCE BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package wyjc.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A Whiley ref, which mirrors an actor in the Actor Model of concurrency.
 * 
 * @author David Pearce
 * @author Timothy Jones
 */
public final class Actor extends Continuation implements Runnable {
	
	private final Scheduler scheduler;
	
	private final Message[] mailbox;
	private int count;
	
	private Object state;
	
	/**
	 * Whether the messager should resume immediately upon completing its yielding
	 * process. This is used mainly to react to a premature resumption, when the
	 * messager is asked to resume before being ready. In this case,
	 * <code>shouldResumeImmediately</code> will cause this actor to immediately
	 * place itself back in the scheduler.
	 */
	private boolean shouldResumeImmediately = false;
	
	/**
	 * Whether the messager is ready to resume. This is important if a message is
	 * sent synchronously and the receiver attempts to resume this messager before
	 * it has completed yielding, in which case the messager will enter an
	 * inconsistent state. Obviously, all messagers are ready to begin with.
	 */
	private boolean isReadyToResume = true;
	
	// Used for sleeping an actor.
	private long wakeAt = 0;
	
	/**
	 * Temporary constructor to get File$native compiling. Remove once native
	 * method can retrieve their running actor.
	 * 
	 * @param state The internal state of the actor
	 */
	public Actor(Object state) {
		this(new Scheduler(0), state);
	}
	
	/**
	 * A convenience constructor for actors with no state.
	 * 
	 * @param scheduler The scheduler to use for concurrency
	 */
	public Actor(Scheduler scheduler) {
		this(scheduler, null);
	}
	
	/**
	 * @param scheduler The scheduler to use for concurrency
	 * @param state The internal state of the actor
	 */
	public Actor(Scheduler scheduler, Object state) {
		this.scheduler = scheduler;
		this.mailbox = new Message[10];
		this.count = 0;
		this.state = state;
	}
	
	/**
	 * @return The internal state of the actor
	 */
	public Object getState() {
		return state;
	}
	
	/**
	 * @param state The internal state of the actor
	 */
	public void setState(Object state) {
		this.state = state;
	}
	
	/**
	 * @return The scheduler used by this actor for concurrency
	 */
	public Scheduler getScheduler() {
		return scheduler;
	}
	
	/**
	 * Send a message asynchronously to this actor. If the mailbox is full, then
	 * this will in fact block.
	 * 
	 * @param sender The sender of this message
	 * @param method The method to call when running the message
	 * @param arguments The arguments to pass to the method
	 */
	public void asyncSend(Method method, Object[] arguments) {
		arguments[0] = this;
		Message msg = new Message(method, arguments);
		
		synchronized (mailbox) {
			// FIXME: when the mailbox gets full we need to yield
			mailbox[count++] = msg;
		}
	}
	
	/**
	 * Send a message synchronously to this actor. This will block the sender
	 * until the message is received, and a return value generated.
	 * 
	 * @param sender The sender of this message
	 * @param method The method to call when running the message
	 * @param arguments The arguments to pass to the method
	 */
	public Object syncSend(Actor sender, Method method, Object[] arguments)
	    throws Throwable {
		if (sender.isYielded()) {
			// Indicates we are resuming from the yield below.
			SyncMessage msg = (SyncMessage) mailbox[0];
			
			if (msg.cause != null) {
				throw msg.cause;
			}
			
			return msg.result;
		} else {
			arguments[0] = this;
			SyncMessage msg = new SyncMessage(sender, method, arguments);
			
			synchronized (mailbox) {
				// FIXME: when the mailbox gets full we need to do something?
				mailbox[count++] = msg;
			}
			
			// Yield the sender actor here.
			sender.yield();
			
			return null;
		}
	}
	
	@Override
	public void run() {
		// Sleep logic. If the actor wakes up before the given time, then it just
		// goes back to sleep.
		if (wakeAt != 0) {
			if (System.currentTimeMillis() < wakeAt) {
				isReadyToResume = true;
				schedule();
				return;
			} else {
				wakeAt = 0;
			}
		}
		
		while (true) {
			Message msg;
			synchronized (mailbox) {
				msg = mailbox[0];
			}
			
			try {
				Object result = msg.method.invoke(null, msg.arguments);
				
				if (!this.isYielded()) {
					isReadyToResume = true;
					
					if (msg instanceof SyncMessage) {
						SyncMessage smsg = (SyncMessage) msg;
						
						// Record the result and alert the sender that we've finished.
						smsg.result = result;
						smsg.sender.schedule();
					}
				} else {
					// This needs to be synchronized in case another actor is trying to
					// schedule this one during the end. In this case, it may see
					// isReadyToResume as false, but then fail to set
					// shouldResumeImmediately to true before the if statement below,
					// which would result in the actor never resuming.
					synchronized (this) {
						isReadyToResume = true;
					}
					
					if (shouldResumeImmediately) {
						schedule();
					}
				}
			} catch (IllegalArgumentException iax) {
				// Not possible - caught by the language compiler.
				System.err.println("Warning - illegal arguments in actor resumption.");
			} catch (IllegalAccessException e) {
				// Not possible - all message invocations are on public methods.
				System.err.println("Warning - illegal access in actor resumption.");
			} catch (InvocationTargetException itx) {
				Throwable cause = itx.getCause();
				
				// Asynchronous messages aren't caught, so errors bottom out here.
				if (msg instanceof SyncMessage) {
					((SyncMessage) msg).cause = cause;
				} else {
					String message = cause.getMessage();
					
					System.err.print(this + " failed in a message to "
					    + msg.method.getName() + " because ");
					
					if (cause instanceof wyjc.runtime.Exception) {
						System.err.println(cause);
					} else if (message == null) {
						System.err.println("of a " + cause);
					} else {
						System.err.println(message);
					}
				}
			}
		}
	}
	
	private void schedule() {
		// This is where the resume race condition needs to be handled. The
		// synchronization prevents this code from running alongside the block in
		// the run method: see the comment there.
		synchronized (this) {
			if (!isReadyToResume) {
				shouldResumeImmediately = true;
				return;
			}
		}
		
		// Schedule is successful, so restore these to default values.
		isReadyToResume = shouldResumeImmediately = false;
		
		scheduler.schedule(this);
	}
	
	/**
	 * Causes the actor to yield control of the thread and schedule itself back
	 * into the thread pool.
	 * 
	 * This will not work outside of the scheduler.
	 */
	public void yield() {
		if (this.isYielded()) {
			unyield();
		} else {
			shouldResumeImmediately = true;
			yield(0);
		}
	}
	
	/**
	 * Blocks the actor for at least the given amount of time.
	 * 
	 * This will not work outside of the scheduler.
	 * 
	 * @param millis The number of milliseconds to sleep for
	 */
	public void sleep(long millis) {
		if (this.isYielded()) {
			unyield();
		} else {
			// Don't bother yielding if there isn't an amount of time to sleep for,
			if (millis <= 0) {
				return;
			}
			
			wakeAt = System.currentTimeMillis() + millis;
			yield();
		}
	}
	
	@Override
	public String toString() {
		return state + "@" + System.identityHashCode(this);
	}
	
	private static class Message {
		
		public final Method method;
		public final Object[] arguments;
		
		public Message(Method method, Object[] arguments) {
			this.method = method;
			this.arguments = arguments;
		}
		
	}
	
	private static final class SyncMessage extends Message {
		
		public final Actor sender;
		public Object result;
		public Throwable cause;
		
		public SyncMessage(Actor sender, Method method, Object[] arguments) {
			super(method, arguments);
			this.sender = sender;
		}
		
	}
	
}
