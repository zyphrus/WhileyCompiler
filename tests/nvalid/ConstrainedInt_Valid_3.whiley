import println from whiley.lang.System

type cr3nat is int

function f(cr3nat x) => cr3nat:
    return 1

method main(System.Console sys) => void:
    y = f(9)
    sys.out.println(Any.toString(y))