import * from whiley.lang.*

void ::main(Console sys):
    x = true
    y = false
    sys.out.println(x)
    x = x && y
    sys.out.println(x)
