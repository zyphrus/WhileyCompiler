import * from whiley.lang.*

define Actor as ref { int state }

// Tests that a synchronous message send blocks the sender.
void ::main(Console sys):
    actor = new { state: 1 }
    sys.out!println(actor->state)
    actor?setState(2)
    sys.out!println(actor->state)

void Actor::setState(int state):
    sleep(100)
    this->state = state
