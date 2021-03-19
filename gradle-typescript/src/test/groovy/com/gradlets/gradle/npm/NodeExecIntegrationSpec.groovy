/*
 * (c) Copyright 2021 Felipe Orozco, Robert Kruszewski. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gradlets.gradle.npm

import com.gradlets.gradle.Versions
import nebula.test.IntegrationSpec

class NodeExecIntegrationSpec extends IntegrationSpec {
    private static final List<String> GRADLE_TEST_VERSIONS = ['6.8', '6.7.1']

    File executable
    def setup() {
        executable = file('executable.js')
        executable.setExecutable(true, true)
        executable << "#!/usr/bin/env node\n"

        buildFile << """
        allprojects {
            apply plugin: 'com.gradlets.typescript-base'
            repositories {
                npm {  url 'https://registry.npmjs.org' }
            }

            // Hack to create properly configured configurations
            typeScriptSourceSets {
                main
            }
            
            task doExec() {
                doFirst {
                    com.gradlets.gradle.npm.NodeExec.exec(project, configurations.deps, { 
                        executable '${executable.absolutePath}'
                    })
                }
            }
        }
        """.stripIndent()
    }

    def 'executes without dependencies'() {
        expect:
        runTasksSuccessfully('doExec')
    }

    def 'fails to resolve unknown dependencies'() {
        when:
        executable << "console.log('DID START')\n"
        executable << "require('conjure-client')\n"
        executable << "console.log('UNREACHABLE')\n"

        then:
        def failure = runTasksWithFailure('doExec')
        failure.standardError.contains "DID START"
        failure.standardError.contains "Cannot find module 'conjure-client'"
        !failure.standardError.contains("UNREACHABLE")
    }

    def '#gradleTestVersion executes resolves dependencies'() {
        setup:
        gradleVersion = gradleTestVersion

        when:
        buildFile << """
        dependencies {
            deps 'npm:conjure-client:${Versions.CONJURE_CLIENT}'
        }
        """.stripIndent()
        executable << "require('conjure-client')\n"
        executable << "console.log('DID EXECUTE')"

        then:
        runTasksSuccessfully('doExec')

        where:
        gradleTestVersion << GRADLE_TEST_VERSIONS
    }

    def '#gradleTestVersion executes resolves scoped dependencies'() {
        setup:
        gradleVersion = gradleTestVersion

        when:
        buildFile << """
        dependencies {
            deps 'npm:blueprintjs/tslint-config:3.0.4'
        }
        """.stripIndent()
        executable << "require('@blueprintjs/tslint-config')\n"

        then:
        runTasksSuccessfully('doExec')

        where:
        gradleTestVersion << GRADLE_TEST_VERSIONS
    }
}
