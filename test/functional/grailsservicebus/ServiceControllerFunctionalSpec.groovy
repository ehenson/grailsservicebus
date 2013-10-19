package grailsservicebus
import grails.converters.JSON
import grailsservicebus.test.ServiceFileHelper
import groovyx.net.http.*
import spock.lang.Specification

class ServiceControllerFunctionalSpec extends Specification {
    def url = "http://localhost:8080"
    def path = "/grailsservicebus/service/index"
    def grailsApplication
    ServiceFileHelper serviceFileHelper

    def setup() {
        grailsApplication = new org.codehaus.groovy.grails.commons.DefaultGrailsApplication()

        // setup the services to look at test folders
        // since the test folders are just now created GroovyScriptEngine needs to be reloaded because it looses
        // its mind if it is constructed and the folder does not exists
        serviceFileHelper = new ServiceFileHelper()
        serviceFileHelper.setup(grailsApplication)
    }

    def cleanup() {
        serviceFileHelper.cleanup()
    }

    def "test request method as GET"() {
        given:
        RESTClient client = new RESTClient(url)
        def json = JSON.parse('{"exception":[{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceProtocolException","exceptionMessage":"Non POST request are not supported."}]}')

        when:
        HttpResponseDecorator response = client.get(path:path)

        then:
        HttpResponseException ex = thrown()
        String responseString = ex.response.data.toString()
        ex.message == "Method Not Allowed"
        ex.response.status == 405
        ex.response.contentType == "application/json"
        JSON.parse(responseString) == json
    }

    def "test for status 406 when contentType is not JSON"() {
        given: // http://coderberry.me/blog/2012/05/07/stupid-simple-post-slash-get-with-groovy-httpbuilder/
        def json = JSON.parse('{"exception":[{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceProtocolException","exceptionMessage":"Only JSON Content Types are supported."}]}')
        HTTPBuilder http = new HTTPBuilder(url)
        HttpResponseDecorator httpResponse
        def text

        when:
        def status = http.request(Method.POST, ContentType.XML) {
            http.parser.'application/xml' = http.parser.'text/plain'
            uri.path = path
            body = [service:[name:"unittest"]]
            response.success = { resp, reader ->
                httpResponse = resp
                text = reader.text
            }
            response.failure = { resp, reader ->
                text = reader.text
                httpResponse = resp
            }
        }

        then:
        httpResponse.status == 406
        httpResponse.contentType == "application/json"
        JSON.parse(text) == json
    }

    def "proper JSON"() {
        given: // http://coderberry.me/blog/2012/05/07/stupid-simple-post-slash-get-with-groovy-httpbuilder/
        serviceFileHelper.writeDefinition("unittest", 'action file:"unittest"')
        serviceFileHelper.writeAction("unittest", """
class Unittest {
    def execute(message, properties) {}
}
""")
        def json = JSON.parse('{"service":{"name":"unittest"}}')
        HTTPBuilder http = new HTTPBuilder(url)
        HttpResponseDecorator httpResponse
        def respJson
        def text

        when:
        def status = http.request(Method.POST, ContentType.JSON) {
            uri.path = path
            body = [service:[name:"unittest"]]
            response.success = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
            response.failure = { resp, reader ->
                text = reader.text
                httpResponse = resp
            }
        }

        then:
        httpResponse.status == 200
        httpResponse.contentType == "application/json"
        JSON.parse(respJson.toString()) == json
        println respJson
    }
}