// Copyright (c) 2011, David J. Pearce (djp@ecs.vuw.ac.nz)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//    * Redistributions of source code must retain the above copyright
//      notice, this list of conditions and the following disclaimer.
//    * Redistributions in binary form must reproduce the above copyright
//      notice, this list of conditions and the following disclaimer in the
//      documentation and/or other materials provided with the distribution.
//    * Neither the name of the <organization> nor the
//      names of its contributors may be used to endorse or promote products
//      derived from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
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

import wyjc.runtime.concurrency.Strand;

/**
 * A Whiley process, which mirrors an actor in the Actor Model of concurrency.
 * 
 * @author Timothy Jones
 */
public final class Actor extends Fibre {	
	/**
	 * Actor's mail box
	 */
	private final Message[] mailbox;
	private volatile int count;
	
	/**
	 * Actor's state
	 */
	private Object state;

	/**
	 * @param state The internal state of the actor
	 * @param scheduler The scheduler to use for concurrency
	 */
	public Actor(Scheduler scheduler, Object state) {
		super(scheduler);
		this.mailbox = new Message[10];
		this.count = 0;
		this.state = state;
	}

	/**
	 * @return The internal state of the actor.
	 */
	public Object getState() {
		return state;
	}

	/**
	 * @param state The internal state of the actor.
	 * @return This actor (Useful for chaining).
	 */
	public Actor setState(Object state) {
		this.state = state;
		return this;
	}

	/**
	 * Send a message asynchronously to this actor. If the mailbox is full, then
	 * this will in fact block.
	 * 
	 * @param from
	 *            --- sender of this message.
	 * @param method
	 *            --- the "message"
	 * @param arguments
	 *            --- the message "arguments"
	 */
	public void asyncSend(Actor from, Method method, Object[] arguments) {		
		arguments[0] = this;
		Message msg = new Message(method, arguments, null);
		synchronized (mailbox) {
			// FIXME: when the mailbox gets full we need to yield
			mailbox[count++] = msg;
		}
	}
	
	/**
	 * Send a message synchronously to this actor. This will block the sender
	 * until the message is received, and a return value generated.
	 * 
	 * @param sender
	 *            --- sender of this message.
	 * @param method
	 *            --- the "message"
	 * @param arguments
	 *            --- the message "arguments"
	 */
	public Object syncSend(Actor sender, Method method, Object[] arguments) {
		arguments[0] = this;
		Message msg = new Message(method, arguments, sender);
		
		synchronized (mailbox) {
			// FIXME: when the mailbox gets full we need to do something?
			mailbox[count++] = msg;
		}
		
		sender.yield(?);
		
		return msg.get();
	}
	
	public void run() {		
		// this is where the action happens
		while(1==1) {
			try {				
				Message msg;
				synchronized(mailbox) {
					msg = mailbox[count-1];
				}
				msg.result = msg.method.invoke(null, msg.arguments);
				
				// TODO: process suspend
				
				Fibre sender = msg.sender;
				if(sender != null) {
					sender.schedule();
				} 
			} catch(InterruptedException e) {
				// do nothing I guess
			} catch(IllegalAccessException e) {
				// do nothing I guess
			} catch(InvocationTargetException ex) {
				// not sure what to do!
				Throwable e = ex.getCause();
				if(e instanceof RuntimeException) {
					RuntimeException re = (RuntimeException) e;
					throw re;
				}
				// do nothing I guess
			}
		}
	}
	
	@Override
	public String toString() {
		return state + "@" + System.identityHashCode(this);
	}

	private final static class Message {
		public final Method method;
		public final Object[] arguments;
		public final Fibre sender; // for synchronous messages
		public volatile boolean ready = false;
		public volatile Object result;
		
		public Message(Method method, Object[] arguments, Fibre sender) {
			this.method = method;
			this.arguments = arguments;
			this.sender = sender;
		}						
	}
}
