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
import wyjvm.lang.Bytecode.ArrayLength;
import wyjvm.lang.Bytecode.ArrayLoad;
import wyjvm.lang.Bytecode.ArrayStore;
import wyjvm.lang.Bytecode.BinOp;
import wyjvm.lang.Bytecode.CheckCast;
import wyjvm.lang.Bytecode.Cmp;
import wyjvm.lang.Bytecode.Conversion;
import wyjvm.lang.Bytecode.Dup;
import wyjvm.lang.Bytecode.DupX1;
import wyjvm.lang.Bytecode.DupX2;
import wyjvm.lang.Bytecode.GetField;
import wyjvm.lang.Bytecode.Iinc;
import wyjvm.lang.Bytecode.InstanceOf;
import wyjvm.lang.Bytecode.Invoke;
import wyjvm.lang.Bytecode.Load;
import wyjvm.lang.Bytecode.LoadConst;
import wyjvm.lang.Bytecode.MonitorEnter;
import wyjvm.lang.Bytecode.MonitorExit;
import wyjvm.lang.Bytecode.Neg;
import wyjvm.lang.Bytecode.Nop;
import wyjvm.lang.Bytecode.Pop;
import wyjvm.lang.Bytecode.PutField;
import wyjvm.lang.Bytecode.Return;
import wyjvm.lang.Bytecode.Swap;
import wyjvm.lang.Bytecode.Throw;
import wyjvm.lang.ClassFile;
import wyjvm.lang.JvmTypes;
import wyjvm.lang.Bytecode.If;
import wyjvm.lang.Bytecode.IfCmp;
import wyjvm.lang.ClassFile.Method;
import wyjvm.lang.JvmType;
import wyjvm.util.dfa.ForwardFlowAnalysis;

/**
 * <p>
 * A forward flow analysis which determines the type of each variable and stack
 * location in a given <code>ClassFile.Method</code>. In the case of a method
 * which is not well-typed, a verification error is reported.
 * </p>
 * 
 * <p>
 * <b>NOTE:</b> this analysis currently has some problems dealing with wide
 * types (i.e. long or double). This is because it does not correctly model the
 * stack (which in their case requires two slots per item).
 * </p>
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
		checkIsSubtype(normalise(code.type),type,index,store);
		return store.pop().set(code.slot,type);
	}

	@Override
	public Store transfer(int index, Load code, Store store) {
		JvmType type = store.get(code.slot);		
		checkIsSubtype(normalise(code.type),type,index,store);
		return store.push(type);		
	}

	@Override
	public Store transfer(int index, LoadConst code, Store store) {
		Object constant = code.constant;		
		if (constant instanceof Boolean || constant instanceof Byte
				|| constant instanceof Short || constant instanceof Character
				|| constant instanceof Integer) {
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
		Store orig = store;

		JvmType i = store.top();
		checkIsSubtype(JvmTypes.T_INT,i,index,orig);
		store = store.pop();
			
		JvmType type = store.top();
		if(type instanceof JvmType.Array) {
			JvmType.Array arrType = (JvmType.Array) type;
			checkIsSubtype(code.type,arrType,index,orig);
			return store.pop().push(normalise(arrType.element())); 			
		} else {
			throw new VerificationException(method, index, store,
					"arrayload expected array type");
		}
	}

	@Override
	public Store transfer(int index, ArrayStore code, Store store) {		
		Store orig = store;
		
		JvmType item = store.top();
		checkIsSubtype(code.type.element(),item,index,orig);
		store = store.pop();		

		JvmType i = store.top();
		checkIsSubtype(JvmTypes.T_INT,i,index,orig);
		store = store.pop();
			
		JvmType type = store.top();
		if(type instanceof JvmType.Array) {
			JvmType.Array arrType = (JvmType.Array) type;
			checkIsSubtype(code.type,arrType,index,orig);			
			return store.pop(); 			
		} else {
			throw new VerificationException(method, index, orig,
					"arrayload expected array type");
		}
	}

	@Override
	public void transfer(int index, Throw code, Store store) {
		JvmType type = store.top();
		checkIsSubtype(JvmTypes.JAVA_LANG_THROWABLE, type, index, store);
	}

	@Override
	public void transfer(int index, Return code, Store store) {		
		if(code.type != null) {
			checkIsSubtype(code.type,store.top(),index,store);
			checkIsSubtype(method.type().returnType(),store.top(),index,store);
		}
	}

	@Override
	public Store transfer(int index, Iinc code, Store store) {
		checkIsSubtype(JvmTypes.T_INT, store.get(code.slot), index, store);
		return store;
	}

	@Override
	public Store transfer(int index, BinOp code, Store store) {
		Store orig = store;
		JvmType rhs = store.top();
		store = store.pop();
		JvmType lhs = store.top();
		checkIsSubtype(code.type,lhs,index,orig);
		checkIsSubtype(code.type,rhs,index,orig);
		return store.push(code.type);
	}

	@Override
	public Store transfer(int index, Neg code, Store store) {
		JvmType mhs = store.top();
		checkIsSubtype(code.type, mhs, index, store);
		return store.push(code.type);
	}

	@Override
	public Store transfer(int index, Bytecode.New code, Store store) {
		Store orig = store;
		
		if(code.type instanceof JvmType.Array) {
			int dims = Math.max(1,code.dims);			
			// In the case of an array construction, there will be one or more
			// dimensions provided for the array.
			for(int i=0;i!=dims;++i) {
				checkIsSubtype(JvmTypes.T_INT,store.top(),index,orig);
				store = store.pop();
			}
		}
		return store.push(code.type);
	}
	
	@Override
	public Store transfer(int index, boolean branch, If code, Store store) {
		JvmType mhs = store.top();
		checkIsSubtype(JvmTypes.T_INT,mhs,index,store);
		return store.pop();
	}

	@Override
	public Store transfer(int index, boolean branch, IfCmp code, Store store) {		
		Store orig = store;
		JvmType rhs = store.top();
		store = store.pop();
		JvmType lhs = store.top();
		checkIsSubtype(code.type,lhs,index,orig);
		checkIsSubtype(code.type,rhs,index,orig);
		return store.pop();
	}

	@Override
	public Store transfer(int index, GetField code, Store store) {
		if(code.mode != Bytecode.STATIC) { 
			JvmType owner = store.top();
			checkIsSubtype(code.owner, owner, index, store);
			store = store.pop();
		}
		return store.push(normalise(code.type));
	}

	@Override
	public Store transfer(int index, PutField code, Store store) {
		Store orig = store;
		if(code.mode != Bytecode.STATIC) { 
			JvmType owner = store.top();
			checkIsSubtype(code.owner, owner, index, orig);
			store = store.pop();
		}
		JvmType type = store.top();
		checkIsSubtype(code.type, normalise(type), index, orig);
		return store.pop();
	}

	@Override
	public Store transfer(int index, ArrayLength code, Store store) {
		JvmType type = store.top();
		if(type instanceof JvmType.Array) {
			throw new VerificationException(method, index, store,
					"arraylength requires array type, found " + type);
		}
		return store.push(JvmTypes.T_INT);
	}

	@Override
	public Store transfer(int index, Invoke code, Store store) {
		Store orig = store;
		JvmType.Function ftype = code.type;
		List<JvmType> parameters = ftype.parameterTypes();
		for(int i=parameters.size()-1;i>=0;--i) {
			JvmType type = store.top();
			checkIsSubtype(normalise(parameters.get(i)),type,index,orig);
			store = store.pop();			
		}
		if (code.mode != Bytecode.STATIC) {
			JvmType type = store.top();
			checkIsSubtype(code.owner, type, index, orig);
			store = store.pop();
		}
		JvmType rtype = ftype.returnType();
		if(!rtype.equals(JvmTypes.T_VOID)) {
			return store.push(normalise(rtype));
		} else {
			return store;
		}
	}

	@Override
	public Store transfer(int index, CheckCast code, Store store) {
		JvmType type = store.top();
		checkIsSubtype(code.type,type,index,store);
		return store.pop().push(code.type);
	}

	@Override
	public Store transfer(int index, Conversion code, Store store) {
		JvmType type = store.top();
		if(!type.equals(code.from)) {
			throw new VerificationException(method, index, store,
					"conversion expected " + code.from + ", found " + type);
		}
		return store.pop().push(code.to);
	}

	@Override
	public Store transfer(int index, InstanceOf code, Store store) {
		JvmType type = store.top();
		checkIsSubtype(code.type,type,index,store);
		return store.pop();
	}

	@Override
	public Store transfer(int index, Pop code, Store store) {
		return store.pop();
	}

	@Override
	public Store transfer(int index, Dup code, Store store) {
		JvmType type = store.top();
		return store.push(type); 
	}

	@Override
	public Store transfer(int index, DupX1 code, Store store) {
		// Duplicate the top operand stack value and insert two values down
		JvmType type = store.top();
		store = store.pop();
		JvmType gate = store.top();
		store = store.pop();
		return store.push(type).push(gate).push(type);
	}

	@Override
	public Store transfer(int index, DupX2 code, Store store) {
		// Duplicate the top operand stack value and insert two or three values
		// down
		JvmType type = store.top();
		store = store.pop();
		JvmType gate1 = store.top();
		store = store.pop();
		JvmType gate2 = store.top();
		store = store.pop();
		return store.push(type).push(gate2).push(gate1).push(type);
	}

	@Override
	public Store transfer(int index, Swap code, Store store) {
		JvmType first = store.top();
		store = store.pop();
		JvmType second = store.top();
		store = store.pop();
		return store.push(first).push(second); 
	}
	
	@Override
	public Store transfer(int index, Cmp code, Store store) {
		Store orig = store; // saved
		JvmType lhs = store.top();
		store = store.pop();
		JvmType rhs = store.top();
		store = store.pop();
		checkIsSubtype(JvmTypes.T_LONG,lhs,index,orig);
		checkIsSubtype(JvmTypes.T_LONG,rhs,index,orig);
		return store.push(JvmTypes.T_INT);
	}

	@Override
	public Store transfer(int index, Nop code, Store store) {
		// does what it says on the tin
		return store;
	}

	@Override
	public Store transfer(int index, MonitorEnter code, Store store) {
		JvmType type = store.top();
		if (type instanceof JvmType.Primitive) {
			throw new VerificationException(method, index, store,
					"monitorenter bytecode requires Object type");
		}
		return store.pop();
	}

	@Override
	public Store transfer(int index, MonitorExit code, Store store) {
		JvmType type = store.top();
		if (type instanceof JvmType.Primitive) {
			throw new VerificationException(method, index, store,
					"monitorexit bytecode requires Object type");
		}
		return store.pop();	}


	@Override
	public Store join(int index, Store original, Store update) {
		if (original.stack != update.stack) {
			throw new VerificationException(method, index, original,
					"incompatible stack heights");
		}
		
		// first check whether any changes are needed without allocating any more memory.
		JvmType[] original_types = original.types;
		JvmType[] update_types = update.types;
		boolean safe = true;
		for(int i=0;i!=original_types.length;++i) {
			JvmType ot = original_types[i];
			JvmType ut = update_types[i];
			safe &= isSubtype(ot,ut);
		}
		if(safe) {
			return original;
		}
		// changes are needed, so allocate memory!
		JvmType[] new_types = new JvmType[original_types.length];
		for(int i=0;i!=original_types.length;++i) {
			JvmType ot = original_types[i];
			JvmType ut = update_types[i];
			new_types[i] = join(ot,ut);
		}
		
		return new Store(new_types,original.stack);
	}
	
	protected JvmType join(JvmType t1, JvmType t2) {
		if (t1.equals(t2)) {
			return t1;
		} else if (t1 instanceof JvmType.Array && t2 instanceof JvmType.Array) {
			JvmType.Array a1 = (JvmType.Array) t1;
			JvmType.Array a2 = (JvmType.Array) t2;
			// FIXME: can we do better here?
			if (a1.element().equals(a2.element())) {
				return a1;
			}
		} else if (t1 instanceof JvmType.Reference
				&& t2 instanceof JvmType.Reference) {
			// FIXME: could do a lot better here.
			return JvmTypes.JAVA_LANG_OBJECT;
		}

		return JvmTypes.T_VOID;
	}
	

	/**
	 * Convert types into their stack based representation.
	 * 
	 * @param type
	 * @return
	 */
	private static JvmType normalise(JvmType type) {
		if (type.equals(JvmTypes.T_BOOL) || type.equals(JvmTypes.T_CHAR)
				|| type.equals(JvmTypes.T_SHORT)) {
			return JvmTypes.T_INT;
		}
		return type;
	}
	
	/**
	 * Check t1 is a supertype of t2 (i.e. t1 :> t2). If not, throw a
	 * VerificationException.
	 */
	protected void checkIsSubtype(JvmType t1, JvmType t2, int index, Store store) {
		if(isSubtype(t1,t2)) {
			return;
		} else {		
			// return
			throw new VerificationException(method, index, store, "expected type "
					+ t1 + ", found type " + t2);
		}
	}	
	
	/**
	 * Determine whether t1 is a supertype of t2 (i.e. t1 :> t2). 
	 */
	protected boolean isSubtype(JvmType t1, JvmType t2) {
		if(t1.equals(t2)) {
			return true;
		} else if(t1 instanceof JvmType.Array && t2 instanceof JvmType.Array) {
			JvmType.Array a1 = (JvmType.Array) t1;
			JvmType.Array a2 = (JvmType.Array) t2;
			// FIXME: can we do better here?
			if(a1.element().equals(a2.element())) {
				return true;
			}
		} else if (t1.equals(JvmTypes.JAVA_LANG_OBJECT)
				&& t2 instanceof JvmType.Array) {
			return true;
		} else if(t1 instanceof JvmType.Clazz && t2 instanceof JvmType.Clazz) {
			// FIXME: could do a lot better here.
			return true;
		}
		return false;
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
		private int maxLocals;
		
		public Store(JvmType[] types, int maxLocals) {
			this.types = types;
			this.stack = maxLocals;
			this.maxLocals = maxLocals;
		}
		
		private Store(Store store) {
			this.types = store.types.clone();
			this.stack = store.stack;
		}
		
		public JvmType get(int index) {
			return types[index];
		}
		
		public Store set(int slot, JvmType type) {
			Store nstore = new Store(this);
			nstore.types[slot] = type;
			return nstore;
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
		
		public String toString() {
			String r = "[";
			
			for(int i=0;i!=stack;++i) {
				if(i == maxLocals) {
					r = r + " | ";
				} else if(i != 0) {
					r = r + ", ";
				}
				r = r + types[i];
			}
			
			return r + "]";
		}
	}
}
