package wyc.builder.invariants;

import wyc.builder.LoopInvariantGenerator;
import wyc.lang.Expr;
import wyc.lang.Stmt;

import java.util.ArrayList;
import java.util.List;

/**
 * Array Access Invariant
 *
 * examples:
 * <code>
 *     while condition:
 *          // infers i >= 0 && i <= |items|
 *          ...
 *          int x = items[i]
 *          ...
 * </code>
 *
 */
public class ArrayAccessInvariant implements InvariantGenerator {

    @Override
    public List<Expr> generateInvariant(Stmt.While whileStmt, LoopInvariantGenerator.Context context) {
        List<Expr> invariants = new ArrayList<>();

        return invariants;
    }

    // TODO: decent AST to find all array accesses

    public List<Expr.LocalVariable> findArrayAccess(Stmt stmt) {
        return null;
    }

}
