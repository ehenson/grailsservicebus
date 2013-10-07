package grailsservicebus

import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.context.ApplicationContext
//import grails.transaction.Transactional
//
//@Transactional
class ScriptExecutionService {

    def getScriptClass(script) {
        def sourceFile = new File(script)

        if (sourceFile.exists()) {
            log.info "Loading script from ${script}"
            def groovySource = new GroovyCodeSource(sourceFile)
            GroovyClassLoader gcl = new
            GroovyClassLoader(this.getClass().getClassLoader())

            def scriptClass = gcl.parseClass(groovySource)
            def classInstance = scriptClass.newInstance()

            def ctx = (ApplicationContext) ServletContextHolder
                    .getServletContext()
                    .getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT);


            ctx.beanFactory.autowireBeanProperties(classInstance,
                    ctx.beanFactory.AUTOWIRE_BY_NAME, false)

            try {
                classInstance.log = log
            } catch (ex) { }
            return classInstance
        } else {
            log.info "Script not found at ${script}"
            return null
        }
    }

    def executeScript(script, message, properties) {
//        def msg = ""
//        def success = false
//        def details

        def scriptClass = getScriptClass(script)

//        if (scriptClass != null) {
//            try {
                // as a closure
                //details = scriptClass[method].call(someVariable, [name:"Hello"])
                // as a method
                scriptClass.invokeMethod("execute", [message, properties])
//                success = true
//            } catch (Exception ex) {
                //success = false
                //details = ex.getMessage()
//                log.warn "Failed to execute '${script}/${method}'", ex
//            }

//            if (success) {
//                msg = "Script '${script}/${method}' completed with '${details}'"
//            } else {
//                msg = "Script '${script}/${method}' failed"
//            }
//        } else {
//            msg = "Cannot execute. Script '${script}' not found."
//        }
//
//        return msg
    }
}