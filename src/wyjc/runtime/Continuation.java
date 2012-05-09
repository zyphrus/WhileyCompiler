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

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * A helper class for the actor hierarchy that involves the yielding of thread
 * control and saving of local state.
 * 
 * @author David J. Pearce
 * @author Timothy Jones
 */
public abstract class Continuation implements Runnable {

	/**
	 * A continuation is in the READY state if it is not currently running, but
	 * is ready to run.
	 */
	public static final byte READY = 0;

	/**
	 * A continuation is in the RUNNING state if it is allocated to some thread
	 * and actually executing (rather than e.g. resuming or yielding).
	 */
	public static final byte RUNNING = 1;

	/**
	 * A continuation is in the BLOCKED state if it is not allocated to a thread
	 * and is awaiting some event.
	 */
	public static final byte BLOCKED = 2;

	/**
	 * A continuation is in the YIELDING_TO_BLOCK state if it has been blocked
	 * by something and is currently unwinding the stack.
	 */
	public static final byte YIELDING_TO_BLOCK = 3;

	/**
	 * A continuation is in the YIELDING_TO_READY state if it has been yielded
	 * but is now ready to resume.
	 */
	public static final byte YIELDING_TO_READY = 4;

	/**
	 * A continuation is in the RESUMING state if it has been allocated to a
	 * thread and is currently restoring the stack.
	 */
	public static final byte RESUMING = 5;

	/**
	 * ThreadPool to which this continuation is allocated.
	 */
	private final Scheduler pool;

	/**
	 * Saved state of continuation, which is required for winding/unwinding.
	 */
	private final Stack<State> state = new Stack<State>();

	/**
	 * Topmost state on stack.
	 */
	private volatile State current = null;

	/**
	 * Indicates the current status of this continuation.
	 */
	private volatile byte status = READY;

	/**
	 * Construct a continuation in a given thread pool
	 * 
	 * @param pool
	 */
	public Continuation(Scheduler pool) {
		this.pool = pool;
	}

	/**
	 * Get the status of this continuation.
	 */
	public int status() {
		return status;
	}

	/**
	 * @return The scheduler used by this actor for concurrency
	 */
	public Scheduler getThreadPool() {
		return pool;
	}
	
	/**
	 * Yield the continuation at a given bytecode location. Note that this only
	 * begins the process, since the state of the locals and stack must be
	 * stored manually. Furthermore, the stack must be unwound.
	 * 
	 * @param location
	 *            The location of the computation in the method
	 */
	public void yield(int location) {
		state.push(current = new State(location));
		status = YIELDING_TO_READY;
	}

	/**
	 * Block the continuation at a given bytecode location. Note that this only
	 * begins the process, since the state of the locals and stack must be
	 * stored manually. Furthermore, the stack must be unwound.
	 * 
	 * @param location
	 *            The location of the computation in the method
	 */
	public void block(int location) {
		state.push(current = new State(location));
		status = YIELDING_TO_BLOCK;
	}

	/**
	 * Unblock a given continuation.
	 */
	public void schedule() {
		status = READY;
		pool.schedule(this);
	}
	
	/**
	 * Unwind the continuation at a given bytecode location. This is called
	 * during the unwinding process for intermediate stack frames. It should not
	 * be called to initiate an unwinding. Furthermore, the state of the locals
	 * and stack must be stored manually.
	 * 
	 * @param location
	 *            The location of the computation in the method
	 */
	public void unwind(int location) {
		state.push(current = new State(location));		
	}

	/**
	 * Indicates the thread has finished restoring the current stack frame.
	 */
	public void restored() {
		current = state.peek();
		state.pop();		
	}	
	
	/**
	 * Pops a local state object off of the stack, moving the get and pop and
	 * pop methods onto the next local values.
	 */
	public void resume() {
		status = RUNNING;
	}

	public void set(int index, Object value) {
		current.localMap.put(index, value);
	}

	public void set(int index, boolean value) {
		current.localMap.put(index, value);
	}

	public void set(int index, char value) {
		current.localMap.put(index, value);
	}

	public void set(int index, double value) {
		current.localMap.put(index, value);
	}

	public void set(int index, int value) {
		current.localMap.put(index, value);
	}

	public void set(int index, float value) {
		current.localMap.put(index, value);
	}

	public void set(int index, long value) {
		current.localMap.put(index, value);
	}

	public Object getObject(int index) {
		return current.localMap.get(index);
	}

	public boolean getBool(int index) {
		return (Boolean) current.localMap.get(index);
	}

	public char getChar(int index) {
		return (Character) current.localMap.get(index);
	}

	public double getDouble(int index) {
		return (Double) current.localMap.get(index);
	}

	public int getInt(int index) {
		return (Integer) current.localMap.get(index);
	}

	public float getFloat(int index) {
		return (Float) current.localMap.get(index);
	}

	public long getLong(int index) {
		return (Long) current.localMap.get(index);
	}

	public void push(Object value) {
		current.localStack.push(value);
	}

	public void push(boolean value) {
		current.localStack.push(value);
	}

	public void push(char value) {
		current.localStack.push(value);
	}

	public void push(double value) {
		current.localStack.push(value);
	}

	public void push(int value) {
		current.localStack.push(value);
	}

	public void push(float value) {
		current.localStack.push(value);
	}

	public void push(long value) {
		current.localStack.push(value);
	}

	public Object popObject() {
		return current.localStack.pop();
	}

	public boolean popBool() {
		return (Boolean) current.localStack.pop();
	}

	public char popChar() {
		return (Character) current.localStack.pop();
	}

	public double popDouble() {
		return (Double) current.localStack.pop();
	}

	public int popInt() {
		return (Integer) current.localStack.pop();
	}

	public float popFloat() {
		return (Float) current.localStack.pop();
	}

	public long popLong() {
		return (Long) current.localStack.pop();
	}

	private static final class State {
		private final int location;

		private final Map<Integer, Object> localMap = new HashMap<Integer, Object>();
		private final Stack<Object> localStack = new Stack<Object>();

		public State(int location) {
			this.location = location;
		}
	}
}
