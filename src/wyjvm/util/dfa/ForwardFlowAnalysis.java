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

package wyjvm.util.dfa;

import wyjvm.attributes.Code;
import wyjvm.lang.*;
import wyjvm.util.TypeAnalysis.Store;

import java.util.*;

/**
 * Represents a generic forward dataflow analysis. This user must implement the
 * transfer functions for the different kinds of bytecode, and the analysis will
 * propagate until a fixed point is reached.
 * 
 * @author David J. Pearce
 * 
 */
public abstract class ForwardFlowAnalysis<T> {
	
	/**
	 * 
	 * @param method
	 */
	public T[] apply(ClassFile.Method method) {
		Code attr = method.attribute(Code.class);
		if (attr == null) {
			// sanity check
			throw new IllegalArgumentException(
					"cannot apply forward flow analysis on method without code attribute");
		}
		List<Bytecode> bytecodes = attr.bytecodes();
		
		// Holds the indices of bytecodes still to be processed. When this set
		// is emtpy, we're done.
		HashSet<Integer> worklist = new HashSet<Integer>();
		worklist.add(0);
		
		// Determine the bytecode index of each declared label.
		HashMap<String,Integer> labels = new HashMap<String,Integer>();
		for(int i=0;i!=bytecodes.size();++i) {
			Bytecode bytecode = bytecodes.get(i);
			if(bytecode instanceof Bytecode.Label) {
				Bytecode.Label label = (Bytecode.Label) bytecode;
				labels.put(label.name, i);
			}
		}
		
		// Stores the abstract store which holds immediately before each
		// bytecode.
		T[] stores = (T[]) new Object[bytecodes.size()];
		stores[0] = initialise(attr,method);
		
		while(!worklist.isEmpty()) {
			int index = select(worklist);				
			Bytecode bytecode = bytecodes.get(index);
			T store = stores[index];
			
			if(bytecode instanceof Bytecode.Label) {
				// basically, a no-op
				merge(index+1,store,worklist,stores);
			} else if(bytecode instanceof Bytecode.Goto) {
				Bytecode.Goto g = (Bytecode.Goto) bytecode;
				int target = labels.get(g.label);
				merge(target,store,worklist,stores);
			} else if(bytecode instanceof Bytecode.If) {
				Bytecode.If i = (Bytecode.If) bytecode;
				T falseBranch = transfer(index,false,i,store);
				T trueBranch = transfer(index,true,i,store);
				merge(index+1,falseBranch,worklist,stores);
				merge(labels.get(i.label),trueBranch,worklist,stores);
			} else if(bytecode instanceof Bytecode.IfCmp) {
				Bytecode.IfCmp i = (Bytecode.IfCmp) bytecode;
				T falseBranch = transfer(index,false,i,store);
				T trueBranch = transfer(index,true,i,store);
				merge(index+1,falseBranch,worklist,stores);
				merge(labels.get(i.label),trueBranch,worklist,stores);
			} else if(bytecode instanceof Bytecode.Switch) {
				// TODO
			} else {
				// sequential
				store = transfer(index,bytecode,store);
				// now update store for destination, ignoring those with no
				// follow on instruction.
				if (!(bytecode instanceof Bytecode.Return)
						&& !(bytecode instanceof Bytecode.Throw)) {
					merge(index + 1, store, worklist, stores);
				}
			}
		}
		
		return stores;
	}
	
	protected int select(HashSet<Integer> worklist) {
		int next = worklist.iterator().next();
		worklist.remove(next);
		return next;
	}
	
	protected void merge(int index, T store, HashSet<Integer> worklist, T[] stores) {
		T old = stores[index];
		if(old == null) {
			stores[index] = store;
			worklist.add(index);
		} else {
			store = join(old,store);
			if(store != old) {
				stores[index] = store;
				worklist.add(index);
			}
		}
	}
	
	/**
	 * Generate an initial store for the given method.
	 * 
	 * @param attribute
	 *            --- code attribute being analysed.
	 * @param method
	 *            --- enclosing method.
	 * @return
	 */
	public abstract T initialise(Code attribute, ClassFile.Method method);
	
	/**
	 * Generate an updated a abstract store by apply the abstract effect(s) of a
	 * given bytecode to an incoming store. In this case, the bytecode in
	 * question will be non-branching.
	 * 
	 * @param index
	 *            --- index in bytecode array of bytecode being analysed.
	 * @param bytecode
	 *            --- to be analysed.
	 * @param store
	 *            --- incoming abstract store.
	 * @return
	 */
	public T transfer(int index, Bytecode code, T store) {
		if(code instanceof Bytecode.Store) {
			return transfer(index, (Bytecode.Store)code, store);
		} else if(code instanceof Bytecode.Load) {
			return transfer(index, (Bytecode.Load)code, store);
		} else if(code instanceof Bytecode.Iinc) {

		}
	}

	/**
	 * Generate an updated a abstract store by apply the abstract effect(s) of a
	 * given bytecode to an incoming store. In this case, the bytecode in
	 * question will be non-branching.
	 * 
	 * @param index
	 *            --- index in bytecode array of bytecode being analysed.
	 * @param bytecode
	 *            --- to be analysed.
	 * @param store
	 *            --- incoming abstract store.
	 * @return
	 */
	public abstract T transfer(int index, Bytecode.Store code, T store);
	
	/**
	 * Generate an updated a abstract store by apply the abstract effect(s) of a
	 * given bytecode to an incoming store. In this case, the bytecode in
	 * question is branching, and we must consider which branch is being
	 * analysed (either the true branch, or the false branch).
	 * 
	 * @param index
	 *            --- index in bytecode array of bytecode being analysed.
	 * @param branch
	 *            --- indicates the true or false branch is to be considered.
	 * @param bytecode
	 *            --- to be analysed.
	 * @param store
	 *            --- incoming abstract store.
	 * @return
	 */
	public abstract T transfer(int index, boolean branch, Bytecode.If code, T store);
	
	/**
	 * Generate an updated a abstract store by apply the abstract effect(s) of a
	 * given bytecode to an incoming store. In this case, the bytecode in
	 * question is branching, and we must consider which branch is being
	 * analysed (either the true branch, or the false branch).
	 * 
	 * @param index
	 *            --- index in bytecode array of bytecode being analysed.
	 * @param branch
	 *            --- indicates the true or false branch is to be considered.
	 * @param bytecode
	 *            --- to be analysed.
	 * @param store
	 *            --- incoming abstract store.
	 * @return
	 */
	public abstract T transfer(int index, boolean branch, Bytecode.IfCmp code, T store);
	
	/**
	 * Join two abstract stores together at a join point in the control-flow
	 * graph.
	 * 
	 * <b>NOTE:</b> a special requirement is that, if the resulting store is the
	 * same as <code>old</code>, then <code>old</code> must be returned.
	 * 
	 * @param original
	 *            --- original store to join "into". In the case of no change, this should
	 *            be returned.
	 * @param update
	 *            --- new store to join "into" the original store.
	 * @return
	 */
	public abstract T join(T original, T udpate);
}
