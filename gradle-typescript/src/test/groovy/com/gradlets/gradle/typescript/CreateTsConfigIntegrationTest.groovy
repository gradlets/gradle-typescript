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

import com.gradlets.gradle.Versions
import nebula.test.IntegrationSpec

class CreateTsConfigIntegrationTest extends IntegrationSpec {

    def setup() {
        buildFile << '''
        allprojects {
            apply plugin: 'com.gradlets.typescript'
            apply plugin: 'idea'
            repositories {
                npm { url 'https://registry.npmjs.org' }
            }
            version '1.0.0'
        }
        '''.stripIndent()
    }

    def 'idea renders all tsconfigs'() {
        when:
        ['b', 'c', 'd'].collect({
            addSubproject(it)
            createFile("${it}/src/main/typescript/index.ts")
        })

        then:
        def result = runTasksSuccessfully('idea')
        ['b', 'c', 'd'].collect({
            result.wasExecuted(":$it:idea")
            result.wasExecuted(":$it:createTsConfig")
            result.wasExecuted(":$it:createTsConfigTest")
            fileExists("$it/src/main/tsconfig.json")
        })
    }

    def 'has typeRoots'() {
        when:
        buildFile << """
            dependencies {
                types 'npm:types/jest:${Versions.JEST_TYPES}'
            }
        """.stripIndent()
        createFile("src/main/typescript/index.ts")

        then:
        runTasksSuccessfully('idea')
        file('src/main/tsconfig.json').text.contains('"typeRoots" : [')
    }

    def 'idea doesnt cause compilation'() {
        when:
        buildFile << """
            dependencies {
                types 'npm:types/jest:${Versions.JEST_TYPES}'
            }
        """.stripIndent()
        createFile("src/main/typescript/index.ts")

        then:
        def result = runTasksSuccessfully('idea')
        result.wasExecuted('createTsConfigTest')
        !result.wasExecuted('compileTypeScript')
    }

    def 'includes transitive project dependencies'() {
        when:
        addSubproject("first")
        file("first/src/main/typescript/index.ts") << '''
            const foo = 3
        '''.stripIndent()
        file("first/build.gradle") << """
            dependencies {
                deps 'npm:conjure-client:${Versions.CONJURE_CLIENT}'
            }
        """.stripIndent()

        addSubproject("second")
        file("second/src/main/typescript/index.ts") << '''
            const bar = 5
        '''.stripIndent()
        file("second/build.gradle") << '''
            dependencies {
                deps project(':first')
            }
        '''.stripIndent()

        addSubproject("third")
        file("third/src/main/typescript/index.ts") << '''
            const baz = 10
        '''.stripIndent()
        file("third/src/test/typescript/index.ts") << '''
            const xyz = 10
        '''.stripIndent()
        file("third/build.gradle") << '''
            dependencies {
                deps project(':second')
            }
        '''.stripIndent()

        then:
        def result = runTasksSuccessfully('idea')
        result.wasExecuted(':third:createTsConfig')
        Map<String, Set<String>> paths = (ObjectMappers.MAPPER.readValue(
                file('third/src/test/tsconfig.json'), TsConfig.class)
                .compilerOptions()
                .get("paths") as Map<String, Set<String>>)
        paths.containsKey('conjure-client')
    }
}
