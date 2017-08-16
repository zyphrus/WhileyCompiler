
function set(int[] items, int amount) -> (int[] r)
    requires |items| > 0
    ensures |r| == |items|
    ensures all { i in 0..|items| | amount == r[i] }:

    int[] out = [0;|items|]
    int i = 0

    while i < |out|:
        out[i] = amount
        i = i +1

    return out


public export method test():
    assume (set([1,2,3], 1) == [1, 1, 1])
