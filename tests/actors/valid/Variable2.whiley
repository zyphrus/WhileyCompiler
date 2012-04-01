import * from whiley.lang.*

define Actor as ref { int state }

// Tests that variables are maintained after synchronous message sends.
void ::main(Console sys):
    actor = new { state: 2 }
    i = actor?getState()
    actor->state = 1
    sys.out!println(actor?getState())
    sys.out!println(i)

int Actor::getState():
    return this->state
