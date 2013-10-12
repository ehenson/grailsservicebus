package grailsservicebus
import org.apache.commons.logging.LogFactory
//import grails.transaction.Transactional
//
//@Transactional
class ServiceDefinitionService {
    private static final log = LogFactory.getLog(this)

    def static transactional =  false

    def grailsApplication

    def parseDefinition(String script, String scriptName) {
        // the Definition object of the parsed DSL
        def definition = [:]

        // preparing the script compiler
        GroovyCodeSource groovySource = new GroovyCodeSource(script, scriptName, scriptName)
        GroovyClassLoader gcl = new GroovyClassLoader(this.getClass().getClassLoader())

        // parse anc cache the script and return its Class
        def scriptClass = gcl.parseClass(groovySource, true)

        // Inject the two main Grails objects
        scriptClass.metaClass.grailsApplication = grailsApplication
        scriptClass.metaClass.applicationContext = grailsApplication.mainContext

        // bean injection "using" method: 'using "bean name"' will inject a bean into the script and the name of the variable is the same as the bean name
        scriptClass.metaClass.using { String bean ->
            scriptClass.metaClass[bean] = grailsApplication.mainContext."$bean"
        }

        // bean injection "using" with "as" method: 'using "bean name", as:"alias"' will inject a bean into the script and the name of the variable is the alias value
        scriptClass.metaClass.using { LinkedHashMap map, String bean ->
            scriptClass.metaClass[map.as] = grailsApplication.mainContext."$bean"
        }

        // Adds an Action to the definition action list
        scriptClass.metaClass.action { Closure cl ->
            ActionDelegate actionDelegate = new ActionDelegate()
            cl.delegate = actionDelegate
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
            if (definition["actions"] == null) {
                definition.actions = [actionDelegate.map]
            } else {
                definition.actions << actionDelegate.map
            }
        }

        // Adds an Action to the definition action list and the closure is a map for the properties of the action
        scriptClass.metaClass.action { LinkedHashMap args, Closure cl ->
            def action = [handler:"script"] << args
            ActionPropertiesDelegate actionPropertiesDelegate = new ActionPropertiesDelegate()
            cl.delegate = actionPropertiesDelegate
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
            action.properties = actionPropertiesDelegate.map
            if (definition["actions"] == null) {
                definition.actions = [action]
            } else {
                definition.actions << action
            }
        }

        // Defines how to validate a property in the message of the service
        scriptClass.metaClass.parameter { LinkedHashMap args ->
            def map = [required:false, type:"any"] << args
            definition.parameters = definition["parameters"] == null ? [map] : definition.parameters << map
        }

        log.trace "Creating a new instance"
        def classInstance = scriptClass.newInstance()

        // parsing the DSL
        classInstance.run()

        // returning the DSL
        return definition
    }
}


// Action Delegate to process an closure based Action definition
class ActionDelegate {
    def map = [handler:"script"]

    void handler(String handler) {
        this.map.handler = handler
    }

    void file(String file) {
        this.map.file = file
    }

    def properties = { Closure cl ->
        ActionPropertiesDelegate actionPropertiesDelegate = new ActionPropertiesDelegate()
        cl.delegate = actionPropertiesDelegate
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
        map.properties = actionPropertiesDelegate.map
    }
}

// Action's Properties Delegate to help process the properties defined as a closure
class ActionPropertiesDelegate {
    def map = [:]
    void setProperty(String property, def value) {
        map[property] = value
    }
}