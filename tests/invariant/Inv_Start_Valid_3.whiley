function double(int[] items) -> (int[] r)
    ensures |r| == |items|
    ensures all { i in 0 .. |r| | r[i] == items[i] * 2 }:

    int i = 0
    int[] doubled = [0; |items|]

    while i < |doubled|
        where all { j in 0 .. i | doubled[j] == items[j] * 2 }:
        doubled[i] = items[i] * 2
        i = i + 1

    return doubled

public export method test():
    assert(double([1,2]) == [2,4])
