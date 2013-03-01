package wycs.lang;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import wybs.lang.Attribute;
import wybs.util.Pair;
import wybs.lang.SyntacticElement;
import wycs.io.Lexer;

public abstract class Expr extends SyntacticElement.Impl implements SyntacticElement {
	
	public Expr(Attribute... attributes) {
		super(attributes);
	}
	
	public Expr(Collection<Attribute> attributes) {
		super(attributes);
	}
	
	// ==================================================================
	// Constructors
	// ==================================================================
	
	public static Variable Variable(String name, Attribute... attributes) {
		return new Variable(name,attributes);
	}
	
	public static Variable Variable(String name, Collection<Attribute> attributes) {
		return new Variable(name,attributes);
	}
	
	public static Constant Constant(Value value, Attribute... attributes) {
		return new Constant(value,attributes);
	}
	
	public static Constant Constant(Value value, Collection<Attribute> attributes) {
		return new Constant(value,attributes);
	}
	
	public static Unary Unary(Unary.Op op, Expr operand, Attribute... attributes) {
		return new Unary(op, operand,attributes);
	}
	
	public static Unary Unary(Unary.Op op, Expr operand, Collection<Attribute> attributes) {
		return new Unary(op, operand,attributes);
	}
	
	public static Binary Binary(Binary.Op op, Expr leftOperand, Expr rightOperand, Attribute... attributes) {
		return new Binary(op, leftOperand, rightOperand, attributes);
	}
	
	public static Binary Binary(Binary.Op op, Expr leftOperand, Expr rightOperand, Collection<Attribute> attributes) {
		return new Binary(op, leftOperand, rightOperand, attributes);
	}
	
	public static Nary Nary(Nary.Op op, Expr[] operands, Attribute... attributes) {
		return new Nary(op, operands, attributes);
	}
	
	public static Nary Nary(Nary.Op op, Expr[] operands, Collection<Attribute> attributes) {
		return new Nary(op, operands, attributes);
	}
	
	public static Nary Nary(Nary.Op op, Collection<Expr> operands, Attribute... attributes) {
		return new Nary(op, operands, attributes);
	}
	
	public static Nary Nary(Nary.Op op, Collection<Expr> operands, Collection<Attribute> attributes) {
		return new Nary(op, operands, attributes);
	}
	
	public static TupleLoad TupleLoad(Expr src, int index, Attribute... attributes) {
		return new TupleLoad(src, index, attributes);
	}
	
	public static TupleLoad TupleLoad(Expr src, int index, Collection<Attribute> attributes) {
		return new TupleLoad(src, index, attributes);
	}
	
	public static FunCall FunCall(String name, SyntacticType[] generics, Expr operand, Attribute... attributes) {
		return new FunCall(name,generics,operand,attributes);
	}
	
	public static FunCall FunCall(String name, SyntacticType[] generics, Expr operand, Collection<Attribute> attributes) {
		return new FunCall(name,generics,operand,attributes);
	}
	
	public static ForAll ForAll(Collection<SyntacticType> unboundedVariables,
			Collection<Pair<String, Expr>> boundedVariables, Expr expr,
			Attribute... attributes) {
		return new ForAll(unboundedVariables,boundedVariables,expr,attributes);
	}
	
	public static ForAll ForAll(Collection<SyntacticType> unboundedVariables,
			Collection<Pair<String, Expr>> boundedVariables, Expr expr, Collection<Attribute> attributes) {
		return new ForAll(unboundedVariables,boundedVariables,expr,attributes);
	}
	
	public static Exists Exists(Collection<SyntacticType> unboundedVariables,
			Collection<Pair<String, Expr>> boundedVariables, Expr expr, Attribute... attributes) {
		return new Exists(unboundedVariables,boundedVariables,expr,attributes);
	}
	
	public static Exists Exists(Collection<SyntacticType> unboundedVariables,
			Collection<Pair<String, Expr>> boundedVariables, Expr expr, Collection<Attribute> attributes) {
		return new Exists(unboundedVariables,boundedVariables,expr,attributes);
	}
	
	// ==================================================================
	// Classes
	// ==================================================================
	
	public static class Variable extends Expr {
		public final String name;
				
		private Variable(String name, Attribute... attributes) {
			super(attributes);
			this.name = name;
		}
		
		private Variable(String name, Collection<Attribute> attributes) {
			super(attributes);
			this.name = name;
		}
		
		public String toString() {
			return name;
		}
	}
	
	public static class Constant extends Expr {
		public final Value value;
		
		private Constant(Value value, Attribute... attributes) {
			super(attributes);
			this.value = value;
		}
		
		private Constant(Value value, Collection<Attribute> attributes) {
			super(attributes);
			this.value = value;
		}
		
		public String toString() {
			return value.toString();
		}
	}
	
	public static class Unary extends Expr {
		public enum Op {
			NOT(0),			
			NEG(1),			
			LENGTHOF(2);
						
			public int offset;

			private Op(int offset) {
				this.offset = offset;
			}			
		}
		
		public final Op op;
		public Expr operand;
		
		private Unary(Op op, Expr expr, Attribute... attributes) {
			super(attributes);			
			this.op = op;
			this.operand = expr;
		}
		
		private Unary(Op op, Expr expr, Collection<Attribute> attributes) {
			super(attributes);			
			this.op = op;
			this.operand = expr;
		}
		
		public String toString() {
			String o = operand.toString();
			if(needsBraces(operand)) {
				o = "(" + o + ")";
			}
			switch(this.op) {
			case NOT:
				return "!" + o;
			case NEG:
				return "-" + o;
			case LENGTHOF:
				return "|" + o + "|";
			}
			return null;
		}
	}
		
	public static class Binary extends Expr {
		public enum Op {			
			ADD(1) {
				public String toString() {
					return "+";
				}
			},
			SUB(2) {
				public String toString() {
					return "-";
				}
			},
			MUL(3) {
				public String toString() {
					return "*";
				}
			},
			DIV(4) {
				public String toString() {
					return "/";
				}
			},
			REM(5) {
				public String toString() {
					return "%";
				}
			},
			EQ(6) {
				public String toString() {
					return "==";
				}
			},
			NEQ(7) {
				public String toString() {
					return "!=";
				}
			},
			IMPLIES(8) {
				public String toString() {
					return "==>";
				}
			},
			IFF(9) {
				public String toString() {
					return "<==>";
				}
			},
			LT(10) {
				public String toString() {
					return "<";
				}
			},
			LTEQ(11) {
				public String toString() {
					return Character.toString(Lexer.UC_LESSEQUALS);
				}				
			},
			GT(12) {
				public String toString() {
					return ">";
				}
			},
			GTEQ(13) {
				public String toString() {
					return Character.toString(Lexer.UC_GREATEREQUALS);					
				}
			},
			IN(14) {
				public String toString() {
					return Character.toString(Lexer.UC_ELEMENTOF);
				}
			},
			SUBSET(14) {
				public String toString() {
					return Character.toString(Lexer.UC_SUBSET);
				}
			},
			SUBSETEQ(15) {
				public String toString() {
					return Character.toString(Lexer.UC_SUBSETEQ);
				}
			},
			SUPSET(16) {
				public String toString() {
					return Character.toString(Lexer.UC_SUPSET);
				}
			},
			SUPSETEQ(17) {
				public String toString() {
					return Character.toString(Lexer.UC_SUPSETEQ);
				}
			};
			
			public int offset;

			private Op(int offset) {
				this.offset = offset;
			}
		};

		public final Op op;
		public Expr leftOperand;
		public Expr rightOperand;
		
		private Binary(Op op, Expr lhs, Expr rhs, Attribute... attributes) {
			super(attributes);
			this.op = op;
			this.leftOperand = lhs;
			this.rightOperand = rhs;
		}
		
		private Binary(Op op, Expr lhs, Expr rhs, Collection<Attribute> attributes) {
			super(attributes);
			this.op = op;
			this.leftOperand = lhs;
			this.rightOperand = rhs;
		}
		
		public String toString() {
			String lhs = leftOperand.toString();
			String rhs = rightOperand.toString();
			if(needsBraces(leftOperand)) {
				lhs = "(" + lhs + ")";
			}
			if(needsBraces(rightOperand)) {
				rhs = "(" + rhs + ")";
			}			
			return lhs + " " + op + " " + rhs;			
		}
	}
	
	public static class Nary extends Expr {
		public enum Op {
			AND(0),
			OR(1),
			SET(2),
			TUPLE(3);			
					
			public int offset;

			private Op(int offset) {
				this.offset = offset;
			}			
		}
		
		public final Op op;
		public final Expr[] operands;
		
		public Nary(Op op, Expr[] operands, Attribute... attributes) {
			super(attributes);			
			this.op = op;
			this.operands = operands;
		}
		
		public Nary(Op op, Expr[] operands, Collection<Attribute> attributes) {
			super(attributes);			
			this.op = op;
			this.operands = operands;
		}
		
		public Nary(Op op, Collection<Expr> operands, Attribute... attributes) {
			super(attributes);			
			this.op = op;
			this.operands = new Expr[operands.size()];
			int i = 0;
			for(Expr e : operands) {
				this.operands[i++] = e;
			}
		}
		
		public Nary(Op op, Collection<Expr> operands, Collection<Attribute> attributes) {
			super(attributes);			
			this.op = op;
			this.operands = new Expr[operands.size()];
			int i = 0;
			for(Expr e : operands) {
				this.operands[i++] = e;
			}
		}
		
		public String toString() {
			String beg;
			String end;
			String sep;
			switch(this.op) {
			case AND:
				beg = "";
				end = "";
				sep = " && ";
				break;
			case OR:
				beg = "";
				end = "";
				sep = " || ";
				break;
			case SET:
				beg = "{";
				end = "}";
				sep = ", ";
				break;
			case TUPLE:
				beg = "(";
				end = ")";
				sep = ", ";
				break;						
			default:
				return "";
			}

			String r = beg;
			for(int i=0;i!=operands.length;++i) {
				if(i != 0) {
					r = r + sep;
				}		
				String os = operands[i].toString();
				if(needsBraces(operands[i])) {
					r = r + "(" + operands[i].toString() + ")";	
				} else {
					r = r + operands[i].toString();
				}
			}
			return r + end;
		}
	}	
	
	public static class TupleLoad extends Expr {
		public Expr operand;
		public int index;
		
		private TupleLoad(Expr expr, int index, Attribute... attributes) {
			super(attributes);			
			this.operand = expr;
			this.index = index;
		}
		
		private TupleLoad(Expr expr, int index, Collection<Attribute> attributes) {
			super(attributes);			
			this.index = index;
			this.operand = expr;
		}
		
		public String toString() {
			return operand + "[" + index + "]";
		}
	}
	
	public static class FunCall extends Expr {
		public final SyntacticType[] generics;
		public final Expr operand;
		public final String name;
		
		private FunCall(String name, SyntacticType[] generics, Expr operand, Attribute... attributes) {
			super(attributes);			
			this.name = name;
			this.generics = generics;
			this.operand = operand;
		}
		
		private FunCall(String name, SyntacticType[] generics, Expr operand, Collection<Attribute> attributes) {
			super(attributes);			
			this.name = name;
			this.generics = generics;
			this.operand = operand;
		}
				
		public String toString() {
			String r = name;
			if(generics.length > 0) {
				r = r + "<";
				for(int i=0;i!=generics.length;++i) {
					if(i != 0) { r += ", "; }									
					r += generics[i];
				}
				r = r + ">";
			}
			return r + operand;
		}
	}
	
	public static abstract class Quantifier extends Expr {
		public final List<SyntacticType> unboundedVariables;
		public final List<Pair<String,Expr>> boundedVariables;
		public Expr operand;
		
		private Quantifier(Collection<SyntacticType> unboundedVariables,
				Collection<Pair<String, Expr>> boundedVariables, Expr operand,
				Attribute... attributes) {
			super(attributes);			
			this.unboundedVariables = new CopyOnWriteArrayList<SyntacticType>(unboundedVariables);
			this.boundedVariables = new CopyOnWriteArrayList<Pair<String, Expr>>(boundedVariables);
			this.operand = operand;
		}
		
		private Quantifier(Collection<SyntacticType> unboundedVariables,
				Collection<Pair<String, Expr>> boundedVariables, Expr operand, Collection<Attribute> attributes) {
			super(attributes);			
			this.unboundedVariables = new CopyOnWriteArrayList<SyntacticType>(unboundedVariables);
			this.boundedVariables = new CopyOnWriteArrayList<Pair<String, Expr>>(boundedVariables);			
			this.operand = operand;
		}
		
		public String toString() {
			String r = "[ ";
			boolean firstTime = true;
			for (SyntacticType var : unboundedVariables) {
				if (!firstTime) {
					r = r + ",";
				}
				firstTime = false;
				r = r + var;
			}
			for (Pair<String,Expr> p : boundedVariables) {
				if (!firstTime) {
					r = r + ",";
				}
				firstTime = false;
				r = r + p.first() + " in " + p.second();
			}
			return r + " : " + operand + " ]";
		}
	}
	
	public static class ForAll extends Quantifier {
		private ForAll(Collection<SyntacticType> unboundedVariables,
				Collection<Pair<String, Expr>> boundedVariables, Expr expr, Attribute... attributes) {
			super(unboundedVariables, boundedVariables, expr, attributes);						
		}
		
		private ForAll(Collection<SyntacticType> unboundedVariables,
				Collection<Pair<String, Expr>> boundedVariables, Expr expr, Collection<Attribute> attributes) {
			super(unboundedVariables, boundedVariables, expr, attributes);						
		}
		
		public String toString() {
			return "all " + super.toString();
		}
	}
	
	public static class Exists extends Quantifier {
		private Exists(Collection<SyntacticType> unboundedVariables,
				Collection<Pair<String, Expr>> boundedVariables, Expr expr, Attribute... attributes) {
			super(unboundedVariables, boundedVariables, expr, attributes);						
		}
		
		private Exists(Collection<SyntacticType> unboundedVariables,
				Collection<Pair<String, Expr>> boundedVariables, Expr expr, Collection<Attribute> attributes) {
			super(unboundedVariables, boundedVariables, expr, attributes);						
		}
		
		public String toString() {
			return "exists " + super.toString();
		}
	}
	
	private static boolean needsBraces(Expr e) {
		 if(e instanceof Expr.Binary) {			
			 return true;
		 } else if(e instanceof Expr.Nary) {
			 Expr.Nary ne = (Expr.Nary) e;
			 switch(ne.op) {
			 case AND:
			 case OR:
				 return true;
			 }
		 } else if(e instanceof Quantifier) {
			 return true;
		 }
		 return false;
	}
}