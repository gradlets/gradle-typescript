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

package com.gradlets.gradle.eslint

import nebula.test.IntegrationSpec

class EslintPluginIntegrationSpec extends IntegrationSpec {

    def setup() {
        buildFile << """
        allprojects {
            apply plugin: 'com.gradlets.eslint'
            
            repositories {
                npm { url 'https://registry.npmjs.org' }
            }
            
            version '1.0.0'

            configurations.all {
                resolutionStrategy {
                    force 'npm:inherits:2.0.4'
                    force 'npm:wrappy:1.0.2'
                }
            }
        }
        """.stripIndent()
    }

    def 'lints code'() {
        when:
        buildFile << """
        eslint {
            rules.put('no-console', 'error')
        }
        """.stripIndent()

        file('src/main/typescript/index.ts') << """
        console.log("Made it here.");
        """.stripIndent()

        then:
        def results = runTasksWithFailure('check')
        results.wasExecuted('eslintMain')
        results.standardError.contains('2:1  error  Unexpected console statement  no-console')
    }

    def 'integrates with prettier'() {
        when:
        buildFile << """
        eslint {
            inheritedConfig = ['plugin:prettier/recommended']
        }
        
        dependencies {
            eslint 'npm:eslint-config-prettier:7.2.0'
            eslint 'npm:eslint-plugin-prettier:3.3.1'
            eslint 'npm:prettier:2.2.1'
        }
        """.stripIndent()

        def indexTs = file('src/main/typescript/index.ts')
        indexTs << """
        console.log("Made it here.");
        """.stripIndent()

        then:
        def results = runTasksWithFailure('check')
        results.wasExecuted('eslintMain')
        results.standardError.contains('1:1  error  Delete `⏎`  prettier/prettier')
        String rawFile = indexTs.text

        def result2 = runTasksSuccessfully("format")
        result2.wasExecuted("eslintMain")
        indexTs.text != rawFile
    }
}
