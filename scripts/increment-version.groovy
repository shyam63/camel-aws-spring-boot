/**
 * Script to increment the "version" number based on the kind of build we are doing. This
 * is mean to be run from Jenkins to generate subsequent build versions.
 */

// CONFIGURATION: You can change this
def config = [
        // list of version folders
        artifactoryUrl: 'http://ec2-54-173-45-74.compute-1.amazonaws.com:8081/artifactory/ext-release-local/com/shyam/camel/camel-aws-spring-boot/',

        buildType: System.getenv("BUILDTYPE") && !System.getenv("BUILDTYPE").isAllWhitespace()  ?: 'SNAPSHOT', // MAJOR, MINOR, PATCH, SNAPSHOT
        workspace: System.getenv("WORKSPACE") ?: "."
]
println(config['buildType'])
/**
 * Get current version. Current version is considered to be the
 */


Map latestVersion

try {
    // download list of folders from artifactory url
    String data = new URL(config['artifactoryUrl']).getText()
    // strip off the first line, because the XML parser doesn't like it
    data = data.substring(data.indexOf('\n'))
    latestVersion = new XmlParser().parseText(data).body.pre[1].a.'*' // all <a> tags for list of folders
            .collect { it.toString() }
            .findAll { it[0].isNumber() }
            .collect { it.replaceAll('/', '').trim() }
            .collect {
        // convert <Major>.<Minor>.<Patch>[-SNAPSHOT] into a pretty map
        Map version = ['raw': it, 'snapshot': false]
        println(version)
        if (it.indexOf('-SNAPSHOT') != -1) {
            version.snapshot = true
            it = it.substring(0, it.indexOf('-SNAPSHOT'))
        }

        List parts = it.split("\\.")
        version['major'] = parts[0] ? new Integer(parts[0]) : (Integer) 0
        version['minor'] = parts[1] ? new Integer(parts[1]) : (Integer) 0
        version['patch'] = parts[2] ? new Integer(parts[2]) : (Integer) 0

        return version
    }.sort {
        a, b ->
            if (a['major'] > b['major']) { // check for bigger major version
                return 1
            } else if (a['major'] < b['major']) {
                return -1
            } else if (a['minor'] > b['minor']) {  // majors match, check for bigger minor version
                return 1
            } else if (a['minor'] < b['minor']) {
                return -1
            } else if (a['patch'] > b['patch']) { // minors match, check for bigger patch version
                return 1
            } else if (a['patch'] < b['patch']) {
                return -1
            } else if (a['snapshot']) { // patch matches, check for one being a snapshot
                return 1 // A is a snapshot, so its bigger than B
            }
            return -1 // B must be a snapshot and a is not a snapshot, so be is bigger
    }.last()
} catch (Exception e) {
    e.printStackTrace()
    println "Failed to get latest version. Defaulting to 0.0.0"
    latestVersion = [
            'snapshot': false,
            'major': 0,
            'minor': 0,
            'patch': 1
    ]
}

println "Current Version: ${latestVersion}"

/**
 * Increment version based on the type of release we are dealing with
 */

Map newVersion = [
        'snapshot': new Boolean(latestVersion['snapshot']),
        'major': new Integer(latestVersion['major']),
        'minor': new Integer(latestVersion['minor']),
        'patch': new Integer(latestVersion['patch'])
]

switch(config['buildType']) {
    case 'MAJOR':
        newVersion['major']++
        newVersion['minor'] = 0
        newVersion['patch'] = 0
        newVersion['snapshot'] = false
        break

    case 'MINOR':
        newVersion['minor']++
        newVersion['patch'] = 0
        newVersion['snapshot'] = false
        break

    case 'PATCH':
        newVersion['patch']++
        newVersion['snapshot'] = false
        break

    case 'SNAPSHOT':
        newVersion['snapshot'] = true
}

newVersion['raw'] = "${newVersion['major']}.${newVersion['minor']}.${newVersion['patch']}"
if (newVersion['snapshot']) {
    newVersion['raw'] = newVersion['raw'] + '-SNAPSHOT'
}

println "Next Version: ${newVersion['raw']}"

/**
 * Update pom.xml version number with the new version number
 */

// read pom.xml file
String pomFilename = config['workspace'] + "/pom.xml"
println "Opening POM file ${pomFilename}"

File pomFile = new File(pomFilename)
String contents = pomFile.text

// replace "<version>1.0</version>" with the new version
contents = contents.replaceAll("<version>.+</version>", "<version>${newVersion['raw']}</version>")
pomFile.write(contents)