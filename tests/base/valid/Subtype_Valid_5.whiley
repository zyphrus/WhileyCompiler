import * from whiley.lang.*

define sr5nat as int

void ::main(System.Console sys):
    x = {f:1}
    x.f = 2
    sys.out.println(Any.toString(x))
    
