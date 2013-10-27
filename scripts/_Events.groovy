import grails.util.BuildSettingsHolder

//includeTargets << grailsScript("_GrailsInit")
//
//target(events: "The description of the script goes here!") {
//    // TODO: Implement script here
//}
//
//setDefaultTarget(events)


//eventTestPhasesStart = { msg ->
//    println "Do something..."
//}

eventTestPhaseStart = { phase ->
    // delete just in case
    ant.delete(dir:"${projectWorkDir}/tests/actions")
    ant.delete(dir:"${projectWorkDir}/tests/definitions")
    ant.mkdir(dir:"${projectWorkDir}/tests/actions")
    ant.mkdir(dir:"${projectWorkDir}/tests/definitions")

//    switch(phase) {
//        case 'unit':
//            println 'Unit Tests About To Run'
//            break
//        case 'integration':
//            println 'Integration Tests About To Run'
//            break
//        case 'functional':
//            println 'Functional Tests About To Run'
//            break
//        default:
//            println "${phase} Tests About To Run"
//    }
}

eventTestPhaseEnd = { phase ->
    ant.delete(dir:"${projectWorkDir}/tests/actions")
    ant.delete(dir:"${projectWorkDir}/tests/definitions")

//    switch(phase) {
//        case 'unit':
//            println 'Unit Tests About To End'
//            break
//        case 'integration':
//            println 'Integration Tests About To End'
//            break
//        case 'functional':
//            println 'Functional Tests About To End'
//            break
//        default:
//            println "${phase} Tests About To End"
//    }
}