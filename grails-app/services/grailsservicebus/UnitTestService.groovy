package grailsservicebus

/**
This is a real service used for unit testing service injections in the definition DSL.
 */
class UnitTestService {
    def static transactional = false

    def echo (String text) {
        return text
    }
}
