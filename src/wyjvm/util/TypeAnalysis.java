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

import java.util.Arrays;
import java.util.List;

import wyjvm.attributes.Code;
import wyjvm.lang.Bytecode;
import wyjvm.lang.Bytecode.ArrayLoad;
import wyjvm.lang.Bytecode.ArrayStore;
import wyjvm.lang.Bytecode.BinOp;
import wyjvm.lang.Bytecode.Iinc;
import wyjvm.lang.Bytecode.Load;
import wyjvm.lang.Bytecode.LoadConst;
import wyjvm.lang.Bytecode.Neg;
import wyjvm.lang.Bytecode.Return;
import wyjvm.lang.Bytecode.Throw;
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
	private ClassFile.Method method; // currently being analysed
	
	/**
	 * Apply the analysis to every method in a classfile.
	 * 
	 * @param cf
	 */
	public void apply(ClassFile cf) {
		for (ClassFile.Method method : cf.methods()) {
			apply(method);
		}
	}

	/**
	 * Apply the analysis to a given method in a classfile.
	 * 
	 * @param cf
	 */
	public Store[] apply(ClassFile.Method method) {
		this.method = method;
		return super.apply(method);
	}
	
	@Override
	public Store[] initialise(Code attr, Method method) {	
		// First, create the initial store from the parameter types.
		List<JvmType> paramTypes = method.type().parameterTypes();
		JvmType[] types = new JvmType[attr.maxLocals() + attr.maxStack()];
		int index = 0;
		for (JvmType t : paramTypes) {
			types[index] = t;
			if (t instanceof JvmType.Long || t instanceof JvmType.Double) {
				// for some reason, longs and doubles occupy two slots.
				index = index + 2;
			} else {
				index = index + 1;
			}
		}
		// Now, create the stores array (one element for each bytecode);
		Store[] stores = new Store[attr.bytecodes().size()];
		stores[0] = new Store(types, attr.maxLocals());
		return stores;
	}

	@Override
	public Store transfer(int index, wyjvm.lang.Bytecode.Store code, Store store) {
		JvmType type = store.top();
		checkIsSubtype(code.type,type,index,store);
		return store.pop();
	}

	@Override
	public Store transfer(int index, Load code, Store store) {
		JvmType type = store.get(code.slot);
		checkIsSubtype(code.type,type,index,store);
		return store.push(type);		
	}

	@Override
	public Store transfer(int index, LoadConst code, Store store) {
		Object constant = code.constant;		
		if(constant instanceof Integer) {
			return store.push(JvmTypes.T_INT);
		} else if(constant instanceof Long) {
			return store.push(JvmTypes.T_LONG);
		} else if(constant instanceof Float) {
			return store.push(JvmTypes.T_FLOAT);
		} else if(constant instanceof Double) {
			return store.push(JvmTypes.T_DOUBLE);
		} else if(constant instanceof String) {
			return store.push(JvmTypes.JAVA_LANG_STRING);
		} else if(constant == null) {
			return store.push(JvmTypes.T_NULL);
		} else {
			throw new RuntimeException("unknown constant encountered ("
					+ constant + ")");
		}
	}

	@Override
	public Store transfer(int index, ArrayLoad code, Store store) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Store transfer(int index, ArrayStore code, Store store) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void transfer(int index, Throw code, Store store) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void transfer(int index, Return code, Store store) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Store transfer(int index, Iinc code, Store store) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Store transfer(int index, BinOp code, Store store) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Store transfer(int index, Neg code, Store store) {
		// TODO Auto-generated method stub
		return null;
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
	 * Check t1 is a supertype of t2 (i.e. t1 :> t2). If not, throw a
	 * VerificationException.
	 */
	protected void checkIsSubtype(JvmType t1, JvmType t2, int index, Store store) {
		if(t1.equals(t2)) {
			return;
		} else if(t1 instanceof JvmType.Array && t2 instanceof JvmType.Array) {
			JvmType.Array a1 = (JvmType.Array) t1;
			JvmType.Array a2 = (JvmType.Array) t2;
			// FIXME: can we do better here?
			if(a1.element().equals(a2.element())) {
				return;
			}
		} else if (t1.equals(JvmTypes.JAVA_LANG_OBJECT)
				&& t2 instanceof JvmType.Array) {
			return;
		} else if(t1 instanceof JvmType.Clazz && t2 instanceof JvmType.Clazz) {
			// FIXME: could do a lot better here.
			return;
		}
		
		// return
		throw new VerificationException(method, index, store, "expected type "
				+ t1 + ", found type " + t2);
	}

	/**
	 * Indicates that the bytecode being analysis is malformed in some manner.
	 * 
	 * @author David J. Pearce
	 * 
	 */
	public static class VerificationException extends RuntimeException {
		
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
		
		public VerificationException(ClassFile.Method method, int index, Store store, String msg) {
			super(msg);
			this.method = method;
			this.index = index;
			this.store = store;
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
		
		public Store(JvmType[] types, int maxLocals) {
			this.types = types;
			this.stack = maxLocals;
		}
		
		private Store(Store store) {
			this.types = store.types.clone();
			this.stack = store.stack;
		}
		
		public JvmType get(int index) {
			return types[index];
		}
		
		public JvmType top() {
			return types[stack-1];
		}
		
		public Store push(JvmType type) {
			Store nstore = new Store(this);
			nstore.types[nstore.stack++] = type;
			return nstore;
		}
		
		public Store pop() {
			Store nstore = new Store(this);
			nstore.stack = nstore.stack-1;
			return nstore;
		}
	}

}
