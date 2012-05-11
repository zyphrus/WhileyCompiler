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
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A Whiley ref, which mirrors an actor in the Actor Model of concurrency.
 * 
 * @author David Pearce
 * @author Timothy Jones
 */
public final class Actor extends Continuation {
	/**
	 * The state this actor refers to.
	 */
	private Object state;
	
	// The linked-queue mailbox of messages.
	private final ConcurrentLinkedQueue<Message> mailbox; 

	/**
	 * A convenience constructor for actors with no state.
	 * 
	 * @param scheduler The scheduler to use for concurrency
	 */
	public Actor(Scheduler scheduler) {
		this(null, scheduler);
	}

	/**
	 * @param scheduler The scheduler to use for concurrency
	 * @param state The internal state of the actor
	 */
	public Actor(Object state, Scheduler pool) {
		super(pool);
		this.state = state;
		this.mailbox = new ConcurrentLinkedQueue<Message>();
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
	 * Send a message asynchronously to this actor. If the mailbox is full, then
	 * this will in fact block.
	 * 
	 * @param sender The sender of this message
	 * @param method The method to call when running the message
	 * @param arguments The arguments to pass to the method
	 */
	public void sendAsync(Actor sender, Method method, Object[] arguments) {
		System.out.println("ASYNC SEND: " + method);		
		arguments[0] = this;
		mailbox.add(new Message(method, arguments));	
		schedule();
	}

	/**
	 * Send a message synchronously to this actor. This will block the sender
	 * until the message is received, and a return value generated.
	 * 
	 * @param sender The sender of this message
	 * @param method The method to call when running the message
	 * @param arguments The arguments to pass to the method
	 * 
	 * @throws Throwable The method call may fail for any reason
	 */
	public Object sendSync(Actor sender, Method method, Object[] arguments) {
		System.out.println("SYNC SEND: " + method);
		
		if (sender.status() == Continuation.RUNNING) {
			// This is the first time this method is entered for this message.
			// Setup and add the message as normal.
			arguments[0] = this;
			SyncMessage message = new SyncMessage(sender, method, arguments);
			// block sender and prep it to receive result
			sender.block(0);
			sender.set(1, message);
			// add message
			mailbox.add(message);			
			return null;
		} else {
			// The sender has yielded and returned to this method. Attempt to
			// add the saved message again.
			SyncMessage message = (SyncMessage) sender.getObject(1);
			return message.result;
		}
	}

	public final void go() {
		System.out.println("ACTOR ENTERS RUN: " + this);
		switch (status()) {
		case Continuation.RUNNING:
			// FIXME: could be more efficient
			while (mailbox.isEmpty()) {
				Message m = mailbox.poll();
				System.out.println("ACTOR DISPATCHES: " + this);
				dispatch(m);
				if (status() != Continuation.RUNNING) {
					System.out.println("ACTOR EXITS RUN: " + this);
					// indicates we're yielding
					return;
				} else if (m instanceof SyncMessage) {
					SyncMessage sm = (SyncMessage) m;
					sm.sender.schedule(); // notify sender
				}
			}
			System.out.println("ACTOR FINISHED MESSAGE LOOP: " + this);
			break;
		case Continuation.RESUMING:
			// what to do here?
			System.out.println("ACTOR ATTEMPTING RESUME: " + this);
			break;
		default:
			System.out.println("ACTOR UNKNOWN STATE (" + status() + "): " + this);
		}
		System.out.println("ACTOR EXITS RUN: " + this);
	}
	
	/**
	 * Dispatch a given message to a given receiver.
	 * 
	 * @param message
	 * @return
	 */
	private final Object dispatch(Message message) {
		try {
			return message.method.invoke(null, message.arguments);
		} catch (IllegalArgumentException iax) {
			// Not possible - caught by the language compiler.
			throw new RuntimeException(iax);
		} catch (IllegalAccessException iae) {
			// Not possible - all message invocations are on public methods.
			throw new RuntimeException(iae);
		} catch (InvocationTargetException itx) {
			throw new RuntimeException(itx);			
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
		public Object result = null;
		public Throwable cause = null;

		public SyncMessage(Actor sender, Method method, Object[] arguments) {
			super(method, arguments);
			this.sender = sender;
		}
	}
}
