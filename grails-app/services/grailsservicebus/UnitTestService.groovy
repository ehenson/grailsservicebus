package grailsservicebus

//import grails.transaction.Transactional
//
//@Transactional
class UnitTestService {
    def static transactional = false

    def echo (String text) {
        return text
    }
}
