type Link is null | {int[] items, Link next}

method create(int n) -> Link:
    Link start = null
    int i = 0
    while i < n:
        start = {items: [0;0], next: start}
        i = i + 1
    return start

public export method test():
    Link l1 = null
    Link l2 = {items: [0;0], next: l1}
    Link l3 = {items: [0;0], next: l2}
    //
    Link result = create(0)
    assume result == l1
    //
    result = create(1)
    assume result == l2
    //
    result = create(2)
    assume result == l3