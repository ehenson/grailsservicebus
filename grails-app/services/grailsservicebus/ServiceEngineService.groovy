package grailsservicebus
import grails.util.Environment
import org.apache.commons.logging.LogFactory
//import grails.transaction.Transactional
//
//@Transactional
class ServiceEngineService {
    private static final log = LogFactory.getLog(this)
    def grailsApplication
    def definitionDir = System.getProperty("grailservicebus.definition.directory", "/opt/grailsservicebus/definitions")


    def execute(message) {
        if (log.isTraceEnabled()) {
            log.trace "Entered def execute(message)"
            log.trace "message = \"${message}\""
            log.trace "definitionDir = \"${definitionDir}\""
            log.trace "Current Environment = \"${Environment.current.name}\""
        }

        def configSlurper = new ConfigSlurper(Environment.current.name)
        //do bindings here
        def definitionFile = new File("${definitionDir}/${message.service.name}.groovy")
        log.trace "definitionFile = \"${definitionFile}\""

        if (definitionFile.exists()) {
            log.trace "Definition file exists.  Parsing with ConfigSlurper"
            def serviceConfig = configSlurper.parse(definitionFile.toURI().toURL())
            def serviceHandlerName = "${serviceConfig.handler}ServiceHandlerService"
            log.trace "Loading \"${serviceHandlerName}\" service"
            def serviceHandler = grailsApplication.mainContext."${serviceHandlerName}"
            def properties = [:]
            if (log.isTraceEnabled()) {
                log.trace "Executing the service with:"
                log.trace "message = \"${message}\""
                log.trace "properties = \"${properties}\""
            }
            serviceHandler.execute serviceConfig, message, properties
            log.trace "Finished executing service"
        } else {
            def errorMessage = "Definition file \"${definitionFile}\" does not exist."
            log.error errorMessage
            ServiceUtil.throwException(message, "ServiceRegistryException", errorMessage)
        }
        if (log.isTraceEnabled()) {
            log.trace "returning message = \"${message}\""
            log.trace "Leaving def execute(message)"
        }
    }
}
