package grailsservicebus
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.control.CompilerConfiguration

import javax.annotation.PostConstruct
// GroovyClassLoader can set common base class.  http://groovy.329449.n5.nabble.com/setScriptBaseClass-does-not-work-for-GroovyScriptEngine-td5710646.html
// Use GroovyScriptEngine for caching.  Page 283 Groovy for DSL
// several sources report that GroovyClassLoader is thread safe.

class ServiceDefinitionService {
    private static final log = LogFactory.getLog(this)
    def static transactional =  false
    def grailsApplication
    GroovyScriptEngine gse
    String[] urls = ["/opt/grailsservicebus/definitions"]

    @PostConstruct
    /**
     * This init is creating the GroovyScriptEngine once because GCE is for servers and will cache and recompile the script if needed
     * http://www.intelligrape.com/blog/2012/08/27/using-postconstruct-annotation-with-grails-services/
     */
    public void init() {
        log.trace "Entered init()"
        CompilerConfiguration configuration = new CompilerConfiguration()
        log.trace "Setting the Script Base Class to \"${DefinitionDSLBaseScript.name}\""
        configuration.setScriptBaseClass(DefinitionDSLBaseScript.name)
        GroovyClassLoader gcl = new GroovyClassLoader(this.getClass().getClassLoader(), configuration)
        log.trace "Instantiating GroovyScriptEngine with url = \"${urls}"
        gse = new GroovyScriptEngine(urls, gcl)
        log.trace "Leaving init()"
    }

    /**
     * Executes the Definition DSL based on the serviceName returning a map of the definition
     * @param serviceName
     * @return map of the definition
     */
    def getDefinition(String serviceName) {
        if (log.isTraceEnabled()) {
            log.trace "Entered getDefinition(String serviceName)"
            log.trace "serviceName = \"${serviceName}\""
            log.trace "binding grailsApplication and applicationContext"
        }
        Binding binding = new Binding()
        binding.setVariable("grailsApplication", grailsApplication)
        binding.setVariable("applicationContext", grailsApplication.mainContext)
        log.trace "Parsing DSL: \"${serviceName}.groovy\""
        def dsl = gse.run("${serviceName}.groovy", binding)
        if (log.isTraceEnabled()) {
            log.trace "Finished parsing DSL: \"${serviceName}.groovy\""
            log.trace "definition = \"${dsl.definition}\""
            log.trace "Leaving getDefinition(String serviceName)"
        }
        return dsl.definition
    }

//    @PreDestroy
//    public void cleanUp() throws Exception {
//    }
}