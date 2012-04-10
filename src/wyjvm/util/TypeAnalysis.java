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
package wyjvm.util;

import wyjvm.attributes.Code;
import wyjvm.lang.Bytecode;
import wyjvm.lang.ClassFile;
import wyjvm.lang.JvmTypes;
import wyjvm.lang.Bytecode.If;
import wyjvm.lang.Bytecode.IfCmp;
import wyjvm.lang.ClassFile.Method;
import wyjvm.lang.JvmType;
import wyjvm.util.dfa.ForwardFlowAnalysis;

/**
 * A forward flow analysis which determines the type of each variable and stack
 * location in a given <code>ClassFile.Method</code>. In the case of a method
 * which is not well-typed, a verification error is reported.
 * 
 * @author David J. Pearce
 * 
 */
public class TypeAnalysis extends ForwardFlowAnalysis<TypeAnalysis.Store>{

	@Override
	public Store initialise(Code attr, Method method) {
		Store store = new Store(attr.maxLocals(), attr.maxStack());
		int index = 0;
		for (JvmType t : method.type().parameterTypes()) {
			store.set(index, t);
			if (t instanceof JvmType.Long || t instanceof JvmType.Double) {
				// for some reason, longs and doubles occupy two slots.
				index = index + 2;
			} else {
				index = index + 1;
			}
		}
		return store;
	}
	
	@Override
	public Store transfer(int index, boolean branch, If code, Store store) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Store transfer(int index, boolean branch, IfCmp code, Store store) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Store join(Store original, Store udpate) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Indicates that the bytecode being analysis is malformed in some manner.
	 * 
	 * @author David J. Pearce
	 * 
	 */
	public static class VerificationError extends RuntimeException {
		
		/**
		 * Classfile method which is malformed.
		 */
		private ClassFile.Method method;
		
		/**
		 * Bytecode index where the problem originated
		 */
		private int index; 
		
		/**
		 * Current state of the abstract store when the problem originated.
		 */
		private Store store;
		
		/**
		 * Message which provides additional description about the fault.
		 */
		private String msg;
		
		public VerificationError(ClassFile.Method method, int index, Store store, String msg) {
			this.method = method;
			this.index = index;
			this.store = store;
			this.msg = msg;
		}
	}
	
	/**
	 * An abstract representation of the typing environment used in the JVM
	 * bytecode verifier.
	 * 
	 * @author David J. Pearce
	 * 
	 */
	protected static class Store {
		private JvmType[] types;
		private int stack; // stack pointer
		
		public Store(int maxLocals, int maxStack) {
			types = new JvmType[maxLocals+maxStack];
			stack = maxLocals;
		}
		
		public void set(int index, JvmType type) {
			types[index] = type;
		}
		
		public JvmType get(int index) {
			return types[index];
		}
		
		public JvmType top() {
			return types[stack-1];
		}
		
		public void push(JvmType type) {
			types[stack++] = type;
		}
		
		public void pop() {
			stack = stack - 1;
		}
	}
}
