import * from whiley.lang.*

// This example was inspired by comments from Stuart Marshall.

define anat as int
define bnat as int

bnat atob(anat x):
    return x

anat btoa(bnat x):
    return x

void ::main(System sys,[string] args):
    x = 1
    sys.out.println(toString(atob(x)))
    sys.out.println(toString(btoa(x)))
    
    
