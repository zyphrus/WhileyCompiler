import * from whiley.lang.*

define Actor as ref { int x }

void ::main(Console sys):
    act = new { x: 1 }
    act!run(sys)

// Tests that calling an internal method correctly yields.
void Actor::run(Console sys):
    this.self(sys)
    sys.out?println(this->x)

void Actor::self(Console sys):
    sys.out?println(this->x)
    this->x = 2
