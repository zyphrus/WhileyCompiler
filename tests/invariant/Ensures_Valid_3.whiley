function selectOver(int[] xs) -> (int[] ys)
ensures |ys| <= |xs|
ensures all { i in 0..|ys| | ys[i] >= 0 }:
    //
    int i = 0
    int size = |xs|
    int count = 0
    // ======================================================
    // First, count positive elements of xs
    // ======================================================
    while i < |xs| 
        where i >= 0 && i <= |xs| && |xs| == size 
        where count >= 0 && count <= i:
        //
        if xs[i] >= 0:
            count = count + 1
        i = i + 1
    // ======================================================
    // Second, extract positive elements of xs
    // ======================================================
    int[] zs = [0; count]
    i = 0
    int j = 0
    while j < |zs|
        where j <= |zs| && |zs| == count
        where all { k in 0 .. j | zs[k] >= 0 }:
        if i < |xs| && xs[i] >= 0:
            zs[j] = xs[i]
            j = j + 1
        i = i + 1
    //
    assert j >= |zs|
    //
    return zs

public export method test() :
    int[] a1 = selectOver([1, -2, 3, 4])
    int[] a2 = selectOver([1, -2, -3, 4])
    assume a1 == [1,3,4]
    assume a2 == [1,4]
