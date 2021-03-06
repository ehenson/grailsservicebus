package grailsservicebus

import grails.converters.JSON
import grailsservicebus.test.ServiceFileHelper
import spock.lang.Specification

/**
 *
 */
class ServiceControllerIntegrationSpec extends Specification {
    ServiceController controller
    ServiceDefinitionService serviceDefinitionService
    ScriptActionHandlerService scriptActionHandlerService
    def grailsApplication
    ServiceFileHelper serviceFileHelper

    def setup() {
        controller = new ServiceController()

        // setup the services to look at test folders
        // since the test folders are just now created GroovyScriptEngine needs to be reloaded because it looses
        // its mind if it is constructed and the folder does not exists
        serviceFileHelper = new ServiceFileHelper()
        serviceFileHelper.setup(grailsApplication)
        serviceDefinitionService.init()
        scriptActionHandlerService.init()
    }

    def cleanup() {
        serviceFileHelper.cleanup()
    }
    
    void "test request method as GET"() {
        given: "request method is set to GET"
        controller.request.method = "GET"
        def json = JSON.parse('{"exception":[{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceProtocolException","exceptionMessage":"Non POST request are not supported."}]}')

        when: "index action is called"
        controller.processRequest()

        then: "response status should be 405"
        controller.response.json == json
        controller.response.contentType == "application/json;charset=UTF-8"
        controller.response.status == 405
    }

    void "test for status 406 when contentType is not JSON"() {
        given: "request method is POST and contentType is xml"
        controller.request.method = "POST"
        controller.request.contentType = "text/xml"
        def json = JSON.parse('{"exception":[{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceProtocolException","exceptionMessage":"Only JSON Content Types are supported."}]}')

        when: "index action is called"
        controller.processRequest()

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
        serviceFileHelper.writeDefinition("unittest", 'action file:"unittest"')
        serviceFileHelper.writeAction("unittest", """
class Unittest {
    def execute(message, properties) {}
}
""")
        serviceDefinitionService.init()
        scriptActionHandlerService.init()

        when: "index action is called"
        controller.processRequest()

        then: "status should be 200 and contentType as JSON"
        controller.response.status == 200
        controller.response.json == json
        controller.response.contentType == "application/json;charset=UTF-8"

        cleanup:
        serviceFileHelper.cleanup()
    }

    void "test status 400 with invalid JSON"() {
        given: "the request object is setup with a syntactically bad JSON"
        controller.request.method = "POST"
        controller.request.contentType = "application/json"
        controller.request.json = '{"service":{"name":"unittest"}' // missing last }
        def json = JSON.parse('{"exception":[{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceProtocolException","exceptionMessage":"Message JSON syntax error"}]}')

        when: "index action is called"
        controller.processRequest()

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
        def expectedJson = JSON.parse('{"exception":[{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceProtocolException","exceptionMessage":"Only JSON Content Types are supported."}]}')

        when: "index action is called"
        controller.processRequest()

        then:
        controller.response.status == 406
        controller.response.json == expectedJson
        controller.response.contentType == "application/json;charset=UTF-8"
    }

    void "test status 400 with no text"() {
        given:
        controller.request.method = "POST"
        controller.request.contentType = "application/json"
        def json = JSON.parse('{"exception":[{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceProtocolException","exceptionMessage":"Only JSON Content Types are supported."}]}')

        when: "index action is called"
        controller.processRequest()

        then:
        controller.response.status == 406
        controller.response.json == json
        controller.response.contentType == "application/json;charset=UTF-8"
    }

    /**
     * Uncomment this test and throw a NPE to see if this test succeeds
     */
    void "test status 400 with null text"() {
        given:
        serviceFileHelper.writeDefinition("unittest", 'action file:"unittest"')
        serviceFileHelper.writeAction("unittest", """
class Unittest {
    def execute(message, properties) {throw new NullPointerException()}
}
""")
        serviceDefinitionService.init()
        scriptActionHandlerService.init()

        controller.request.method = "POST"
        controller.request.contentType = "application/json"
        controller.request.json = '{"service":{"name":"unittest"}}'
        def expectedJson = JSON.parse('{"service":{"name":"unittest"},"exception":[{"actionType":"groovy","actionName":"unknown","exceptionType":"ScriptActionUncaughtException","exceptionMessage":"Script Action Error: java.lang.NullPointerException"}]}')

        when: "index action is called"
        controller.processRequest()

        then:
        def actualJson = controller.response.json
        controller.response.status == 500
        expectedJson == actualJson
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
