/**
 * Created by ehenson on 9/30/13.
 */
package grailsservicebus

import grails.converters.JSON
import spock.lang.Specification

/**
 *
 */
class ServiceUtilSpec extends Specification {
    def message

    def setup() {
        message = [service: [name: "unittest"], testKey: "testValue"]
    }

    void "test has exception without the exception key"() {
        expect:
        ServiceUtil.hasException(message) is false
    }

    void "test has exception with the exception key"() {
        given:
        message.exception = []

        expect:
        ServiceUtil.hasException(message) is true
    }

    void "test throwException as normal"() {
        when:
        ServiceUtil.throwException(message, "unitTestType", "unit test message")

        then:
        message == [service: [name: "unittest"], testKey: "testValue",
                exception: [actionType: "groovy", actionName: "unknown",
                        exceptionType: "unitTestType", exceptionMessage: "unit test message"]]
    }

    void "test throwException with action specified"() {
        when:
        ServiceUtil.throwException(message, "unitTestType", "unit test message", "action type", "action name")

        then:
        message == [service: [name: "unittest"], testKey: "testValue",
                exception: [actionType: "action type", actionName: "action name",
                        exceptionType: "unitTestType", exceptionMessage: "unit test message"]]
    }

    void "what is the groovy types of json"() {
        when:
        def json = JSON.parse('{"object":{"key":"value"}, "string":"string", "integer":1, "double":1.0, "boolean":true, "array":["a", "list"], "null":null}')

        then:
        json.object instanceof org.codehaus.groovy.grails.web.json.JSONObject
        json.string instanceof java.lang.String
        json.integer instanceof java.lang.Integer
        json.double instanceof java.lang.Double
        json.boolean instanceof java.lang.Boolean
        json.array instanceof org.codehaus.groovy.grails.web.json.JSONArray
        json.null instanceof org.codehaus.groovy.grails.web.json.JSONObject.Null
    }

    void "test getRuntimeType"() {
        given:
        def json = JSON.parse('{"object":{"key":"value"}, "string":"string", "integer":1, "double":1.0, "boolean":true, "array":["a", "list"], "null":null}')

        expect:
        ServiceUtil.getRuntimeType(json.object) == "object"
        ServiceUtil.getRuntimeType(json.string) == "string"
        ServiceUtil.getRuntimeType(json.integer) == "integer"
        ServiceUtil.getRuntimeType(json.double) == "double"
        ServiceUtil.getRuntimeType(json.boolean) == "boolean"
        ServiceUtil.getRuntimeType(json.array) == "array"
        ServiceUtil.getRuntimeType(json.null) == "null"
    }

    //TODO:  Unit Test array_in_array
}
