package grailsservicebus.test

import grails.util.BuildSettingsHolder

/**
 * Created by ehenson on 10/15/13.
 */
class ServiceFileHelper {
    def definitionsPath
    def actionsPath
    String[] definitionURLs
    String[] actionURLs
    def testDefinition
    def resultDefinition
    def definitionName
    def definitionFilename

    ServiceFileHelper() {

    }

    def setup(grailsApplication) {
        new File("${BuildSettingsHolder.getSettings().projectWorkDir}/tests").deleteDir()  // just in case
        // create the folder structure for test definitions
//        [new File("${BuildSettingsHolder.getSettings().projectWorkDir}/tests"), new File("${BuildSettingsHolder.getSettings().projectWorkDir}/tests/definitions"), new File("${BuildSettingsHolder.getSettings().projectWorkDir}/tests/actions")].each{it.mkdirs()}
//        definitionsPath = "${BuildSettingsHolder.getSettings().projectWorkDir}/tests/definitions"
//        actionsPath = "${BuildSettingsHolder.getSettings().projectWorkDir}/tests/actions"
        definitionsPath = grailsApplication.config?.grailsservicebus?.definitions?.locations ?: ["${BuildSettingsHolder.getSettings().projectWorkDir}/tests/definitions"]
        actionsPath = grailsApplication.config?.grailsservicebus?.actions?.locations ?: ["${BuildSettingsHolder.getSettings().projectWorkDir}/tests/actions"]
        definitionsPath = definitionsPath[0]
        actionsPath = actionsPath[0]
        new File(definitionsPath).mkdirs()
        new File(actionsPath).mkdirs()
        definitionURLs = [definitionsPath]
        actionURLs = [actionsPath]
        definitionName = "test"
        definitionFilename = "${definitionsPath}/${definitionName}.groovy"
        testDefinition = """using "unitTestService" alias "echo"

parameter name: "firstName", required: true,  default: "Eric",   type: "string"
parameter name: "lastName",  required: false, default: "Henson", type: "string"

// action handler defaults to script
action {
    handler "script"
    file "/opt/grailsservicebus/actions/test.groovy"
    properties {
        name = "my name"
        hello = "world"
        sample = grailsApplication.config.unittest.sample
        echo = echo.echo "echo1 hello"
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
}
"""

        new File(definitionFilename).write(testDefinition)

        resultDefinition =
                [
                        parameters:
                                [
                                        [required:true, type:"string", name:"firstName", default:"Eric"],
                                        [required:false, type:"string", name:"lastName", default:"Henson"]
                                ],
                        actions:
                                [
                                        [handler:"script", file:"/opt/grailsservicebus/actions/test.groovy", properties: [name:"my name", hello:"world", echo:"echo1 hello", sample:"test"] ],
                                        [handler:"script", file:"file2", properties:[name:[[a:"b"], 2, 3, "file2"]]],
                                        [handler:"script", file:"file3", properties:[name:[[a:"b"], 2, 3, "file3"]]],
                                        [handler:"script", file:"file4", properties:[name:[[a:"b"], 2, 3, "file4"]]],
                                        [handler:"script", file:"file5", properties:[name:[[a:"b"], 2, 3, "file5"]]],
                                        [handler:"script", file:"file6", properties:[name:[[a:"b"], 2, 3, "file6"]]]
                                ]
                ]

    }

    def cleanup() {
        new File("${BuildSettingsHolder.getSettings().projectWorkDir}/tests").deleteDir()
    }

    def writeDefinition(script) {
        new File(definitionFilename).write(script)
    }

    def writeDefinition(filename, script) {
        new File("${definitionsPath}/${filename}.groovy").write(script)
    }

    def writeAction(filename, script) {
        new File("${actionsPath}/${filename}.groovy").write(script)
    }

}