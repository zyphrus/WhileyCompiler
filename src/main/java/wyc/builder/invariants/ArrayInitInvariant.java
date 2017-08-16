package wyc.builder.invariants;

import wyc.lang.Expr;
import wyc.lang.Stmt;
import wycc.util.Pair;
import wycc.util.Triple;
import wyil.lang.Type;

import java.util.*;
import java.util.stream.Collectors;

public class ArrayInitInvariant implements InvariantGenerator {

    @Override
    public List<Expr> generateInvariant(Stmt.While whileStmt, Util.Context context) {
        List<Expr> invariants = new ArrayList<>();

        // 1. Identify variants with a sequence direction (the index)
        Map<String, Pair<StartingBoundInvariant.SequenceDirection, Expr.AssignedVariable>>
                variableSquence = StartingBoundInvariant.findVariants(whileStmt, context);

        // 2. Find all arrays that assign to an element each iteration (no if's / inner-loops)
        Map<Expr.IndexOf, Expr> elements = findElementAssignment(whileStmt, context);

        for (Map.Entry<Expr.IndexOf, Expr> entry : elements.entrySet()) {

            // 3. Ensure it is a simple index into array
            Expr.IndexOf index = entry.getKey();
            if (!(index.index instanceof Expr.LocalVariable)) {
                continue;
            }

            // 4. Create new variable for the index
            Expr.LocalVariable var = (Expr.LocalVariable) index.index;
            String indexName = var.var;
            String newIndexName =  "_" + var.var ;

            if (!variableSquence.containsKey(indexName) ||
                    !allVariablesInContext(entry.getValue(), context)) {
                continue;
            }

            // 5. Determine the range of the quantifier
            Pair<StartingBoundInvariant.SequenceDirection, Expr.AssignedVariable>
                v = variableSquence.get(indexName);

            Expr start = null;
            Expr end = null;

            // make sure the range of values are in the right order
            if (v.first() == StartingBoundInvariant.SequenceDirection.ASCENDING) {
                start = context.getValue(indexName).getAssigned();
                end = var;
            } else if (v.first() == StartingBoundInvariant.SequenceDirection.DESCENDING) {
                start = var;
                end = context.getValue(indexName).getAssigned();
            }

            // ensure that values from context are valid
            if (start == null || end == null) {
                continue;
            }

            // 6. Build quantifier expression
            // Building the initialise for the quantifier
            List<Triple<String, Expr, Expr>> ranges = new ArrayList<>();
            Triple<String, Expr, Expr> range = new Triple<>(newIndexName, start, end);
            ranges.add(range);

            // building the condition for the quantifier
            Expr condition = new Expr.BinOp(Expr.BOp.EQ,
                    replaceVariable(index, indexName, newIndexName),
                    replaceVariable(entry.getValue(), indexName, newIndexName));

            // Assembling the quantifier
            Expr.Quantifier invariant = new Expr.Quantifier(Expr.QOp.ALL, ranges, condition,
                    new Util.GeneratedAttribute("Elements are predictability set iteratively"));
            invariant.type = Type.T_BOOL;

            invariants.add(invariant);
        }

        return invariants;
    }

    /**
     * Replaces a variable within an expression with another
     *
     * @param original expression to be replaced
     * @param from the original name of the variable
     * @param to the name of the new variable after the replacement
     * @return a clone of the expression with the variable names swapped
     */
    private Expr replaceVariable(Expr original, String from, String to) {

        if (original instanceof Expr.LocalVariable) {
            Expr.LocalVariable local = (Expr.LocalVariable) original;

            if (local.var.equals(from)) {
                Expr.LocalVariable newLocal = new Expr.LocalVariable(to, local.attributes());
                newLocal.type = local.type;
                return newLocal;
            } else {
                Expr.LocalVariable newLocal = new Expr.LocalVariable(local.var, local.attributes());
                newLocal.type = local.type;
                return newLocal;
            }
        } else if (original instanceof Expr.IndexOf) {

            Expr.IndexOf indexOf = (Expr.IndexOf) original;
            Expr index = replaceVariable(indexOf.index, from ,to);
            Expr src = replaceVariable(indexOf.src, from ,to);

            Expr.IndexOf newIndex = new Expr.IndexOf(src, index, original.attributes());
            newIndex.srcType =  indexOf.srcType;
            return newIndex;
        } else if (original instanceof Expr.BinOp) {
            Expr.BinOp binOp = (Expr.BinOp) original;

            Expr lhs = replaceVariable(binOp.lhs, from, to);
            Expr rhs = replaceVariable(binOp.rhs, from, to);

            Expr.BinOp newBinOp = new Expr.BinOp(binOp.op, lhs, rhs, binOp.attributes());
            newBinOp.srcType = binOp.srcType;
            return newBinOp;
        } else if (original instanceof Expr.FunctionCall) {
            Expr.FunctionCall call = (Expr.FunctionCall) original;

            Collection<Expr> args = call.arguments.stream()
                    .map(arg -> replaceVariable(arg, from ,to))
                    .collect(Collectors.toList());

            Expr.FunctionCall newCall = new Expr.FunctionCall(call.nid, call.qualification, args, call.attributes());
            newCall.functionType = call.functionType;
            return newCall;
        }

        throw new UnsupportedOperationException();

    }

    /**
     * Identifies arrays with assignments to elements
     *
     * Filters out
     *
     * @param whileStmt
     * @param context
     * @return A mapping between inxdexOf assignments to the expression used
     */
    public Map<Expr.IndexOf, Expr> findElementAssignment(Stmt.While whileStmt, Util.Context context) {
        Map<Expr.IndexOf, Expr> variants = new HashMap<>();

        for (Stmt stmt : whileStmt.body) {
            findElementAssignment(stmt, variants, context);
        }

        variants.values().removeIf(Objects::isNull);

        return variants;
    }

    private void findElementAssignment(Stmt stmt, Map<Expr.IndexOf, Expr> variants, Util.Context context) {
        if (stmt instanceof Stmt.Assign) {
            Stmt.Assign assign = (Stmt.Assign) stmt;

            Iterator<Expr.LVal> leftIter = assign.lvals.iterator();
            Iterator<Expr> rightIter = assign.rvals.iterator();

            while (leftIter.hasNext() && rightIter.hasNext()) {
                Expr.LVal left = leftIter.next();
                Expr right = rightIter.next();

                if (left instanceof Expr.IndexOf) {
                    Expr.IndexOf index = (Expr.IndexOf) left;

                    if (index.src instanceof Expr.LocalVariable) {
                        Expr.LocalVariable localIndex = (Expr.LocalVariable) index.src;
                        Optional<Expr.IndexOf> duplicate =  variants.keySet().stream()
                                .filter(i -> {
                                    if (i.src instanceof Expr.LocalVariable) {
                                        Expr.LocalVariable l = (Expr.LocalVariable)(i.src);
                                        return l.var.equals(localIndex.var);
                                    }
                                    return false;
                                })
                                .findFirst();

                        if (!isDependentOnSelf(localIndex.var, right) && !duplicate.isPresent()) {
                            variants.put(index, right);
                        } else{
                            if (duplicate.isPresent()) {
                                variants.put(duplicate.get(), null);
                            } else {
                                variants.put(index, null);
                            }
                        }
                    }
                }
            }

        }
    }

    private boolean allVariablesInContext(Expr expr, Util.Context context) {

        if (expr instanceof Expr.LocalVariable) {
            Expr.LocalVariable local = (Expr.LocalVariable) expr;

            return context.hasValue(local.var) || context.hasParam(local.var);
        } else if (expr instanceof Expr.BinOp) {
            Expr.BinOp binOp = (Expr.BinOp) expr;

            return allVariablesInContext(binOp.lhs, context) &&
                    allVariablesInContext(binOp.rhs, context);
        } else if (expr instanceof Expr.IndexOf) {
            Expr.IndexOf index = (Expr.IndexOf) expr;

            return allVariablesInContext(index.src, context) &&
                    allVariablesInContext(index.index, context);
        } else if (expr instanceof Expr.Constant) {
            return true;
        } else if (expr instanceof Expr.FunctionCall) {
            Expr.FunctionCall call = (Expr.FunctionCall) expr;

            boolean result = true;
            for (Expr arg : call.arguments) {
                result &= allVariablesInContext(arg, context);
            }
            return result;
        } else if (expr instanceof Expr.ArrayInitialiser) {
            Expr.ArrayInitialiser arrayInit = (Expr.ArrayInitialiser) expr;

            boolean result = true;
            for (Expr arg : arrayInit.arguments) {
                result &= allVariablesInContext(arg, context);
            }
            return result;
        }

        throw new UnsupportedOperationException();
    }

    /**
     * Checks if the given variable is in the mutation expression
     *
     * @param name of the variable to check
     * @param mutation expression to be checked
     * @return is true if the expression contains the given variable
     */
    private boolean isDependentOnSelf(String name, Expr mutation) {


        if (mutation instanceof Expr.LocalVariable) {
            Expr.LocalVariable local = (Expr.LocalVariable) mutation;

            return local.var.equals(name);
        } else if (mutation instanceof Expr.BinOp) {
            Expr.BinOp binOp = (Expr.BinOp) mutation;

            return isDependentOnSelf(name, binOp.lhs) ||
                    isDependentOnSelf(name, binOp.rhs);
        } else if (mutation instanceof Expr.IndexOf) {
            Expr.IndexOf index = (Expr.IndexOf) mutation;

            return isDependentOnSelf(name, index.src) ||
                    isDependentOnSelf(name, index.index);
        } else if (mutation instanceof Expr.Constant) {
            return true;
        } else if (mutation instanceof Expr.FunctionCall) {
            Expr.FunctionCall call = (Expr.FunctionCall) mutation;

            boolean result = false;
            for (Expr arg : call.arguments) {
                result |= isDependentOnSelf(name, arg);
            }
            return result;
        } else if (mutation instanceof Expr.ArrayInitialiser) {
            Expr.ArrayInitialiser arrayInit = (Expr.ArrayInitialiser) mutation;

            boolean result = false;
            for (Expr arg : arrayInit.arguments) {
                result |= isDependentOnSelf(name, arg);
            }
            return result;
        }

        throw new UnsupportedOperationException();
    }

}
