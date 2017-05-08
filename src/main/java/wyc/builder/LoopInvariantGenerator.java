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
                    Expr invariant = startingBoundInvariant((Expr.AssignedVariable) variant.getKey(), variant.getValue(), context);

                    if (invariant != null) {
                        invariant.attributes().add(whileStmt.attribute(Attribute.Source.class));
                        whileStmt.invariants.add(invariant);
                    }
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

    /**
     * Checks the properties founds to see if the starting bound invariant can be generated
     * from inspecting the AST
     *
     * This checks:
     *  * if the variant can determine which direction of the mutation is being
     *    applied every iteration.
     *  * if the value of the variant at the start of the loop can be safely used and
     *    that that value will not change during the loop.
     *
     * @param variant variable that changes in the loop
     * @param variantExpr the expression that mutates the variant
     * @param context a store of local variables and paramters
     * @return expression of invariant, otherwise null
     */
    private Expr startingBoundInvariant(Expr.AssignedVariable variant, Expr variantExpr, Context context) {

        // determine the direction of the mutation, + or -
        SequenceDirection direction = determineSequenceDirection(variant, variantExpr);
        // check if the mutation is invalid
        if (direction == SequenceDirection.UNKNOWN) {
            System.err.println("unable to determine sequence direction of variant");
            return null;
        }

        // TODO: detect if the value before the loop is safe or not
        // could extend this to create a ghost variable instead, however for now keeping to safe
        Expr preLoopValue = context.getValue(variant.var);
        if (!checkPreLoopValue(preLoopValue, context)) {
            System.err.println("Oh my, the entrant value is not safe for " + variant + " with " + preLoopValue );
            return null;
        }

        // given the
        return new Expr.BinOp(direction.toBOp(), variant, preLoopValue,
                        new GeneratedAttribute("Inferred starting boundary of variable " + variant + " from loop body"));
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

    /**
     * collect variants and their mutating expressions
     *
     * If a variant is found to have two
     *
     * @param stmt statement to be checked
     * @param variants map of variants already identified
     * @param context collection of local variables and parameters
     */
    private void findVariants(Stmt stmt, Map<Expr.LVal, Expr> variants, Context context) {
        // only check the guaranteed sections of the loop
        // and ignoring any branches or inner-loops to lessen complexity of identifying variants
        if (stmt instanceof Stmt.NamedBlock) {
            Stmt.NamedBlock namedBlock = (Stmt.NamedBlock) stmt;
            findVariants(namedBlock.body, variants, context);
        } else if (stmt instanceof Stmt.Assign) {
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

                    // must be a variable of int type
                    // must have a simple mutation
                    if (lval.result().equals(wyil.lang.Type.T_INT)
                            && !duplicate.isPresent()
                            // restrict variants to values defined out of the loop
                            && context.hasValue(assignedVariable.var)
                            // make sure that they have a 'simple' assignment
                            && context.getValue(assignedVariable.var) != null
                            && isSimpleMutationOfVar(assignedVariable, rval)
                            ) {
                        variants.put(lval, rval);
                    } else {
                        // invalidate the variant to be a candidate to check
                        // since the mutations are too complex to be handled
                        variants.put(lval, null);
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
     * Determines if the expression is a simple mutation of the given variable
     *
     * @param var variable that is being assigned
     * @param expr mutation expression
     * @return true if the assignment is a simple mutation
     */
    private boolean isSimpleMutationOfVar(Expr.AssignedVariable var, Expr expr) {
        return isSimpleMutationOfVar(var, expr, true);
    }

    /**
     * Determines if the expression is a simple mutation of the given variable
     *
     * A simple mutation is defined as the use of addition or subtraction of constants and the assigned variable.
     * This is
     * The given variable must be contained within the expression but cannot be the whole expression.
     *
     * This method can give a false positive in the case of <code>x - x</code>
     *
     * @param var variable that is being assigned
     * @param expr mutation expression
     * @param topLevel true if the current mutation is a sub-expression of the total expression, otherwise false
     * @return true if the assignment is a simple mutation, otherwise false
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
            // only allow reference to the variable that is being assigned to for simplicity
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
