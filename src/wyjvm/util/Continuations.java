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

package wyjvm.util;

import static wyjvm.lang.JvmTypes.JAVA_LANG_OBJECT;
import static wyjvm.lang.JvmTypes.T_BOOL;
import static wyjvm.lang.JvmTypes.T_INT;
import static wyjvm.lang.JvmTypes.T_VOID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import wyil.util.Pair;
import wyjvm.attributes.Code;
import wyjvm.attributes.Code.Handler;
import wyjvm.lang.Bytecode;
import wyjvm.lang.Bytecode.CheckCast;
import wyjvm.lang.Bytecode.Goto;
import wyjvm.lang.Bytecode.If;
import wyjvm.lang.Bytecode.Invoke;
import wyjvm.lang.Bytecode.Label;
import wyjvm.lang.Bytecode.Load;
import wyjvm.lang.Bytecode.LoadConst;
import wyjvm.lang.Bytecode.Return;
import wyjvm.lang.Bytecode.Store;
import wyjvm.lang.Bytecode.Swap;
import wyjvm.lang.Bytecode.Switch;
import wyjvm.lang.BytecodeAttribute;
import wyjvm.lang.ClassFile;
import wyjvm.lang.ClassFile.Method;
import wyjvm.lang.JvmType;
import wyjvm.lang.JvmType.Bool;
import wyjvm.lang.JvmType.Char;
import wyjvm.lang.JvmType.Clazz;
import wyjvm.lang.JvmType.Function;
import wyjvm.lang.JvmType.Int;
import wyjvm.lang.JvmType.Reference;
import wyjvm.util.dfa.StackAnalysis;
import wyjvm.util.dfa.VariableAnalysis;

/**
 * Bytecode rewriter that adds yield and resumption points on actor
 * continuations.
 * 
 * @author Timothy Jones
 */
public class Continuations {

	private static final Clazz STRAND = new Clazz("wyjc.runtime.concurrency",
	    "Strand"), MESSAGER = new Clazz("wyjc.runtime.concurrency", "Messager"),
	    YIELDER = new Clazz("wyjc.runtime.concurrency", "Yielder");

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

		int location = 0;

		VariableAnalysis variableAnalysis = new VariableAnalysis(method);
		StackAnalysis stackAnalysis = new StackAnalysis(method);

		List<Handler> handlers = code.handlers();
		
		for (int i = 0; i < bytecodes.size(); ++i) {
			Bytecode bytecode = bytecodes.get(i);
			
			int original = i;

			if (bytecode instanceof Invoke) {
				Invoke invoke = (Invoke) bytecode;
				String name = invoke.name;

				if (invoke.owner.equals(MESSAGER) && name.equals("sendSync")) {
					// A strand may have to yield after a synchronous message send. If
					// the strand should yield afterwards, the stack needs to yield,
					// then
					// later resume AFTER the method call.

					i = addStrand(bytecodes, i);
					bytecodes.add(++i, new Invoke(YIELDER, "shouldYield", new Function(
					    T_BOOL), Bytecode.VIRTUAL));
					bytecodes.add(++i, new If(If.EQ, "skip" + location));

					Map<Integer, JvmType> types = variableAnalysis.typesAt(i + 1);
					Stack<JvmType> stack = stackAnalysis.typesAt(i + 1);

					i =
					    addResume(bytecodes,
					        addYield(method, bytecodes, i, location, types, stack),
					        location, types, stack);

					bytecodes.add(++i, new Label("skip" + location));

					location += 1;
				} else if (canYield(invoke)) {
					// A strand may yield inside another method. If the method returns
					// and the current strand is yielded, the stack needs to keep
					// yielding, then later resume BEFORE the method call, so it reenters
					// the yielded method.

					Map<Integer, JvmType> types = variableAnalysis.typesAt(i);
					Stack<JvmType> stack = stackAnalysis.typesAt(i);

					List<JvmType> pTypes = invoke.type.parameterTypes();
					int size = pTypes.size();

					// The types we have are from before the method was invoked. We need
					// to remove the types of the arguments from the stack.
					for (int j = 0; j < size; ++j) {
						stack.pop();
					}

					// If the code flow is arriving here for the first time, it needs to
					// skip the resume. Future resumptions will jump to the start of the
					// function to right after this goto with the following resume.
					bytecodes.add(i++, new Goto("invoke" + location));
					i = addResume(bytecodes, i - 1, location, types, stack) + 1;

					// Because we're resuming, the arguments don't actually matter. The
					// analysis on the other end of the method will put the local
					// variables into the right place.
					for (int j = 0; j < size; ++j) {
						bytecodes.add(i++, addNullValue(pTypes.get(j)));
					}

					// This label is just for the first-time skip commented above.
					bytecodes.add(i++, new Label("invoke" + location));

					// Now the method has been invoked, this method needs to check if
					// it caused the actor to yield.
					i = addStrand(bytecodes, i);
					bytecodes.add(++i, new Invoke(YIELDER, "isYielded", new Function(
					    T_BOOL), Bytecode.VIRTUAL));
					bytecodes.add(++i, new If(If.EQ, "skip" + location));

					i = addYield(method, bytecodes, i, location, types, stack);

					bytecodes.add(++i, new Label("skip" + location));

					location += 1;
				}
			}
			
			// If code has been added, the exception table needs to be updated.
			if (i != original) {
				int diff = i - original;
				
				for (Handler handler : handlers) {
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

			i = addStrand(bytecodes, i);
			bytecodes.add(++i, new Invoke(YIELDER, "getCurrentStateLocation",
			    new Function(T_INT), Bytecode.VIRTUAL));

			List<Pair<Integer, String>> cases =
			    new ArrayList<Pair<Integer, String>>(location);
			for (int j = 0; j < location; ++j) {
				cases.add(new Pair<Integer, String>(j, "resume" + j));
			}

			bytecodes.add(++i, new Switch("begin", cases));
			bytecodes.add(++i, new Label("begin"));
			
			for (Handler handler : handlers) {
				handler.start += i;
				handler.end += i;
				System.out.println(handler.start + " > " + handler.end);
			}
		}
	}

	private int addStrand(List<Bytecode> bytecodes, int i) {
		// Ugh. Until we can tell whether a Java method operates on a Whiley
		// actor,
		// this is the only way to retrieve a strand for any method.

		bytecodes.add(++i, new Bytecode.Invoke(STRAND, "getCurrentStrand",
		    new Function(STRAND), Bytecode.STATIC));

		return i;
	}

	private int addYield(Method method, List<Bytecode> bytecodes, int i,
	    int location, Map<Integer, JvmType> types, Stack<JvmType> stack) {
		i = addStrand(bytecodes, i);

		bytecodes.add(++i, new LoadConst(location));
		bytecodes.add(++i, new Invoke(YIELDER, "yield",
		    new Function(T_VOID, T_INT), Bytecode.VIRTUAL));

		for (int var : types.keySet()) {
			JvmType type = types.get(var);
			i = addStrand(bytecodes, i);
			bytecodes.add(++i, new LoadConst(var));
			bytecodes.add(++i, new Load(var, type));

			if (type instanceof Reference) {
				type = JAVA_LANG_OBJECT;
			}

			bytecodes.add(++i, new Invoke(YIELDER, "set", new Function(T_VOID, T_INT,
			    type), Bytecode.VIRTUAL));
		}

		for (int j = stack.size() - 1; j >= 0; --j) {
			JvmType type = stack.get(j);
			i = addStrand(bytecodes, i);
			bytecodes.add(++i, new Swap());

			if (type instanceof Reference) {
				type = JAVA_LANG_OBJECT;
			}

			bytecodes.add(++i, new Invoke(YIELDER, "push",
			    new Function(T_VOID, type), Bytecode.VIRTUAL));
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
	    Map<Integer, JvmType> types, Stack<JvmType> stack) {
		bytecodes.add(++i, new Label("resume" + location));

		for (JvmType type : stack) {
			JvmType methodType = type;
			i = addStrand(bytecodes, i);

			String name;
			if (type instanceof Reference) {
				name = "popObject";
				methodType = JAVA_LANG_OBJECT;
			} else {
				// This is a bit of a hack. Method names in Yielder MUST match the
				// class names in JvmType.
				name = "pop" + type.getClass().getSimpleName();
			}

			bytecodes.add(++i, new Invoke(YIELDER, name, new Function(methodType),
			    Bytecode.VIRTUAL));
			if (type instanceof Reference) {
				bytecodes.add(++i, new CheckCast(type));
			}
		}

		for (int var : types.keySet()) {
			JvmType type = types.get(var), methodType = type;
			i = addStrand(bytecodes, i);
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

			bytecodes.add(++i, new Invoke(YIELDER, name, new Function(methodType,
			    T_INT), Bytecode.VIRTUAL));
			if (type instanceof Reference) {
				bytecodes.add(++i, new CheckCast(type));
			}
			bytecodes.add(++i, new Store(var, type));
		}

		i = addStrand(bytecodes, i);
		bytecodes.add(++i, new Invoke(YIELDER, "unyield", new Function(T_VOID),
		    Bytecode.VIRTUAL));

		return i;
	}

	private Bytecode addNullValue(JvmType type) {
		Object value;

		if (type instanceof Reference) {
			value = null;
		} else if (type instanceof Bool) {
			value = false;
		} else if (type instanceof Char) {
			value = '\u0000';
		} else if (type instanceof JvmType.Double) {
			value = (Double) 0.0;
		} else if (type instanceof Int) {
			value = (Integer) 0;
		} else if (type instanceof JvmType.Float) {
			value = (Float) 0f;
		} else if (type instanceof JvmType.Long) {
			value = (Long) 0l;
		} else {
			throw new UnsupportedOperationException("Unknown primitive type.");
		}

		return new LoadConst(value);
	}

	private boolean canYield(Invoke invoke) {
		// TODO Analyse the method body (how?) to tell if the method will actually
		// yield at any point.
		String pkg = invoke.owner.pkg();
		return invoke.mode == Bytecode.STATIC && !pkg.startsWith("wyjc")
		    && !pkg.startsWith("java");
	}

}
