package wycs.io;

import java.io.*;
import java.util.*;

import static wybs.lang.SyntaxError.*;
import wybs.util.Pair;
import wycs.lang.*;

public class WycsFilePrinter {
	public static final String INDENT = "  ";
	
	private PrintStream out;
	
	public WycsFilePrinter(PrintStream writer) {
		this.out = writer;
	}
		
	public WycsFilePrinter(OutputStream writer) {
		try {
			this.out = new PrintStream(writer, true, "UTF-8");
		} catch(UnsupportedEncodingException e) {
			this.out = new PrintStream(writer);
		}
	}
	
	public void write(WycsFile wf) {
		for(WycsFile.Declaration d : wf.declarations()) {
			write(wf, d);
			out.println();
			out.println();
		}
		out.flush();
	}	
	
	private void write(WycsFile wf, WycsFile.Declaration s) {
		if(s instanceof WycsFile.Function) {
			write((WycsFile.Function)s);
		} else if(s instanceof WycsFile.Assert) {
			write((WycsFile.Assert)s);
		} else if(s instanceof WycsFile.Import) {
			write((WycsFile.Import)s);
		} else {
			internalFailure("unknown statement encountered " + s,
					wf.filename(), s);
		}
	}
	
	private void write(WycsFile.Import s) {
		if (s.name == null) {
			out.print("import " + s.filter);
		} else {
			out.print("import " + s.name + " from " + s.filter);
		}
	}
	
	private void write(WycsFile.Function s) {
		if(s instanceof WycsFile.Define) {
			out.print("define ");
		} else {
			out.print("function ");
		}
		out.print(s.name);
		if(s.generics.size() > 0) {
			out.print("<");
			boolean firstTime=true;
			for(String g : s.generics) {
				if(!firstTime) {
					out.print(", ");
				}
				firstTime=false;
				out.print(g);
			}
			out.print(">");
		}
		boolean firstTime=true;
		out.print(s.from);
		if(!(s instanceof WycsFile.Define)) {
			out.print(" => " + s.to);
		}
		if(s.condition != null) {
			out.print(" where ");
			write(s.condition,1,true);
		}
	}
	
	private void write(WycsFile.Assert s) {
		out.print("assert ");
		if(s.message != null) {
			out.print("\"" + s.message + "\" ");
		}
		write(s.expr,1,true);
	}
	
	private void write(Expr e, int indent, boolean indented) {
		if(e instanceof Expr.Nary) {
			write((Expr.Nary)e,indent,indented);
		} else if(e instanceof Expr.Quantifier) {
			write((Expr.Quantifier)e,indent,indented);
		} else if(e instanceof Expr.Binary) {
			write((Expr.Binary)e,indent,indented);
		} else {
			indent(indent,indented);
			out.print(e);
		}
	}
	
	private void write(Expr.Nary e, int indent, boolean indented) {
		switch(e.op) {
		case AND:
		case OR:
			String op = e.op == Expr.Nary.Op.AND ? "&&" : "||";
			boolean firstTime=true;
			for(Expr operand : e.operands) {
				if(!firstTime) {
					out.println(" " + op);
					indented = false;
				} else {
					firstTime = false;
				}							
				write(operand,indent,indented);
			}
			return;
		}
		indent(indent,indented);
		out.print(e.toString());
	}
	
	private void write(Expr.Binary e, int indent, boolean indented) {
		switch(e.op) {
		case IMPLIES:
			write(e.leftOperand,indent,indented);
			out.println();
			indent(indent,false);
			out.println("==>");
			write(e.rightOperand,indent+1,false);
			return;
		}
		indent(indent,indented);
		out.print(e.toString());
	}
	
	private void write(Expr.Quantifier e, int indent, boolean indented) {
		indent(indent,indented);
		
		if(e instanceof Expr.ForAll) {
			out.print("forall [ ");
		} else {
			out.print("some [ ");
		}
		
		boolean firstTime=true;
		for(SyntacticType p : e.unboundedVariables) {
			if(!firstTime) {
				out.print(", ");
			} else {
				firstTime=false;
			}
			out.print(p);
		}
		if(e.boundedVariables.size() > 0) {
			out.print(" ; ");
			firstTime=true;
			for(Pair<String,Expr> p : e.boundedVariables) {
				if(!firstTime) {
					out.print(", ");
				} else {
					firstTime=false;
				}
				out.print(p.first() + " in " + p.second());
			}
		}
		out.println(" :");
		write(e.operand,indent + 1,false);
		out.println();
		indent(indent,false);
		out.print("]");
	}
	
	private void indent(int level, boolean indented) {
		if(!indented) {
			for(int i=0;i<level;++i) {
				out.print(INDENT);
			}		
		}
	}	
}