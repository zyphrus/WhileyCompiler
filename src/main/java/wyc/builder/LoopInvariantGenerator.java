package wyc.builder;

import wyc.lang.Stmt;
import wyc.lang.WhileyFile;

import java.util.ArrayList;

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

    public LoopInvariantGenerator(WhileyFile whileyFile) {
        this.whileyFile = whileyFile;
    }

    public void generate() {
        for (WhileyFile.FunctionOrMethodOrProperty method : whileyFile.declarations(WhileyFile.FunctionOrMethodOrProperty.class)) {
            findLoops(method.statements);
        }
    }


    /**
     * First stage of generating loop invariants
     *
     * @param statements
     */
    private void findLoops(ArrayList<Stmt> statements) {
        for (Stmt stmt : statements) {
            if (stmt instanceof Stmt.IfElse) {
                Stmt.IfElse stmtIfElse = (Stmt.IfElse) stmt;
                findLoops(stmtIfElse.trueBranch);
                findLoops(stmtIfElse.falseBranch);
            } else if (stmt instanceof Stmt.NamedBlock) {
                Stmt.NamedBlock namedBlock = (Stmt.NamedBlock) stmt;
                findLoops(namedBlock.body);
            } else if (stmt instanceof Stmt.While) {
                Stmt.While whileStmt = (Stmt.While) stmt;
                // handle the while loop
                findLoops(whileStmt.body);
            } else if (stmt instanceof Stmt.Switch) {
                Stmt.Switch switchStmt = (Stmt.Switch) stmt;
                for (Stmt.Case caseStmt : switchStmt.cases) {
                    findLoops(caseStmt.stmts);
                }
            } else if (stmt instanceof Stmt.DoWhile) {
                Stmt.DoWhile whileStmt = (Stmt.DoWhile) stmt;
                // handle the do-while loop ?
                findLoops(whileStmt.body);
            } else if (stmt instanceof Stmt.VariableDeclaration) {
                // skip
            } else if (stmt instanceof Stmt.Assign) {
                // skip
            } else if (stmt instanceof Stmt.Break) {
                // skip
            } else if (stmt instanceof Stmt.Continue) {
                // skip
            } else if (stmt instanceof Stmt.Assert) {
                // skip
            } else if (stmt instanceof Stmt.Assume) {
                // skip
            } else if (stmt instanceof Stmt.Fail) {
                // skip
            } else if (stmt instanceof Stmt.Debug) {
                // skip
            } else if (stmt instanceof Stmt.Return) {
                // skip
            } else if (stmt instanceof Stmt.Skip) {
                // skip
            }
        }
    }


}
