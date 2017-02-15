package wyil.util;

import java.util.List;

import wybs.lang.Build;
import wybs.util.ResolveError;
import wyil.lang.Bytecode;
import wyil.lang.SyntaxTree;
import wyil.lang.WyilFile;
import wyil.lang.SyntaxTree.Location;
import wyil.lang.Type;

/**
 * Responsible for back propagating type information from "sinks" to "sources".
 * This is helpful in allowing compiler back-ends in making good decisions about
 * how to construct a particular value. For example:
 *
 * <pre>
 * type Point is {int x, int y}
 *
 * function origin() -> Point:
 * 	  return {x: 0, y: 0}
 * </pre>
 *
 * The key issue above is that is uses an <i>anonymous record</i> constructor,
 * namely <code>{x: 0, y: 0}</code>. Translating such a constructor on the
 * back-end can be tricky if it is attempting to create a specific instance of a
 * type (e.g. <code>Point</code>). In such case, back propagation pushes the
 * type <code>Point</code> from the <code>return</code> statement up through the
 * expression tree and associates it with the record construct in the WyIL
 * bytecode.
 *
 * @author David J. Pearce
 *
 */
public class BackPropagation implements Build.Stage<WyilFile> {
	private final TypeSystem types;

	public BackPropagation(Build.Task builder) {
		this.types = new TypeSystem(builder.project());
	}

	@Override
	public void apply(WyilFile module) {
		for(WyilFile.Type type : module.types()) {
			check(type.getTree(),type);
		}
		for(WyilFile.FunctionOrMethod method : module.functionOrMethods()) {
			check(method.getTree(),method);
		}
	}

	private void check(SyntaxTree tree, WyilFile.Declaration context) {
		// Examine all entries in this block looking for a conversion bytecode
		List<SyntaxTree.Location<?>> expressions = tree.getLocations();
		for (int i = 0; i != expressions.size(); ++i) {
			SyntaxTree.Location<?> l = expressions.get(i);
			if (l.getBytecode() instanceof Bytecode.Stmt) {
				check((Location<Bytecode.Stmt>) l, context);
			}
		}
	}

	private  void check(Location<Bytecode.Stmt> stmt, WyilFile.Declaration context) {
		switch(stmt.getOpcode()) {
		case Bytecode.OPCODE_assign: {
			Location<?>[] lhs = stmt.getOperandGroup(SyntaxTree.LEFTHANDSIDE);
			Location<?>[] rhs = stmt.getOperandGroup(SyntaxTree.RIGHTHANDSIDE);
			for(int i=0;i!=rhs.length;++i) {
				Type t = lhs[i].getType();
				check(t,(Location<Bytecode.Expr>) rhs[i]);
			}
			break;
		}
		case Bytecode.OPCODE_invoke:{
			Bytecode.Invoke ivk = (Bytecode.Invoke) stmt.getBytecode();
			Type[] parameterTypes = ivk.type().params();
			for(int i=0;i!=stmt.numberOfOperands();++i) {
				check(parameterTypes[i],(Location<Bytecode.Expr>) stmt.getOperand(i));
			}
			break;
		}
		case Bytecode.OPCODE_indirectinvoke: {
			Bytecode.IndirectInvoke ivk = (Bytecode.IndirectInvoke) stmt.getBytecode();
			Type[] parameterTypes = ivk.type().params();
			for(int i=1;i!=stmt.numberOfOperands();++i) {
				check(parameterTypes[i-1],(Location<Bytecode.Expr>) stmt.getOperand(i));
			}
			break;
		}
		case Bytecode.OPCODE_return: {
			WyilFile.FunctionOrMethod m = (WyilFile.FunctionOrMethod) context;
			Type[] returnTypes = m.type().returns();
			for(int i=0;i!=stmt.numberOfOperands();++i) {
				check(returnTypes[i],(Location<Bytecode.Expr>) stmt.getOperand(i));
			}
			break;
		}
		case Bytecode.OPCODE_vardeclinit:{
			check(stmt.getType(),(Location<Bytecode.Expr>) stmt.getOperand(0));
			break;
		}
		case Bytecode.OPCODE_assert:
		case Bytecode.OPCODE_assume:
		case Bytecode.OPCODE_dowhile:
		case Bytecode.OPCODE_debug:
		case Bytecode.OPCODE_if:
		case Bytecode.OPCODE_ifelse:
		case Bytecode.OPCODE_switch:
		case Bytecode.OPCODE_while:
		{
			for(int i=0;i!=stmt.numberOfOperands();++i) {
				Type type = stmt.getOperand(i).getType();
				check(type,(Location<Bytecode.Expr>) stmt.getOperand(i));
			}
			break;
		}
		case Bytecode.OPCODE_aliasdecl:
		case Bytecode.OPCODE_break:
		case Bytecode.OPCODE_block:
		case Bytecode.OPCODE_continue:
		case Bytecode.OPCODE_fail:
		case Bytecode.OPCODE_namedblock:
		case Bytecode.OPCODE_skip:
		case Bytecode.OPCODE_vardecl:
			break;
		default:
			throw new IllegalArgumentException(
					"Unknown bytecode encountered: " + stmt.getBytecode().getClass().getName());
		}
	}

	private void check(Type target, Location<Bytecode.Expr> expr) {
		try {
			SyntaxTree parent = expr.getEnclosingTree();
			switch(expr.getOpcode()) {
			case Bytecode.OPCODE_const:
			case Bytecode.OPCODE_record: {
				// Update the type of this bytecode
				Type type = expr.getType();
				// FIXME: this feels something like a hack
				if(types.isSubtype(target,type)) {
					type = target;
				} else {
					type = Type.Intersection(type,target);
				}
				Location<Bytecode.Expr> nExpr = new Location<>(parent, type, expr.getBytecode(), expr.attributes());
				expr.getEnclosingTree().getLocations().set(expr.getIndex(), nExpr);
				break;
			}
			case Bytecode.OPCODE_invoke: {
				Bytecode.Invoke ivk = (Bytecode.Invoke) expr.getBytecode();
				Type[] parameterTypes = ivk.type().params();
				for(int i=0;i!=expr.numberOfOperands();++i) {
					check(parameterTypes[i],(Location<Bytecode.Expr>) expr.getOperand(i));
				}
				break;
			}
			case Bytecode.OPCODE_indirectinvoke: {
				Bytecode.IndirectInvoke ivk = (Bytecode.IndirectInvoke) expr.getBytecode();
				Type[] parameterTypes = ivk.type().params();
				for(int i=1;i!=expr.numberOfOperands();++i) {
					check(parameterTypes[i-1],(Location<Bytecode.Expr>) expr.getOperand(i));
				}
				break;
			}
			case Bytecode.OPCODE_lambda:
			case Bytecode.OPCODE_all:
			case Bytecode.OPCODE_some:
			case Bytecode.OPCODE_convert:
			case Bytecode.OPCODE_fieldload:
			case Bytecode.OPCODE_neg:
			case Bytecode.OPCODE_dereference:
			case Bytecode.OPCODE_add:
			case Bytecode.OPCODE_sub:
			case Bytecode.OPCODE_mul:
			case Bytecode.OPCODE_div:
			case Bytecode.OPCODE_rem:
			case Bytecode.OPCODE_eq:
			case Bytecode.OPCODE_ne:
			case Bytecode.OPCODE_lt:
			case Bytecode.OPCODE_le:
			case Bytecode.OPCODE_gt:
			case Bytecode.OPCODE_ge:
			case Bytecode.OPCODE_logicalnot:
			case Bytecode.OPCODE_logicaland:
			case Bytecode.OPCODE_logicalor:
			case Bytecode.OPCODE_bitwiseinvert:
			case Bytecode.OPCODE_bitwiseor:
			case Bytecode.OPCODE_bitwisexor:
			case Bytecode.OPCODE_bitwiseand:
			case Bytecode.OPCODE_shl:
			case Bytecode.OPCODE_shr:
			case Bytecode.OPCODE_arrayindex:
			case Bytecode.OPCODE_arraygen:
			case Bytecode.OPCODE_array:
			case Bytecode.OPCODE_newobject:
			case Bytecode.OPCODE_is:
			case Bytecode.OPCODE_arraylength:
			case Bytecode.OPCODE_varmove:
			case Bytecode.OPCODE_varcopy: {
				// FIXME: could do better with all of these
				break;
			}
			default:
				throw new IllegalArgumentException("Unknown bytecode encountered: " + expr.getBytecode().getClass().getName());
			}
		} catch(ResolveError e) {
			throw new RuntimeException(e);
		}
	}
}
