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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A task scheduler for the actor system that distributes the processes amongst
 * a certain number of threads. Once all threads are busy newly scheduled tasks
 * are delayed until an existing thread becomes available.
 * 
 * @author Timothy Jones
 */
public final class ThreadPool {
	
	// The thread pool that tasks will be distributed across.
	private ExecutorService pool;
	
	private int taskCount = 0;
	
	private int threadCount;
	
	/**
	 * Creates a new scheduler with a cached thread pool, meaning threads will be
	 * booted up as needed, rather than all at once.
	 */
	public ThreadPool() {
		this.threadCount = 0;
		pool = Executors.newCachedThreadPool();
	}
	
	/**
	 * Creates a new scheduler with a thread pool of a fixed size.
	 * 
	 * @param threadCount The number of threads to have in the pool
	 */
	public ThreadPool(int threadCount) {
		this.threadCount = threadCount;
		pool = Executors.newFixedThreadPool(threadCount);
	}
	
	/**
	 * @return The number of threads in the thread pool, or 0 if dynamic
	 */
	public int getThreadCount() {
		return threadCount;
	}
	
	/**
	 * Shuts the current thread pool down and starts a new one with the given
	 * number of threads. Performs no operation if the thread count is the same.
	 * 
	 * @param threadCount The number of threads to have in the thread pool
	 */
	public void setThreadCount(int threadCount) {
		if (this.threadCount != threadCount) {
			pool.shutdown();
			this.threadCount = threadCount;
			pool = Executors.newFixedThreadPool(threadCount);
		}
	}
	
	/**
	 * Schedules the given object to resume as soon as a thread is available.
	 * 
	 * @param strand The object to schedule a resume for
	 */
	public void run(Actor actor) {
		synchronized (this) {
			taskCount += 1;
		}
		
		pool.execute(actor);
	}
	
	protected void taskCompleted() {
		int count;
		
		synchronized (this) {
			count = taskCount -= 1;
		}
		
		if (count == 0) {
			pool.shutdown();
		}
	}
	
}
