import * from whiley.lang.*

define Actor as ref { int state }

// Tests that a synchronous message will wait for an asynchronous message.
void ::main(Console sys):
    actor = new { state: 1 }
    sys.out!println(actor?getState())
    actor!setState(2)
    sys.out!println(actor?getState())

int Actor::getState():
    return this->state

void Actor::setState(int state):
    sleep(100)
    this->state = state
