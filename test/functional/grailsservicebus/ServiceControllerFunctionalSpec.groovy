package grailsservicebus
import grails.converters.JSON
import grails.test.mixin.TestFor
import grailsservicebus.test.ServiceFileHelper
import groovyx.net.http.*
import spock.lang.Specification

@TestFor(ServiceController)
class ServiceControllerFunctionalSpec extends Specification {
    def url = "http://localhost:8080"
    def path = "/grailsservicebus/service/index"
    ServiceFileHelper serviceFileHelper

    def setup() {
        // setup the services to look at test folders
        // since the test folders are just now created GroovyScriptEngine needs to be reloaded because it looses
        // its mind if it is constructed and the folder does not exists
        // NOTE:  Look in the scripts/_Events.goovy because it creates the folders needed.
        serviceFileHelper = new ServiceFileHelper()
        serviceFileHelper.setup(grailsApplication)
    }

    def cleanup() {
        //serviceFileHelper.cleanup()
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
    }

    def "test definition not having valid handler"() {
        given:
        serviceFileHelper.writeDefinition("unittest", 'action handler:"unitTestHandlerNotValid", file:"unittest"')
        serviceFileHelper.writeAction("unittest", """
class Unittest {
    def execute(message, properties) {}
}
""")
        def json = JSON.parse('{"service":{"name":"unittest"},"exception":[{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceEngineException","exceptionMessage":"Action handler \\"unitTestHandlerNotValidActionHandlerService\\" reference is null"}]}')

        HTTPBuilder http = new HTTPBuilder(url)
        HttpResponseDecorator httpResponse
        def respJson

        when:
        def status = http.request(Method.POST, ContentType.JSON) {
            uri.path = path
            body = [service:[name:"unittest"]]
            response.success = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
            response.failure = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
        }

        then:
        httpResponse.status == 500
        httpResponse.contentType == "application/json"
        JSON.parse(respJson.toString()) == json
    }

    def "test NPE from the Action"() {
        given:
        serviceFileHelper.writeDefinition("unittest", 'action file:"unittest"')
        serviceFileHelper.writeAction("unittest", """
class Unittest {
    def execute(message, properties) {
        def npe = null
        def split = npe.split("/")
    }
}
""")
        def json = JSON.parse('{"exception":[{"actionType":"groovy","actionName":"unknown","exceptionType":"ScriptActionUncaughtException","exceptionMessage":"Script Action Error: java.lang.NullPointerException: Cannot invoke method split() on null object"}],"service":{"name":"unittest"}}')

        HTTPBuilder http = new HTTPBuilder(url)
        HttpResponseDecorator httpResponse
        def respJson

        when:
        def status = http.request(Method.POST, ContentType.JSON) {
            uri.path = path
            body = [service:[name:"unittest"]]
            response.success = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
            response.failure = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
        }

        then:
        def responseJson = respJson.toString()
        httpResponse.status == 500
        httpResponse.contentType == "application/json"
        JSON.parse(responseJson) == json
    }

    def "test NPE from the Definition"() {
        given:
        serviceFileHelper.writeDefinition("unittest", """
action file:"unittest.groovy", {
    key = {
        def npe = null
        def split = npe.split("/")
        return split
    }()
}
""")

        serviceFileHelper.writeAction("unittest", """
class Unittest {
    def execute(message, properties) {}
}
""")
        def json = JSON.parse('{"exception":[{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceDefinitionException","exceptionMessage":"Service Definition Error: Cannot invoke method split() on null object"},{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceEngineException","exceptionMessage":"Definition for service \\"unittest\\" is null"}],"service":{"name":"unittest"}}')

        HTTPBuilder http = new HTTPBuilder(url)
        HttpResponseDecorator httpResponse
        def respJson

        when:
        def status = http.request(Method.POST, ContentType.JSON) {
            uri.path = path
            body = [service:[name:"unittest"]]
            response.success = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
            response.failure = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
        }

        then:
        def responseJson = respJson.toString()
        httpResponse.status == 500
        httpResponse.contentType == "application/json"
        JSON.parse(responseJson) == json
    }

    def "test empty content with proper contentType"() {
        given:
        serviceFileHelper.writeDefinition("unittest", 'action file:"unittest"')
        serviceFileHelper.writeAction("unittest", """
class Unittest {
    def execute(message, properties) {}
}
""")
        def json = JSON.parse('{"exception":[{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceProtocolException","exceptionMessage":"The message does not have a proper service object"}]}')
        HTTPBuilder http = new HTTPBuilder(url)
        HttpResponseDecorator httpResponse
        def respJson

        when:
        def status = http.request(Method.POST, ContentType.JSON) {
            uri.path = path
            body = ""
            response.success = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
            response.failure = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
        }

        then:
        def responseJson = respJson.toString()
        httpResponse.status == 406
        httpResponse.contentType == "application/json"
        JSON.parse(responseJson) == json
    }

    def "test empty json as an empty list"() {
        given:
        serviceFileHelper.writeDefinition("unittest", 'action file:"unittest"')
        serviceFileHelper.writeAction("unittest", """
class Unittest {
    def execute(message, properties) {}
}
""")
        def json = JSON.parse('{"exception":[{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceProtocolException","exceptionMessage":"The message does not have a proper service object"}]}')
        HTTPBuilder http = new HTTPBuilder(url)
        HttpResponseDecorator httpResponse
        def respJson

        when:
        def status = http.request(Method.POST, ContentType.JSON) {
            uri.path = path
            body = []
            response.success = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
            response.failure = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
        }

        then:
        def responseJson = respJson.toString()
        httpResponse.status == 406
        httpResponse.contentType == "application/json"
        JSON.parse(responseJson) == json
    }

    def "test empty json as an empty object"() {
        given:
        serviceFileHelper.writeDefinition("unittest", 'action file:"unittest"')
        serviceFileHelper.writeAction("unittest", """
class Unittest {
    def execute(message, properties) {}
}
""")
        def json = JSON.parse('{"exception":[{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceProtocolException","exceptionMessage":"The message does not have a proper service object"}]}')
        HTTPBuilder http = new HTTPBuilder(url)
        HttpResponseDecorator httpResponse
        def respJson

        when:
        def status = http.request(Method.POST, ContentType.JSON) {
            uri.path = path
            body = [:]
            response.success = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
            response.failure = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
        }

        then:
        def responseJson = respJson.toString()
        httpResponse.status == 406
        httpResponse.contentType == "application/json"
        JSON.parse(responseJson) == json
    }

    def "test service with non object"() {
        given:
        serviceFileHelper.writeDefinition("unittest", 'action file:"unittest"')
        serviceFileHelper.writeAction("unittest", """
class Unittest {
    def execute(message, properties) {}
}
""")
        def json = JSON.parse('{"exception":[{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceProtocolException","exceptionMessage":"The message does not have a proper service object"}]}')
        HTTPBuilder http = new HTTPBuilder(url)
        HttpResponseDecorator httpResponse
        def respJson

        when:
        def status = http.request(Method.POST, ContentType.JSON) {
            uri.path = path
            body = [service:"non object"]
            response.success = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
            response.failure = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
        }

        then:
        def responseJson = respJson.toString()
        httpResponse.status == 406
        httpResponse.contentType == "application/json"
        JSON.parse(responseJson) == json
    }

    def "test service with object and no name"() {
        given:
        serviceFileHelper.writeDefinition("unittest", 'action file:"unittest"')
        serviceFileHelper.writeAction("unittest", """
class Unittest {
    def execute(message, properties) {}
}
""")
        def json = JSON.parse('{"exception":[{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceProtocolException","exceptionMessage":"The message does not have a proper service object"}]}')
        HTTPBuilder http = new HTTPBuilder(url)
        HttpResponseDecorator httpResponse
        def respJson

        when:
        def status = http.request(Method.POST, ContentType.JSON) {
            uri.path = path
            body = [service:[noName:"bad structure"]]
            response.success = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
            response.failure = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
        }

        then:
        def responseJson = respJson.toString()
        httpResponse.status == 406
        httpResponse.contentType == "application/json"
        JSON.parse(responseJson) == json
    }

    def "test service with name but not a value of string"() {
        given:
        serviceFileHelper.writeDefinition("unittest", 'action file:"unittest"')
        serviceFileHelper.writeAction("unittest", """
class Unittest {
    def execute(message, properties) {}
}
""")
        def json = JSON.parse('{"exception":[{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceProtocolException","exceptionMessage":"The message does not have a proper service object"}]}')
        HTTPBuilder http = new HTTPBuilder(url)
        HttpResponseDecorator httpResponse
        def respJson

        when:
        def status = http.request(Method.POST, ContentType.JSON) {
            uri.path = path
            body = [service:[name:["bad structure"]]]
            response.success = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
            response.failure = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
        }

        then:
        def responseJson = respJson.toString()
        httpResponse.status == 406
        httpResponse.contentType == "application/json"
        JSON.parse(responseJson) == json
    }

    // I don't know how to make this test work.
    // I need to post without a class parsing responses
    def "test JSON syntax error"() {
        given:
        serviceFileHelper.writeDefinition("unittest", 'action file:"unittest"')
        serviceFileHelper.writeAction("unittest", """
class Unittest {
    def execute(message, properties) {}
}
""")
        def json = JSON.parse('{"exception":[{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceProtocolException","exceptionMessage":"Message JSON syntax error"}]}')

        HTTPBuilder http = new HTTPBuilder(url)
        HttpResponseDecorator httpResponse
        def respJson

        when:
        def status = http.request(Method.POST, ContentType.JSON) {
            uri.path = path
            body = '{"service":{"name":"unittest"}'
            response.success = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
            response.failure = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
        }

        then:
        def responseJson = respJson.toString()
        httpResponse.status == 400
        httpResponse.contentType == "application/json"
        JSON.parse(responseJson) == json
    }

    def "test script action not found"() {
        given:
        serviceFileHelper.writeDefinition("unittest", 'action file:"cannot_find_this_definition"')
        serviceFileHelper.writeAction("unittest", """
class Unittest {
    def execute(message, properties) {}
}
""")
        def json = JSON.parse('{"exception":[{"actionType":"groovy","actionName":"unknown","exceptionType":"ScriptActionFileNotFoundException","exceptionMessage":"Script Action not found"}],"service":{"name":"unittest"}}')
        HTTPBuilder http = new HTTPBuilder(url)
        HttpResponseDecorator httpResponse
        def respJson

        when:
        def status = http.request(Method.POST, ContentType.JSON) {
            uri.path = path
            body = [service:[name:"unittest"]]
            response.success = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
            response.failure = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
        }

        then:
        def responseJson = respJson.toString()
        httpResponse.status == 500
        httpResponse.contentType == "application/json"
        JSON.parse(responseJson) == json
    }

    def "test service definition not found"() {
        given:
        serviceFileHelper.writeDefinition("unittest", 'action file:"unittest"')
        serviceFileHelper.writeAction("unittest", """
class Unittest {
    def execute(message, properties) {}
}
""")
        def json = JSON.parse('{"exception":[{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceDefinitionFileNotFoundException","exceptionMessage":"Service Definition not found"},{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceEngineException","exceptionMessage":"Definition for service \\"cannot_find_the_definition\\" is null"}],"service":{"name":"cannot_find_the_definition"}}')
        HTTPBuilder http = new HTTPBuilder(url)
        HttpResponseDecorator httpResponse
        def respJson

        when:
        def status = http.request(Method.POST, ContentType.JSON) {
            uri.path = path
            body = [service:[name:"cannot_find_the_definition"]]
            response.success = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
            response.failure = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
        }

        then:
        def responseJson = respJson.toString()
        httpResponse.status == 500
        httpResponse.contentType == "application/json"
        JSON.parse(responseJson) == json
    }

    def "test service definition syntax error"() {
        given:
        serviceFileHelper.writeDefinition("unittest", """
// fail on treating a list like a map
def list = []
list.foo = bar

action file:"unittest"
""")
        serviceFileHelper.writeAction("unittest", """
class Unittest {
    def execute(message, properties) {}
}
""")
        def json = JSON.parse('{"exception":[{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceDefinitionException","exceptionMessage":"Service Definition Error: No such property: bar for class: unittest"},{"actionType":"groovy","actionName":"unknown","exceptionType":"ServiceEngineException","exceptionMessage":"Definition for service \\"unittest\\" is null"}],"service":{"name":"unittest"}}')
        HTTPBuilder http = new HTTPBuilder(url)
        HttpResponseDecorator httpResponse
        def respJson

        when:
        def status = http.request(Method.POST, ContentType.JSON) {
            uri.path = path
            body = [service:[name:"unittest"]]
            response.success = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
            response.failure = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
        }

        then:
        def responseJson = respJson.toString()
        httpResponse.status == 500
        httpResponse.contentType == "application/json"
        JSON.parse(responseJson) == json
    }

    def "test action script syntax error"() {
        given:
        serviceFileHelper.writeDefinition("unittest", 'action file:"unittest"')
        serviceFileHelper.writeAction("unittest", """
class Unittest {
    def execute(message, properties) {
        // fail on treating a list like a map
        def list = []
        list.foo = bar
    }
}
""")
        def json = JSON.parse('{"exception":[{"actionType":"groovy","actionName":"unknown","exceptionType":"ScriptActionUncaughtException","exceptionMessage":"Script Action Error: groovy.lang.MissingPropertyException: No such property: bar for class: Unittest"}],"service":{"name":"unittest"}}')
        HTTPBuilder http = new HTTPBuilder(url)
        HttpResponseDecorator httpResponse
        def respJson

        when:
        def status = http.request(Method.POST, ContentType.JSON) {
            uri.path = path
            body = [service:[name:"unittest"]]
            response.success = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
            response.failure = { resp, httpJson ->
                httpResponse = resp
                respJson = httpJson
            }
        }

        then:
        def responseJson = respJson.toString()
        httpResponse.status == 500
        httpResponse.contentType == "application/json"
        JSON.parse(responseJson) == json
    }
}