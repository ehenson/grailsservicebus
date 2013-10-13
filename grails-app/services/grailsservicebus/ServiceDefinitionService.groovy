package grailsservicebus
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.control.CompilerConfiguration

// GroovyClassLoader can set common base class.  http://groovy.329449.n5.nabble.com/setScriptBaseClass-does-not-work-for-GroovyScriptEngine-td5710646.html
// Use GroovyScriptEngine for caching.  Page 283 Groovy for DSL

class ServiceDefinitionService {
    private static final log = LogFactory.getLog(this)
    def static transactional =  false
    def grailsApplication
    GroovyScriptEngine gse

    /**
     * This Constructor is creating the GroovyScriptEngine once because GCE is for servers and will cache and recompile the script if needed
     */
    ServiceDefinitionService() {
        CompilerConfiguration configuration = new CompilerConfiguration()
        configuration.setScriptBaseClass(DefinitionDSLBaseScript.name)
        GroovyClassLoader gcl = new GroovyClassLoader(this.getClass().getClassLoader(), configuration)
        String[] roots = ["/opt/grailsservicebus/definitions"]
        gse = new GroovyScriptEngine(roots, gcl)
    }

    def getScriptClass(String serviceName) {
        Binding binding = new Binding()
        binding.setVariable("grailsApplication", grailsApplication)
        binding.setVariable("applicationContext", grailsApplication.mainContext)
        def dsl = gse.run("${serviceName}.groovy", binding)
        return dsl.definition
    }
}