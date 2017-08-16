package wyc.builder.invariants;

import wyc.lang.Expr;
import wyc.lang.Stmt;
import wyil.lang.WyilFile;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class ConditionAgingInvariant implements InvariantGenerator {

    @Override
    public List<Expr> generateInvariant(Stmt.While whileStmt, Util.Context context) {
        List<Expr> invariants = new ArrayList<>();

        // 1. breakdown loop condition into small segments
        List<Expr> breakdown = breakdownConditional(whileStmt.condition);

        // 2. identify 'simple' conditions (<, > for now)
        List<Expr> simpleConditions = filterSimpleConditions(breakdown, context);

        // 3. search loop body for simple variants (e.g. i = i + 1 )
        Map<String, BigInteger> variants = findSimpleVariants(whileStmt.body, context);

        // 4. Find the intersection between the simple variants and the simple conditions
        for (Expr cond : simpleConditions) {
            Set<String> usedVariables = variablesUsedIn(cond);
            for (String used : usedVariables) {
                if (variants.containsKey(used)) {
                    // 5. apply ageing to the condition based off the variants (should it be max difference?)
                    Expr aged = applyAgingTo(cond, used, variants.get(used));
                    // 6. add invariant
                    aged.attributes().add(new Util.GeneratedAttribute("Aged loop condition"));
                    invariants.add(aged);
                }
            }
        }

        return invariants;
    }


    /**
     * Breaks down a boolean expression into conjunctive components
     * e.g. i > 0 && i < |xs| is broken down to [ i > 0, i < |xs| ]
     *
     * @param condition expression to be broken down into components
     * @return
     */
    private List<Expr> breakdownConditional(Expr condition) {
        List<Expr> breakdown = new ArrayList<>();

        if (condition instanceof Expr.BinOp) {
            Expr.BinOp binOp = (Expr.BinOp) condition;

            switch(binOp.op) {
                case AND:
                    breakdown.addAll(breakdownConditional(binOp.lhs));
                    breakdown.addAll(breakdownConditional(binOp.rhs));
                    break;
                default:
                    breakdown.add(binOp);
            }
        } else {
            breakdown.add(condition);
        }

        return breakdown;
    }

    /**
     * Filters conditions list so that only simple conditions remain
     *
     * A Simple condition is <, <=, > and => between constants and variables
     *
     * @param conditions to filter from
     * @param context of the variables
     * @return list of expressions that are simple
     */
    private List<Expr> filterSimpleConditions(List<Expr> conditions, Util.Context context) {
        List<Expr> simple = new ArrayList<>();

        for (Expr cond : conditions) {
            if (cond instanceof Expr.BinOp) {
                Expr.BinOp binOp = (Expr.BinOp) cond;

                switch (binOp.op) {
                    case LT:
                    //case LTEQ:
                    case GT:
                    //case GTEQ:
                        if (isSimple(binOp.lhs, context) && isSimple(binOp.rhs, context)) {
                            simple.add(binOp);
                        }
                        break;
                }
            }
        }
        return simple;
    }

    /**
     *
     * Does not allow parameters to be used in a simple expression
     *
     * @param expr
     * @param context
     * @return
     */
    private boolean isSimple(Expr expr, Util.Context context) {
        if (expr instanceof Expr.LocalVariable) {
            Expr.LocalVariable local = (Expr.LocalVariable) expr;

            Util.Variable var = context.getValue(local.var);
            if (var != null && var.getAssigned() != null) {
                return true;
            }
        } else if (expr instanceof Expr.Constant) {
            return true;
        } else if (expr instanceof Expr.UnOp) {
            Expr.UnOp unOp = (Expr.UnOp) expr;

            return isSimple(unOp.mhs, context);
        }

        return false;
    }

    private Map<String, BigInteger> findSimpleVariants(ArrayList<Stmt> body, Util.Context context) {
        Environment root = new Environment();

        findSimpleVariants(body, root, context);


        return root.getMutations();
    }

    private void findSimpleVariants(ArrayList<Stmt> body, Environment env, Util.Context context) {
       for (Stmt stmt : body) {
           findSimpleVariants(stmt, env, context);
       }
    }

    private void findSimpleVariants(Stmt stmt, Environment env, Util.Context context) {
        if (stmt instanceof Stmt.IfElse) {
            Stmt.IfElse stmtIfElse = (Stmt.IfElse) stmt;

            Environment trueEnv = new Environment(env);
            Environment falseEnv = new Environment(env);

            findSimpleVariants(stmtIfElse.trueBranch, trueEnv, context);
            findSimpleVariants(stmtIfElse.falseBranch, falseEnv, context);

            env.merge(trueEnv, falseEnv);

        } else if (stmt instanceof Stmt.Switch) {
            Stmt.Switch switchStmt = (Stmt.Switch) stmt;

            for (Stmt.Case caseStmt : switchStmt.cases) {
                findSimpleVariants(caseStmt.stmts, env, context);
            }

        } else if (stmt instanceof Stmt.DoWhile) {
            Stmt.DoWhile whileStmt = (Stmt.DoWhile) stmt;
            // handle the do-while loop ?
            findSimpleVariants(whileStmt.body, env, context);
        } else if (stmt instanceof Stmt.NamedBlock) {
            Stmt.NamedBlock namedBlock = (Stmt.NamedBlock) stmt;
            findSimpleVariants(namedBlock.body, env, context);
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
                            && Util.isSimpleMutationOfVar(assignedVariable, rval)
                            ) {

                        BigInteger change = Util.evalConstExpr(assignedVariable, rval, BigInteger.ZERO);
                        if (change.equals(BigInteger.ONE) || change.equals(BigInteger.ONE.negate())) {
                            env.update(assignedVariable.var, change);
                        }
                    } else {
                        // invalidate the variant to be a candidate to check
                        // since the mutations are too complex to be handled
                        env.update(assignedVariable.var,  null);
                    }
                }
            }
        }
    }

    private Set<String> variablesUsedIn(Expr expr) {
        Set<String> names = new HashSet<>();

        variablesUsedIn(expr, names);

        return names;
    }

    private void variablesUsedIn(Expr expr, Set<String> names) {
        if (expr instanceof Expr.LocalVariable) {
            Expr.LocalVariable local = (Expr.LocalVariable) expr;

            names.add(local.var);
        } else  if (expr instanceof Expr.Constant) {
            // ignore
        } else if (expr instanceof Expr.IndexOf) {
            Expr.IndexOf index = (Expr.IndexOf) expr;
            variablesUsedIn(index.src, names);
            variablesUsedIn(index.index, names);
        } else if (expr instanceof Expr.BinOp) {
            Expr.BinOp binOp = (Expr.BinOp) expr;

            variablesUsedIn(binOp.lhs, names);
            variablesUsedIn(binOp.rhs, names);
        } else if (expr instanceof Expr.UnOp) {
            Expr.UnOp uOp = (Expr.UnOp) expr;

            variablesUsedIn(uOp.mhs, names);
        } else if (expr instanceof Expr.IndirectFunctionOrMethodCall) {
            Expr.IndirectFunctionOrMethodCall call = (Expr.IndirectFunctionOrMethodCall) expr;
            for (Expr param : call.arguments) {
                variablesUsedIn(param, names);
            }
        } else if (expr instanceof Expr.FunctionOrMethodCall) {
            Expr.FunctionOrMethodCall call = (Expr.FunctionOrMethodCall) expr;
            for (Expr param : call.arguments) {
                variablesUsedIn(param, names);
            }
        } else if (expr instanceof Expr.Cast) {
            Expr.Cast cast = (Expr.Cast) expr;

            variablesUsedIn(cast.expr, names);
        } else if (expr instanceof Expr.FieldAccess) {
            Expr.FieldAccess access = (Expr.FieldAccess) expr;

            variablesUsedIn(access.src, names);
        } else if (expr instanceof Expr.ArrayInitialiser) {
            Expr.ArrayInitialiser initialiser = (Expr.ArrayInitialiser) expr;

            for (Expr e : initialiser.arguments) {
                variablesUsedIn(e, names);
            }
        } else if (expr instanceof Expr.Record) {
            Expr.Record record = (Expr.Record) expr;

            for (Expr e : record.fields.values()) {
                variablesUsedIn(e, names);
            }
        } else if (expr instanceof Expr.ArrayGenerator) {
            Expr.ArrayGenerator gen = (Expr.ArrayGenerator) expr;

            variablesUsedIn(gen.count, names);
            variablesUsedIn(gen.element, names);
        } else if (expr instanceof Expr.Dereference) {
            Expr.Dereference deref = (Expr.Dereference) expr;

            variablesUsedIn(deref.src, names);
        } else {
            throw new IllegalStateException();
        }
    }

    private Expr applyAgingTo(Expr expr, String name, BigInteger amount) {
        if (expr instanceof Expr.LocalVariable) {
            Expr.LocalVariable local = (Expr.LocalVariable) expr;

            if (local.var.equals(name)) {
                Expr.Constant aged = new Expr.Constant(new wyil.lang.Constant.Integer(amount));

                Expr.BinOp updated = new Expr.BinOp(Expr.BOp.SUB, local, aged);
                updated.srcType = local.type;
                return updated;
            } else {
                return local;
            }
        } else  if (expr instanceof Expr.Constant) {
            Expr.Constant constant = (Expr.Constant) expr;
            Expr.Constant updated = new Expr.Constant(constant.value, constant.attributes());
            return updated;
        } else if (expr instanceof Expr.BinOp) {
            Expr.BinOp binOp = (Expr.BinOp) expr;

            if (amount.equals(BigInteger.ONE) || amount.equals(BigInteger.valueOf(-1))) {
                // special case
                boolean hasVariable = false;
                if (binOp.lhs instanceof Expr.LocalVariable) {
                    hasVariable |= ((Expr.LocalVariable)binOp.lhs).var.equals(name);
                }
                if (binOp.rhs instanceof Expr.LocalVariable) {
                    hasVariable |= ((Expr.LocalVariable)binOp.rhs).var.equals(name);
                }

                if (hasVariable) {
                    Expr.BOp equals = null;
                    switch (binOp.op) {
                        case GT:
                            equals = Expr.BOp.GTEQ;
                            break;
                        case LT:
                            equals = Expr.BOp.LTEQ;
                            break;
                    }

                    if (equals != null) {
                        Expr.BinOp updated = new Expr.BinOp(equals, binOp.lhs, binOp.rhs, binOp.attributes());
                        updated.srcType = binOp.srcType;
                        return updated;
                    }
                }
            }

            Expr lhs = applyAgingTo(binOp.lhs, name, amount);
            Expr rhs = applyAgingTo(binOp.rhs, name, amount);

            Expr.BinOp updated = new Expr.BinOp(binOp.op, lhs, rhs, binOp.attributes());
            updated.srcType = binOp.srcType;
            return updated;
        } else if (expr instanceof Expr.UnOp) {
            Expr.UnOp uOp = (Expr.UnOp) expr;

            Expr mhs = applyAgingTo(uOp.mhs, name, amount);

            Expr.UnOp updated = new Expr.UnOp(uOp.op, mhs, uOp.attributes());
            updated.type = uOp.type;
            return updated;
        }
        throw new IllegalStateException();
    }

    private static class Environment {
        private final Map<String, BigInteger> mutation;

        private Environment() {
            this.mutation = new HashMap<>();
        }

        private Environment(Environment env) {
            this.mutation = new HashMap<>(env.mutation);
        }

        private void update(String name, BigInteger change) {
            if (mutation.containsKey(name)) {
                BigInteger old = mutation.get(change);

                // check if already undecidable
                if (old == null) {
                    return;
                }
                if (!old.equals(change)) {
                    mutation.put(name, null);
                }
            } else {
                mutation.put(name, change);
            }
        }

        private Map<String, BigInteger> getMutations() {
            return this.mutation.entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        private void merge(Environment... envs) {
            for (Environment env : envs) {
                for (Map.Entry<String, BigInteger> value : env.mutation.entrySet()) {
                    this.update(value.getKey(), value.getValue());
                }
            }
        }
    }
}
