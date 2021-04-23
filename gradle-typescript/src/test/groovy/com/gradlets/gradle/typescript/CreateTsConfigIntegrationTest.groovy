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

        createFile("src/main/typescript/index.ts")
    }

    def 'tsconfig includes project and external dependencies'()  {
        when:
        buildFile << """
            dependencies {
                deps project(':child')
                deps 'npm:conjure-client:${Versions.CONJURE_CLIENT}'
            }
        """.stripIndent()

        addSubproject("child")
        createFile("child/src/main/typescript/index.ts")

        then:
        def result = runTasksSuccessfully('createTsConfig')
        !result.wasExecuted('child:compileTypeScript')
        Map<String, Set<String>> paths = getTsConfigPaths(file('src/main/tsconfig.json'))
        paths.containsKey('conjure-client')
        paths.containsKey('child')
        paths.containsKey('child/*')
    }

    def 'idea renders all tsconfigs'() {
        when:
        addSubproject("child")
        createFile("child/src/main/typescript/index.ts")

        then:
        def result = runTasksSuccessfully('idea')
        result.wasExecuted('createTsConfig')
        result.wasExecuted(':child:createTsConfig')
    }

    def 'tsconfig includes typeRoots'() {
        when:
        buildFile << """
            dependencies {
                types 'npm:types/jest:${Versions.JEST_TYPES}'
            }
        """.stripIndent()

        then:
        runTasksSuccessfully('createTsconfig')
        file('src/main/tsconfig.json').text.contains('"typeRoots" : [')
    }

    def 'includes transitive project dependencies'() {
        when:
        buildFile << """
        dependencies {
            deps project(':first')
        }
        """.stripIndent()

        addSubproject("first", """
        dependencies {
            deps project(':second')
        }
        """.stripIndent())
        file("first/src/main/typescript/index.ts");

        addSubproject("second", """
        dependencies {
            deps 'npm:conjure-client:${Versions.CONJURE_CLIENT}'
        }
        """.stripIndent())
        file("second/src/main/typescript/index.ts");

        then:
        def result = runTasksSuccessfully('createTsConfig')
        Map<String, Set<String>> paths = getTsConfigPaths(file('src/main/tsconfig.json'))
        paths.containsKey('conjure-client')
        ['first', 'second'].forEach {
            assert paths.containsKey(it)
            assert paths.containsKey(it + "/*")
        }
    }

    private static Map<String, Set<String>> getTsConfigPaths(File file) throws IOException {
        return ObjectMappers.MAPPER.readValue(
                file, TsConfig.class)
                .compilerOptions()
                .get('paths') as Map<String, Set<String>>;
    }
}
