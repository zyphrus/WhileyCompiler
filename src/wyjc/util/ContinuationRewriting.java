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

package wyjc.util;

import static wyjvm.lang.JvmTypes.JAVA_LANG_OBJECT;
import static wyjvm.lang.JvmTypes.T_BOOL;
import static wyjvm.lang.JvmTypes.T_INT;
import static wyjvm.lang.JvmTypes.T_VOID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import wyil.util.Pair;
import wyjvm.attributes.Code;
import wyjvm.attributes.Code.Handler;
import wyjvm.attributes.StackMapTable;
import wyjvm.lang.Bytecode;
import wyjvm.lang.Bytecode.CheckCast;
import wyjvm.lang.Bytecode.Goto;
import wyjvm.lang.Bytecode.If;
import wyjvm.lang.Bytecode.Invoke;
import wyjvm.lang.Bytecode.Label;
import wyjvm.lang.Bytecode.Load;
import wyjvm.lang.Bytecode.LoadConst;
import wyjvm.lang.Bytecode.Pop;
import wyjvm.lang.Bytecode.Return;
import wyjvm.lang.Bytecode.Store;
import wyjvm.lang.Bytecode.Swap;
import wyjvm.lang.Bytecode.Switch;
import wyjvm.lang.BytecodeAttribute;
import wyjvm.lang.ClassFile;
import wyjvm.lang.ClassFile.Method;
import wyjvm.lang.JvmType;
import wyjvm.lang.JvmType.Clazz;
import wyjvm.lang.JvmType.Function;
import wyjvm.lang.JvmType.Reference;
import wyjvm.lang.JvmTypes;

/**
 * Bytecode rewriter that adds yield and resumption points on actor
 * continuations.
 * 
 * @author Timothy Jones
 */
public class ContinuationRewriting {
 
	// FIXME: should be wyjc.runtime.Continuation
	private static final Clazz CONTINUATION = new Clazz("wyjc.runtime", "Actor");

	public void apply(ClassFile classfile) {
		for (Method method : classfile.methods()) {
			if (!method.name().equals("main")) {
				apply(method);
			}
		}
	}

	public void apply(Method method) {
		for (BytecodeAttribute attribute : method.attributes()) {
			if (attribute instanceof Code) {
				apply(method, (Code) attribute);
			}
		}
	}

	public void apply(Method method, Code code) {
		List<Bytecode> bytecodes = code.bytecodes();
		StackMapTable stackMap = code.attribute(StackMapTable.class);
		int location = 0;

		for (int i = 0; i < bytecodes.size(); ++i) {
			Bytecode bytecode = bytecodes.get(i);

			int original = i;

			if (bytecode instanceof Invoke) {
				Invoke invoke = (Invoke) bytecode;

				if (canYield(invoke)) {
					// A continuation may yield inside another method. If the method returns
					// and the current continuation is yielded, the stack needs to keep
					// yielding, then later resume before the method call so it reenters
					// the yielded method.

					StackMapTable.Frame frame = stackMap.frameAt(i);

					// Ignore return indicates whether or not we need to save
					// the return value or not. 
					boolean ignoreReturn = !invoke.type.returnType().equals(T_VOID);
					int ignores = ignoreReturn ? 4 : 5; 
							
					// If the code flow is arriving here for the first time, it needs to
					// skip the resume. Future resumptions will jump to the start of the
					// function to right after this goto with the following resume.
					bytecodes.add(i++, new Goto("invoke" + location));
					i = addResume(bytecodes, i - 1, location, frame, ignores) + 1;

					bytecodes.add(i++, new Load(0, CONTINUATION));

					// Because we're resuming, the arguments don't actually matter. The
					// analysis on the other end of the method will put the local
					// variables into the right place.
					List<JvmType> paramTypes = invoke.type.parameterTypes();
					for (int j = 0; j < paramTypes.size(); ++j) {
						bytecodes.add(i++, addNullValue(paramTypes.get(j)));
					}
					
					// This label is just for the first-time skip commented above.
					bytecodes.add(i++, new Label("invoke" + location));

					// Now the method has been invoked, this method needs to check if
					// it caused the actor to yield.
					bytecodes.add(++i, new Bytecode.Load(0, CONTINUATION));
					bytecodes.add(++i, new Invoke(CONTINUATION, "isYielding",
							new Function(T_BOOL), Bytecode.VIRTUAL));
					bytecodes.add(++i, new If(If.EQ, "skip" + location));

					// Pop the irrelevant return value.
					if (!ignoreReturn) {
						bytecodes.add(++i, new Pop(JvmTypes.JAVA_LANG_OBJECT));
					}

					i = addYield(method, bytecodes, i, location, frame, ignores);

					bytecodes.add(++i, new Label("skip" + location));

					location += 1;
				}
			}

			// If code has been added, the exception table needs to be updated.
			if (i != original) {
				int diff = i - original;

				for (Handler handler : code.handlers()) {
					if (handler.start <= original && handler.end > original) {
						handler.end += diff;
					} else if (handler.start > original) {
						handler.start += diff;
						handler.end += diff;
					}
				}
			}
		}

		// If the method may resume at some point, then the start needs to be
		// updated in order to cause the next invocation to jump to the right
		// point in the code.
		if (location > 0) {
			int i = -1;

			bytecodes.add(++i, new Bytecode.Load(0, CONTINUATION));
			bytecodes.add(++i, new Invoke(CONTINUATION, "location",
					new Function(T_INT), Bytecode.VIRTUAL));

			List<Pair<Integer, String>> cases =
					new ArrayList<Pair<Integer, String>>(location);
			for (int j = 0; j < location; ++j) {
				cases.add(new Pair<Integer, String>(j, "resume" + j));
			}

			bytecodes.add(++i, new Switch("begin", cases));
			bytecodes.add(++i, new Label("begin"));

			for (Handler handler : code.handlers()) {
				handler.start += i;
				handler.end += i;
			}
		}
	}

	private int addYield(Method method, List<Bytecode> bytecodes, int i,
			int location, StackMapTable.Frame frame, int ignores) {
		
		bytecodes.add(++i, new Bytecode.Load(0, CONTINUATION));

		bytecodes.add(++i, new LoadConst(location));
		bytecodes.add(++i, new Invoke(CONTINUATION, "yield", new Function(
				T_VOID, T_INT), Bytecode.VIRTUAL));

		// TODO: incorporate liveness information here so that we don't store
		// variables which are no longer live. This would help to cut down
		// potentially expensive boxing operations. It also simply reduces the
		// number of bytecode instructions required to implement the yield.
		for (int var = 1; var != frame.numLocals; var++) {
			JvmType type = frame.types[var];
			if (!type.equals(JvmTypes.T_VOID)) {
				bytecodes.add(++i, new Bytecode.Load(0, CONTINUATION));
				bytecodes.add(++i, new LoadConst(var));
				bytecodes.add(++i, new Load(var, type));

				if (type instanceof Reference) {
					type = JAVA_LANG_OBJECT;
				}

				bytecodes.add(++i, new Invoke(CONTINUATION, "set", new Function(T_VOID, T_INT,
						type), Bytecode.VIRTUAL));
			}
		}
		
		int length = frame.types.length;
		System.out.println("FRAME: " + Arrays.toString(frame.types) + " : " + frame.numLocals);
		for (int j = (length - ignores - 1); j >= frame.numLocals; --j) {
			JvmType type = frame.types[j];
			bytecodes.add(++i, new Bytecode.Load(0, CONTINUATION));
			bytecodes.add(++i, new Swap());
			System.out.println("SAVING: " + type);
			if (type instanceof Reference) {
				type = JAVA_LANG_OBJECT;
			}

			bytecodes.add(++i, new Invoke(CONTINUATION, "push", new Function(
					T_VOID, type), Bytecode.VIRTUAL));
		}

		JvmType returnType = method.type().returnType();
		if (returnType.equals(T_VOID)) {
			bytecodes.add(++i, new Return(null));
		} else {
			bytecodes.add(++i, addNullValue(returnType));
			bytecodes.add(++i, new Return(returnType));
		}

		return i;
	}

	private int addResume(List<Bytecode> bytecodes, int i, int location,
			StackMapTable.Frame frame, int ignores) {
		bytecodes.add(++i, new Label("resume" + location));
		int length = frame.types.length;

		for (int j = frame.numLocals; j < (length - ignores); ++j) {
			JvmType type = frame.types[j];
			JvmType methodType = type;
			bytecodes.add(++i, new Bytecode.Load(0, CONTINUATION));

			String name;
			System.out.println("RESTORING: " + type);
			if (type instanceof Reference) {
				name = "popObject";
				methodType = JAVA_LANG_OBJECT;
			} else {
				// This is a bit of a hack. Method names in Yielder MUST match the
				// class names in JvmType.
				name = "pop" + type.getClass().getSimpleName();
			}

			bytecodes.add(++i, new Invoke(CONTINUATION, name, new Function(methodType),
					Bytecode.VIRTUAL));
			if (type instanceof Reference) {
				bytecodes.add(++i, new CheckCast(type));
			}
		}

		for (int var = 1; var != frame.numLocals; var++) {
			JvmType type = frame.types[var];
			if (!type.equals(JvmTypes.T_VOID)) {
				JvmType methodType = type;
				bytecodes.add(++i, new Bytecode.Load(0, CONTINUATION));
				bytecodes.add(++i, new LoadConst(var));

				String name;
				if (type instanceof Reference) {
					name = "getObject";
					methodType = JAVA_LANG_OBJECT;
				} else {
					// This is a bit of a hack. Method names in Yielder MUST match the
					// class names in JvmType.
					name = "get" + type.getClass().getSimpleName();
				}

				bytecodes.add(++i, new Invoke(CONTINUATION, name, new Function(methodType,
						T_INT), Bytecode.VIRTUAL));
				if (type instanceof Reference) {
					bytecodes.add(++i, new CheckCast(type));
				}
				bytecodes.add(++i, new Store(var, type));
			}
		}

		bytecodes.add(++i, new Bytecode.Load(0, CONTINUATION));
		bytecodes.add(++i, new Invoke(CONTINUATION, "restored", new Function(T_VOID),
				Bytecode.VIRTUAL));

		return i;
	}

	private Bytecode addNullValue(JvmType type) {
		Object value;

		if (type instanceof Reference) {
			value = null;
		} else if (type instanceof JvmType.Bool) {
			value = false;
		} else if (type instanceof JvmType.Char) {
			value = '\u0000';
		} else if (type instanceof JvmType.Double) {
			value = 0.0;
		} else if (type instanceof JvmType.Int) {
			value = 0;
		} else if (type instanceof JvmType.Float) {
			value = 0f;
		} else if (type instanceof JvmType.Long) {
			value = 0l;
		} else {
			throw new UnsupportedOperationException("Unknown primitive type.");
		}

		return new LoadConst(value);
	}

	private boolean canYield(Invoke invoke) {
		// TODO Analyse the method body (how?) to tell if the method will actually
		// yield at any point. The current implementation just cuts out obvious
		// cases where there will be no yield.
		String pkg = invoke.owner.pkg();

		if (pkg.startsWith("wyjc")) {
			return pkg.equals("wyjc.runtime") && invoke.name.startsWith("send");
		}
		
		if (invoke.mode == Bytecode.STATIC && !pkg.startsWith("java")) {
			return true;
		}

		List<JvmType> params = invoke.type.parameterTypes();
		
		return params.size() > 0 && params.get(0).equals(CONTINUATION);
	}

}
