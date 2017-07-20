package wyc.builder.invariants;

import wyc.lang.Expr;
import wyc.lang.Stmt;

import java.util.ArrayList;
import java.util.List;

public class ConditionAgingInvariant implements InvariantGenerator {

    @Override
    public List<Expr> generateInvariant(Stmt.While whileStmt, Util.Context context) {
        List<Expr> invariants = new ArrayList<>();

        // 1. breakdown loop condition into small segments

        // 2. identify 'simple' conditions (<, > for now)

        // 3. search loop body for simple variants (e.g. i = i + 1 )

        // 4. Find the intersection between the simple variants and the simple conditions

        // 5. apply ageing to the condition based off the variants (should it be max difference?)

        // 6. add invariant

        return invariants;
    }

}
