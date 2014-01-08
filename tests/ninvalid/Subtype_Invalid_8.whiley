
type scf8nat is int where $ > 0

type scf8tup is {scf8nat f, int g} where g > f

function f([scf8tup] xs) => int:
    return |xs|

method main(System.Console sys) => void:
    x = [{f: 1, g: 2}, {f: 4, g: 8}]
    x[0].f = 2
    f(x)