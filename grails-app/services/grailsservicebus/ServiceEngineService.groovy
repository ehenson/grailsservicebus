package grailsservicebus
import grails.util.Environment
import org.apache.commons.logging.LogFactory
//import grails.transaction.Transactional
//
//@Transactional
class ServiceEngineService {
    private static final log = LogFactory.getLog(this)
    def grailsApplication

    def execute(def message) {
        if (log.isTraceEnabled()) {
            log.trace "Entered def execute(def message)"
            log.trace "message = \"${message}\""
        }
        def definitionDir = System.getProperty("grailservicebus.definition.directory", "/opt/grailsservicebus/definitions")

        log.trace "definitionDir = \"${definitionDir}\""
        log.trace "Current Environment = \"${Environment.current.name}\""

        def configSlurper = new ConfigSlurper(Environment.current.name)
        //do bindings here
        def definitionFile = new File("${definitionDir}/${message.service.name}.groovy")
        if (definitionFile.exists()) {
            def serviceConfig = configSlurper.parse(definitionFile.toURI().toURL())
            def serviceHandler = grailsApplication.mainContext."${serviceConfig.handler}ServiceHandlerService"
            serviceHandler.execute serviceConfig, message, [:]
        } else {
            log.error "Definition file \"${definitionFile}\" does not exist."
        }
    }
}
