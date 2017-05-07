package wyc.builder;

import wybs.lang.Attribute;
import wyc.lang.Expr;
import wyc.lang.Stmt;
import wyc.lang.WhileyFile;
import wyil.lang.Constant;

import java.math.BigInteger;
import java.util.*;

/**
 * Checks loops for possible invariants that can be inferred statically.
 *
 * Has 4 main stages
 *  * find a loop
 *  * infer which variables are variant in the loop
 *  * determine the direction of the sequence for the variable
 *  * create invariant from the starting point from the entry of the loop
 */
public class LoopInvariantGenerator {

    private final WhileyFile whileyFile;

    public LoopInvariantGenerator(WhileyFile whileyFile) {
        this.whileyFile = whileyFile;
    }

    public void generate() {
        for (WhileyFile.FunctionOrMethodOrProperty method : whileyFile.declarations(WhileyFile.FunctionOrMethodOrProperty.class)) {
            Context context = new Context();

            for (WhileyFile.Parameter param : method.parameters) {
                context.putParam(param.name());
            }

            findLoops(method.statements, context);
        }
    }


    /**
     * First stage of generating loop invariants
     *
     * @param statements
     */
    private void findLoops(ArrayList<Stmt> statements, Context context) {
        for (Stmt stmt : statements) {
            findLoops(stmt, context);
        }
    }

    void findLoops(Stmt stmt, Context context) {
        if (stmt instanceof Stmt.IfElse) {
            Stmt.IfElse stmtIfElse = (Stmt.IfElse) stmt;

            findLoops(stmtIfElse.trueBranch, new Context(context));
            findLoops(stmtIfElse.falseBranch, new Context(context));
        } else if (stmt instanceof Stmt.NamedBlock) {
            Stmt.NamedBlock namedBlock = (Stmt.NamedBlock) stmt;
            findLoops(namedBlock.body, new Context(context));
        } else if (stmt instanceof Stmt.While) {
            Stmt.While whileStmt = (Stmt.While) stmt;
            // handle the while loop
            Map<Expr.LVal, Expr> variants = findVariants(whileStmt, context);
            for (Map.Entry<Expr.LVal, Expr> variant : variants.entrySet()) {

                if (variant.getValue() != null && variant.getKey() instanceof Expr.AssignedVariable) {
                    SequenceDirection direction = determineSequenceDirection( (Expr.AssignedVariable) variant.getKey(), variant.getValue());

                    // TODO: detect if the value before the loop is safe or not
                    // could extend this to create a ghost variable instead, however for now keeping to safe
                    Expr preLoopValue = context.getValue(((Expr.AssignedVariable) variant.getKey()).var);

                    if (!checkPreLoopValue(preLoopValue, context)) {
                        System.err.println("Oh my, the entrant value is not safe for " + variant.getKey() + " with " + preLoopValue );
                        continue;
                    }

                    // NOTE: This works very well with While_Valid_[1,3].whiley
                    whileStmt.invariants.add(
                            new Expr.BinOp(direction.toBOp(), variant.getKey(), preLoopValue,
                                    new GeneratedAttribute("Inferred starting boundary of variable " + variant.getKey() + " from loop body"),
                                    whileStmt.attribute(Attribute.Source.class))
                    );

                    System.out.println(direction);
                }
            }

            findLoops(whileStmt.body, new Context(context));
        } else if (stmt instanceof Stmt.Switch) {
            Stmt.Switch switchStmt = (Stmt.Switch) stmt;
            for (Stmt.Case caseStmt : switchStmt.cases) {
                findLoops(caseStmt.stmts, new Context(context));
            }
        } else if (stmt instanceof Stmt.DoWhile) {
            Stmt.DoWhile whileStmt = (Stmt.DoWhile) stmt;
            // handle the do-while loop ?
            findLoops(whileStmt.body, new Context(context));
        } else if (stmt instanceof Stmt.VariableDeclaration) {
            // skip
            Stmt.VariableDeclaration stmtDecl = (Stmt.VariableDeclaration) stmt;

            if (stmtDecl.expr != null) {
                context.putValue(stmtDecl.parameter.name, stmtDecl.expr);
            }
        } else if (stmt instanceof Stmt.Assign) {
            // skip
            Stmt.Assign stmtAssign = (Stmt.Assign) stmt;

            Iterator<Expr.LVal> lvals = stmtAssign.lvals.iterator();
            Iterator<Expr> rvals = stmtAssign.rvals.iterator();
            // would of liked to chain these two lists together

            while (lvals.hasNext() && rvals.hasNext()) {
                Expr.LVal lval = lvals.next();
                Expr rval = rvals.next();

                if (context.hasValue(lval.toString())) {
                    System.err.println("Oh dear, two assignments for " + lval + " removing from set of safe variants");
                    context.putValue(lval.toString(), null);
                    continue;
                }

                // a candidate that could be inferred must be an integer type
                // and be assigned to a constant, e.g. int i = 0
                if (lval.result().equals(wyil.lang.Type.T_INT) &&
                        lval instanceof Expr.AssignedVariable) {
                    context.putValue(((Expr.AssignedVariable)lval).var, rval);
                }
            }
        }
    }

    private Map<Expr.LVal, Expr> findVariants(Stmt.While whileStmt, Context context) {
        Map<Expr.LVal, Expr> variants = new HashMap<>();

        findVariants(whileStmt.body, variants, context);

        return variants;
    }

    private void findVariants(List<Stmt> stmts, Map<Expr.LVal, Expr> variants, Context context) {
        for (Stmt stmt : stmts) {
            findVariants(stmt, variants, context);
        }
    }

    private void findVariants(Stmt stmt, Map<Expr.LVal, Expr> variants, Context context) {
        // only check the guaranteed sections of the loop
        // and ignoring any branches or inner-loops to lessen complexity of identifying variants
        if (stmt instanceof Stmt.NamedBlock) {
            Stmt.NamedBlock namedBlock = (Stmt.NamedBlock) stmt;
            findVariants(namedBlock.body, variants, context);
        } else if (stmt instanceof Stmt.Assign) {
            // skip
            Stmt.Assign stmtAssign = (Stmt.Assign) stmt;

            Iterator<Expr.LVal> lvals = stmtAssign.lvals.iterator();
            Iterator<Expr> rvals = stmtAssign.rvals.iterator();
            // would of liked to chain these two lists together

            while (lvals.hasNext() && rvals.hasNext()) {
                Expr.LVal lval = lvals.next();
                Expr rval = rvals.next();

                if (lval instanceof Expr.AssignedVariable) {
                    Expr.AssignedVariable assignedVariable = (Expr.AssignedVariable) lval;

                    Optional<Expr.AssignedVariable> duplicate = variants.keySet()
                            .stream()
                            .filter(e -> e instanceof Expr.AssignedVariable)
                            .map(e -> (Expr.AssignedVariable) e)
                            .filter(e ->  e.var.equals(assignedVariable.var))
                            .findFirst();

                    if (duplicate.isPresent()) {
                        System.err.println("Oh bother, two assignments inside the loop for " + lval);
                        // invalidate the variant as assigning twice makes too complicated to handle
                        // and it should be the programmer to navigate this situation
                        variants.put(duplicate.get(), null);
                        continue;
                    }

                    // must be a variable of int type
                    // must have a simple mutation
                    if (lval.result().equals(wyil.lang.Type.T_INT)
                            // restrict variants to values defined out of the loop
                            && context.hasValue(assignedVariable.var)
                            // make sure that they have a 'simple' assignment
                            && context.getValue(assignedVariable.var) != null
                            && isSimpleMutationOfVar(assignedVariable, rval)) {
                        variants.put(lval, rval);
                    }
                }
            }
        }
    }

    /**
     * Determine if an expression is safe to be used as the value of variable
     * before the loop
     *
     * The safety check is conservative check of the expression for simple expressions
     * such as constants, lengths of arrays, use of parameters and checking referenced
     * local variables.
     *
     * @param preloop expression of the value of the variant before the loop
     * @param context
     * @return true if the expression is determined to be safe, otherwise false.
     */
    private boolean checkPreLoopValue(Expr preloop, Context context) {
        if (preloop instanceof Expr.Constant) {
            return true;
        } else if (preloop instanceof Expr.UnOp) {
            Expr.UnOp unOpExpr = (Expr.UnOp) preloop;
            switch (unOpExpr.op) {
                case ARRAYLENGTH:
                    return checkPreLoopValue(unOpExpr.mhs, context);
                default:
                    return true;
            }
        } else if (preloop instanceof Expr.LocalVariable) {
            Expr.LocalVariable localVar = (Expr.LocalVariable) preloop;

            if (context.hasParam(localVar.var)) {
                return true;
            }

            Expr e = context.getValue(localVar.var);
            if (e != null) {
                return checkPreLoopValue(e, context);
            }
        }

        return false;
    }

    /**
     *
     *
     * @param var
     * @param expr
     * @return
     */
    private boolean isSimpleMutationOfVar(Expr.AssignedVariable var, Expr expr) {
        return isSimpleMutationOfVar(var, expr, false);
    }

    /**
     * Determines if the expression is a simple mutation of the given variable
     *
     * A simple mutation is defined as the use of addition or subtraction of constants and the assigned variable
     *
     * The given expression must be contained in the expression.
     *
     * This method can give a false positive in the case of <code>x - x</code>
     *
     * @param var
     * @param expr
     * @param topLevel
     * @return
     */
    private boolean isSimpleMutationOfVar(Expr.AssignedVariable var, Expr expr, boolean topLevel) {
        if (expr instanceof Expr.Constant) {
            return !topLevel;
        } else if (expr instanceof Expr.BinOp) {
            Expr.BinOp binop = (Expr.BinOp) expr;

            // only support ADD & SUB for simple mutation
            if (!(binop.op.equals(Expr.BOp.ADD) || binop.op.equals(Expr.BOp.SUB))) {
               return false;
            }
            return isSimpleMutationOfVar(var, binop.lhs, false) && isSimpleMutationOfVar(var, binop.rhs, false);
        } else if (expr instanceof Expr.LocalVariable) {
            Expr.LocalVariable local = (Expr.LocalVariable) expr;
            return local.var.equals(var.var);
        }
        return false;
    }
    
    private enum SequenceDirection {
        ASCENDING,
        DESCENDING,
        UNKNOWN;

        public Expr.BOp toBOp() {
            switch (this) {
                case ASCENDING:
                    return Expr.BOp.GTEQ;
                case DESCENDING:
                    return Expr.BOp.LTEQ;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }


    private SequenceDirection determineSequenceDirection(Expr.AssignedVariable variable, Expr expr) {
        BigInteger exprEval = evalConstExpr(variable, expr);
        if (exprEval.compareTo(BigInteger.ZERO) > 0) {
            return SequenceDirection.ASCENDING;
        } else if (exprEval.compareTo(BigInteger.ZERO) < 0)  {
            return SequenceDirection.DESCENDING;
        } else {
            return SequenceDirection.UNKNOWN;
        }
    }

    /**
     * Evaluate an expression that contains only constants and the assigned variable
     *
     *
     *
     * @param variable
     * @param expr
     * @return the change in value the variable will
     */
    private BigInteger evalConstExpr(Expr.AssignedVariable variable, Expr expr) {
        if (expr instanceof Expr.BinOp) {
            Expr.BinOp binop = (Expr.BinOp) expr;
            switch (binop.op) {
                case ADD:
                    return evalConstExpr(variable, binop.lhs).add(evalConstExpr(variable, binop.rhs));
                case SUB:
                    return evalConstExpr(variable, binop.lhs).subtract(evalConstExpr(variable, binop.rhs));
                default:
                    throw new UnsupportedOperationException();
            }
        } else if (expr instanceof Expr.Constant) {
            Expr.Constant constant = (Expr.Constant) expr;
            return ((Constant.Integer) constant.value).value();
        } else if (expr instanceof Expr.LocalVariable) {
            Expr.LocalVariable local = (Expr.LocalVariable) expr;

            if (local.var.equals(variable.var)) {
                return BigInteger.ZERO;
            } else {
                throw new UnsupportedOperationException();
            }
        }

        throw new UnsupportedOperationException();
    }

    public static class GeneratedAttribute implements wybs.lang.Attribute {
        private final String reason;
        public GeneratedAttribute(String reason) {
            this.reason = reason;
        }

        @Override
        public String toString() {
            return "GeneratedAttribute{" +
                    "reason='" + reason + '\'' +
                    '}';
        }
    }


    private class Context {
        private final Context parent;

        private Map<String, Expr> variables = new HashMap<>();
        private Set<String> parameters = new HashSet<>();

        public Context() {
            this.parent = null;
        }

        public Context(Context parent) {
            this.parent = parent;
        }

        Expr getValue(String variable) {
            if (variables.containsKey(variable)) {
                return variables.get(variable);
            } else if (this.parent != null) {
                return this.parent.getValue(variable);
            }
            return null;
        }

        boolean hasValue(String variable) {
            if (variables.containsKey(variable)) {
                return true;
            } else if (this.parent != null) {
                return this.parent.hasValue(variable);
            }
            return false;
        }

        void putValue(String variable, Expr expr) {
            this.variables.put(variable, expr);
        }


        void putParam(String variable) {
            if (this.parent != null) {
                throw new IllegalStateException("Only the root node can put parameters to the context");
            }
            this.parameters.add(variable);
        }

        boolean hasParam(String variable) {
            if (this.parent != null) {
                return this.parent.hasParam(variable);
            }
            return this.parameters.contains(variable);
        }
    }
}
