// Copyright (c) 2011, David J. Pearce (djp@ecs.vuw.ac.nz)
// All rights reserved.
//
// This software may be modified and distributed under the terms
// of the BSD license.  See the LICENSE file for details.

package wyil.util;

import wyil.lang.Type;

/**
 * <p>
 * Contains global constants mapping syntax errors to their error messages. The
 * purpose of this class is bring all syntax error messages into one place, such
 * that:
 * </p>
 * <ol>
 * <li>It's easy to change error messages, and be sure the changes are applied
 * universally.</li>
 * <li>It's easy to reflect on the set of all error messages, and perhaps find
 * ways to improve them.</li>
 * </ol>
 * <p>
 * In short, error message reporting is done poorly by modern compilers and it
 * would be nice to do a better job!
 * </p>
 *
 * @author David J. Pearce
 *
 */
public class ErrorMessages {

	private static class Msg {
		String msg;
		public Msg(String msg) {
			this.msg = msg;
		}
	}

	private static final class MsgWithNoParams extends Msg {
		public MsgWithNoParams(String msg) {
			super(msg);
		}
	}

	private static final class MsgWithStringParam extends Msg {
		public MsgWithStringParam(String msg) {
			super(msg);
		}
	}

	private static final class MsgWithTypeParam extends Msg {
		public MsgWithTypeParam(String msg) {
			super(msg);
		}
	}

	private static final class MsgWithTypeParams extends Msg {
		public MsgWithTypeParams(String msg) {
			super(msg);
		}
	}

	public static final MsgWithNoParams CYCLIC_CONSTANT_DECLARATION = new MsgWithNoParams("cyclic constant expression");
	public static final MsgWithNoParams INVALID_CONSTANT_EXPRESSION = new MsgWithNoParams("invalid constant expression");
	public static final MsgWithNoParams INVALID_BOOLEAN_EXPRESSION = new MsgWithNoParams("invalid boolean expression");
	public static final MsgWithNoParams INVALID_NUMERIC_EXPRESSION = new MsgWithNoParams("invalid numeric expression");
	public static final MsgWithNoParams INVALID_UNARY_EXPRESSION = new MsgWithNoParams("invalid unary expression");
	public static final MsgWithNoParams INVALID_BINARY_EXPRESSION = new MsgWithNoParams("invalid binary expression");
	public static final MsgWithNoParams INVALID_ARRAY_EXPRESSION  = new MsgWithNoParams("invalid array expression");
	public static final MsgWithNoParams INVALID_LVAL_EXPRESSION = new MsgWithNoParams("invalid assignment expression");
	public static final MsgWithNoParams INVALID_TUPLE_LVAL = new MsgWithNoParams("invalid tuple lval");
	public static final MsgWithNoParams INVALID_FILE_ACCESS = new MsgWithNoParams("invalid file access");
	public static final MsgWithNoParams INVALID_PACKAGE_ACCESS = new MsgWithNoParams("invalid package access");
	public static final MsgWithNoParams BREAK_OUTSIDE_SWITCH_OR_LOOP = new MsgWithNoParams("break outside switch or loop");
	public static final MsgWithNoParams CONTINUE_OUTSIDE_LOOP = new MsgWithNoParams("continue outside loop");
	public static final MsgWithStringParam RESOLUTION_ERROR = new MsgWithStringParam("unable to resolve name ($0)");
	public static final MsgWithNoParams UNKNOWN_VARIABLE = new MsgWithNoParams("unknown variable");
	public static final MsgWithNoParams UNKNOWN_FUNCTION_OR_METHOD = new MsgWithNoParams("unknown function or method");
	public static final MsgWithNoParams VARIABLE_POSSIBLY_UNITIALISED = new MsgWithNoParams("variable may be uninitialised");
	public static final MsgWithStringParam VARIABLE_ALREADY_DEFINED = new MsgWithStringParam("variable $0 already defined");
	public static final MsgWithNoParams DUPLICATE_DEFAULT_LABEL = new MsgWithNoParams("duplicate default label");
	public static final MsgWithNoParams DUPLICATE_CASE_LABEL = new MsgWithNoParams("duplicate case label");
	public static final MsgWithNoParams DEAD_CODE = new MsgWithNoParams("dead-code encountered (i.e. this statement has no effect)");
	public static final MsgWithNoParams UNREACHABLE_CODE = new MsgWithNoParams("unreachable code encountered (i.e. execution can never reach this statement)");
	public static final MsgWithNoParams ALLOCATION_NOT_PERMITTED = new MsgWithNoParams("object allocation not permitted");
	public static final MsgWithNoParams REFERENCE_NOT_PERMITTED = new MsgWithNoParams("reference not permitted");
	public static final MsgWithNoParams METHODCALL_NOT_PERMITTED = new MsgWithNoParams("method invocation not permitted");
	public static final MsgWithNoParams REFERENCE_ACCESS_NOT_PERMITTED = new MsgWithNoParams("dereference not permitted");
	public static final MsgWithTypeParams SUBTYPE_ERROR = new MsgWithTypeParams("expected type $0, found $1");
	public static final MsgWithTypeParams INCOMPARABLE_OPERANDS = new MsgWithTypeParams("incomparable operands: $0 and $1");
	public static final MsgWithTypeParam RECORD_TYPE_REQUIRED = new MsgWithTypeParam("record required, got: $0");
	public static final MsgWithTypeParam REFERENCE_TYPE_REQUIRED = new MsgWithTypeParam("reference required, got: $0");
	public static final MsgWithTypeParam FUNCTION_OR_METHOD_TYPE_REQUIRED = new MsgWithTypeParam("function or method required, got: $0");
	public static final MsgWithStringParam RECORD_MISSING_FIELD = new MsgWithStringParam("record has no field named $0");
	public static final MsgWithNoParams RETURN_FROM_VOID = new MsgWithNoParams("cannot return value from method with void return type");
	public static final MsgWithNoParams MISSING_RETURN_VALUE = new MsgWithNoParams("missing return value");
	public static final MsgWithNoParams BRANCH_ALWAYS_TAKEN = new MsgWithNoParams("branch always taken");
	public static final MsgWithTypeParams AMBIGUOUS_COERCION = new MsgWithTypeParams("ambiguous coercion (from $0 to $1)");

	/**
	 * Return the error message for an error with no parameters.
	 *
	 * @param kind
	 * @param data
	 * @return
	 */
	public static String errorMessage(MsgWithNoParams msg) {
		return msg.msg;
	}

	/**
	 * Return the error message for an error with a single string parameter.
	 *
	 * @param kind
	 * @param data
	 * @return
	 */
	public static String errorMessage(MsgWithStringParam msg, String param) {
		return msg.msg.replaceAll("\\$0",param);
	}

	/**
	 * Return the error message for an error with a single type parameter.
	 *
	 * @param kind
	 * @param data
	 * @return
	 */
	public static String errorMessage(MsgWithTypeParam msg, Type t1) {
		return msg.msg.replaceAll("\\$0",t1.toString());
	}

	/**
	 * Return the error message for an error with two type parameters.
	 *
	 * @param kind
	 * @param data
	 * @return
	 */
	public static String errorMessage(MsgWithTypeParams msg, Type t1, Type t2) {
		return msg.msg.replaceAll("\\$0",t1.toString()).replaceAll("\\$1",t2.toString());
	}
}
