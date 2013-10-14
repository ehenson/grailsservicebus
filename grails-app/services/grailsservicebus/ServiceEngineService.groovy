package grailsservicebus

import org.apache.commons.logging.LogFactory
//import grails.transaction.Transactional
//
//@Transactional
class ServiceEngineService {
    private static final log = LogFactory.getLog(this)
    def grailsApplication
    def serviceDefinitionService

    static transactional = false

    @org.grails.plugins.yammermetrics.groovy.Timed
    @org.grails.plugins.yammermetrics.groovy.Metered
    def execute(message) {
        if (log.isTraceEnabled()) {
            log.trace "Entered def execute(message)"
            log.trace "message = \"${message}\""
//            log.trace "Current Environment = \"${Environment.current.name}\""
        }

//        def configSlurper = new ConfigSlurper(Environment.current.name)
        //do bindings here
//        def definitionDir = System.getProperty("grailservicebus.definition.directory", "/opt/grailsservicebus/definitions")
//        log.trace "definitionDir = \"${definitionDir}\""
//        def definitionFile = new File("${definitionDir}/${message.service.name}.groovy")
//        log.trace "definitionFile = \"${definitionFile}\""

//        if (definitionFile.exists()) {
//            log.trace "Definition file exists.  Parsing with ConfigSlurper"
//            def serviceConfig = configSlurper.parse(definitionFile.toURI().toURL())

        def definition = serviceDefinitionService.getDefinition(message.service.name)

        for(action in definition.actions) {
            def actionHandlerName = "${action.handler}ActionHandlerService"
            log.trace "Loading \"${actionHandlerName}\" service"
            def actionHandler = grailsApplication.mainContext."${actionHandlerName}"

            if (log.isTraceEnabled()) {
                log.trace "Executing the action with:"
                log.trace "message = \"${message}\""
                log.trace "properties = \"${action.properties}\""
            }
            actionHandler.execute(action, message, action.properties)
            log.trace "return from executing action"
        }

//        } else {
//            def errorMessage = "Definition file \"${definitionFile}\" does not exist."
//            log.error errorMessage
//            ServiceUtil.throwException(message, "ServiceRegistryException", errorMessage)
//        }
        if (log.isTraceEnabled()) {
            log.trace "returning message = \"${message}\""
            log.trace "Leaving def execute(message)"
        }
    }
}
