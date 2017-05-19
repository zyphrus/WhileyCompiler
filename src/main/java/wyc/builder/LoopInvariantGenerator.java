package wyc.builder;

import wybs.lang.Attribute;
import wyc.builder.invariants.ArrayLengthCopyInvariant;
import wyc.builder.invariants.InvariantGenerator;
import wyc.builder.invariants.StartingBoundInvariant;
import wyc.lang.Expr;
import wyc.lang.Stmt;
import wyc.lang.WhileyFile;
import wyil.lang.Type;

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
    private final List<InvariantGenerator> generators;

    public LoopInvariantGenerator(WhileyFile whileyFile) {
        this.whileyFile = whileyFile;
        this.generators = new ArrayList<>();

        // TODO: allow this to be user controlled list of invariant generators
        this.generators.add(new StartingBoundInvariant());
        this.generators.add(new ArrayLengthCopyInvariant());
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

    /**
     * Recursively goes through the AST of the method to find while loops
     * and collect information for the Context as it goes.
     *
     * @param stmt The statement being processed
     * @param context current information about the traversed AST
     */
    void findLoops(Stmt stmt, Context context) {
        if (stmt instanceof Stmt.IfElse) {
            Stmt.IfElse stmtIfElse = (Stmt.IfElse) stmt;

            findLoops(stmtIfElse.trueBranch, new Context(context));
            findLoops(stmtIfElse.falseBranch, new Context(context));
        } else if (stmt instanceof Stmt.NamedBlock) {
            Stmt.NamedBlock namedBlock = (Stmt.NamedBlock) stmt;
            findLoops(namedBlock.body, new Context(context));
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
            Stmt.VariableDeclaration stmtDecl = (Stmt.VariableDeclaration) stmt;

            if (stmtDecl.expr != null) {
                context.putValue(stmtDecl.parameter.name, stmtDecl.type, stmtDecl.expr);
            }
        } else if (stmt instanceof Stmt.Assign) {
            Stmt.Assign stmtAssign = (Stmt.Assign) stmt;

            Iterator<Expr.LVal> lvals = stmtAssign.lvals.iterator();
            Iterator<Expr> rvals = stmtAssign.rvals.iterator();
            // would of liked to chain these two lists together

            while (lvals.hasNext() && rvals.hasNext()) {
                Expr.LVal lval = lvals.next();
                Expr rval = rvals.next();

                if (context.hasValue(lval.toString())) {
                    System.err.println("Oh dear, two assignments for " + lval + " removing from set of safe variants");
                    context.getValue(lval.toString()).setAssigned(null);
                    continue;
                }

                // a candidate that could be inferred must be an integer type
                // and be assigned to a constant, e.g. int i = 0
                if (lval instanceof Expr.AssignedVariable) {
                    Expr.AssignedVariable assigned = (Expr.AssignedVariable) lval;

                    context.putValue(assigned.var, assigned.type, rval);
                }
            }
        } else if (stmt instanceof Stmt.While) {
            Stmt.While whileStmt = (Stmt.While) stmt;
            // handle the while loop

            for (InvariantGenerator generator : generators)  {
                generator.generateInvariant(whileStmt, context).stream()
                        .filter(invariant -> invariant != null)
                        .forEach(invariant -> {
                    invariant.attributes().add(whileStmt.attribute(Attribute.Source.class));
                    whileStmt.invariants.add(invariant);
                });
            }

            findLoops(whileStmt.body, new Context(context));
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

        private Map<String, Variable> variables = new HashMap<>();
        private Set<String> parameters = new HashSet<>();

        public Context() {
            this.parent = null;
        }

        public Context(Context parent) {
            this.parent = parent;
        }

        public Variable getValue(String variable) {
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

        void putValue(String variable, wyil.lang.Type type, Expr expr) {
            this.variables.put(variable, new Variable(type, expr));
        }


        void putParam(String variable) {
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
    }

    public static class Variable {
        private final wyil.lang.Type type;
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
