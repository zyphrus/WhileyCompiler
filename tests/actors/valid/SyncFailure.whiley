import * from whiley.lang.*

define Actor as ref Error

void ::main(Console sys):
    actor = new { msg: "failed" }
    try:
        actor?doThrow()
    catch(Error e):
        sys.out!println(e.msg)

void Actor::doThrow() throws Error:
    throw *this
