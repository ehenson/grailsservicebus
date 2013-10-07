package grailsservicebus

import org.apache.commons.logging.LogFactory

//import grails.transaction.Transactional
//
//@Transactional
class ScriptServiceHandlerService {
    private static final log = LogFactory.getLog(this)

    def scriptExecutionService
    def log

    def execute(definitionConfig, message, properties) {
        if (log.isTraceEnabled()) {
            log.trace "Entered def execute(definitionConfig, message, properties)"
            log.trace "definitionConfig = \"${definitionConfig}\""
            log.trace "message = \"${message}\""
            log.trace "properties = \"${properties}\""
        }

        scriptExecutionService.executeScript(definitionConfig.file, message, properties)

        if (log.isTraceEnabled()) {
            log.trace "Entered def execute(definitionConfig, message, properties)"
        }
    }
}
