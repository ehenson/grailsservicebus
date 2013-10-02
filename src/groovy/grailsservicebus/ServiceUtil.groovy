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
}