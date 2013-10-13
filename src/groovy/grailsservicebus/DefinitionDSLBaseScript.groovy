package grailsservicebus
/**
 *
 * Created by ehenson on 10/12/13.
 */
abstract class DefinitionDSLBaseScript extends Script {
    /* the "this" pointer is having to be returned whenever possible because the return value is what
        is what is being returned from the run() method.

     */
    def definition = [:]

    // Adds an Action to the definition action list
    def action(Closure cl) {
        ActionDelegate actionDelegate = new ActionDelegate()
        cl.delegate = actionDelegate
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
        if (definition["actions"] == null) {
            definition.actions = [actionDelegate.map]
        } else {
            definition.actions << actionDelegate.map
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
        def binding = getBinding()
        def applicationContext = binding.getVariable("applicationContext")
        def aliasName = bean
        binding.setVariable(aliasName, applicationContext."$bean")
        [alias: { String alias ->
            binding.getVariables().remove(aliasName)
            binding.setVariable(alias, applicationContext."$aliasName")
        }]
    }

    // Adds an Action to the definition action list and the closure is a map for the properties of the action
    def action (LinkedHashMap args, Closure cl ) {
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
        return this
    }

    // Defines how to validate a property in the message of the service
    def parameter (LinkedHashMap args) {
        def map = [required:false, type:"any"] << args
        definition.parameters = definition["parameters"] == null ? [map] : definition.parameters << map
        return this
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
