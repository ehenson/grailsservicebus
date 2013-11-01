package grailsservicebus
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.beans.factory.BeanFactory
import org.springframework.context.ApplicationContext

import javax.annotation.PostConstruct

class ScriptActionHandlerService {
    private static final log = LogFactory.getLog(this)
    static transactional = false
    def grailsApplication
    GroovyScriptEngine groovyScriptEngine
    ApplicationContext applicationContext
    GroovyClassLoader groovyClassLoader
    BeanFactory beanFactory
    String[] urls = ["/opt/grailsservicebus/actions"]

    /**
     * This init is creating the GroovyScriptEngine once because GCE is for servers and will cache and recompile the script if needed
     * http://www.intelligrape.com/blog/2012/08/27/using-postconstruct-annotation-with-grails-services/
     */
    @PostConstruct
    public void init() {
        log.trace "Entered init()"
        applicationContext = (ApplicationContext) ServletContextHolder.getServletContext().getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT);
        groovyClassLoader = new GroovyClassLoader(this.getClass().getClassLoader())
        urls = grailsApplication.config?.grailsservicebus?.actions?.locations as String[] ?: urls
        log.trace "Instantiating GroovyScriptEngine with locations = \"${urls}"
        groovyScriptEngine = new GroovyScriptEngine(urls, groovyClassLoader)
        beanFactory = applicationContext.beanFactory
        log.trace "Leaving init()"
    }

    @org.grails.plugins.yammermetrics.groovy.Timed
    @org.grails.plugins.yammermetrics.groovy.Metered
    void execute(action, message) {
        if (log.isTraceEnabled()) {
            log.trace "Entered execute(action, message, properties)"
            log.trace "action = \"${action}\""
            log.trace "message = \"${message}\""
            log.trace "getting scriptClass from file = \"${action.file}.groovy\""
        }

        def scriptClass
        try {
            scriptClass = groovyScriptEngine.loadScriptByName("${action.file}.groovy")
        } catch(groovy.util.ResourceException e) {
            def errormsg = "Script Action not found"
            log.error errormsg
            ServiceUtil.throwException(message, "ScriptActionFileNotFoundException", errormsg)
        } catch (Exception e) {
            def errormsg = "Script Action Error: ${e.message}"
            log.error errormsg
            ServiceUtil.throwException(message, "ScriptActionException", errormsg)
        }

        if (scriptClass != null) {
            log.trace "loading the \"script action\" script is successful"
            def classInstance = scriptClass.newInstance()

            log.trace "Autowiring bean properties"
            beanFactory.autowireBeanProperties(classInstance, beanFactory.AUTOWIRE_BY_NAME, false)

            try {
                log.trace "invoking the execute method on the script"
                classInstance.invokeMethod("execute", [message, action.properties])
            } catch (Throwable e) {
                def errormsg = "Script Action Error: ${e.toString()}"
                log.error errormsg, e
                ServiceUtil.throwException(message, "ScriptActionUncaughtException", errormsg)
            }
        } else {
            log.trace "error loading the \"script action\" script.  Skipped execution logic."
        }
        if (log.isTraceEnabled()) {
            log.trace "Finished executing action: \"${action.file}.groovy\""
            log.trace "message = \"${message}\""
            log.trace "Leaving execute(action, message)"
        }
    }
}
