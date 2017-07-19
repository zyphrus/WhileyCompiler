package wyc.builder.invariants;

import wyc.lang.Expr;
import wyc.lang.WhileyFile;
import wyil.lang.Type;

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
