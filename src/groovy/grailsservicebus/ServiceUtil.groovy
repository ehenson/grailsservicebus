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
    public static def hasException(def message) {
        Log log = LogFactory.getLog(this)
        if (log.isTraceEnabled()) {
            log.trace "Entered static def hasException(def message)"
            log.trace "message = \"$message\""
        }
        def value = message['exception'] != null
        if (log.isTraceEnabled()) {
            log.trace "returning value = \"$value\""
            log.trace "Leaving static def hasException(def message)"
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
    public static void throwException(
            def message,
            def exceptionType, def exceptionMessage, def actionType = "groovy", def actionName = "unknown") {
        Log log = LogFactory.getLog(this)
        if (log.isTraceEnabled()) {
            log.trace "Entered static void throwException(def message, def exceptionType, def exceptionMessage, def actionType = \"groovy\", def actionName = \"unknown\")"
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
        message.exception = exception

        log.trace "Leaving static void throwException(def message, def exceptionType, def exceptionMessage, def actionType = \"groovy\", def actionName = \"unknown\")"
    }

    /**
     * Verifies the message against a service interface
     * @param message
     * @param serviceInterface
     */

    /*
    def static verifyInterface(def message, def serviceInterface) {
        Log log = LogFactory.getLog(this)
        if (log.isTraceEnabled()) {
            log.trace "Entered static verifyInterface(def message, def interface)"
            log.trace "message = \"$message\""
            log.trace "interface = \"$serviceInterface"
        }
        def returnCode = true
        if (!serviceInterface instanceof Object[]) {
            def errormessage = "Service interface is not an array."
            log.error errormessage
            ServiceUtil.throwException(message, "ServiceInterfaceException", errormessage)
            returnCode = false
        } else {
            serviceInterface.each { param ->
                if (param['name'] != null) {
                    def name = param.name.trim()
                    if(name) {
                        def required = true
                        if (param['required'] != null) {
                            required = param.required ? true : false
                        }
                        if (required) {
                            if (message[name] == null) {
                                def errormessage = "Property \"$name\" is required"
                                log.error errormessage
                                ServiceUtil.throwException(message, "ServiceInterfaceException", errormessage)
                                returnCode = false
                                break;
                            }
                        }
                        // check for default and apply
                        if (!required && param['default'] != null) {
                            message[name] = param.default
                        }
                        // check type
                        if (param['type'] != null) {
                            // type can be one or many of 'string', 'integer', 'double',  'boolean', 'array', 'object', 'null'
                            def validTypes = ['string', 'numeric', 'bool', 'array', 'object']
                            if (!param.type instanceof Object[]) {
                                param.type = [param.type]
                            }
                            if (array_in_array(param.type, validTypes)) {
          //*********************  this is where you left off ********************************
                            } else {
                                def errormessage = "Interface syntax error: Property type specified for property \"$name\" is invalid."
                                log.error errormessage
                                ServiceUtil.throwException(message, "ServiceInterfaceException", errormessage)
                                returnCode = false
                                break;
                            }
                        }
                    } else {
                        def errormessage = "Interface syntax error: Property \"name\" is a blank value."
                        log.error errormessage
                        ServiceUtil.throwException(message, "ServiceInterfaceException", errormessage)
                        returnCode = false
                        break;
                    }
                } else {
                    //param name is not found
                    def errormessage = "Interface syntax error: Interface property \"name\" is required."
                    log.error errormessage
                    ServiceUtil.throwException(message, "ServiceInterfaceException", errormessage)
                    returnCode = false
                    break;
                }
            }
        }

    }

*/
    /**
     * check to see if any part of the needleList is in the haystack
     * @param needleList
     * @param haystack
     * @return
     */
    public static boolean array_in_array(def needleList, def haystack) {
//TODO: Unit Test Me!!!
        Log log = LogFactory.getLog(this)
        if (log.isTraceEnabled()) {
            log.trace "Entered public static boolean array_in_array(def needleList, def haystack)"
            log.trace "needleList = $needleList"
            log.trace "haystack = $haystack"
            log.trace "setting rc to false"
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
            log.trace "Leaving public static boolean array_in_array(def needleList, def haystack)"
        }
        return rc
    }

    /**
     * get the runtime type of value
     * This should be the possible values from the JSON.parse API
     * @param value
     * @return
     */
    public static def getRuntimeType(def value) {
        Log log = LogFactory.getLog(this)
        if (log.isTraceEnabled()) {
            log.trace "Entered public static def getRuntimeType(def value)"
            log.trace "value = \"$value\""
            log.trace "value type is ${value.getClass()}"
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
        }
        if (log.isTraceEnabled()) {
            log.trace "returning type = \"$type\""
            log.trace "Leaving public static def getRuntimeType(def value)"
        }
        return type
    }
}