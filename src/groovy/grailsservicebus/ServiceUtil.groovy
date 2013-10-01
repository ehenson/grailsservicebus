/**
 * Created by ehenson on 9/30/13.
 */
package grailsservicebus

public class ServiceUtil {

    /**
     * check for the exception key in the message
     * @param message
     * @return boolean
     */
    public static def hasException(def message) {
        message['exception'] != null
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
        def exceptions = []

        if (hasException(message)) {
            exceptions = message.exception
        }

        def exception = [actionType: actionType,
                actionName: actionName,
                exceptionType: exceptionType,
                exceptionMessage: exceptionMessage]

        exceptions << exception

        message.exception = exception
    }
}