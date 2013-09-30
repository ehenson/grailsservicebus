package grailsservicebus

import org.apache.commons.logging.LogFactory

class EsbController {
    private static final log = LogFactory.getLog(EsbController.class)

    def index() {
        def message

        if (log.isTraceEnabled()) {
            log.trace "Entered index()"
            log.trace "Checking request method for POST"
        }

        if (request.method == 'POST') {
            if (log.isTraceEnabled()) {
                log.trace "Request method is POST"
                log.trace "Checking request format to be JSON"
            }
            if (request.format == 'json') {
                log.trace "Request has format of JSON"

                try {
                    log.trace("Getting JSON object of Message in the request")
                    message = request.JSON
                } catch (org.codehaus.groovy.grails.web.converters.exceptions.ConverterException e) {
                    log.error("JSON parsing error in Message", e)
                    response.sendError(400, "Message JSON syntax error")
                }

                // at this point JSON is validated by Grails
                if (message != null) {
                    if (log.isTraceEnabled()) {
                        log.trace "Message = \"${message}\""
                    }

                    render (contentType:'application/json') {
                        if (log.isTraceEnabled()) {
                            log.trace "Returning message = \"${message.toString()}\""
                        }
                        message
                    }
                }
           } else {
                // not json
                log.error "Content type is not JSON.  Setting status to 406."
                response.sendError(406, "invalid content type")
            }
        } else {
            // not post
            log.error "Request method is not POST.  Setting status to 405."
            response.sendError(405)
        }
        log.trace("Leaving index()")
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
