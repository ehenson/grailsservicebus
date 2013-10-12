package grailsservicebus

import spock.lang.Specification
/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
//@TestFor(UnitTestService)
class UnitTestServiceSpec extends Specification {

    def unitTestService

    def setup() {
    }

    def cleanup() {
    }

    void "test echo"() {
        expect:
        unitTestService.echo "test" == "test"
    }
}
