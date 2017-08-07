// Copyright (c) 2011, David J. Pearce (djp@ecs.vuw.ac.nz)
// All rights reserved.
//
// This software may be modified and distributed under the terms
// of the BSD license.  See the LICENSE file for details.

package wyc.io;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import wyal.lang.WyalFile;
import wyc.lang.Expr;
import wyc.lang.Stmt;
import wyc.lang.WhileyFile;
import wycc.util.Pair;
import wycc.util.Triple;
import wyil.lang.*;

/**
 * Responsible for "pretty printing" a Whiley File. This is useful for
 * formatting Whiley Files. Also, it can be used to programatically generate
 * Whiley Files.
 *
 * @author David J. Pearce
 *
 */
public class WhileyFilePrinter {
	private PrintStream out;


	public WhileyFilePrinter(OutputStream stream) {
		try {
			this.out = new PrintStream(stream, true, "UTF-8");
		} catch(Exception e) {
			this.out = new PrintStream(stream);
		}
	}

	public void print(WhileyFile wf) {
		for(WhileyFile.Declaration d : wf.declarations) {
			print(d);
		}
		out.flush();
	}

	public void print(WhileyFile.Declaration decl) {
		if(decl instanceof WhileyFile.Import) {
			print((WhileyFile.Import)decl);
		} else if(decl instanceof WhileyFile.Constant) {
			print((WhileyFile.Constant)decl);
		} else if(decl instanceof WhileyFile.Type) {
			print((WhileyFile.Type)decl);
		} else if(decl instanceof WhileyFile.FunctionOrMethodOrProperty) {
			print((WhileyFile.FunctionOrMethodOrProperty)decl);
		} else {
			throw new RuntimeException("Unknown construct encountered: "
					+ decl.getClass().getName());
		}
	}

	public void print(WhileyFile.FunctionOrMethodOrProperty fm) {
		out.println();
		print(fm.modifiers());

		if(fm instanceof WhileyFile.Method) {
			out.print("method ");
		} else {
			out.print("function ");
		}

		out.print(fm.name());
		printParameters(fm.parameters);
		if (!fm.returns.isEmpty()) {
            out.print(" -> ");
            boolean needParams = fm.returns.size() > 1 || fm.returns.stream().anyMatch(r -> !r.name.equals("$"));
            printParameters(fm.returns, needParams);
        }

		for(Expr r : fm.requires) {
			out.println();
			out.print("requires ");
			print(r);
		}
		for(Expr r : fm.ensures) {
			out.println();
			out.print("ensures ");
			print(r);
		}

		out.println(":");

		print(fm.statements,1);
	}

	public void print(WhileyFile.Import decl) {
		out.print("import ");
		if(decl.name != null) {
			out.print(decl.name);
			out.print(" from ");
		}
		for(int i=0;i!=decl.filter.size();++i) {
			if(i != 0) {
				out.print(".");
			}
			String item = decl.filter.get(i);
			if(!item.equals("**")) {
				out.print(decl.filter.get(i));
			}
		}
		out.println();
	}

	public void print(WhileyFile.Constant decl) {
		out.println();
		out.print("constant ");
		out.print(decl.name());
		out.print(" is ");
		print(decl.constant);
		out.println();
	}

	public void print(WhileyFile.Type decl) {
		out.println();
		out.print("type ");
		out.print(decl.name());
		out.print(" is ");
		printParameter(decl.parameter,true);

		for(Expr invariant : decl.invariant) {
			out.print(" where ");
			print(invariant);
		}

		out.println();
	}

	public void print(List<Stmt> statements, int indent) {
		for(Stmt s : statements) {
			print(s,indent);
		}
	}

	public void print(Stmt stmt, int indent) {
		indent(indent);

		if(stmt instanceof Stmt.Assert) {
			print((Stmt.Assert) stmt);
		} else if(stmt instanceof Stmt.Assign) {
			print((Stmt.Assign) stmt);
		} else if(stmt instanceof Stmt.Assume) {
			print((Stmt.Assume) stmt);
		} else if(stmt instanceof Stmt.Break) {
			print((Stmt.Break) stmt);
		} else if(stmt instanceof Stmt.Continue) {
			print((Stmt.Continue) stmt);
		} else if(stmt instanceof Stmt.Debug) {
			print((Stmt.Debug) stmt);
		} else if(stmt instanceof Stmt.DoWhile) {
			print((Stmt.DoWhile) stmt, indent);
		} else if(stmt instanceof Stmt.IfElse) {
			print((Stmt.IfElse) stmt, indent);
		} else if(stmt instanceof Stmt.Return) {
			print((Stmt.Return) stmt);
		} else if(stmt instanceof Stmt.Skip) {
			print((Stmt.Skip) stmt);
		} else if(stmt instanceof Stmt.Switch) {
			print((Stmt.Switch) stmt, indent);
		} else if(stmt instanceof Stmt.NamedBlock) {
			print((Stmt.NamedBlock) stmt, indent);
		} else if(stmt instanceof Stmt.While) {
			print((Stmt.While) stmt, indent);
		} else if(stmt instanceof Stmt.VariableDeclaration) {
			print((Stmt.VariableDeclaration) stmt, indent);
		} else if(stmt instanceof Expr.AbstractInvoke) {
			print((Expr.AbstractInvoke) stmt);
			out.println();
		} else if (stmt instanceof Stmt.Fail) {
            print((Stmt.Fail) stmt, indent);
		} else {
			// should be dead-code
			throw new RuntimeException("Unknown statement kind encountered: "
					+ stmt.getClass().getName());
		}
	}

	public void print(Stmt.Assert s) {
		out.print("assert ");
		print(s.expr);
		out.println();
	}

	public void print(Stmt.Assume s) {
		out.print("assume ");
		printWithBrackets(s.expr);
		out.println();
	}

	public void print(Stmt.Debug s) {
		out.print("debug ");
		print(s.expr);
		out.println();
	}

	public void print(Stmt.Break s) {
		out.println("break");
	}

	public void print(Stmt.Continue s) {
		out.println("break");
	}

	public void print(Stmt.Skip s) {
		out.println("skip");
	}

	public void print(Stmt.Return s) {
		out.print("return");
		for(int i=0;i!=s.returns.size();++i) {
			if(i != 0) {
				out.print(",");
			}
			out.print(" ");
			print(s.returns.get(i));
		}
		out.println();
	}

	public void print(Stmt.Assign s) {
		for(int i=0;i!=s.lvals.size();++i) {
			if(i!=0) {
				out.print(", ");
			}
			print(s.lvals.get(i));
		}
		out.print(" = ");
		for(int i=0;i!=s.rvals.size();++i) {
			if(i!=0) {
				out.print(", ");
			}
			print(s.rvals.get(i));
		}
		out.println();
	}

	public void print(Stmt.IfElse s, int indent) {
		out.print("if ");
		print(s.condition);
		out.println(":");
		print(s.trueBranch,indent+1);
		if(!s.falseBranch.isEmpty()) {
			indent(indent);
			out.println("else:");
			print(s.falseBranch, indent+1);
		}
	}

	public void print(Stmt.DoWhile s, int indent) {
		out.println("do:");
		print(s.body,indent+1);
		indent(indent);
		// TODO: loop invariant
		out.print("while ");
		print(s.condition);
		boolean first = true;
        for(Expr i : s.invariants) {
            if (first) {
                out.println();
                indent(indent);
            }
            first = false;
            out.print(" where ");
            print(i);
        }

		out.println();
	}

	public void print(Stmt.NamedBlock s, int indent) {
		out.println(s.name + ":");
		print(s.body,indent+1);
	}

	public void print(Stmt.While s, int indent) {
		out.print("while ");
		print(s.condition);

		for(Expr i : s.invariants) {
			out.println();
            indent(indent);
			out.print("where ");
			print(i);
		}

		out.println(":");
		print(s.body,indent+1);
	}

	public void print(Stmt.Switch s, int indent) {
		out.print("switch ");
		print(s.expr);
		out.println(":");
		for(Stmt.Case cas : s.cases) {
			indent(indent+1);
			boolean firstTime = true;
			if(cas.expr.isEmpty()) {
				out.print("default");
			} else {
				out.print("case ");
				for(Expr e : cas.expr) {
					if(!firstTime) {
						out.print(", ");
					}
					firstTime=false;
					print(e);
				}
			}

			out.println(":");
			print(cas.stmts,indent+2);
		}
	}

	public void print(Stmt.VariableDeclaration s, int indent) {
		printParameter(s.parameter,false);
		if(s.expr != null) {
			out.print(" = ");
			print(s.expr);
		}
		out.println();
	}

	public void print(Stmt.Fail s, int indent) {
		out.println("fail");
	}

	public void printWithBrackets(Expr expression, Class<? extends Expr>... matches) {
		boolean withBrackets = false;
		// First, decide whether brackets are needed or not
		for(Class<? extends Expr> match : matches) {
			if(match.isInstance(expression)) {
				withBrackets = true;
				break;
			}
		}
		// Second, print with brackets if needed
		if(withBrackets) {
			out.print("(");
			print(expression);
			out.print(")");
		} else {
			print(expression);
		}
	}

	public void print(Expr expression) {
		if (expression instanceof Expr.Constant) {
			print ((Expr.Constant) expression);
		} else if (expression instanceof Expr.AbstractVariable) {
			print ((Expr.AbstractVariable) expression);
		} else if (expression instanceof Expr.ConstantAccess) {
			print ((Expr.ConstantAccess) expression);
		} else if (expression instanceof Expr.ArrayInitialiser) {
			print ((Expr.ArrayInitialiser) expression);
		} else if (expression instanceof Expr.BinOp) {
			print ((Expr.BinOp) expression);
		} else if (expression instanceof Expr.Dereference) {
			print ((Expr.Dereference) expression);
		} else if (expression instanceof Expr.Cast) {
			print ((Expr.Cast) expression);
		} else if (expression instanceof Expr.IndexOf) {
			print ((Expr.IndexOf) expression);
		} else if (expression instanceof Expr.UnOp) {
			print ((Expr.UnOp) expression);
		} else if (expression instanceof Expr.AbstractInvoke) {
			print ((Expr.AbstractInvoke) expression);
		} else if (expression instanceof Expr.IndirectFunctionCall) {
			print ((Expr.IndirectFunctionCall) expression);
		} else if (expression instanceof Expr.IndirectMethodCall) {
			print ((Expr.IndirectMethodCall) expression);
		} else if (expression instanceof Expr.Quantifier) {
			print ((Expr.Quantifier) expression);
		} else if (expression instanceof Expr.FieldAccess) {
			print ((Expr.FieldAccess) expression);
		} else if (expression instanceof Expr.Record) {
			print ((Expr.Record) expression);
		} else if (expression instanceof Expr.AbstractFunctionOrMethod) {
			print ((Expr.AbstractFunctionOrMethod) expression);
		} else if (expression instanceof Expr.Lambda) {
			print ((Expr.Lambda) expression);
		} else if (expression instanceof Expr.New) {
			print ((Expr.New) expression);
		} else if (expression instanceof Expr.TypeVal) {
			print((Expr.TypeVal) expression);
		} else if (expression instanceof Expr.AbstractIndirectInvoke) {
			print((Expr.AbstractIndirectInvoke) expression);
		} else if (expression instanceof Expr.ArrayGenerator) {
            print((Expr.ArrayGenerator) expression);
		} else {
			// should be dead-code
			throw new RuntimeException("Unknown expression kind encountered: " + expression.getClass().getName());
		}
	}

	public void print(Expr.Constant c) {
		out.print(c.value);
	}

	public void print(Expr.AbstractVariable v) {
	    if (!v.var.equals("$")) {
            out.print(v);
        }
	}

	public void print(Expr.ConstantAccess v) {
		if(v.qualification != null) {
			out.print(v.qualification + "." + v.name);
		} else {
			out.print(v.name);
		}
	}

	public void print(Expr.ArrayInitialiser e) {
		out.print("[");
		boolean firstTime = true;
		for(Expr i : e.arguments) {
			if(!firstTime) {
				out.print(", ");
			}
			firstTime=false;
			print(i);
		}
		out.print("]");
	}

	public void print(Expr.ArrayGenerator e) {
		out.print("[");
		print(e.element);
		out.print(";");
		print(e.count);
		out.print("]");
	}

	public void print(Expr.BinOp e) {
		printWithBrackets(e.lhs, Expr.BinOp.class, Expr.Cast.class, Expr.UnOp.class);
		out.print(" ");
		out.print(e.op);
		out.print(" ");
		printWithBrackets(e.rhs, Expr.BinOp.class, Expr.Cast.class, Expr.UnOp.class);
	}

	public void print(Expr.Dereference e) {
		out.print("*");
		print(e.src);
	}

	public void print(Expr.Cast e) {
		out.print("(");
		print(e.unresolvedType);
		out.print(") ");
		printWithBrackets(e.expr,Expr.BinOp.class,Expr.Cast.class);
	}

	public void print(Expr.IndexOf e) {
		print(e.src);
		out.print("[");
		print(e.index);
		out.print("]");
	}

	public void print(Expr.UnOp e) {
		switch(e.op) {
		case NEG:
			out.print("-");
			break;
		case NOT:
			out.print("!");
			break;
		case INVERT:
			out.print("~");
			break;
		case ARRAYLENGTH:
			out.print("|");
			print(e.mhs);
			out.print("|");
			return;
		}
		printWithBrackets(e.mhs,Expr.BinOp.class,Expr.Cast.class);
	}

	public void print(Expr.AbstractInvoke e) {
		if(e.qualification != null) {
			out.print(e.qualification.toString());
			out.print(".");
		}
		out.print(e.name);
		if (e.lifetimeArguments != null && !e.lifetimeArguments.isEmpty()) {
			out.print("<");
			boolean firstTime = true;
			for (String lifetime : e.lifetimeArguments) {
				if (!firstTime) {
					out.print(", ");
				}
				firstTime = false;
				out.print(lifetime);
			}
			out.print(">");
		}
		out.print("(");
		boolean firstTime = true;
		for(Expr i : e.arguments) {
			if(!firstTime) {
				out.print(", ");
			}
			firstTime=false;
			print(i);
		}
		out.print(")");
	}

	public void print(Expr.IndirectFunctionCall e) {
		print(e.src);
		if (!e.lifetimeArguments.isEmpty()) {
			out.print("<");
			boolean firstTime = true;
			for (String lifetime : e.lifetimeArguments) {
				if (!firstTime) {
					out.print(", ");
				}
				firstTime = false;
				out.print(lifetime);
			}
			out.print(">");
		}
		out.print("(");
		boolean firstTime = true;
		for(Expr i : e.arguments) {
			if(!firstTime) {
				out.print(", ");
			}
			firstTime=false;
			print(i);
		}
		out.print(")");
	}

	public void print(Expr.IndirectMethodCall e) {
		print(e.src);
		if (!e.lifetimeArguments.isEmpty()) {
			out.print("<");
			boolean firstTime = true;
			for (String lifetime : e.lifetimeArguments) {
				if (!firstTime) {
					out.print(", ");
				}
				firstTime = false;
				out.print(lifetime);
			}
			out.print(">");
		}
		out.print("(");
		boolean firstTime = true;
		for(Expr i : e.arguments) {
			if(!firstTime) {
				out.print(", ");
			}
			firstTime=false;
			print(i);
		}
		out.print(")");
	}

	public void print(Expr.Quantifier e) {
		switch(e.cop) {
		case SOME:
			out.print("some ");
			break;
		case ALL:
			out.print("all ");
			break;
		}

		out.print("{ ");

		boolean firstTime=true;
		for(Pair<String,Expr> src : e.sources) {
			if(!firstTime) {
				out.print(", ");
			}
			firstTime=false;
			out.print(src.first());
			out.print(" in ");
            print(src.second());
			if (src instanceof Triple) {
                out.print("..");
                Triple<String, Expr, Expr> tripe = (Triple) src;
                print((Expr) tripe.third());
            }
		}
		out.print(" | ");
		print(e.condition);

		out.print(" }");
	}

	public void print(Expr.FieldAccess e) {
		if(e.src instanceof Expr.Dereference) {
			printWithBrackets(((Expr.Dereference)e.src).src,Expr.New.class);
			out.print("->");
			out.print(e.name);
		} else {
			printWithBrackets(e.src,Expr.New.class);
			if (!e.src.toString().equals("$")) {
                out.print(".");
            }
			out.print(e.name);
		}
	}

	public void print(Expr.Record e) {
		out.print("{");
		boolean firstTime = true;
		for(Map.Entry<String,Expr> i : e.fields.entrySet()) {
			if(!firstTime) {
				out.print(", ");
			}
			firstTime=false;
			out.print(i.getKey());
			out.print(": ");
			print(i.getValue());
		}
		out.print("}");
	}

	public void print(Expr.AbstractFunctionOrMethod e) {
		out.print("&");
		out.print(e.name);
		if(e.paramTypes != null && e.paramTypes.size() > 0) {
			out.print("(");
			boolean firstTime = true;
			for(WyalFile.Type t : e.paramTypes) {
				if(!firstTime) {
					out.print(", ");
				}
				firstTime=false;
				print(t);
			}
			out.print(")");
		}
	}

	public void print(Expr.Lambda e) {
		out.print("&");
		if (!e.contextLifetimes.isEmpty()) {
			out.print("[");
			boolean firstTime = true;
			for (String lifetime : e.contextLifetimes) {
				if (!firstTime) {
					out.print(", ");
				}
				firstTime = false;
				out.print(lifetime);
			}
			out.print("]");
		}
		if (!e.lifetimeParameters.isEmpty()) {
			out.print("<");
			boolean firstTime = true;
			for (String lifetime : e.lifetimeParameters) {
				if (!firstTime) {
					out.print(", ");
				}
				firstTime = false;
				out.print(lifetime);
			}
			out.print(">");
		}
		out.print("(");
		boolean firstTime = true;
		for(WhileyFile.Parameter p : e.parameters) {
			if(!firstTime) {
				out.print(", ");
			}
			firstTime=false;
			print(p.type);
			out.print(" ");
			out.print(p.name);
		}
		out.print(" -> ");
		print(e.body);
		out.print(")");
	}

	public void print(Expr.New e) {
		out.print("new ");
		print(e.expr);
	}

	public void print(Expr.TypeVal e) {
		print(e.unresolvedType);
	}

	public void print(Expr.AbstractIndirectInvoke e) {
		// TODO
        print(e.src);
        out.print("(");
		boolean firstTime = true;
		for(Expr arg : e.arguments) {
			if(!firstTime) {
				out.print(", ");
			}
			firstTime=false;
			print(arg);
		}
        out.print(")");
	}

    private void printParameters(List<WhileyFile.Parameter> parameters, boolean brackets) {
	    if (brackets) {
            out.print("(");
        }
        boolean firstTime = true;
        for(int i = 0; i < parameters.size();++i) {
            WhileyFile.Parameter p = parameters.get(i);
            if(!firstTime) {
                out.print(", ");
            }
            firstTime=false;
            printParameter(p,false);
        }
        if (brackets) {
            out.print(")");
        }
    }
	private void printParameters(List<WhileyFile.Parameter> parameters) {
	    printParameters(parameters, true);
	}

	private void printParameter(WhileyFile.Parameter parameter, boolean braces) {
		braces &= (parameter.name == null ||  !parameter.name.equals("$"));
		if(braces) {
			out.print("(");
		}
		print(parameter.type);
		if (!parameter.name.equals("$")) {
            out.print(" ");
            out.print(parameter.name);
        }
		if(braces) {
			out.print(")");
		}
	}

	public void print(List<Modifier> modifiers) {
		for(Modifier m : modifiers) {
			out.print(m);
			out.print(" ");
		}
	}


	public void print(WyalFile.Type t) {
		if(t instanceof WyalFile.Type.Any) {
			out.print("any");
		} else if(t instanceof WyalFile.Type.Bool) {
			out.print("bool");
		} else if(t instanceof WyalFile.Type.Byte) {
			out.print("byte");
		} else if(t instanceof WyalFile.Type.Int) {
			out.print("int");
		} else if(t instanceof WyalFile.Type.Null) {
			out.print("null");
		} else if(t instanceof WyalFile.Type.Void) {
			out.print("void");
		} else if (t instanceof WyalFile.Type.Nominal) {
			WyalFile.Type.Nominal nt = (WyalFile.Type.Nominal) t;
			boolean firstTime = true;
			for (WyalFile.Identifier name : nt.getName().getComponents()) {
				if (!firstTime) {
					out.print(".");
				}
				firstTime = false;
				out.print(name);
			}
		} else if(t instanceof WyalFile.Type.Array) {
            WyalFile.Type type = ((WyalFile.Type.Array)t).getElement();
            boolean braces = false;
            braces |= type instanceof WyalFile.Type.Union;
            if (braces) {
                out.print("(");
            }
            print(type);
            if (braces) {
                out.print(")");
            }

            out.print("[");
			out.print("]");
		} else if(t instanceof WyalFile.Type.FunctionOrMethodOrProperty) {
			WyalFile.Type.FunctionOrMethodOrProperty tt = (WyalFile.Type.FunctionOrMethodOrProperty) t;

			if(t instanceof WyalFile.Type.Function) {
				out.print("function ");
			} else if(t instanceof WyalFile.Type.Method) {
				out.print("method ");
			} else {
				out.print("property ");
			}
//			if (!tt.contextLifetimes.isEmpty()) {
//				out.print("[");
//				boolean firstTime = true;
//				for (String lifetime : tt.contextLifetimes) {
//					if (!firstTime) {
//						out.print(", ");
//					}
//					firstTime = false;
//					out.print(lifetime);
//				}
//				out.print("]");
//			}
//			if (!tt.lifetimeParameters.isEmpty()) {
//				out.print("<");
//				boolean firstTime = true;
//				for (String lifetime : tt.lifetimeParameters) {
//					if (!firstTime) {
//						out.print(", ");
//					}
//					firstTime = false;
//					out.print(lifetime);
//				}
//				out.print(">");
//			}
			printParameterTypes(tt.getParameters());
			out.print("->");
			printParameterTypes(tt.getReturns());
		} else if(t instanceof WyalFile.Type.Record) {
			WyalFile.Type.Record tt = (WyalFile.Type.Record) t;
			out.print("{");
			boolean firstTime = true;
			for(WyalFile.FieldDeclaration fd : tt.getFields()) {
				if(!firstTime) {
					out.print(", ");
				}
				firstTime=false;
				print(fd.getType());
				out.print(" ");
				out.print(fd.getVariableName());
			}
			if(tt.isOpen()) {
				out.print(", ...");
			}
			out.print("}");
		} else if(t instanceof WyalFile.Type.Reference) {
		    WyalFile.Type type = ((WyalFile.Type.Reference) t).getElement();
			boolean braces = false;
			braces |= type instanceof WyalFile.Type.Reference;
            out.print("&");
			if (braces) {
                out.print("(");
            }
			print(type);
            if (braces) {
                out.print(")");
            }
		} else if(t instanceof WyalFile.Type.Negation) {
			out.print("!");
			print(((WyalFile.Type.Negation) t).getElement());
		} else if(t instanceof WyalFile.Type.Union) {
			WyalFile.Type.Union ut = (WyalFile.Type.Union) t;
            //out.print("(");
			boolean firstTime = true;
			for(WyalFile.Type et : ut.getOperands()) {
				if(!firstTime) {
					out.print(" | ");
				}
				firstTime=false;
				print(et);
			}
            //out.print(")");
		} else if(t instanceof WyalFile.Type.Intersection) {
			WyalFile.Type.Intersection ut = (WyalFile.Type.Intersection) t;
			boolean firstTime = true;
			for(WyalFile.Type et : ut.getOperands()) {
				if(!firstTime) {
					out.print(" & ");
				}
				firstTime=false;
				print(et);
			}
		} else {
			// should be dead-code
			throw new RuntimeException("Unknown type kind encountered: " + t.getClass().getName());
		}
	}

	private void printParameterTypes(WyalFile.Tuple<WyalFile.Type> parameters) {
		out.print("(");
		boolean firstTime = true;
		for(int i = 0; i < parameters.size();++i) {
			WyalFile.Type p = parameters.getOperand(i);
			if(!firstTime) {
				out.print(", ");
			}
			firstTime=false;
			print(p);
		}
		out.print(")");
	}


	public void indent(int level) {
		for(int i=0;i!=level;++i) {
			out.print("    ");
		}
	}
}
