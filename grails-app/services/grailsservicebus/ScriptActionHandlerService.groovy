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
        log.trace "Instantiating GroovyScriptEngine with url = \"${urls}"
        groovyScriptEngine = new GroovyScriptEngine(urls, groovyClassLoader)
        beanFactory = applicationContext.beanFactory
        log.trace "Leaving init()"
    }

    @org.grails.plugins.yammermetrics.groovy.Timed
    @org.grails.plugins.yammermetrics.groovy.Metered
    void execute(action, message, properties) {
        if (log.isTraceEnabled()) {
            log.trace "Entered execute(action, message, properties)"
            log.trace "action = \"${action}\""
            log.trace "message = \"${message}\""
            log.trace "properties = \"${properties}\""
        }

        def scriptClass
        try {
            scripClass = groovyScriptEngine.loadScriptByName(action.file)
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

            log.trace "invoking the execute method on the script"
            classInstance.invokeMethod("execute", [message, properties])
        } else {
            log.trace "error loading the \"script action\" script.  Skipped execution logic."
        }
        if (log.isTraceEnabled()) {
            log.trace "Finished executing action: \"${action.file}.groovy\""
            log.trace "message = \"${message}\""
            log.trace "Leaving execute(action, message, properties)"
        }
    }

//    def aexecute(action, message, properties) {
//        if (log.isTraceEnabled()) {
//            log.trace "Entered def execute(action, message, properties)"
//            log.trace "action = \"${action}\""
//            log.trace "message = \"${message}\""
//            log.trace "properties = \"${properties}\""
//            log.trace "setting script to default of null"
//        }
//
//        def script
//        def scriptName
//
//        log.trace "Checking the type of action to get the script"
//        if (action instanceof Map) {
//            if (log.isTraceEnabled()) {
//                log.trace "action is a map.  Getting file name of the script"
//                log.trace "filename = \"${action.file}\""
//            }
//            def sourceFile = new File(action.file)
//            scriptName = action.file
//
//            if (sourceFile.exists() && sourceFile.isFile() && sourceFile.canRead()) {
//                log.trace "file exists, is a file, and is readable"
//                script = sourceFile.text
//            } else {
//                def errormsg = "Script is not found at \"${action.file}\" or is not a file or is not readable"
//                log.error errormsg
//                ServiceUtil.throwException(message, "ScriptActionHandlerServiceException", errormsg)
//            }
//        } else if(action instanceof String) {
//            log.trace "action is a String"
//            script = action
//            scriptName = "source as string"
//        } else {
//            def errormsg = "action is of the wrong type"
//            log.error errormsg
//            ServiceUtil.throwException(message, "ScriptActionHandlerServiceException", errormsg)
//        }
//
//        if (log.isTraceEnabled()) {
//            log.trace "source = \"${script}\""
//        }
//
//        if (script != null) {
//            log.trace "processing script"
//            def groovySource = new GroovyCodeSource(script, scriptName, scriptName)
//            GroovyClassLoader gcl = new GroovyClassLoader(this.getClass().getClassLoader())
//
//            log.trace "Parsing the source and caching"
//            def scriptClass = gcl.parseClass(groovySource, true)
//            log.trace "Creating a new instance"
//            def classInstance = scriptClass.newInstance()
//
//            def ctx = (ApplicationContext) ServletContextHolder
//                    .getServletContext()
//                    .getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT);
//
//            log.trace "Autowiring bean properties"
//            ctx.beanFactory.autowireBeanProperties(classInstance, ctx.beanFactory.AUTOWIRE_BY_NAME, false)
//
//            log.trace "invoking the execute method on the script"
//            classInstance.invokeMethod("execute", [message, properties])
//        } else {
//            def errormsg = "script is null"
//            log.error errormsg
//            ServiceUtil.throwException(message, "ScriptActionHandlerServiceException", errormsg)
//        }
//
//        if (log.isTraceEnabled()) {
//            log.trace "Finished executing the script."
//            log.trace "message = \"${message}\""
//            log.trace "Leaving def execute(action, message, properties)"
//        }
//    }
}
