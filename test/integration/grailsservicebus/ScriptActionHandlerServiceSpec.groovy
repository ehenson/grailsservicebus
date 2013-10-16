package grailsservicebus

import grailsservicebus.test.ServiceFileHelper
import spock.lang.Specification

class ScriptActionHandlerServiceSpec extends Specification {
    ScriptActionHandlerService scriptActionHandlerService
    ServiceFileHelper serviceFileHelper

    def setup() {
        serviceFileHelper = new ServiceFileHelper()
        serviceFileHelper.setup()
        scriptActionHandlerService.urls = serviceFileHelper.actionURLs
        scriptActionHandlerService.init()
    }

    def cleanup() {
        serviceFileHelper.cleanup()
    }

    void "test good service"() {
        given:
        def source = """
package grailsservicebus.script.unittest

class UnitTest {
    def execute(message, properties) {
        message.hello = "world"
    }
}
"""
        serviceFileHelper.writeAction("unittest", source)
        def action = [handler:"script", file:"unittest"]
        def message = [service:[name:"unittest"]]
        def properties = [:]

        when:
        scriptActionHandlerService.execute(action, message, properties)

        then:
        message == [service:[name:"unittest"], hello:"world"]
    }

    void "test a manually thrown NPE from the service"() {
        def source = """
package grailsservicebus.script.unittest

class UnitTest {
    def execute(message, properties) {
        message.hello = "world"
        throw new NullPointerException()
    }
}
"""
        serviceFileHelper.writeAction("unittest", source)
        def action = [handler:"script", file:"unittest"]
        def message = [service:[name:"unittest"]]
        def properties = [:]

        when:
        scriptActionHandlerService.execute(action, message, properties)

        then:
        message == [service:[name:"unittest"], hello:"world", exception:[[actionType:"groovy", actionName:"unknown", exceptionType:"ScriptActionUncaughtException", exceptionMessage:"Script Action Error: java.lang.NullPointerException"]]]
    }
}
