import * from whiley.lang.*

int f(int x):
    return x + 1

int g(void(int) func):
    return func(1)
    
void ::main(System.Console sys):
    sys.out.println(Any.toString(g(&f)))