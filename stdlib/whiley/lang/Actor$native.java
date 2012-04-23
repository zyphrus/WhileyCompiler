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

package whiley.lang;

import java.math.BigInteger;

import wyjc.runtime.Actor;

public class Actor$native {
	
	public static void yield(Actor self) {
		if (self.isYielded()) {
			self.unyield();
		} else {
			self.yield(0);
			self.getScheduler().schedule(self);
		}
	}
	
	public static void sleep(Actor self, BigInteger millis) {
		if (self.isYielded()) {
			long time = self.getLong(0);
			if (System.currentTimeMillis() >= time) {
				self.unyield();
			}
		} else {
			long time = millis.longValue();
			
			if (time <= 0) {
				return;
			}
			
			self.yield(0);
			self.getScheduler().schedule(self);
			self.set(0, System.currentTimeMillis() + time);
		}
	}
	
	public static BigInteger getThreadCountUnfiltered(Actor self) {
		return BigInteger.valueOf(self.getScheduler().getThreadCount());
	}
	
	public static void setThreadCount(Actor self, BigInteger count) {
		self.getScheduler().setThreadCount(count.intValue());
	}
	
}
