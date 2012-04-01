import * from whiley.lang.*

// Tests that the type flow analysis can use the exception table correctly.
void ::main(Console sys):
    try:
        sys.out.println("trying")
        mightThrow()
    catch(Error e):
        sys.out.println("failed")

    sys.out.println("completed")

void ::mightThrow() throws Error:
    return
