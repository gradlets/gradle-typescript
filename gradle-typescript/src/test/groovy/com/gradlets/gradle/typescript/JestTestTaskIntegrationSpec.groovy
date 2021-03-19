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

package com.gradlets.gradle.typescript

import nebula.test.IntegrationSpec

class JestTestTaskIntegrationSpec extends IntegrationSpec {

    def setup() {
        System.properties.setProperty('ignoreDeprecations', 'true');
        buildFile << """
        allprojects {
            apply plugin: 'com.gradlets.typescript'
            repositories {
                npm { url 'https://registry.npmjs.org' }
            }
            version '1.0.0'
        }
        """.stripIndent()
    }

    def "executes jest"() {
        when:
        buildFile << """
        dependencies {
            testDeps 'npm:jest:26.6.3'
        }
        """.stripIndent()
        file("src/test/typescript/foo.ts") << '''
            describe("foo", () => {
                it("runs a test", () => {});
            });
        '''.stripIndent()

        then:
        runTasksSuccessfully("jestTest")
    }

    def 'no-op if no test code'() {
        when:
        def result = runTasksSuccessfully("jestTest")

        then:
        result.standardOutput.contains("Task :jestTest NO-SOURCE")
    }
}
