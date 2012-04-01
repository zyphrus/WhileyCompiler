import * from whiley.lang.*

// Tests that a for loop does not affect the correctness of the yielding.
void ::main(Console sys):
    // x just exists to add values to the stack.
    x = "done"
    for i in 0..5:
        x = x + i
        sys.out.println(i)
    sys.out.println(x)
