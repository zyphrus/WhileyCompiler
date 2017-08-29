package wyc.builder;

import wybs.lang.Attribute;
import wybs.lang.SyntaxError;
import wyc.builder.invariants.*;
import wyc.io.WhileyFilePrinter;
import wyc.lang.Expr;
import wyc.lang.Stmt;
import wyc.lang.WhileyFile;

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
        this.generators.add(new ConditionAgingInvariant());
        this.generators.add(new ArrayInitInvariant());
    }

    public void generate() {
        for (WhileyFile.FunctionOrMethodOrProperty method : whileyFile.declarations(WhileyFile.FunctionOrMethodOrProperty.class)) {
            Util.Context context = new Util.Context(whileyFile);

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
    private void findLoops(ArrayList<Stmt> statements, Util.Context context) {
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
    void findLoops(Stmt stmt, Util.Context context) {
        if (stmt instanceof Stmt.IfElse) {
            Stmt.IfElse stmtIfElse = (Stmt.IfElse) stmt;

            findLoops(stmtIfElse.trueBranch, new Util.Context(context));
            findLoops(stmtIfElse.falseBranch, new Util.Context(context));
        } else if (stmt instanceof Stmt.NamedBlock) {
            Stmt.NamedBlock namedBlock = (Stmt.NamedBlock) stmt;
            findLoops(namedBlock.body, new Util.Context(context));
        } else if (stmt instanceof Stmt.Switch) {
            Stmt.Switch switchStmt = (Stmt.Switch) stmt;
            for (Stmt.Case caseStmt : switchStmt.cases) {
                findLoops(caseStmt.stmts, new Util.Context(context));
            }
        } else if (stmt instanceof Stmt.DoWhile) {
            Stmt.DoWhile whileStmt = (Stmt.DoWhile) stmt;
            // handle the do-while loop ?
            findLoops(whileStmt.body, new Util.Context(context));
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

                // if (context.hasValue(lval.toString())) {
                //     context.getValue(lval.toString()).setAssigned(null);
                //     continue;
                // }

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
                        .filter(inv -> !Objects.isNull(inv))
                        .forEach(invariant -> {

                            invariant.attributes().add(whileStmt.attribute(Attribute.Source.class));

                            Util.GeneratedAttribute reason = invariant.attribute(Util.GeneratedAttribute.class);
                            if (reason == null) {
                                throw new IllegalStateException("Generated invariants must be marked with GeneratedAttributes");
                            }

                            SyntaxError warn = new SyntaxError("WARN: Generated Loop Invariant: " + reason.toString(), whileyFile.getEntry(), whileStmt);
                            warn.outputSourceError(System.out, false);

                            System.out.print("\twhere ");
                            new WhileyFilePrinter(System.out).print(invariant);
                            System.out.println();
                            System.out.println();

                            whileStmt.invariants.add(invariant);
                        });
            }

            findLoops(whileStmt.body, new Util.Context(context));
        }
    }


}
