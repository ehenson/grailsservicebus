package grailsservicebus

import org.apache.commons.logging.LogFactory
/**
 *
 * Created by ehenson on 10/12/13.
 */
abstract class DefinitionDSLBaseScript extends Script {
    private static final log = LogFactory.getLog(this)

    /* the "this" pointer is having to be returned whenever possible because the return value is what
        is what is being returned from the run() method.

     */
    def definition = [:]

    // Adds an Action to the definition action list
    def action(Closure cl) {
        log.trace "Entered action(Closure c)"
        ActionDelegate actionDelegate = new ActionDelegate()
        cl.delegate = actionDelegate
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        log.trace "calling closure"
        cl()
        log.trace "return from closure"
        if (definition["actions"] == null) {
            log.trace "creating actions map"
            definition.actions = [actionDelegate.map]
        } else {
            log.trace "appending to actions map"
            definition.actions << actionDelegate.map
        }
        if (log.isTraceEnabled()) {
            log.trace "assigned actionDelegate.map = \"${actionDelegate.map}\""
            log.trace "Leaving action(Closure c)"
        }
        return this
    }

    /**
     * usage:  using <bean name as string> alias <binding variable name as string>
     *     using "unitTestService" alias "echo"
     *
     * @param bean
     * @return
     */
    def using(String bean) {
        if (log.isTraceEnabled()) {
            log.trace "Entered using(String bean)"
            log.trace "bean = \"${bean}\""
        }
        def binding = getBinding()
        def applicationContext = binding.getVariable("applicationContext")
        def aliasName = bean
        if (log.isTraceEnabled()) {
            log.trace "binding applicationContext.\"$bean\" to \"$aliasName\""
        }
        binding.setVariable(aliasName, applicationContext."$bean")
        def aliasMap = [alias: { String alias ->
            if (log.isTraceEnabled()) {
                log.trace "Entered using.alias"
                log.trace "alias = \"${alias}\""
                log.trace "remove binding of \"$aliasName\""
            }
            binding.getVariables().remove(aliasName)
            if (log.isTraceEnabled()) {
                log.trace "binding applicationContext.\"$aliasName\" to \"$alias\""
            }
            binding.setVariable(alias, applicationContext."$aliasName")
            log.trace "Leaving using.alias"
        }]
        log.trace "Leaving using(String bean)"
        return aliasMap
    }

    // Adds an Action to the definition action list and the closure is a map for the properties of the action
    def action(LinkedHashMap args, Closure cl) {
        if (log.isTraceEnabled()) {
            log.trace "Entered action(LinkedHashMap args, Closure cl)"
            log.trace "args = \"${args}\""
        }
        def action = [handler:"script"] << args
        ActionPropertiesDelegate actionPropertiesDelegate = new ActionPropertiesDelegate()
        cl.delegate = actionPropertiesDelegate
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        log.trace "calling closure"
        cl()
        log.trace "return from closure"
        action.properties = actionPropertiesDelegate.map
        if (definition["actions"] == null) {
            definition.actions = [action]
            log.trace "creating actions map"
        } else {
            log.trace "appending to actions map"
            definition.actions << action
        }
        if (log.isTraceEnabled()) {
            log.trace "assigned action = \"${action}\""
            log.trace "Leaving action(LinkedHashMap args, Closure cl)"
        }
        return this
    }

    // Defines how to validate a property in the message of the service
    def parameter(LinkedHashMap args) {
        if (log.isTraceEnabled()) {
            log.trace "Entered parameter(LinkedHashMap args)"
            log.trace "args = \"${args}\""
        }
        def map = [required:false, type:"any"] << args
        if (definition["parameters"] == null) {
            log.trace "creating parameters map"
            definition.parameters = [map]
        } else {
            log.trace "adding to parameters map"
            definition.parameters << map
        }
        if (log.isTraceEnabled()) {
            log.trace "assigned using map = \"$map\""
            log.trace "Leaving parameter(LinkedHashMap args)"
        }
        return this
    }
}

// Action Delegate to process an closure based Action definition
class ActionDelegate {
    private static final log = LogFactory.getLog(this)
    def map = [handler:"script"]

    void handler(String handler) {
        if (log.isTraceEnabled()) {
            log.trace "Entered handler(String handler)"
            log.trace "handler = \"${handler}\""
        }
        this.map.handler = handler
        log.trace "Leaving handler(String handler)"
    }

    void file(String file) {
        if (log.isTraceEnabled()) {
            log.trace "Entered file(String file)"
            log.trace "file = \"${file}\""
        }
        this.map.file = file
        log.trace "Leaving file(String file)"
    }

    def properties(Closure cl) {
        log.trace "Entered properties(Closure cl)"
        ActionPropertiesDelegate actionPropertiesDelegate = new ActionPropertiesDelegate()
        cl.delegate = actionPropertiesDelegate
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        log.trace "calling closure"
        cl()
        log.trace "returned from closure"
        map.properties = actionPropertiesDelegate.map
        if (log.isTraceEnabled()) {
            log.trace "assigned to map.properties = \"${actionPropertiesDelegate.map}\""
            log.trace "Leaving properties(Closure cl)"
        }
    }
}

// Action's Properties Delegate to help process the properties defined as a closure
class ActionPropertiesDelegate {
    private static final log = LogFactory.getLog(this)
    def map = [:]
    void setProperty(String property, def value) {
        if (log.isTraceEnabled()) {
            log.trace "Entered setProperty(String property, def value)"
            log.trace "property = \"${property}\""
            log.trace "value = \"${value}\""
        }
        map[property] = value
        log.trace "Leaving setProperty(String property, def value)"
    }
}
