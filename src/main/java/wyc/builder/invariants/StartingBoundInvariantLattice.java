package wyc.builder.invariants;

import wyc.lang.Expr;
import wyc.lang.Stmt;
import wycc.util.Pair;
import wyil.lang.Constant;

import java.math.BigInteger;
import java.util.*;

public class StartingBoundInvariantLattice implements InvariantGenerator {

    @Override
    public List<Expr> generateInvariant(Stmt.While whileStmt, Util.Context context) {
        List<Expr> invariants = new ArrayList<>();

        Map<String, Pair<SequenceDirection, Expr.AssignedVariable>>
                env = findVariants(whileStmt, context);

        for (Map.Entry<String, Pair<SequenceDirection, Expr.AssignedVariable>> variant : env.entrySet()) {

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
    private Expr startingBoundInvariant(Expr.AssignedVariable variant, SequenceDirection direction, Util.Context context) {

        // check if the mutation is invalid
        if (direction == SequenceDirection.UNKNOWN || direction == SequenceDirection.UNDETERMINED) {
            System.err.println("unable to determine sequence direction of variant");
            return null;
        }

        // TODO: detect if the value before the loop is safe or not
        // could extend this to create a ghost variable instead, however for now keeping to safe
        Util.Variable preLoopValue = context.getValue(variant.var);
        if (!checkPreLoopValue(preLoopValue.getAssigned(), context)) {
            System.err.println("Oh my, the entrant value is not safe for " + variant + " with " + preLoopValue );
            return null;
        }

        // given the
        return new Expr.BinOp(direction.toBOp(), variant, preLoopValue.getAssigned(),
                new Util.GeneratedAttribute("Inferred starting boundary of variable " + variant));
    }

    public static Map<String, Pair<SequenceDirection, Expr.AssignedVariable>> findVariants(Stmt.While whileStmt, Util.Context context) {
        Environment env = new Environment();

        findVariants(whileStmt.body, env, context);

        return env.variables;
    }

    private static void findVariants(List<Stmt> stmts, Environment env, Util.Context context) {
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
    private static void findVariants(Stmt stmt, Environment env, Util.Context context) {
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
    private boolean checkPreLoopValue(Expr preloop, Util.Context context) {
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

            Util.Variable e = context.getValue(localVar.var);
            if (e.getAssigned() != null) {
                return checkPreLoopValue(e.getAssigned(), context);
            }
        }

        return false;
    }

    public enum SequenceDirection {
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


    private static SequenceDirection determineSequenceDirection(Expr.AssignedVariable variable, Expr expr) {
        if (!Util.isSimpleMutationOfVar(variable, expr)) {
           return SequenceDirection.UNDETERMINED;
        }

        BigInteger exprValInitial = Util.evalConstExpr(variable, expr, BigInteger.ZERO);
        BigInteger exprValStep = Util.evalConstExpr(variable, expr, exprValInitial);

        BigInteger difference = exprValStep.subtract(exprValInitial);

        if (difference.compareTo(BigInteger.ZERO) > 0) {
            return SequenceDirection.ASCENDING;
        } else if (difference.compareTo(BigInteger.ZERO) < 0)  {
            return SequenceDirection.DESCENDING;
        } else {
            return SequenceDirection.UNKNOWN;
        }
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

        public Map<String, Pair<SequenceDirection, Expr.AssignedVariable>> variables() {
            return this.variables;
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
