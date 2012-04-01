import * from whiley.lang.*

// Tests that a while loop does not affect the correctness of the yielding.
void ::main(Console sys):
    i = 0
    while i < 5:
        sys.out.println(i)
        i = i + 1
