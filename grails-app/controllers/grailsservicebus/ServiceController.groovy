package grailsservicebus
import org.apache.commons.logging.LogFactory

class ServiceController {
    private static final log = LogFactory.getLog(ServiceController.class)

    def index() {
        if (log.isTraceEnabled()) {
            log.trace "Entered index()"
            log.trace "getting servlet Async context"
        }
        def ctx = startAsync()
        log.trace "Starting Async index()"
        ctx.start {
            log.trace "Processing request in async block"
            processRequest()
            log.trace "Finished processing request.  Conpleting Async servlet to close connection"
            ctx.complete()
            log.trace "Leaving async index()"
        }
        log.trace("Leaving index()")
    }

    /**
     * This is so the main logic can be unit tested outside of the async index()
     * @return
     */
    private def processRequest() {
        if (log.isTraceEnabled()) {
            log.trace "Entered processRequest()"
            log.trace "Checking request method for POST"
        }

        def message = [:]
        def httpStatus = 200

        try {
            if (request.method == 'POST') {
                if (log.isTraceEnabled()) {
                    log.trace "Request method is POST"
                    log.trace "Checking request format to be JSON"
                }
                if (request.format == 'json') {
                    log.trace "Request has format of JSON"

                    try {
                        log.trace "Getting JSON object of Message in the request"
                        message = request.JSON
                    } catch (org.codehaus.groovy.grails.web.converters.exceptions.ConverterException e) {
                        log.error("JSON parsing error in Message", e)
                        ServiceUtil.throwException(message, "ServiceProtocolException", "Message JSON syntax error")
                        httpStatus = 400
                    }

                    // at this point JSON is validated by Grails
                    if (httpStatus == 200) {
                        if (log.isTraceEnabled()) {
                            log.trace "Message = \"${message}\""
                            log.trace "Checking for valid service object"
                        }

                        // check for valid service object and throwException if not
                        if (checkForValidServiceObject(message)) {
                            // message is good at this point
                            log.trace "Message has a proper service object"

                            // This is for unit testing.  Consider a different way of testing an uncaught exception and remove this.
                            if (message.npe) throw NullPointerException();

                            /***************************   Main Driver part of service engine begins here **************************/
                            // use ConfigSlurper for the definition files: http://mrhaki.blogspot.com/2009/10/groovy-goodness-using-configslurper.html
                            // support "environments"

                            // to find the current environment
                            // grails.util.Environment.current

                            // for the "production" use of the ConfigSlurper("<env name>") you have to have a "runtime"
                            // environment such as "production-test".
                            // this will allow to have a production built WAR but it can have modes to have normal and test
                            // where test can read config.groovy files are runtime and then the settings can be overriden in a test environment
                            // on a customer's machine.
                        } else {
                            if (log.isTraceEnabled()) {
                                log.trace "Message does not have a proper service object"
                                log.trace "throwing a message exception and setting status to 400"
                            }
                            ServiceUtil.throwException(message, "ServiceProtocolException", "The message does not have a proper \"service\" object")
                            httpStatus = 406
                        }
                    }
                } else {
                    // not json
                    log.error "Content type is not JSON.  Setting status to 406."
                    ServiceUtil.throwException(message, "ServiceProtocolException", "Only JSON Content Types are supported.")
                    httpStatus = 406
                }
            } else {
                // not post
                log.error "Request method is not POST.  Setting status to 405."
                ServiceUtil.throwException(message, "ServiceProtocolException", "Non POST request are not supported.")
                httpStatus = 405
            }
        } catch (Exception e) {
            log.error "Uncaught Error.  Setting status to 500."
            ServiceUtil.throwException(message, "UncaughtException", "An uncaught exception as occured.")
            httpStatus = 500
        }

        render(contentType: 'application/json', status: httpStatus) {
            if (log.isTraceEnabled()) {
                log.trace "Returning message = \"${message.toString()}\""
                log.trace "Content-Type: application/json"
                log.trace "status = $httpStatus"
            }
            message
        }

        log.trace("Leaving processRequest()");
    }

    /**
     * Checks for a valid service object
     * @param message
     * @return boolean
     */
    private def checkForValidServiceObject(def message) {
        if (log.isTraceEnabled()) {
            log.trace "Entered def checkForValidServiceObject(def message)"
            log.trace "message = \"${message}\""
            log.trace "setting default return value to false"
        }
        // default to false
        boolean success = false
        // does a service object exists
        log.trace "checking for 'service' key"
        if (message['service'] != null) {
            def service = message.service
            // is it a map
            if (log.isTraceEnabled()) {
                log.trace "'service' key exists"
                log.trace "is 'service' a Map"
            }
            if (service instanceof Map) {
                if (log.isTraceEnabled()) {
                    log.trace "'service' is a Map"
                    log.trace "checking if 'service' has 'name' key"
                }
                // does service have a key of name
                if (service['name'] != null) {
                    if (log.isTraceEnabled()) {
                        log.trace "'service' has a 'name' key"
                        log.trace "checking if 'name' is a String"
                    }
                    def name = service.name
                    // is name a String
                    if (name instanceof String) {
                        if (log.isTraceEnabled()) {
                            log.trace "'name' is a String"
                            log.trace "checking if 'name' is a valid non-empty string"
                        }
                        // is it a valid non-emtpy string
                        success = (name?.trim()) as boolean
                        if (success) {
                            log.trace "'name' is a valid non-empty string"
                        } else {
                            log.trace "'name' is not a valid non-empty string"
                        }
                    } else {
                        log.trace "'name' is not a String"
                    }
                } else {
                    log.trace "'service' does not have a 'name' key"
                }
            } else {
                log.trace "'service' is not a Map"
            }
        } else {
            log.trace "'service' key was not found"
        }

        if (log.isTraceEnabled()) {
            log.trace "returning success = ${success}"
            log.trace "leaving def checkForValidServiceObject(def message)"
        }
        return success
    }
}
