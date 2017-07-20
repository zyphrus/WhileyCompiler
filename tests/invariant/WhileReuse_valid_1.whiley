
function count2(int[] ls) -> (int r)
    ensures r == 0:
    int i = 0
    while i < |ls|
    where i <= |ls|:
        i = i + 1

    i = |ls|
    while i > 0
    where i >= 0:
        i = i - 1

    return i

public export method test() :
    int rs = count2([1, 2, 3, 4, 5])
    assume rs == 0 
