package wyc.builder.invariants;

import wyc.lang.Expr;
import wyc.lang.Stmt;
import wyil.lang.Constant;

import java.math.BigInteger;
import java.util.*;

public class StartingBoundInvariant implements InvariantGenerator {

    @Override
    public List<Expr> generateInvariant(Stmt.While whileStmt, Util.Context context) {
        List<Expr> invariants = new ArrayList<>();

        Map<Expr.LVal, Expr> variants = findVariants(whileStmt, context);
        for (Map.Entry<Expr.LVal, Expr> variant : variants.entrySet()) {

            if (variant.getValue() != null && variant.getKey() instanceof Expr.AssignedVariable) {
                Expr invariant = startingBoundInvariant((Expr.AssignedVariable) variant.getKey(), variant.getValue(), context);

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
     * @param variantExpr the expression that mutates the variant
     * @param context a store of local variables and paramters
     * @return expression of invariant, otherwise null
     */
    private Expr startingBoundInvariant(Expr.AssignedVariable variant, Expr variantExpr, Util.Context context) {

        // determine the direction of the mutation, + or -
        SequenceDirection direction = determineSequenceDirection(variant, variantExpr);
        // check if the mutation is invalid
        if (direction == SequenceDirection.UNKNOWN) {
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
                new Util.GeneratedAttribute("Inferred starting boundary of variable " + variant + " from loop body"));
    }

    private Map<Expr.LVal, Expr> findVariants(Stmt.While whileStmt, Util.Context context) {
        Map<Expr.LVal, Expr> variants = new HashMap<>();

        findVariants(whileStmt.body, variants, true, context);

        return variants;
    }

    private void findVariants(List<Stmt> stmts, Map<Expr.LVal, Expr> variants, boolean topLevel, Util.Context context) {
        for (Stmt stmt : stmts) {
            findVariants(stmt, variants, topLevel, context);
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
     * @param variants map of variants already identified
     * @param topLevel check to make sure we are at the top level of the while loop
     * @param context collection of local variables and parameters
     */
    private void findVariants(Stmt stmt, Map<Expr.LVal, Expr> variants, boolean topLevel, Util.Context context) {
        // only check the guaranteed sections of the loop
        // and ignoring any branches or inner-loops to lessen complexity of identifying variants
        if (stmt instanceof Stmt.IfElse) {
            Stmt.IfElse stmtIfElse = (Stmt.IfElse) stmt;

            findVariants(stmtIfElse.trueBranch, variants, false, context);
            findVariants(stmtIfElse.falseBranch, variants, false, context);
        } else if (stmt instanceof Stmt.Switch) {
            Stmt.Switch switchStmt = (Stmt.Switch) stmt;
            for (Stmt.Case caseStmt : switchStmt.cases) {
                findVariants(caseStmt.stmts, variants, false, context);
            }
        } else if (stmt instanceof Stmt.DoWhile) {
            Stmt.DoWhile whileStmt = (Stmt.DoWhile) stmt;
            // handle the do-while loop ?
            findVariants(whileStmt.body, variants, false, context);
        } else if (stmt instanceof Stmt.NamedBlock) {
            Stmt.NamedBlock namedBlock = (Stmt.NamedBlock) stmt;
            findVariants(namedBlock.body, variants, topLevel, context);
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
                            && Util.isSimpleMutationOfVar(assignedVariable, rval)
                            // ensure that variants found to be assigned in inner branches/loops are invalidated as
                            // candidates
                            && topLevel
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

}
