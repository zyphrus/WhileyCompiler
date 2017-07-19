package wyc.builder.invariants;

import wyc.lang.Expr;
import wyc.lang.Stmt;

import java.util.List;

/**
 * Interface for the strategy pattern
 */
public interface InvariantGenerator {

    /**
     * Generate invariants for the given while loop with the given context
     *
     *
     * @param whileStmt Loop to generator loops for, readonly
     * @param context the context from the statements before the loop
     * @return a list of valid loop invariants
     */
    List<Expr> generateInvariant(Stmt.While whileStmt, Util.Context context);

}
