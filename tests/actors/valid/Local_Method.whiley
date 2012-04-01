import * from whiley.lang.*

// Tests that if a local method yields, then the whole stack yields.
void ::main(Console sys):
    sendMessage(sys)
    sys.out!println(2)

void ::sendMessage(Console sys):
    yield()
    sys.out!println(1)
