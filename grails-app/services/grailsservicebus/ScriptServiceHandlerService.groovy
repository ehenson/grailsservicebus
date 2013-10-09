package grailsservicebus
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.context.ApplicationContext
//import grails.transaction.Transactional
//
//@Transactional
class ScriptServiceHandlerService {
    private static final log = LogFactory.getLog(this)

    def execute(definitionConfig, message, properties) {
        if (log.isTraceEnabled()) {
            log.trace "Entered def execute(definitionConfig, message, properties)"
            log.trace "definitionConfig = \"${definitionConfig}\""
            log.trace "message = \"${message}\""
            log.trace "properties = \"${properties}\""
        }

        def sourceFile = new File(definitionConfig.file)

        if (sourceFile.exists()) {
            log.trace "Loading script from ${definitionConfig.file}"
            def groovySource = new GroovyCodeSource(sourceFile)
            GroovyClassLoader gcl = new GroovyClassLoader(this.getClass().getClassLoader())

            log.trace "Parsing the source and caching"
            def scriptClass = gcl.parseClass(groovySource, true)
            log.trace "Creating a new instance"
            def classInstance = scriptClass.newInstance()

            def ctx = (ApplicationContext) ServletContextHolder
                    .getServletContext()
                    .getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT);

            log.trace "Autowiring bean properties"
            ctx.beanFactory.autowireBeanProperties(classInstance, ctx.beanFactory.AUTOWIRE_BY_NAME, false)

            log.trace "invoking the execute method on the script"
            classInstance.invokeMethod("execute", [message, properties])
        } else {
            log.error "Script not found at ${definitionConfig.file}"
            ServiceUtil.throwException("ScriptServiceException", "Script not found at ${definitionConfig.file}")
        }

        if (log.isTraceEnabled()) {
            log.trace "Finished executing the script."
            log.trace "message = \"${message}\""
            log.trace "Leaving def execute(definitionConfig, message, properties)"
        }
    }
}
