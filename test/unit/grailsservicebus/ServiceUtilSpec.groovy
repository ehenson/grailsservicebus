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
                exception: [[actionType: "groovy", actionName: "unknown",
                        exceptionType: "unitTestType", exceptionMessage: "unit test message"]]]
    }

    void "test throwException with action specified"() {
        when:
        ServiceUtil.throwException(message, "unitTestType", "unit test message", "action type", "action name")

        then:
        message == [service: [name: "unittest"], testKey: "testValue",
                exception: [[actionType: "action type", actionName: "action name",
                        exceptionType: "unitTestType", exceptionMessage: "unit test message"]]]
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

    void "testing array_in_array"() {
        when:
        ServiceUtil.array_in_array(null, null)
        then:
        thrown(java.lang.NullPointerException)

        when:
        ServiceUtil.array_in_array(null, ["array"])
        then:
        thrown(java.lang.NullPointerException)

        when:
        ServiceUtil.array_in_array(["array"], null)
        then:
        thrown(java.lang.NullPointerException)

        when:
        ServiceUtil.array_in_array(null, "string")
        then:
        thrown(java.lang.NullPointerException)

        when:
        ServiceUtil.array_in_array("string", null)
        then:
        thrown(java.lang.NullPointerException)

        when:
        ServiceUtil.array_in_array(null, 1)
        then:
        thrown(java.lang.NullPointerException)

        when:
        ServiceUtil.array_in_array(1, null)
        then:
        thrown(java.lang.NullPointerException)

        when:
        ServiceUtil.array_in_array(["array1", "array2"], null)
        then:
        thrown(java.lang.NullPointerException)

        when:
        ServiceUtil.array_in_array(null, ["array1", "array2"])
        then:
        thrown(java.lang.NullPointerException)

        when:
        ServiceUtil.array_in_array(["array1", "array2"], "string")
        then:
        thrown(IllegalArgumentException)

        when:
        ServiceUtil.array_in_array("string", ["array1", "array2"])
        then:
        thrown(IllegalArgumentException)

        when:
        ServiceUtil.array_in_array(["array1", "array2"], 1)
        then:
        thrown(IllegalArgumentException)

        when:
        ServiceUtil.array_in_array(1, ["array1", "array2"])
        then:
        thrown(IllegalArgumentException)

        expect:
        ServiceUtil.array_in_array(["not found"], ["array1", "array2"]) == false
        ServiceUtil.array_in_array(["array1"], ["array1", "array2"]) == true
        ServiceUtil.array_in_array(["array2"], ["array1", "array2"]) == true
        ServiceUtil.array_in_array(["array1", "array2"], ["array1", "array2"]) == true
        ServiceUtil.array_in_array(["array2", "array1"], ["array1", "array2"]) == true
    }

    def "testing getRuntimeType"() {
        given:
        def json = JSON.parse('{"object":{"key":"value"}, "string":"string", "integer":1, "double":1.0, "boolean":true, "array":["a", "list"], "null":null}')

        when: "a null is used"
        ServiceUtil.getRuntimeType(null)
        then: "a NPE should be thrown"
        thrown(NullPointerException)

        when: "an unknwon object is used"
        ServiceUtil.getRuntimeType(new ServiceUtil())
        then: "an IllegalArgumentException should be thrown"
        thrown(IllegalArgumentException)

        expect:
        ServiceUtil.getRuntimeType(json.object) == "object"
        ServiceUtil.getRuntimeType(json.string) == "string"
        ServiceUtil.getRuntimeType(json.integer) == "integer"
        ServiceUtil.getRuntimeType(json.double) == "double"
        ServiceUtil.getRuntimeType(json.boolean) == "boolean"
        ServiceUtil.getRuntimeType(json.array) == "array"
        ServiceUtil.getRuntimeType(json.null) == "null"
    }

    def "test verifyInterface for null parameters"() {
        when:
        ServiceUtil.verifyInterface(null, [])
        then:
        thrown(NullPointerException)

        when:
        ServiceUtil.verifyInterface([], null)
        then:
        thrown(NullPointerException)
    }

    def "test object type of serviceInterface to be invalid"() {
        given:
        def serviceInterface = [name:"username", required:true, type:"string"]
        def message = [service:[name:"unittest"], username:"bob"]

        when:
        ServiceUtil.verifyInterface(message, serviceInterface)

        then:
        ServiceUtil.hasException(message) == true
        message.exception[0].exceptionType == "ServiceInterfaceException"
        message.exception[0].exceptionMessage == "Service interface is not an array."
    }

    def "test serviceInterface with no type definitions should throw message exception"() {
        given:
        def serviceInterface = []
        def message = [service:[name:"unittest"], username:"bob"]

        when:
        ServiceUtil.verifyInterface(message, serviceInterface)

        then:
        ServiceUtil.hasException(message) == true
        message.exception[0].exceptionType == "ServiceInterfaceException"
        message.exception[0].exceptionMessage == "Service interface definition list is empty."
    }

    def "test serviceInterface with invalid type definition map"() {
        given:
        def serviceInterface = [["an invalid type definition"]]
        def message = [service:[name:"unittest"], username:"bob"]

        when:
        ServiceUtil.verifyInterface(message, serviceInterface)

        then:
        ServiceUtil.hasException(message) == true
        message.exception[0].exceptionType == "ServiceInterfaceException"
        message.exception[0].exceptionMessage == "Service interface type definition map is invalid."
    }

    def "test serviceInterface with missing name from type definition map"() {
        given:
        def serviceInterface = [[required:true, type:"string"]]
        def message = [service:[name:"unittest"], username:"bob"]

        when:
        ServiceUtil.verifyInterface(message, serviceInterface)

        then:
        ServiceUtil.hasException(message) == true
        message.exception[0].exceptionType == "ServiceInterfaceException"
        message.exception[0].exceptionMessage == "Interface syntax error: Interface property \"name\" is required."
    }

    def "test serviceInterface with name being empty"() {
        given:
        def serviceInterface = [[name:"", required:true, type:"string"]]
        def message = [service:[name:"unittest"], username:"bob"]

        when:
        ServiceUtil.verifyInterface(message, serviceInterface)

        then:
        ServiceUtil.hasException(message) == true
        message.exception[0].exceptionType == "ServiceInterfaceException"
        message.exception[0].exceptionMessage == "Interface syntax error: Property \"name\" is an empty value."
    }

    def "test serviceInterface with required of wrong type"() {
        def serviceInterface = [[name:"username", required:"true", type:"string"]]
        def message = [service:[name:"unittest"], username:"bob"]

        when:
        ServiceUtil.verifyInterface(message, serviceInterface)

        then:
        ServiceUtil.hasException(message) == true
        message.exception[0].exceptionType == "ServiceInterfaceException"
        message.exception[0].exceptionMessage == "Service interface required specified is not of type Boolean."
    }

    def "testing serviceInterface with valid required of true"() {
        def serviceInterface = [[name:"username", required:true, type:"string"]]
        def message = [service:[name:"unittest"], username:"bob"]

        expect:
        ServiceUtil.verifyInterface(message, serviceInterface) == true
    }

    def "testing serviceInterface with valid default required of true because the definition was specified"() {
        def serviceInterface = [[name:"username"]]
        def message = [service:[name:"unittest"], username:"bob"]

        expect:
        ServiceUtil.verifyInterface(message, serviceInterface) == true
    }

    def "testing serviceInterface with valid required of false"() {
        def serviceInterface = [[name:"username", required:true, type:"string"]]
        def message = [service:[name:"unittest"]]

        expect:
        ServiceUtil.verifyInterface(message, serviceInterface) == false
    }

    def "testing serviceInterface with default value"() {
        def serviceInterface = [[name:"username", default:"bob"]]
        def message = [service:[name:"unittest"]]

        expect:
        ServiceUtil.verifyInterface(message, serviceInterface) == true
        message == [service:[name:"unittest"], username:"bob"]
    }

    def "testing failing serviceInterface of type array with string value"() {
        when:
        def serviceInterface = [[name:"key", type:"array"]]
        def json = "{service:{\"name\":\"unittest\"}, \"key\":\"not an array\"}"
        println json
        def message = JSON.parse(json)

        then:
        ServiceUtil.verifyInterface(message, serviceInterface) == false
        message == [exception:[[actionType:"groovy", actionName:"unknown", exceptionType:"ServiceInterfaceException", exceptionMessage:"Runtime type of the value does not match the specified value type."]], service:[name:"unittest"], key:"not an array"]
    }

    def "testing serviceInterface type validations"() {
        when:
        def serviceInterface = [[name:"key", type:type]]
        def json = "{service:{\"name\":\"unittest\"}, \"key\":${value}}"
        def message = JSON.parse(json)

        then:
        ServiceUtil.verifyInterface(message, serviceInterface) == truth

        where:
        type      | value                 | truth
        "any"     | '{"key1":"value1"}'   | true
        "any"     | '[1, 2, 3]'           | true
        "any"     | 'true'                | true
        "any"     | 'false'               | true
        "any"     | '1.0'                 | true
        "any"     | '1'                   | true
        "any"     | 'null'                | true
        "any"     | '"string"'            | true
        "array"   | '[1, 2, 3]'           | true
        "array"   | '"not an array"'      | false
        "boolean" | true                  | true
        "boolean" | false                 | true
        "boolean" | '"not a boolean"'     | false
        "double"  | 1                     | false
        "double"  | 1.0                   | true
        "double"  | '"not a double"'      | false
        "integer" | 1                     | true
        "integer" | 1.0                   | false
        "integer" | '"not an integer"'    | false
        "null"    | null                  | true
        "null"    | '"not a null"'        | false
        "numeric" | 1                     | true
        "numeric" | 1.0                   | true
        "numeric" | '"not a numeric"'     | false
        "object"  | '{"key1":"value1"}'   | true
        "object"  | '"not an object"'     | false
        "string"  | '"this is a string"'  | true
        "string"  | false                 | false
    }

    def "testing serviceInterface if it can handle multiple definitions"() {
        when:
        def serviceInterface = [
                [name:"firstName", type:"string", required:true],
                [name:"lastName", type:"string", required:true]
        ]
        def json = '{service:{"name":"unittest"}, "firstName":"John", "lastName":"Doe"}'
        def message = JSON.parse(json)

        then:
        ServiceUtil.verifyInterface(message, serviceInterface) == true
    }

    def "testing serviceInterface if it can handle multiple definitions as a failed validation"() {
        when:
        def serviceInterface = [
                [name:"firstName", type:"string", required:true],
                [name:"lastName", type:"string", required:true]
        ]
        def json = '{service:{"name":"unittest"}, "firstName":"John"}'
        def message = JSON.parse(json)

        then:
        ServiceUtil.verifyInterface(message, serviceInterface) == false
    }
}
