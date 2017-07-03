
function sum(int i) -> (int r)
    requires i > 0
    ensures i == r:
    int n = 0
    //
    while n < i
        // where n >= 0 inferred
        where n <= i:
        n = n + 1
    //
    return n


public export method test():
    assert(sum(10) == 10)
    assert(sum(1) == 1)
