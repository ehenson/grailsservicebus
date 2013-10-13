package grailsservicebus

import spock.lang.Specification
/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
//@TestFor(ServiceDefinitionService)
class ServiceDefinitionServiceSpec extends Specification {
    ServiceDefinitionService serviceDefinitionService

    def setup() {
    }

    def cleanup() {
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
        when:
        def definition = serviceDefinitionService.parseDefinition(script, "unittest")

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
        when:
        def definition = serviceDefinitionService.parseDefinition(script, "unittest")

        then:
        definition == result
    }

    void "test good definition with grailsApplication injection using 'as' property"() {
        given:
        def result = [actions:[[handler:"script", file:"unit test file", properties:[sample:"test"]]]]

        def script = """
using "grailsApplication", as: "grails"

action {
    file "unit test file"
    properties {
        sample = grails.config.unittest.sample
    }
}
"""
        when:
        def definition = serviceDefinitionService.parseDefinition(script, "unittest")

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
        when:
        def definition = serviceDefinitionService.parseDefinition(script, "unittest")

        then:
        definition == result
    }

    void "test injecting unitTestService using 'as' keyword"() {
        given:
        def result = [actions:[[handler:"script", file:"unit test file", properties:[sample:"test"]]]]

        def script = """
using "unitTestService", as:"unitTest"

action {
    file "unit test file"
    properties {
        sample = unitTest.echo "test"
    }
}
"""
        when:
        def definition = serviceDefinitionService.parseDefinition(script, "unittest")

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
        when:
        def definition = serviceDefinitionService.parseDefinition(script, "unittest")

        then:
        definition == result
    }

    void "testNew"() {
        given:
        def service = "testservice"

        when:
        def definition = serviceDefinitionService.getScriptClass(service)
        println "definition 1 = \"${definition}\""
        definition = serviceDefinitionService.getScriptClass(service)
        println "definition 2 = \"${definition}\""

        then:
        true
    }
}
