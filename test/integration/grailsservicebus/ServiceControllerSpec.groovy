package grailsservicebus

import grails.converters.JSON
import spock.lang.Specification

/**
 *
 */
class ServiceControllerSpec extends Specification {
    ServiceController controller
    
    def setup() {
        controller = new ServiceController()
    }
    
    void "test request method as GET"() {
        given: "request method is set to GET"
        controller.request.method = "GET"
        def json = JSON.parse('{"exception":{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceProtocolException","exceptionMessage":"Non POST request are not supported."}}')

        when: "index action is called"
        controller.index()

        then: "response status should be 405"
        controller.response.json == json
        controller.response.contentType == "application/json;charset=UTF-8"
        controller.response.status == 405
    }

    void "test for status 406 when contentType is not JSON"() {
        given: "request method is POST and contentType is xml"
        controller.request.method = "POST"
        controller.request.contentType = "text/xml"
        def json = JSON.parse('{"exception":{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceProtocolException","exceptionMessage":"Only JSON Content Types are supported."}}')

        when: "index action is called"
        controller.index()

        then: "response status should be 406"
        controller.response.status == 406
        controller.response.json == json
        controller.response.contentType == "application/json;charset=UTF-8"
    }

    void "test for contentType JSON and status 200 in response"() {
        given: "a properly formed message, method, and contentType"
        controller.request.method = "POST"
        controller.request.contentType = "application/json"
        controller.request.json = '{"service":{"name":"unittest"}}'
        def json = JSON.parse('{"service":{"name":"unittest"}}')

        when: "index action is called"
        controller.index()

        then: "status should be 200 and contentType as JSON"
        controller.response.status == 200
        controller.response.json == json
        controller.response.contentType == "application/json;charset=UTF-8"
    }

    void "test status 400 with invalid JSON"() {
        given: "the request object is setup with a syntactically bad JSON"
        controller.request.method = "POST"
        controller.request.contentType = "application/json"
        controller.request.json = '{"service":{"name":"unittest"}' // missing last }
        def json = JSON.parse('{"exception":{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceProtocolException","exceptionMessage":"Message JSON syntax error"}}')

        when: "index action is called"
        controller.index()

        then: "response status should be 400"
        controller.response.status == 400
        controller.response.json == json
        controller.response.contentType == "application/json;charset=UTF-8"
    }

    void "test status 400 with empty text"() {
        given:
        controller.request.method = "POST"
        controller.request.contentType = "application/json"
        controller.request.content = "".bytes
        def json = JSON.parse('{"exception":{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceProtocolException","exceptionMessage":"The message does not have a proper \\"service\\" object"}}')

        when: "index action is called"
        controller.index()

        then:
        controller.response.status == 406
        controller.response.json == json
        controller.response.contentType == "application/json;charset=UTF-8"
    }

    void "test status 400 with no text"() {
        given:
        controller.request.method = "POST"
        controller.request.contentType = "application/json"
        def json = JSON.parse('{"exception":{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceProtocolException","exceptionMessage":"The message does not have a proper \\"service\\" object"}}')

        when: "index action is called"
        controller.index()

        then:
        controller.response.status == 406
        controller.response.json == json
        controller.response.contentType == "application/json;charset=UTF-8"
    }

    void "message has service key with valid message"() {
        given: "a properly build message"
        def message = [service: [name: 'unittest']]

        expect:
        controller.checkForValidServiceObject(message) is true
    }

    void "message without a service key"() {
        given:
        def message = [key: 'value']

        expect:
        controller.checkForValidServiceObject(message) is false
    }

    void "message has service key with a value that is not a map"() {
        given:
        def message = [service: 'unittest']

        expect:
        controller.checkForValidServiceObject(message) is false
    }

    void "message has service key without the name key"() {
        given:
        def message = [service: [key: 'value']]

        expect:
        controller.checkForValidServiceObject(message) is false
    }

    void "message has service key with a value name that is not a string"() {
        given:
        def message = [service: [name: 1]]

        expect:
        controller.checkForValidServiceObject(message) is false
    }

    void "message has a service key with the value name empty"() {
        given:
        def message = [service: [name: ""]]

        expect:
        controller.checkForValidServiceObject(message) is false
    }

    void "how does JSON.parse work"() {
        given:
        controller.request.method = "POST"
        controller.request.contentType = "application/json"
        controller.request.content = "".bytes

        when:
        def text = controller.request.reader.text
        def json = controller.request.JSON

        then:
        true
    }
}
