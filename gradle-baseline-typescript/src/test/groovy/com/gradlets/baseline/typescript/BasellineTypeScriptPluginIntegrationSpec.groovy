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

package com.gradlets.baseline.typescript

import nebula.test.IntegrationSpec

class BaselineTypeScriptPluginIntegrationSpec extends IntegrationSpec {

    def setup() {
        buildFile << """
        buildscript {
            repositories {
                gradlePluginPortal()
            }

            dependencies {
                classpath 'com.palantir.gradle.consistentversions:gradle-consistent-versions:1.26.1'
            }
        }

        apply plugin: 'com.palantir.consistent-versions'
        apply plugin: 'com.gradlets.baseline-typescript'

        allprojects {
            apply plugin: 'com.gradlets.typescript'
            repositories {
                npm { url 'https://registry.npmjs.org' }
            }

            dependencies {
                deps 'npm:conjure-client'
            }

            version '1.0.0'
        }
        """.stripIndent()

        file("versions.props") << """
        npm:conjure-client* = ${Versions.CONJURE_CLIENT}
        """.stripIndent()

        runTasksSuccessfully('--write-locks')
    }

    def 'compatible with java plugin with gcv'() {
        when:
        file("src/main/typescript/foo.ts") << '''
            const foo = 10;
        '''.stripIndent()
        buildFile << "apply plugin: 'java-library'"

        then:
        runTasksSuccessfully('compileTypeScript', 'generatePackageJson')
    }
}
