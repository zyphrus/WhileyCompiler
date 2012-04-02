import * from whiley.lang.*

void ::main(Console sys):
    x = true
    y = false
    x = x && y
    sys.out.println(x)
