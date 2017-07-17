package wyc.builder.invariants;

import wyc.builder.LoopInvariantGenerator;
import wyc.lang.Expr;
import wyc.lang.Stmt;
import wycc.util.Pair;
import wyil.lang.Constant;

import java.math.BigInteger;
import java.util.*;

public class StartingBoundInvariantLattice implements InvariantGenerator {

    @Override
    public List<Expr> generateInvariant(Stmt.While whileStmt, LoopInvariantGenerator.Context context) {
        List<Expr> invariants = new ArrayList<>();

        Environment env = findVariants(whileStmt, context);
        for (Map.Entry<String, Pair<SequenceDirection, Expr.AssignedVariable>> variant : env.variables.entrySet()) {

            SequenceDirection direction = variant.getValue().first();
            if (direction != SequenceDirection.UNDETERMINED &&
                    direction != SequenceDirection.UNKNOWN) {
                Expr invariant = startingBoundInvariant(variant.getValue().second(), direction, context);

                if (invariant != null) {
                    invariants.add(invariant);
                }
            }

        }

        return invariants;
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
     * @param direction
     * @param context a store of local variables and paramters
     * @return expression of invariant, otherwise null
     */
    private Expr startingBoundInvariant(Expr.AssignedVariable variant, SequenceDirection direction, LoopInvariantGenerator.Context context) {

        // check if the mutation is invalid
        if (direction == SequenceDirection.UNKNOWN || direction == SequenceDirection.UNDETERMINED) {
            System.err.println("unable to determine sequence direction of variant");
            return null;
        }

        // TODO: detect if the value before the loop is safe or not
        // could extend this to create a ghost variable instead, however for now keeping to safe
        LoopInvariantGenerator.Variable preLoopValue = context.getValue(variant.var);
        if (!checkPreLoopValue(preLoopValue.getAssigned(), context)) {
            System.err.println("Oh my, the entrant value is not safe for " + variant + " with " + preLoopValue );
            return null;
        }

        // given the
        return new Expr.BinOp(direction.toBOp(), variant, preLoopValue.getAssigned(),
                new LoopInvariantGenerator.GeneratedAttribute("Inferred starting boundary of variable " + variant + " from loop body"));
    }

    private Environment findVariants(Stmt.While whileStmt, LoopInvariantGenerator.Context context) {
        Environment env = new Environment();

        findVariants(whileStmt.body, env, context);

        return env;
    }

    private void findVariants(List<Stmt> stmts, Environment env, LoopInvariantGenerator.Context context) {
        for (Stmt stmt : stmts) {
            findVariants(stmt, env, context);
        }
    }

    /**
     * collect variants and their mutating expressions
     *
     * If a variant is found to have two
     *
     * If not in the top level, variants found to be mutated in sub-scopes will
     * be removed from the candidate pool as they are too hard to determine if they are
     * safe for invariant generation.
     *
     * @param stmt statement to be checked
     * @param env
     * @param context collection of local variables and parameters
     */
    private void findVariants(Stmt stmt, Environment env, LoopInvariantGenerator.Context context) {
        // only check the guaranteed sections of the loop
        // and ignoring any branches or inner-loops to lessen complexity of identifying variants
        if (stmt instanceof Stmt.IfElse) {
            Stmt.IfElse stmtIfElse = (Stmt.IfElse) stmt;

            Environment trueEnv = new Environment(env);
            Environment falseEnv = new Environment(env);

            findVariants(stmtIfElse.trueBranch, trueEnv, context);
            findVariants(stmtIfElse.falseBranch, falseEnv, context);

            env.merge(trueEnv, falseEnv);

        } else if (stmt instanceof Stmt.Switch) {
            Stmt.Switch switchStmt = (Stmt.Switch) stmt;

            for (Stmt.Case caseStmt : switchStmt.cases) {
                findVariants(caseStmt.stmts, env, context);
            }

        } else if (stmt instanceof Stmt.DoWhile) {
            Stmt.DoWhile whileStmt = (Stmt.DoWhile) stmt;
            // handle the do-while loop ?
            findVariants(whileStmt.body, env, context);
        } else if (stmt instanceof Stmt.NamedBlock) {
            Stmt.NamedBlock namedBlock = (Stmt.NamedBlock) stmt;
            findVariants(namedBlock.body, env, context);
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

                    // must be a variable of int type
                    // must have a simple mutation
                    if (lval.result().equals(wyil.lang.Type.T_INT)
                            // restrict variants to values defined out of the loop
                            && context.hasValue(assignedVariable.var)
                            && context.getValue(assignedVariable.var) != null
                            ) {
                        env.update(assignedVariable, determineSequenceDirection(assignedVariable, rval));
                    } else {
                        // invalidate the variant to be a candidate to check
                        // since the mutations are too complex to be handled
                        env.update(assignedVariable, SequenceDirection.UNDETERMINED);
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
    private boolean checkPreLoopValue(Expr preloop, LoopInvariantGenerator.Context context) {
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

            LoopInvariantGenerator.Variable e = context.getValue(localVar.var);
            if (e.getAssigned() != null) {
                return checkPreLoopValue(e.getAssigned(), context);
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
        UNDETERMINED,
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
        if (!isSimpleMutationOfVar(variable, expr)) {
           return SequenceDirection.UNDETERMINED;
        }

        BigInteger exprValInitial = evalConstExpr(variable, expr, BigInteger.ZERO);
        BigInteger exprValStep = evalConstExpr(variable, expr, exprValInitial);

        BigInteger difference = exprValStep.subtract(exprValInitial);

        if (difference.compareTo(BigInteger.ZERO) > 0) {
            return SequenceDirection.ASCENDING;
        } else if (difference.compareTo(BigInteger.ZERO) < 0)  {
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
    private BigInteger evalConstExpr(Expr.AssignedVariable variable, Expr expr, BigInteger varValue) {
        if (expr instanceof Expr.BinOp) {
            Expr.BinOp binop = (Expr.BinOp) expr;
            switch (binop.op) {
                case ADD:
                    return evalConstExpr(variable, binop.lhs, varValue).add(evalConstExpr(variable, binop.rhs, varValue));
                case SUB:
                    return evalConstExpr(variable, binop.lhs, varValue).subtract(evalConstExpr(variable, binop.rhs, varValue));
                default:
                    throw new UnsupportedOperationException();
            }
        } else if (expr instanceof Expr.Constant) {
            Expr.Constant constant = (Expr.Constant) expr;
            return ((Constant.Integer) constant.value).value();
        } else if (expr instanceof Expr.LocalVariable) {
            Expr.LocalVariable local = (Expr.LocalVariable) expr;

            if (local.var.equals(variable.var)) {
                return varValue;
            } else {
                throw new UnsupportedOperationException();
            }
        }

        throw new UnsupportedOperationException();
    }


    public static class Environment {

        private final Map<String, Pair<SequenceDirection, Expr.AssignedVariable>> variables;

        public Environment() {
           variables = new HashMap<>();
        }

        public Environment(Environment env) {
            // copy constructor
            variables = new HashMap<>(env.variables);
        }

        public void update(Expr.AssignedVariable variable, SequenceDirection direction) {
            String name = variable.var;

            if (variables.containsKey(name)) {
                SequenceDirection current = variables.get(name).first();

                if (direction != current) {
                    variables.put(name, new Pair<>(SequenceDirection.UNDETERMINED, null));
                }
            } else {
                variables.put(name, new Pair<>(direction, variable));
            }
        }

        public void merge(Environment... right) {
            for (Environment env : right) {
                for (Map.Entry<String, Pair<SequenceDirection, Expr.AssignedVariable>> entry : env.variables.entrySet()) {
                    this.update(entry.getValue().second(), entry.getValue().first());
                }
            }
        }
    }
}
