import * from whiley.lang.*

define Actor as ref { string state }

// Tests that an asynchronous message send does not block the sender.
void ::main(Console sys):
    actor = new { state: "state" }
    sys.out!println(actor->state)
    actor!setState("never")
    sys.out?println(actor->state)

void Actor::setState(string state):
    sleep(1000)
    this->state = state
