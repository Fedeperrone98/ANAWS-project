# Flow reservation in Data Centers

 Implementation of a system that:

 1. Exposes a RESTful interface allowing hosts to subscribe for a new host-to-host flow, specifying the expected flow load (in Gigabytes).
 2. Guarantees that each physical link is reserved for at most one host-to-host flow.
