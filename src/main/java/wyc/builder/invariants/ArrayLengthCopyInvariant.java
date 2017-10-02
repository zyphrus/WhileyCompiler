package wyc.builder.invariants;

import wyc.lang.Expr;
import wyc.lang.Stmt;
import wycc.util.Pair;
import wyil.lang.Type;

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
    public List<Expr> generateInvariant(Stmt.While whileStmt, Util.Context context) {
        List<Expr> invariants = new ArrayList<>();

        // 1. identify arrays that are associated via array generators or copying
        Map<Expr.LocalVariable, Expr> associated =  associatedArrays(context);

        // 2. find array usages in the loop body
        Map<String, Boolean> validArrays = findArrays(whileStmt, context);
        Set<Pair<String, String>> added = new HashSet<>();

        for (Map.Entry<Expr.LocalVariable, Expr> entry : associated.entrySet()) {

            // 3. merges mapping with validArrays & associated arrays

            if (entry.getValue() == null ||
                    entry.getValue() instanceof Expr.LocalVariable &&
                    !validArrays.getOrDefault(((Expr.LocalVariable)entry.getValue()).var, false)) {
                continue;
            }

            // 4. Build the invariant expression
            Expr left = entry.getValue();
            if (left instanceof Expr.LocalVariable) {
                left = new Expr.UnOp(Expr.UOp.ARRAYLENGTH, entry.getKey());
            }
            Expr right = entry.getValue();
            if (right instanceof Expr.LocalVariable) {
                right = new Expr.UnOp(Expr.UOp.ARRAYLENGTH, entry.getValue());
            }

/*            if (left instanceof Expr.LocalVariable)
            if (added.contains(new Pair<>(entry.getKey().var, entry.getValue().var)) ||
                    added.contains(new Pair<>(entry.getValue().var, entry.getKey().var))) {
                continue;
            }*/

            Expr lengthOfLocal = new Expr.UnOp(Expr.UOp.ARRAYLENGTH, entry.getKey());
            Expr lengthOfBase = new Expr.UnOp(Expr.UOp.ARRAYLENGTH, entry.getValue());
            Expr invariant = new Expr.BinOp(Expr.BOp.EQ, left, right,
                    new Util.GeneratedAttribute("local array is generated with length equal to another array"));

            invariants.add(invariant);
            // added.add(new Pair<>(entry.getKey().var, entry.getValue().var));
        }

        return invariants;
    }

    /**
     * Find arrays that are associated through their length
     *
     * This is defined by an array being a copy of another array or using the
     * length of it in an array generator
     *
     * @param context
     * @return Mapping between associated arrays, only key->value
     */
    Map<Expr.LocalVariable, Expr> associatedArrays(Util.Context context) {

        Map<Expr.LocalVariable, Expr> associated = new HashMap<>();

        for (Map.Entry<String, Util.Variable> var : context.getVariables().entrySet()) {
            Type varType = Util.resolveType(var.getValue().getType(), context);

            if (varType instanceof Type.Array) {
                Expr assigned = var.getValue().getAssigned();
                Expr.LocalVariable left = new Expr.LocalVariable(var.getKey(), var.getValue().getAssigned().attributes());
                left.type = var.getValue().getType();
                if (assigned instanceof Expr.LocalVariable) {
                    Expr.LocalVariable localAssigned = (Expr.LocalVariable) assigned;

                    Expr right = getArrayGeneratorName(context, localAssigned);
                    if (right != null) {
                        if (right instanceof Expr.LocalVariable) {
                            associated.put(left, resolveTransative((Expr.LocalVariable) right, associated));
                        } else {
                            associated.put(left, right);
                        }
                        if (right instanceof Expr.LocalVariable && !associated.containsKey(right)) {
                            associated.put((Expr.LocalVariable) right, left);
                        }
                    }
                } else if (assigned instanceof Expr.ArrayGenerator) {
                    Expr.ArrayGenerator localGenerated = (Expr.ArrayGenerator) assigned;

                    if (localGenerated.count instanceof Expr.UnOp) {
                        Expr.UnOp lengthOf = (Expr.UnOp) localGenerated.count;

                        if (lengthOf.mhs instanceof Expr.LocalVariable) {
                            Expr.LocalVariable countVar = (Expr.LocalVariable) lengthOf.mhs;
                            Type resolved = Util.resolveType(countVar.type, context);

                            if (resolved instanceof Type.Array) {
                                associated.put(left, resolveTransative(countVar, associated));
                                if ( !associated.containsKey(countVar)) {
                                    associated.put(countVar, left);
                                }
                            }
                        }
                    }
                }
            }
        }
        return associated;
    }

    private Expr resolveTransative(Expr.LocalVariable val, Map<Expr.LocalVariable, Expr> associated) {
        Expr transative  = associated.get(val);

        if (transative == null) {
            return val;
        } else if (transative instanceof Expr.LocalVariable) {
            Expr.LocalVariable local = (Expr.LocalVariable) val;
            return resolveTransative(local, associated);
        } else if (transative instanceof  Expr.Constant) {
            return transative;
        }

        throw new IllegalArgumentException();
    }

    public static Map<String, Boolean> findArrays(Stmt.While whileStmt, Util.Context context) {
        Map<String, Boolean> variants = new HashMap<>();

        findArrays(whileStmt.body, variants, context);

        return variants;
    }

    public static void findArrays(List<Stmt> stmts, Map<String, Boolean> variants, Util.Context context) {
        for (Stmt stmt : stmts) {
            findArrays(stmt, variants, context);
        }
    }

    public static void findArrays(Stmt stmt, Map<String, Boolean> variants, Util.Context context) {
        // only check the guaranteed sections of the loop
        // and ignoring any branches or inner-loops to lessen complexity of identifying variants
        if (stmt instanceof Stmt.IfElse) {
            Stmt.IfElse stmtIfElse = (Stmt.IfElse) stmt;

            findArrays(stmtIfElse.trueBranch, variants, context);
            findArrays(stmtIfElse.falseBranch, variants, context);
        } else if (stmt instanceof Stmt.Switch) {
            Stmt.Switch switchStmt = (Stmt.Switch) stmt;
            for (Stmt.Case caseStmt : switchStmt.cases) {
                findArrays(caseStmt.stmts, variants, context);
            }
        } else if (stmt instanceof Stmt.DoWhile) {
            Stmt.DoWhile whileStmt = (Stmt.DoWhile) stmt;
            // handle the do-while loop ?
            findArrays(whileStmt.body, variants,  context);
        } else if (stmt instanceof Stmt.NamedBlock) {
            Stmt.NamedBlock namedBlock = (Stmt.NamedBlock) stmt;
            findArrays(namedBlock.body, variants, context);
        } else if (stmt instanceof Stmt.Assign) {
            Stmt.Assign stmtAssign = (Stmt.Assign) stmt;

            // would of liked to chain these two lists together
            for (Expr.LVal lval : stmtAssign.lvals) {
                if (lval instanceof Expr.AssignedVariable) {
                    Expr.AssignedVariable assignedVariable = (Expr.AssignedVariable) lval;
                    // remove from variants
                    variants.put(assignedVariable.var, false);
                } else {

                    findArrays(lval, variants);

                }
            }

            for (Expr rval : stmtAssign.rvals) {
                findArrays(rval, variants);
            }
        }
    }

    public static void findArrays(Expr expr, Map<String, Boolean> arrays) {

        if (expr instanceof Expr.LocalVariable) {
            Expr.LocalVariable local = (Expr.LocalVariable) expr;

            if (local.type instanceof Type.Array) {
                arrays.put(local.var, true);
            }
        } else  if (expr instanceof Expr.Constant) {
            // ignore
        } else if (expr instanceof Expr.IndexOf) {
            Expr.IndexOf index = (Expr.IndexOf) expr;
            findArrays(index.src, arrays);
            findArrays(index.index, arrays);
        } else if (expr instanceof Expr.BinOp) {
            Expr.BinOp binOp = (Expr.BinOp) expr;

            findArrays(binOp.lhs, arrays);
            findArrays(binOp.rhs, arrays);
        } else if (expr instanceof Expr.UnOp) {
            Expr.UnOp uOp = (Expr.UnOp) expr;

            findArrays(uOp.mhs, arrays);
        } else if (expr instanceof Expr.IndirectFunctionOrMethodCall) {
            Expr.IndirectFunctionOrMethodCall call = (Expr.IndirectFunctionOrMethodCall) expr;
            for (Expr param : call.arguments) {
                findArrays(param, arrays);
            }
        } else if (expr instanceof Expr.FunctionOrMethodCall) {
            Expr.FunctionOrMethodCall call = (Expr.FunctionOrMethodCall) expr;
            for (Expr param : call.arguments) {
                findArrays(param, arrays);
            }
        } else if (expr instanceof Expr.Cast) {
            Expr.Cast cast = (Expr.Cast) expr;

            findArrays(cast.expr, arrays);
        } else if (expr instanceof Expr.FieldAccess) {
            Expr.FieldAccess access = (Expr.FieldAccess) expr;

            findArrays(access.src, arrays);
        } else if (expr instanceof Expr.ArrayInitialiser) {
            Expr.ArrayInitialiser initialiser = (Expr.ArrayInitialiser) expr;

            for (Expr e : initialiser.arguments) {
                findArrays(e, arrays);
            }
        } else if (expr instanceof Expr.Record) {
            Expr.Record record = (Expr.Record) expr;

            for (Expr e : record.fields.values()) {
                findArrays(e, arrays);
            }
        } else if (expr instanceof Expr.ArrayGenerator) {
            Expr.ArrayGenerator gen = (Expr.ArrayGenerator) expr;

            findArrays(gen.count, arrays);
            findArrays(gen.element, arrays);
        } else if (expr instanceof Expr.Dereference) {
            Expr.Dereference deref = (Expr.Dereference) expr;

            findArrays(deref.src, arrays);
        } else {
            throw new IllegalStateException();
        }

    }


    /**
     * Get the name of the value
     *
     * @param context
     * @param local The local variable to be checked
     */
    private Expr getArrayGeneratorName(Util.Context context, Expr.LocalVariable local) {

        Util.Variable var = context.getValue(local.var);

        if (var == null || var.getAssigned() == null) {
            if (context.hasParam(local.var)) {
                return local;
            }
            return null;
        }

        if (var.getAssigned() instanceof Expr.ArrayGenerator) {
            Expr.ArrayGenerator generator = (Expr.ArrayGenerator) var.getAssigned();

            if (generator.count instanceof Expr.UnOp) {
                Expr.UnOp generatorCount = (Expr.UnOp) generator.count;

                if (generatorCount.op == Expr.UOp.ARRAYLENGTH) {
                    if (generatorCount.mhs instanceof Expr.LocalVariable) {
                        Expr.LocalVariable localLengthOf = (Expr.LocalVariable) generatorCount.mhs;

                        Util.Variable basedOff = context.getValue(localLengthOf.var);
                        if (basedOff != null && basedOff.getAssigned() != null) {
                            return localLengthOf;
                        } else if (context.hasParam(localLengthOf.var)) {
                            return localLengthOf;
                        }
                    }
                }
            } else if (generator.count instanceof Expr.Constant){
                return generator.count;
            }
        } else if (var.getAssigned() instanceof Expr.LocalVariable) {
            // second case where the array is a full copy of the other array
            return (Expr.LocalVariable) var.getAssigned();
        }

        return null;
    }

}
