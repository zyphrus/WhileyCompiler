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
public final class Scheduler {

	/**
	 * The number of threads to used in the thread pool
	 */
	private final int threadCount;
	
	/**
	 * Responsible for actually managing the scheduling and execution of
	 * continuations on the thread pool.
	 */
	private volatile ExecutorService pool;
	
	/**
	 * Maintains a count of the number of active continuations. When this count
	 * is zero, the pool is automatically shutdown.
	 */
	private volatile int continuationCount;
	
	/**
	 * Creates a new scheduler with a cached thread pool, meaning threads will be
	 * booted up as needed, rather than all at once.
	 */
	public Scheduler() {
		this(Runtime.getRuntime().availableProcessors());
	}
	
	/**
	 * Creates a new scheduler with a thread pool of a fixed size.
	 * 
	 * @param threadCount The number of threads to have in the pool
	 */
	public Scheduler(int threadCount) {
		this.threadCount = threadCount;
	}
	
	/**
	 * @return The number of threads in the thread pool, or 0 if dynamic
	 */
	public int getThreadCount() {
		return threadCount;
	}
	
	/**
	 * Schedules the given object to resume as soon as a thread is available.
	 * 
	 * @param strand The object to schedule a resume for
	 */
	public synchronized void schedule(Continuation continuation) {
		if(pool == null) {
			// the pool is created lazily to ensure that it does not prevent the
			// JVM from shutting down when all tasks are completed. 
			System.out.println("SCHEDULER CREATING THREADPOOL WITH " + threadCount + " THREADS.");
			pool = Executors.newFixedThreadPool(threadCount);
		}
		pool.execute(continuation);
	}
	
	/**
	 * Start the execution of a given continuation in this pool. Before a
	 * continuation can be scheduled, it must be officially started by calling
	 * this method. 
	 */
	public synchronized void start(Continuation continuation) {
		System.out.println("SCHEDULER STARTIED CONTINUATION (" + continuation + ")");
		continuationCount++;
	}
	
	/**
	 * Indicates that a continuation has completed
	 * 
	 * @param continuation
	 */
	public synchronized void completed(Continuation continuation) {
		System.out.println("SCHEDULER COMPLETED CONTINUATION (" + continuation +")");
		continuationCount--;
		// TODO: currently, the continuation count never goes below 1. The
		// reason for this is that the continuation representing the
		if(continuationCount == 0) {
			System.out.println("SHUTTING DOWN THREADPOOL.");
			pool.shutdown();
			pool = null;
		}
	}
}
