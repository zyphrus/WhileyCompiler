import * from whiley.lang.*

define Actor as ref { string state }

void ::main(Console sys):
    act = new { state: "state" }
    act?method(sys)

// Tests that calling an internal method as an expression correctly yields.
void Actor::method(Console sys):
    sys.out!println(this?getState())

string Actor::getState():
    return this->state
