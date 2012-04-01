import * from whiley.lang.*

define Actor as ref { string state }

// Tests that an actor can correctly use the result of a synchronous message.
void ::main(Console sys):
    actor = new { state: "state" }
    i = actor?getState()
    sys.out?println(i)
    sys.out?println(i)

string Actor::getState():
    return this->state
