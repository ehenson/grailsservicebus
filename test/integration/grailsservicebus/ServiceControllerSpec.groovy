package grailsservicebus

import grails.test.mixin.TestFor
import spock.lang.*

/**
 *
 */
@TestFor(ServiceController)
class ServiceControllerSpec extends Specification {

    void "test request method as GET"() {
        given: "request method is set to GET"
        request.method = "GET"

        when: "index action is called"
        controller.index()

        then: "response status should be 405"
        response.status == 405
    }

    void "test for status 406 when contentType is not JSON"() {
        given: "request method is POST and contentType is xml"
        request.method = "POST"
        request.contentType = "text/xml"

        when: "index action is called"
        controller.index()

        then: "response status should be 406"
        response.status == 406
    }

    void "test for contentType JSON and status 200 in response"() {
        given: "a properly formed message, method, and contentType"
        request.method = "POST"
        request.contentType = "application/json"
        request.json = '{"service":{"name":"unittest"}}'

        when: "index action is called"
        controller.index()

        then: "status should be 200 and contentType as JSON"
        response.status == 200
        response.contentType == "application/json;charset=UTF-8"
    }

    void "test status 400 with invalid JSON"() {
        given: "the request object is setup with a syntactically bad JSON"
        request.method = "POST"
        request.contentType = "application/json"
        request.json = '{"service":{"name":"unittest"}' // missing last }

        when: "index action is called"
        controller.index()

        then: "response status should be 400"
        response.status == 400
    }

    void "message has service key with valid message"() {
        given: "a properly build message"
        def message = [service:[name:'unittest']]

        expect:
        controller.checkForValidServiceObject(message) is true
    }

    void "message without a service key"() {
        given:
        def message = [key:'value']

        expect:
        controller.checkForValidServiceObject(message) is false
    }

    void "message has service key with a value that is not a map"() {
        given:
        def message = [service:'unittest']

        expect:
        controller.checkForValidServiceObject(message) is false
    }

    void "message has service key without the name key"() {
        given:
        def message = [service:[key:'value']]

        expect:
        controller.checkForValidServiceObject(message) is false
    }

    void "message has service key with a value name that is not a string"() {
        given:
        def message = [service:[name:1]]

        expect:
        controller.checkForValidServiceObject(message) is false
    }

    void "message has a service key with the value name empty"() {
        given:
        def message = [service:[name:""]]

        expect:
        controller.checkForValidServiceObject(message) is false
    }
}
