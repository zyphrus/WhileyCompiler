package wyc.builder.invariants;

import wyc.builder.LoopInvariantGenerator;
import wyc.lang.Expr;
import wyc.lang.Stmt;

import java.util.*;

/**
 * This generator creates an invariant when an array is initialised
 * with a generator and the length given is the length of another list.
 *
 * The invariant is only then applied if the new array is indexed & assigned a value
 *
 * Example of valid Whiley code to generate the invariant for
 * <code>
 *     int i = 0
 *     int[] xs = [0;|as|]
 *     while i < |xs|:
 *          xs[i] = f(as[i]) // what is being assigned does not matter
 * </code>
 */
public class ArrayLengthCopyInvariant implements InvariantGenerator {

    @Override
    public List<Expr> generateInvariant(Stmt.While whileStmt, LoopInvariantGenerator.Context context) {
        List<Expr> invariants = new ArrayList<>();

        Map<Expr.LocalVariable, Expr.LocalVariable> l = findGeneratedArrays(whileStmt, context);

        for (Map.Entry<Expr.LocalVariable, Expr.LocalVariable> entry : l.entrySet()) {

            if (entry.getValue() == null) {
                continue;
            }

            Expr lengthOfLocal = new Expr.UnOp(Expr.UOp.ARRAYLENGTH, entry.getKey());
            Expr lengthOfBase = new Expr.UnOp(Expr.UOp.ARRAYLENGTH, entry.getValue());
            Expr invariant = new Expr.BinOp(Expr.BOp.EQ, lengthOfLocal, lengthOfBase,
                    new LoopInvariantGenerator.GeneratedAttribute("local array is generated with length equal to another array"));

            invariants.add(invariant);
        }

        return invariants;
    }

    private Map<Expr.LocalVariable, Expr.LocalVariable> findGeneratedArrays(Stmt.While whileStmt, LoopInvariantGenerator.Context context) {
        Map<Expr.LocalVariable, Expr.LocalVariable> variants = new HashMap<>();

        this.findGeneratedArrays(whileStmt.body, variants, true, context);

        return variants;
    }

    private void findGeneratedArrays(List<Stmt> stmts, Map<Expr.LocalVariable, Expr.LocalVariable> variants, boolean topLevel, LoopInvariantGenerator.Context context) {
        for (Stmt stmt : stmts) {
            findGeneratedArrays(stmt, variants, topLevel, context);
        }
    }

    private void findGeneratedArrays(Stmt stmt, Map<Expr.LocalVariable, Expr.LocalVariable> variants, boolean topLevel, LoopInvariantGenerator.Context context) {
        // only check the guaranteed sections of the loop
        // and ignoring any branches or inner-loops to lessen complexity of identifying variants
        if (stmt instanceof Stmt.IfElse) {
            Stmt.IfElse stmtIfElse = (Stmt.IfElse) stmt;

            this.findGeneratedArrays(stmtIfElse.trueBranch, variants, false, context);
            this.findGeneratedArrays(stmtIfElse.falseBranch, variants, false, context);
        } else if (stmt instanceof Stmt.Switch) {
            Stmt.Switch switchStmt = (Stmt.Switch) stmt;
            for (Stmt.Case caseStmt : switchStmt.cases) {
                this.findGeneratedArrays(caseStmt.stmts, variants, false, context);
            }
        } else if (stmt instanceof Stmt.DoWhile) {
            Stmt.DoWhile whileStmt = (Stmt.DoWhile) stmt;
            // handle the do-while loop ?
            this.findGeneratedArrays(whileStmt.body, variants, false, context);
        } else if (stmt instanceof Stmt.NamedBlock) {
            Stmt.NamedBlock namedBlock = (Stmt.NamedBlock) stmt;
            this.findGeneratedArrays(namedBlock.body, variants, topLevel, context);
        } else if (stmt instanceof Stmt.Assign) {
            Stmt.Assign stmtAssign = (Stmt.Assign) stmt;

            // would of liked to chain these two lists together
            for (Expr.LVal lval : stmtAssign.lvals) {

                if (lval instanceof Expr.IndexOf) {
                    Expr.IndexOf indexOf = (Expr.IndexOf) lval;


                    if (indexOf.src instanceof Expr.LocalVariable) {
                        Expr.LocalVariable local = (Expr.LocalVariable) indexOf.src;

                        Optional<Expr.LocalVariable> duplicate = variants.keySet().stream()
                                .filter(k -> k.var.equals(local.var))
                                .findFirst();

                        Expr.LocalVariable name = getArrayGeneratorName(context, local);

                        if (name != null &&
                                (!duplicate.isPresent() || duplicate.get() != null)) {
                            variants.put(local, name);
                        } else {
                            variants.put(local, null);
                        }
                    }
                } else if (lval instanceof Expr.AssignedVariable) {
                    Expr.AssignedVariable assignedVariable = (Expr.AssignedVariable) lval;

                    // remove from variants
                    variants.put(new Expr.LocalVariable(assignedVariable.var), null);
                }
            }
        }
    }

    /**
     * Get the name of the value
     *
     * @param context
     * @param local The local variable to be checked
     */
    private Expr.LocalVariable getArrayGeneratorName(LoopInvariantGenerator.Context context, Expr.LocalVariable local) {

        LoopInvariantGenerator.Variable var = context.getValue(local.var);

        if (var == null || var.getAssigned() == null) {
            return null;
        }

        if (var.getAssigned() instanceof Expr.ArrayGenerator) {
            Expr.ArrayGenerator generator = (Expr.ArrayGenerator) var.getAssigned();

            if (generator.count instanceof Expr.UnOp) {
                Expr.UnOp generatorCount = (Expr.UnOp) generator.count;

                if (generatorCount.op == Expr.UOp.ARRAYLENGTH) {
                    if (generatorCount.mhs instanceof Expr.LocalVariable) {
                        Expr.LocalVariable localLengthOf = (Expr.LocalVariable) generatorCount.mhs;

                        LoopInvariantGenerator.Variable basedOff = context.getValue(localLengthOf.var);
                        if (basedOff != null && basedOff.getAssigned() != null) {
                            return localLengthOf;
                        } else if (context.hasParam(localLengthOf.var)) {
                            return localLengthOf;
                        }
                    }
                }
            }
        } else if (var.getAssigned() instanceof Expr.LocalVariable) {
            // second case where the array is a full copy of the other array
            return (Expr.LocalVariable) var.getAssigned();
        }

        return null;
    }

}
