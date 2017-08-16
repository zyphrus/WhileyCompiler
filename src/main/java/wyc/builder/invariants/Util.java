package wyc.builder.invariants;

import wyc.lang.Expr;
import wyc.lang.WhileyFile;
import wyil.lang.Constant;
import wyil.lang.Type;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Util {

    public static Type resolveType(Type type, Context context) {
        if (type instanceof Type.Nominal) {
            Type.Nominal nominal = (Type.Nominal) type;
            Type resolved = context.resolveType(nominal.name().name());
            if (resolved != null) {
                return resolveType(resolved, context);
            } else {
                return null;
            }
        } else {
            return type;
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
    public static BigInteger evalConstExpr(Expr.AssignedVariable variable, Expr expr, BigInteger varValue) {
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

    /**
     * Determines if the expression is a simple mutation of the given variable
     *
     * @param var variable that is being assigned
     * @param expr mutation expression
     * @return true if the assignment is a simple mutation
     */
    public static boolean isSimpleMutationOfVar(Expr.AssignedVariable var, Expr expr) {
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
    private static boolean isSimpleMutationOfVar(Expr.AssignedVariable var, Expr expr, boolean topLevel) {
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

    public static class GeneratedAttribute implements wybs.lang.Attribute {
        private final String reason;
        public GeneratedAttribute(String reason) {
            this.reason = reason;
        }

        @Override
        public String toString() {
            return reason;
        }
    }

    public static class Context {
        private final Context parent;

        private WhileyFile whileyFile;
        private Map<String, Util.Variable> variables = new HashMap<>();
        private Set<String> parameters = new HashSet<>();

        public Context(WhileyFile whileyFile) {
            this.parent = null;
            this.whileyFile = whileyFile;
        }

        public Context(Context parent) {
            this.parent = parent;
        }

        public Util.Variable getValue(String variable) {
            if (variables.containsKey(variable)) {
                return variables.get(variable);
            } else if (this.parent != null) {
                return this.parent.getValue(variable);
            }
            return null;
        }

        public boolean hasValue(String variable) {
            if (variables.containsKey(variable)) {
                return true;
            } else if (this.parent != null) {
                return this.parent.hasValue(variable);
            }
            return false;
        }

        public void putValue(String variable, Type type, Expr expr) {
            this.variables.put(variable, new Util.Variable(type, expr));
        }


        public void putParam(String variable) {
            if (this.parent != null) {
                throw new IllegalStateException("Only the root node can put parameters to the context");
            }
            this.parameters.add(variable);
        }

        public boolean hasParam(String variable) {
            if (this.parent != null) {
                return this.parent.hasParam(variable);
            }
            return this.parameters.contains(variable);
        }

        public Map<String, Util.Variable> getVariables() {
            return new HashMap<>(this.variables);
        }

        public Type resolveType(String name) {
            if (this.parent != null) {
                return this.parent.resolveType(name);
            }

            WhileyFile.Type resolvedType = whileyFile.typeDecl(name);
            if (resolvedType != null) {
                return resolvedType.resolvedType;
            } else {
                return null;
            }
        }
    }

    public static class Variable {
        private final Type type;
        private Expr assigned;

        Variable(Type type, Expr assignment) {
            this.type = type;
            this.assigned = assignment;
        }

        public Type getType() {
            return type;
        }

        public Expr getAssigned() {
            return assigned;
        }

        public void setAssigned(Expr assigned) {
            this.assigned = assigned;
        }
    }
}
