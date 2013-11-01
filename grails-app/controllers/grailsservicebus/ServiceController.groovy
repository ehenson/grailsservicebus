package grailsservicebus
import org.apache.commons.logging.LogFactory

class ServiceController {
    private static final log = LogFactory.getLog(ServiceController.class)
    ServiceEngineService serviceEngineService

    @org.grails.plugins.yammermetrics.groovy.Timed
    @org.grails.plugins.yammermetrics.groovy.Metered
    def index() {
        log.trace "Entered index()"
        def webRender = processRequest()
        log.trace "Leaving index()"
        return webRender
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
            if ("POST" == request.method) {
                if (log.isTraceEnabled()) {
                    log.trace "Request method is POST"
                    log.trace "Checking request format to be JSON"
                }
                if ("json" == request.format) {
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

                            log.trace "Executing the service engine"
                            serviceEngineService.execute(message)
                            log.trace "Finished executing the service engine"
                        } else {
                            if (log.isTraceEnabled()) {
                                log.trace "Message does not have a proper service object"
                                log.trace "throwing a message exception and setting status to 400"
                            }
                            if(!(message instanceof LinkedHashMap)) {
                                message = [:]
                            }
                            ServiceUtil.throwException(message, "ServiceProtocolException", "The message does not have a proper service object")
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
        } catch (Throwable e) {
            log.error "Uncaught Error.  Setting status to 500. Throwable.message = \"${e.message}\""
            ServiceUtil.throwException(message, "UncaughtException", "An uncaught exception has occurred.")
            httpStatus = 500
        }

        if (ServiceUtil.hasException(message) && httpStatus == 200) {
            log.trace "Message has an exception.  Setting status to 500"
            httpStatus = 500
        }

        if (log.isTraceEnabled()) {
            log.trace "Returning message = \"${message}\""
            log.trace "Content-Type: application/json"
            log.trace "status = $httpStatus"
            log.trace "Leaving processRequest() while rendering the message"
        }

        render(contentType: 'application/json', status: httpStatus) {
            message
        }
    }

    /**
     * Checks for a valid service object
     * @param message
     * @return boolean
     */
    private def checkForValidServiceObject(message) {
        if (log.isTraceEnabled()) {
            log.trace "Entered def checkForValidServiceObject(message)"
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
            log.trace "leaving def checkForValidServiceObject(message)"
        }
        return success
    }
}
