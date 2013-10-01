/**
 * Created by ehenson on 9/30/13.
 */
package grailsservicebus

import spock.lang.Specification

/**
 *
 */
class ServiceUtilSpec extends Specification {
    def message

    def setup() {
        message = [service: [name: "unittest"], testKey: "testValue"]
    }

    void "test has exception without the exception key"() {
        expect:
        ServiceUtil.hasException(message) is false
    }

    void "test has exception with the exception key"() {
        given:
        message.exception = []

        expect:
        ServiceUtil.hasException(message) is true
    }

    void "test thowException as normal"() {
        when:
        ServiceUtil.throwException(message, "unitTestType", "unit test message")

        then:
        message == [service: [name: "unittest"], testKey: "testValue",
                exception: [actionType: "groovy", actionName: "unknown",
                        exceptionType: "unitTestType", exceptionMessage: "unit test message"]]
    }

    void "test thowException with action specified"() {
        when:
        ServiceUtil.throwException(message, "unitTestType", "unit test message", "action type", "action name")

        then:
        message == [service: [name: "unittest"], testKey: "testValue",
                exception: [actionType: "action type", actionName: "action name",
                        exceptionType: "unitTestType", exceptionMessage: "unit test message"]]
    }
}
