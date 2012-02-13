import * from whiley.lang.*

define edict as {int=>int}|{real=>real}

edict f(int x):
    if x < 0:
        return {1=>2,2=>3}
    else:
        return {1=>1.5, 2=>2.5}

public void ::main(System.Console sys):
    d = f(-1)
    d[3] = 4
    sys.out.println("Dictionary=" + d)
    d = f(2)
    d[3] = 4
    sys.out.println("Dictionary=" + d)