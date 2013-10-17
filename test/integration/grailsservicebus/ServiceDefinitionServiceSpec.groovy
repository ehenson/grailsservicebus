package grailsservicebus

import grailsservicebus.test.ServiceFileHelper
import spock.lang.Specification
/**
 * Testing of the ServiceDefinitionService
 * This majority of the tests overwrites the test.groovy definition file so it forces the GroovyScriptEngine to recompile
 * because of a change to the file so this provides the test of knowing that a definition will have its changes reflected.
 */
class ServiceDefinitionServiceSpec extends Specification {
    ServiceDefinitionService serviceDefinitionService
    def serviceFileHelper
    def grailsApplication

    def setup() {
        serviceFileHelper = new ServiceFileHelper()
        serviceFileHelper.setup(grailsApplication)

        serviceDefinitionService.urls = serviceFileHelper.definitionURLs
        serviceDefinitionService.init()
    }

    def cleanup() {
        serviceFileHelper.cleanup()
    }

    void "test parameter"() {
        def result =
                [
                        parameters:
                                [
                                        [required:true, type:"string", name:"firstName", default:"Eric"],
                                        [required:false, type:"string", name:"lastName", default:"Henson"]
                                ]
                ]

        def script = """
parameter name: "firstName", required: true,  default: "Eric",   type: "string"
parameter name: "lastName",  required: false, default: "Henson", type: "string"
"""
        serviceFileHelper.writeDefinition script

        when:
        def definition = serviceDefinitionService.getDefinition(serviceFileHelper.definitionName, [:])

        then:
        definition == result
    }

    void "test good definition"() {
        given:
        def result =
                [
                    parameters:
                    [
                        [required:true, type:"string", name:"firstName", default:"Eric"],
                        [required:false, type:"string", name:"lastName", default:"Henson"]
                    ],
                    actions:
                    [
                        [handler:"script", file:"/opt/grailsservicebus/actions/test.groovy", properties: [name:"my name", hello:"world"] ],
                        [handler:"script", file:"file2", properties:[name:[[a:"b"], 2, 3, "file2"]]],
                        [handler:"script", file:"file3", properties:[name:[[a:"b"], 2, 3, "file3"]]],
                        [handler:"script", file:"file4", properties:[name:[[a:"b"], 2, 3, "file4"]]],
                        [handler:"script", file:"file5", properties:[name:[[a:"b"], 2, 3, "file5"]]],
                        [handler:"script", file:"file6", properties:[name:[[a:"b"], 2, 3, "file6"]]]
                    ]
                ]

        def script = """
// action handler defaults to script
parameter name: "firstName", required: true,  default: "Eric",   type: "string"
parameter name: "lastName",  required: false, default: "Henson", type: "string"

action {
    handler "script"
    file "/opt/grailsservicebus/actions/test.groovy"
    properties {
        name = "my name"
        hello = "world"
    }
}

action {
    file "file2"
    properties {
        name = [[a:"b"], 2, 3, "file2"]
    }
}

action handler:"script", file:"file3", {
    name = [[a:"b"], 2, 3, "file3"]
}

action file:"file4", {
    name = [[a:"b"], 2, 3, "file4"]
}

action (handler:"script", file:"file5") {
    name = [[a:"b"], 2, 3, "file5"]
}

action (file:"file6") {
    name = [[a:"b"], 2, 3, "file6"]
}"""
        serviceFileHelper.writeDefinition script

        when:
        def definition = serviceDefinitionService.getDefinition(serviceFileHelper.definitionName, [:])

        then:
        definition == result
    }

    void "test good definition with grailsApplication injection"() {
        given:
        def result = [actions:[[handler:"script", file:"unit test file", properties:[sample:"test"]]]]

        def script = """
action {
    file "unit test file"
    properties {
        sample = grailsApplication.config.unittest.sample
    }
}
"""
        serviceFileHelper.writeDefinition script

        when:
        def definition = serviceDefinitionService.getDefinition(serviceFileHelper.definitionName, [:])

        then:
        definition == result
    }

    void "test good definition with grailsApplication injection using 'as' property"() {
        given:
        def result = [actions:[[handler:"script", file:"unit test file", properties:[sample:"test"]]]]

        def script = """
using "grailsApplication" alias "grails"

action {
    file "unit test file"
    properties {
        sample = grails.config.unittest.sample
    }
}
"""
        serviceFileHelper.writeDefinition script

        when:
        def definition = serviceDefinitionService.getDefinition(serviceFileHelper.definitionName, [:])

        then:
        definition == result
    }

    void "test injecting unitTestService"() {
        given:
        def result = [actions:[[handler:"script", file:"unit test file", properties:[sample:"test"]]]]

        def script = """
using "unitTestService"

action {
    file "unit test file"
    properties {
        sample = unitTestService.echo "test"
    }
}
"""
        serviceFileHelper.writeDefinition script

        when:
        def definition = serviceDefinitionService.getDefinition(serviceFileHelper.definitionName, [:])

        then:
        definition == result
    }

    void "test injecting unitTestService using 'as' keyword"() {
        given:
        def result = [actions:[[handler:"script", file:"unit test file", properties:[sample:"test"]]]]

        def script = """
using "unitTestService" alias "unitTest"

action {
    file "unit test file"
    properties {
        sample = unitTest.echo "test"
    }
}
"""
        serviceFileHelper.writeDefinition script

        when:
        def definition = serviceDefinitionService.getDefinition(serviceFileHelper.definitionName, [:])

        then:
        definition == result
    }

    void "only one action"() {
        given:
        def result = [actions:[[handler:"script", file:"unittest"]]]
        def script = 'action file:"unittest"'
        serviceFileHelper.writeDefinition script

        when:
        def definition = serviceDefinitionService.getDefinition(serviceFileHelper.definitionName, [:])

        then:
        definition == result
    }

    void "are the grailsApplication and the applicationContext injected"() {
        given:
        def result = [actions:[[handler:"script", file:"unit test file", properties:[grailsApplicationInjected:true, applicationContextInjected:true]]]]

        def script = """
action {
    file "unit test file"
    properties {
        grailsApplicationInjected = grailsApplication != null
        applicationContextInjected = applicationContext != null
    }
}
"""
        serviceFileHelper.writeDefinition script

        when:
        def definition = serviceDefinitionService.getDefinition(serviceFileHelper.definitionName, [:])

        then:
        definition == result
    }

    void "test default"() {
        when:
        def definition = serviceDefinitionService.getDefinition(serviceFileHelper.definitionName, [:])

        then:
        serviceFileHelper.resultDefinition == definition
    }

    void "test service definition file not found"() {
        given:
        def message = [:]

        when:
        def definition = serviceDefinitionService.getDefinition("unittest.definition.missing", message)

        then:
        definition == null
        message == [exception:[[actionType:"groovy", actionName:"unknown", exceptionType:"ServiceDefinitionFileNotFoundException", exceptionMessage:"Service Definition not found"]]]
    }

    /**
     * to catch any other error other than a file not found just catch an Exception and report a generic error for parsing
     * definition
     */
    void "syntax error"() {
        given:
        def message = [:]
        def script = """
I am an obvious syntax error
using "grailsApplication" alias "grails"

action {
    file "unit test file"
    properties {
        sample = grails.config.unittest.sample
    }
}
"""
        serviceFileHelper.writeDefinition script

        when:
        def definition = serviceDefinitionService.getDefinition(serviceFileHelper.definitionName, message)

        then:
        definition == null
        message == [exception:[[actionType:"groovy", actionName:"unknown", exceptionType:"ServiceDefinitionException", exceptionMessage:"Service Definition Error: No such property: am for class: test"]]]
    }

}
