Grails Service Bus
---

This project is a pure Grails service bus based web service engine using JSON as the message payload.
My goal is to use lots of logging to aide in the case of support with lots of tests to build confidence in the code.

One of my design aspects is to not strive to be the tightest and fastest implementation but one that is very stable, flexible, and very support friendly.  This is why I use lots of logging and even though I use lots of logging I try to have an efficient use of the logging APIs as much as possible.

Features
---
* A simple DSL for the service definitions
* Groovy based Actions
* Pluggable action handlers which allows custom code to execute an action, ie: calling another web service with the attributes of the message mapped to the parameters of the web service call


Please use the [wiki](https://github.com/ehenson/grailsservicebus/wiki) for more information.
