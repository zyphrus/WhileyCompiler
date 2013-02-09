package wycs.lang;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import wyil.lang.Attribute;
import wyil.util.Pair;
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
	
	public static Constant Constant(wyil.lang.Constant value, Attribute... attributes) {
		return new Constant(value,attributes);
	}
	
	public static Constant Constant(wyil.lang.Constant value, Collection<Attribute> attributes) {
		return new Constant(value,attributes);
	}
	
	public static Unary Unary(Unary.Op op, Expr operand, Attribute... attributes) {
		return new Unary(op, operand,attributes);
	}
	
	public static Unary Unary(Unary.Op op, Expr operand, Collection<Attribute> attributes) {
		return new Unary(op, operand,attributes);
	}
	
	public static FieldOf FieldOf(Expr expr, String field, Attribute... attributes) {
		return new FieldOf(expr,field,attributes);
	}
	
	public static FieldOf FieldOf(Expr expr, String field, Collection<Attribute> attributes) {
		return new FieldOf(expr,field,attributes);
	}
	
	public static FieldUpdate FieldUpdate(Expr source, String field, Expr expr, 
			Attribute... attributes) {
		return new FieldUpdate(source, expr, field, attributes);
	}
	
	public static FieldUpdate FieldUpdate(Expr source, String field, Expr expr, 
			Collection<Attribute> attributes) {
		return new FieldUpdate(source, expr, field, attributes);
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
	
	public static Record Record(String[] fields, Expr[] operands, Attribute... attributes) {
		return new Record(fields, operands, attributes);
	}
	
	public static Record Record(String[] fields, Expr[] operands, Collection<Attribute> attributes) {
		return new Record(fields, operands, attributes);
	}
	
	public static Fn Fn(String name, Expr[] operands, Attribute... attributes) {
		return new Fn(name,operands,attributes);
	}
	
	public static Fn Fn(String name, Expr[] operands, Collection<Attribute> attributes) {
		return new Fn(name,operands,attributes);
	}

	public static ForAll ForAll(Collection<Pair<Expr.Variable,Expr>> vars, Expr expr, Attribute... attributes) {
		return new ForAll(vars,expr,attributes);
	}
	
	public static ForAll ForAll(Collection<Pair<Expr.Variable,Expr>> vars, Expr expr, Collection<Attribute> attributes) {
		return new ForAll(vars,expr,attributes);
	}
	
	public static Exists Exists(Collection<Pair<Expr.Variable,Expr>> vars, Expr expr, Attribute... attributes) {
		return new Exists(vars,expr,attributes);
	}
	
	public static Exists Exists(Collection<Pair<Expr.Variable,Expr>> vars, Expr expr, Collection<Attribute> attributes) {
		return new Exists(vars,expr,attributes);
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
		public final wyil.lang.Constant value;
		
		private Constant(wyil.lang.Constant value, Attribute... attributes) {
			super(attributes);
			this.value = value;
		}
		
		private Constant(wyil.lang.Constant value, Collection<Attribute> attributes) {
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
		public final Expr operand;
		
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
	
	public static class FieldOf extends Expr {
		public final Expr operand;
		public final String field;
		
		private FieldOf(Expr expr, String field, Attribute... attributes) {
			super(attributes);						
			this.operand = expr;
			this.field = field;
		}
		
		private FieldOf(Expr expr, String field, Collection<Attribute> attributes) {
			super(attributes);						
			this.operand = expr;
			this.field = field;
		}
				
		public String toString() {
			return operand.toString() + "." + field;
		}
	}
	
	public static class FieldUpdate extends Expr {
		public final Expr source;
		public final Expr operand;
		public final String field;
		
		private FieldUpdate(Expr source, Expr operand, String field, Attribute... attributes) {
			super(attributes);		
			this.source = source;
			this.operand = operand;
			this.field = field;
		}
		
		private FieldUpdate(Expr source, Expr operand, String field,
				Collection<Attribute> attributes) {
			super(attributes);
			this.source = source;
			this.operand = operand;
			this.field = field;
		}
				
		public String toString() {
			return source.toString() + "[" + field + ":=" + operand + "]";
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
			LT(8) {
				public String toString() {
					return "<";
				}
			},
			LTEQ(9) {
				public String toString() {
					return Character.toString(Lexer.UC_LESSEQUALS);
				}				
			},
			GT(10) {
				public String toString() {
					return ">";
				}
			},
			GTEQ(11) {
				public String toString() {
					return Character.toString(Lexer.UC_GREATEREQUALS);					
				}
			},
			IN(12) {
				public String toString() {
					return Character.toString(Lexer.UC_ELEMENTOF);
				}
			},
			SUBSET(13) {
				public String toString() {
					return Character.toString(Lexer.UC_SUBSETEQ);
				}
			},
			SUBSETEQ(14) {
				public String toString() {
					return Character.toString(Lexer.UC_SUBSETEQ);
				}
			},
			SUPSET(15) {
				public String toString() {
					return Character.toString(Lexer.UC_SUPSET);
				}
			},
			SUPSETEQ(16) {
				public String toString() {
					return Character.toString(Lexer.UC_SUPSETEQ);
				}
			},
			DIFFERENCE(19) {
				public String toString() {
					return "{=";
				}
			},
			INDEXOF(20) {
				public String toString() {
					return "[]";
				}
			},
			APPEND(21) {
				public String toString() {
					return "++";
				}
			},
			RANGE(22) {
				public String toString() {
					return "..";
				}
			};
			
			public int offset;

			private Op(int offset) {
				this.offset = offset;
			}
		};

		public final Op op;
		public final Expr leftOperand;
		public final Expr rightOperand;
		
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
			if(op == Op.INDEXOF) {
				return lhs + "[" + rhs + "]";
			} else if(op == Op.RANGE) {
				return lhs + ".." + rhs;
			} else {
				return lhs + " " + op + " " + rhs;
			}
		}
	}
	
	public static class Nary extends Expr {
		public enum Op {
			AND(0),
			OR(1),
			SET(2),
			MAP(3),
			LIST(4),			
			UNION(5),
			INTERSECTION(6),
			SUBLIST(7),
			UPDATE(8);
					
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
			case LIST:
				beg = "[";
				end = "]";
				sep = ", ";
				break;			
			case UNION:
				beg = "";
				end = "";
				sep = " " + Lexer.UC_SETUNION + " ";
				break;
			case INTERSECTION:
				beg = "";
				end = "";
				sep = " " + Lexer.UC_SETINTERSECTION + " ";
				break;
			case MAP:				
			case SUBLIST:				
				return operands[0].toString() + "[" + operands[1] + ".." + operands[2] + "]";
			case UPDATE:
				return operands[0].toString() + "[" + operands[1] + ":=" + operands[2] + "]";
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
	
	public static class Record extends Expr {
		public final Expr[] operands;
		public final String[] fields;
		
		private Record(String[] fields, Expr[] operands, Attribute... attributes) {
			super(attributes);			
			this.fields = fields;
			this.operands = operands;
		}
		
		private Record(String[] fields, Expr[] operands, Collection<Attribute> attributes) {
			super(attributes);			
			this.fields = fields;
			this.operands = operands;
		}
		
		public String toString() {
			String r = "{";
			for(int i=0;i!=fields.length;++i) {
				String field = fields[i];
				Expr operand = operands[i];
				r += field + " : ";
				if(needsBraces(operand)) {
					r += "(" + operand + ")";
				} else {
					r += operand;
				}
			}
			return r + "}";
		}
	}
	
	public static class Fn extends Expr {
		public final Expr[] operands;
		public final String name;
		
		private Fn(String name, Expr[] operands, Attribute... attributes) {
			super(attributes);			
			this.name = name;
			this.operands = operands;
		}
		
		private Fn(String name, Expr[] operands, Collection<Attribute> attributes) {
			super(attributes);			
			this.name = name;
			this.operands = operands;
		}
				
		public String toString() {
			String r = name + "(";
			for(int i=0;i!=operands.length;++i) {
				if(i != 0) { r += ", "; }
				Expr operand = operands[i];				
				r += operand;
			}
			return r + ")";
		}
	}
	
	public static abstract class Quantifier extends Expr {
		public final List<Pair<Expr.Variable,Expr>> vars;
		public final Expr expr;
		
		private Quantifier(Collection<Pair<Expr.Variable,Expr>> vars, Expr expr, Attribute... attributes) {
			super(attributes);			
			this.vars = new CopyOnWriteArrayList<Pair<Expr.Variable,Expr>>(vars);
			this.expr = expr;
		}
		
		private Quantifier(Collection<Pair<Expr.Variable,Expr>> vars, Expr expr, Collection<Attribute> attributes) {
			super(attributes);			
			this.vars = new CopyOnWriteArrayList<Pair<Expr.Variable,Expr>>(vars);
			this.expr = expr;
		}
		
		public String toString() {
			String r = "";
			boolean firstTime = true;
			for(Pair<Expr.Variable,Expr> var : vars) {
				if(!firstTime) {
					r = r + ",";
				}
				firstTime=false;
				r = r + var.first() + " " + Lexer.UC_ELEMENTOF + " " + var.second();
			}
			return r + "." + expr;
		}
	}
	
	public static class ForAll extends Quantifier {
		private ForAll(Collection<Pair<Expr.Variable,Expr>> vars, Expr expr, Attribute... attributes) {
			super(vars, expr, attributes);						
		}
		
		private ForAll(Collection<Pair<Expr.Variable,Expr>> vars, Expr expr, Collection<Attribute> attributes) {
			super(vars, expr, attributes);						
		}
		
		public String toString() {
			return Lexer.UC_FORALL + super.toString();
		}
	}
	
	public static class Exists extends Quantifier {
		private Exists(Collection<Pair<Expr.Variable,Expr>> vars, Expr expr, Attribute... attributes) {
			super(vars, expr, attributes);						
		}
		
		private Exists(Collection<Pair<Expr.Variable,Expr>> vars, Expr expr, Collection<Attribute> attributes) {
			super(vars, expr, attributes);						
		}
		
		public String toString() {
			return Lexer.UC_EXISTS + super.toString();
		}
	}
	
	private static boolean needsBraces(Expr e) {
		 if(e instanceof Expr.Binary) {
			 Expr.Binary ne = (Expr.Binary) e;
			 switch(ne.op) {
			 case INDEXOF:
				 return false;
			 }
			 return true;
		 } else if(e instanceof Expr.Nary) {
			 Expr.Nary ne = (Expr.Nary) e;
			 switch(ne.op) {
			 case AND:
			 case OR:
			 case UNION:
			 case INTERSECTION:
				 return true;
			 }
		 } else if(e instanceof Quantifier) {
			 return true;
		 }
		 return false;
	}
}
