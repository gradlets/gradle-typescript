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
import spock.lang.Unroll

class NpmBasePluginIntegrationSpec extends IntegrationSpec {
    private static final List<String> GRADLE_TEST_VERSIONS = ['6.8', '6.7.1']

    def setup() {
        buildFile << """
        allprojects {
            apply plugin: 'com.gradlets.typescript-base'
            repositories {
                npm { url 'https://registry.npmjs.org' }
            }

            // Hack to create properly configured configurations
            typeScriptSourceSets {
                main
            }

            task printFiles() {
                doFirst {
                    configurations.deps.files.forEach {
                        println 'test-output-file: ' + it
                    }
                }
            }
        }
        """.stripIndent()
    }

    @Unroll
    def '#gradleTestVersion loads simple packages'() {
        setup:
        gradleVersion = gradleTestVersion

        when:
        buildFile << """
        dependencies {
            deps 'npm:conjure-client:${Versions.CONJURE_CLIENT}'
        }
        """.stripIndent()

        then:
        def result = runTasksSuccessfully('printFiles')

        matchingFile("conjure-client-${Versions.CONJURE_CLIENT}/conjure-client").any {
            result.standardOutput.find(it)
        }
        matchingFile("web-streams-polyfill-2.0.6/web-streams-polyfill").any {
            result.standardOutput.find(it)
        }

        where:
        gradleTestVersion << GRADLE_TEST_VERSIONS
    }

    @Unroll
    def '#gradleTestVersion loads scoped packages'() {
        setup:
        gradleVersion = gradleTestVersion

        when:
        buildFile << """
        dependencies {
            deps 'npm:gradlets/test-service-api:0.0.1'
        }
        """.stripIndent()

        then:
        def result = runTasksSuccessfully('printFiles')

        matchingFile("gradlets/test-service-api-0.0.1/@gradlets/test-service-api").any {
            result.standardOutput.find(it)
        }

        where:
        gradleTestVersion << GRADLE_TEST_VERSIONS
    }

    def 'package bin are set to executable'() {
        when:
        buildFile << """
        dependencies {
            deps 'npm:typescript:${Versions.TYPESCRIPT}'
        }
        
        task executeTypeScript(type: Exec) {
            commandLine new File(configurations.deps.singleFile, '/bin/tsc').absolutePath, '--help'
        }
        """.stripIndent()

        then:
        runTasksSuccessfully('executeTypeScript')
    }

    def 'supports multiple projects'() {
        when:
        addSubproject("project1")
        addSubproject("project2")

        then:
        runTasksSuccessfully()
    }

    def 'supports multiple repositories'() {
        when:
        buildFile << """
        repositories {
            npm { url 'https://registry.yarnpkg.com' }
        }
        """.stripIndent()

        then:
        runTasksSuccessfully()
    }

    private static List<String> matchingFile(String filename) {
       return ["files-2.1/[0-9a-fA-F]+/" + filename, "transforms-3/[0-9a-fA-F]+/transformed/" + filename]
    }
}
