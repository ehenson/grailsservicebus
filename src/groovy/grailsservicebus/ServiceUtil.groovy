/**
 * Created by ehenson on 9/30/13.
 */
package grailsservicebus

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

public class ServiceUtil {

    /**
     * check for the exception key in the message
     * @param message
     * @return boolean
     */
    public static def hasException(message) {
        Log log = LogFactory.getLog(this)
        if (log.isTraceEnabled()) {
            log.trace "Entered static def hasException(message)"
            log.trace "message = \"$message\""
        }
        def value = message['exception'] != null
        if (log.isTraceEnabled()) {
            log.trace "returning value = \"$value\""
            log.trace "Leaving static def hasException(message)"
        }
        value
    }

    /**
     * adds an exception to the exception list to a message
     * @param message
     * @param exceptionType
     * @param exceptionMessage
     * @param actionType
     * @param actionName
     */
    public static void throwException(message, exceptionType, exceptionMessage, actionType = "groovy", actionName = "unknown") {
        Log log = LogFactory.getLog(this)
        if (log.isTraceEnabled()) {
            log.trace "Entered static void throwException(message, exceptionType, exceptionMessage, actionType = \"groovy\", actionName = \"unknown\")"
            log.trace "message = \"$message\""
            log.trace "exceptionType = \"$exceptionType\""
            log.trace "exceptionMessage = \"$exceptionMessage\""
            log.trace "actionType = \"$actionType\""
            log.trace "actionName = \"$actionName\""

            log.trace "setting exceptions list to empty"
        }

        def exceptions = []

        log.trace "check message for an existing exception"
        if (hasException(message)) {
            exceptions = message.exception
            if (log.isTraceEnabled()) {
                log.trace "exceptions found = $exceptions"
            }
        } else {
            log.trace "no exceptions found"
        }

        log.trace "building exception object"
        def exception = [actionType: actionType,
                actionName: actionName,
                exceptionType: exceptionType,
                exceptionMessage: exceptionMessage]

        if (log.isTraceEnabled()) {
            log.trace "exception object = $exception"
            log.trace "adding exception to the exceptions list"
        }

        exceptions << exception

        log.trace "adding exceptions to the message"
        message.exception = exceptions

        log.trace "Leaving static void throwException(message, exceptionType, exceptionMessage, actionType = \"groovy\", actionName = \"unknown\")"
    }

    /**
     * Verifies the message against a service interface
     * The Service Definition key = "interface" as an array
     * Each element:
     * Key, Type, Required, Description
     * "name", string, true, This is the name of key in the message
     * "required", boolean, false, If the key in the message is mandatory
     * "default", any, false, If the key does not exists then create it with the value provided
     * "type", array of strings, false, Defaults to "any" and is used to validate the type of the value of the key in the message
     *
     * Valid types are: 'any', 'array', 'boolean', 'double', 'integer', 'null', 'numeric', 'object', 'string'
     * Notes:  If 'numeric" is used then this allows a 'double' or an 'integer' for the value
     *
     * @param message
     * @param serviceInterface
     * @return boolean true if the message validates the interface
     * @throws NullPointerException if message or serviceInterface is null
     */
    def static verifyInterface(message, serviceInterface) {
        Log log = LogFactory.getLog(this)
        if (log.isTraceEnabled()) {
            log.trace "Entered static verifyInterface(message, interface)"
            log.trace "message = \"$message\""
            log.trace "interface = \"$serviceInterface"
            log.trace "checking params for null"
        }
        if (message == null || serviceInterface == null) {
            if (message == null) {
                log.error "Message is null.  Throwing NPE."
            } else {
                log.error "ServiceInterface is null.  Throwing NPE"
            }
            throw new NullPointerException()
        }
        def returnCode = true
        if (log.isTraceEnabled()) {
            log.trace "Setting returnCode to a default of True"
            log.trace "Checking serviceInterface to me a list"
        }
        if (!(serviceInterface instanceof java.util.ArrayList) ) {
            def errormessage = "Service interface is not an array."
            log.error errormessage
            ServiceUtil.throwException(message, "ServiceInterfaceException", errormessage)
            returnCode = false
        } else {
            log.trace "ServiceInterface is a list.  Checking size > 0"
            if (serviceInterface.size() == 0) {
                def errormessage = "Service interface definition list is empty."
                log.error errormessage
                ServiceUtil.throwException(message, "ServiceInterfaceException", errormessage)
                returnCode = false
            } else {
                log.trace "serviceInterface has elements.  Looping the list"
                serviceInterface.each { param ->
                    if (log.isTraceEnabled()) {
                        log.trace "param = \"${param}\""
                        log.trace "checking param to be an instance of LinkedHashMap"
                    }
                    if (param.getClass() != LinkedHashMap) {
                        def errormessage = "Service interface type definition map is invalid."
                        log.error errormessage
                        ServiceUtil.throwException(message, "ServiceInterfaceException", errormessage)
                        returnCode = false
                    } else {
                        log.trace "param is a map.  Checking for the name attribute"
                        if (param['name'] != null) {
                            log.trace "param has name"
                            def name = param.name.trim()
                            log.trace "checking to see if name is not empty and not false"
                            if(name) {
                                log.trace "name is a valid string.  setting required to default of true"
                                def required = true
                                if (param['required'] != null) {
                                    log.trace "required was specified.  checking class type"
                                    if (param.required instanceof Boolean) {
                                        log.trace "required is a Boolean and = ${param.required}"
                                        required = param.required
                                    } else {
                                        def errormessage = "Service interface required specified is not of type Boolean."
                                        log.error errormessage
                                        ServiceUtil.throwException(message, "ServiceInterfaceException", errormessage)
                                        returnCode = false
                                        return; // break;
                                    }
                                }
                                if (required) {
                                    log.trace "message attribute is required"
                                    if (message[name] == null) {
                                        // check for default and apply
                                        if (param['default'] != null) {
                                            log.trace "applying default value"
                                            message[name] = param.default
                                        } else {
                                            def errormessage = "Property \"$name\" is required"
                                            log.error errormessage
                                            ServiceUtil.throwException(message, "ServiceInterfaceException", errormessage)
                                            returnCode = false
                                            return //break;
                                        }
                                    } else {
                                        log.trace "message has the required attribute"
                                    }
                                } else {
                                    log.trace "message attribute is not required"
                                }

                                // check type
                                if (param['type'] != null) {
                                    // type can be one or many of:
                                    def validTypes = ['any', 'array', 'boolean', 'double', 'integer', 'null', 'numeric', 'object', 'string']

                                    // convert to an array
                                    if (!(param.type instanceof ArrayList)) {
                                        param.type = [param.type]
                                    }

                                    log.trace "Checking specified type is a valid type"
                                    // check if the specified type is in the list
                                    if (array_in_array(param.type, validTypes)) {
                                        if (log.isTraceEnabled()) {
                                            log.trace "specified type is valid"
                                            log.trace "getting runtime type of value"
                                        }
                                        // specified type has been validated
                                        def runtimeType = getRuntimeType(message[name])

                                        if (log.isTraceEnabled()) {
                                            log.trace "runtime type value = \"${runtimeType}\""
                                            log.trace "checking if runtime type value is of valid core types"
                                        }

                                        if (validTypes.contains(runtimeType)) {
                                            log.trace "runtime type is of core types"

                                            if (param.type.contains("any")) {
                                                log.trace "param.type = any value is validated"
                                                return
                                            }

                                            // we match the core json types
                                            if (param.type.contains("numeric")) {
                                                log.trace "param.type = numeric.  Checking for numeric value type"
                                                if (!(runtimeType == "double" || runtimeType == "integer")) {
                                                    def errormessage = "Type of numeric was specified but the type is not a Double or an Integer."
                                                    log.error errormessage
                                                    ServiceUtil.throwException(message, "ServiceInterfaceException", errormessage)
                                                    returnCode = false
                                                    return //break;
                                                } else {
                                                    log.trace "value type is of a numeric value"
                                                }
                                            } else {
                                                if (!param.type.contains(runtimeType)) {
                                                    def errormessage = "Runtime type of the value does not match the specified value type."
                                                    log.error errormessage
                                                    ServiceUtil.throwException(message, "ServiceInterfaceException", errormessage)
                                                    returnCode = false
                                                    return //break;
                                                }
                                            }
                                        } else {
                                            def errormessage = "Runtime type is not of the valid types of ${validTypes}"
                                            log.error errormessage
                                            ServiceUtil.throwException(message, "ServiceInterfaceException", errormessage)
                                            returnCode = false
                                            return //break;
                                        }
                                    } else {
                                        def errormessage = "Interface syntax error: Property type specified for property \"$name\" is invalid."
                                        log.error errormessage
                                        ServiceUtil.throwException(message, "ServiceInterfaceException", errormessage)
                                        returnCode = false
                                        return //break;
                                    }
                                }
                            } else {
                                def errormessage = "Interface syntax error: Property \"name\" is an empty value."
                                log.error errormessage
                                ServiceUtil.throwException(message, "ServiceInterfaceException", errormessage)
                                returnCode = false
                                return //break;
                            }
                        } else {
                            //param name is not found
                            def errormessage = "Interface syntax error: Interface property \"name\" is required."
                            log.error errormessage
                            ServiceUtil.throwException(message, "ServiceInterfaceException", errormessage)
                            returnCode = false
                            return //break;
                        }
                    }
                }
            }
        }

        if (log.isTraceEnabled()) {
            log.trace "returning validation of = ${returnCode}"
            log.trace "Leaving static verifyInterface(message, interface)"
        }
        return returnCode
    }

    /**
     * check to see if any part of the needleList is in the haystack
     * @param needleList
     * @param haystack
     * @return boolean
     * @throws NullPointerException if needle or haystack is null
     * @throws IllegalArgumentException if needle or haystack is not an ArrayList or []
     */
    public static boolean array_in_array(needleList, haystack) {
        Log log = LogFactory.getLog(this)
        if (log.isTraceEnabled()) {
            log.trace "Entered public static boolean array_in_array(needleList, haystack)"
            log.trace "needleList = $needleList"
            log.trace "haystack = $haystack"
            log.trace "setting rc to false"
            log.trace "Checking for needleList and haystack for null"
        }
        if (needleList == null || haystack == null ) {
            log.error "Either needleList or haystack is null.  Throwing NPE."
            throw new java.lang.NullPointerException();
        }
        if (!(needleList instanceof ArrayList) || !(haystack instanceof ArrayList)) {
            log.error "Either needleList or haystack is null.  Throwing IllegalArgumentException."
            throw new IllegalArgumentException()
        }
        boolean rc = false
        log.trace "looping needleList"
        needleList.each { needle ->
            log.trace "checking haystack for \"$needle\""
            if (haystack.contains(needle)) {
                log.trace "found needle in the haystack.  setting rc = true an breaking loop"
                rc = true
                return;
            }
            log.trace "didn't find the needle.  trying next needle"
        }
        if (log.isTraceEnabled()) {
            log.trace "Finished searching haystack.  returning rc = $rc"
            log.trace "Leaving public static boolean array_in_array(needleList, haystack)"
        }
        return rc
    }

    /**
     * get the runtime type of value based upon the org.codehaus.groovy.grails.web.json package
     * This should be the possible values from the JSON.parse API
     * Sample JSON string with all the types: {"object":{"key":"value"}, "string":"string", "integer":1, "double":1.0, "boolean":true, "array":["a", "list"], "null":null}
     * JSON type = return string
     * java.lang.String = "string"
     * org.codehaus.groovy.grails.web.json.JSONObject = "object"
     * java.lang.Integer = "integer"
     * java.lang.Double = "double"
     * java.lang.Boolean = "boolean"
     * org.codehaus.groovy.grails.web.json.JSONArray = "array"
     * org.codehaus.groovy.grails.web.json.JSONObject.Null = "null"
     * @param value
     * @return String
     * @throws IllegalArgumentException when the type does not match a known JSON type
     */
    public static def getRuntimeType(value) {
        Log log = LogFactory.getLog(this)
        if (log.isTraceEnabled()) {
            log.trace "Entered public static def getRuntimeType(value)"
            log.trace "value = \"$value\""
            log.trace "value type is ${value.getClass()}"
            log.trace "checking value to be null"
        }
        if (value == null) {
            throw new NullPointerException()
        }
        def type
        if (value instanceof java.lang.String) {
            type = "string"
        } else if (value instanceof org.codehaus.groovy.grails.web.json.JSONObject) {
            type = "object"
        } else if (value instanceof java.lang.Integer) {
            type = "integer"
        } else if (value instanceof java.lang.Double) {
            type = "double"
        } else if (value instanceof java.lang.Boolean) {
            type = "boolean"
        } else if (value instanceof org.codehaus.groovy.grails.web.json.JSONArray) {
            type = "array"
        } else if (value instanceof org.codehaus.groovy.grails.web.json.JSONObject.Null) {
            type = "null"
        } else {
            log.error "Unknown JSON data type: ${value.getClass()}.  Throwing IllegalArgumentException."
            throw new IllegalArgumentException("Unknown JSON data type: ${value.getClass()}")
        }
        if (log.isTraceEnabled()) {
            log.trace "returning type = \"$type\""
            log.trace "Leaving public static def getRuntimeType(value)"
        }
        return type
    }
}