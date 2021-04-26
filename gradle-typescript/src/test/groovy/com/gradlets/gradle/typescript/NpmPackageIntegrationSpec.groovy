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

import com.fasterxml.jackson.core.type.TypeReference
import com.gradlets.gradle.Versions
import nebula.test.IntegrationSpec

class NpmPackageIntegrationSpec extends IntegrationSpec {

    def setup() {
        buildFile << """
            allprojects {
                apply plugin: "com.gradlets.typescript"
                repositories {
                    npm { url 'https://registry.npmjs.org' }
                }

                version '1.0.0'

                task unpack (type: Copy, dependsOn: 'distNpm') {
                    from { tarTree(tasks.distNpm.outputs.files.singleFile) }
                    into "dist"
                }
            }
        """.stripIndent()
    }

    def 'produces npm package'() {
        when:
        file("src/main/typescript/foo.ts") << '''
            const foo = 10;
        '''.stripIndent()

        then:
        def result = runTasksSuccessfully("distNpm", "unpack")
        result.wasExecuted("compileTypeScript")
        result.wasExecuted("distNpm")

        fileExists("build/distributions/${moduleName}/${moduleName}-1.0.0.tgz")
        fileExists('dist/package/package.json')
        fileExists('dist/package/lib/foo.js')
    }

    def 'adds scope to packageJson'() {
        when:
        buildFile << """
        packageJson { scope = 'foundry' }
        """.stripIndent()

        then:
        runTasksSuccessfully('generatePackageJson')

        def packageJson = loadPackageJson()
        packageJson.name == "@foundry/${moduleName}"
    }


    def 'adds dependencies to packageJson'() {
        when:
        buildFile << """
            dependencies {
                deps 'npm:conjure-client:${Versions.CONJURE_CLIENT}'
                deps 'npm:gradlets/test-service-api:0.0.1'
            }
        """.stripIndent()

        then:
        runTasksSuccessfully('generatePackageJson')

        fileExists('build/tmp/generatePackageJson/package.json')
        def packageJsonFile = file('build/tmp/generatePackageJson/package.json')
        def packageJson = ObjectMappers.MAPPER.readValue(packageJsonFile, new TypeReference<Map<String, Object>>() {})
        packageJson.name == moduleName
        packageJson.version == '1.0.0'
        packageJson.dependencies == [
                'conjure-client': '>=' + Versions.CONJURE_CLIENT,
                '@gradlets/test-service-api': '>=0.0.1'
        ]
    }

    def 'works with GCV'() {
        when:
        buildFile.text = """
        buildscript {
            repositories {
                gradlePluginPortal()
            }
            dependencies {
                classpath 'com.palantir.gradle.consistentversions:gradle-consistent-versions:1.26.1'
            }
        }

        apply plugin: 'com.palantir.consistent-versions'

        versionsLock {
            production { from 'deps' }
        }
        """.stripIndent() + buildFile.text
        buildFile << """
            dependencies {
                deps 'npm:blueprintjs/core'
                deps 'npm:conjure-client'
            }
        """.stripIndent()
        file('versions.props') << """
        npm:conjure-client = ${Versions.CONJURE_CLIENT}
        npm:blueprintjs/core = 3.23.1
        npm:classnames = 2.3.1
        npm:js-tokens = 6.0.0
        """.stripIndent()

        then:
        runTasksSuccessfully('--write-locks')
        runTasksSuccessfully('generatePackageJson')

        file('versions.lock').text.contains 'npm:web-streams-polyfill:2.0.6'
        def packageJson = loadPackageJson()
        packageJson.name == moduleName
        packageJson.version == '1.0.0'
        packageJson.dependencies == [ 'conjure-client': '>=' + Versions.CONJURE_CLIENT, '@blueprintjs/core': '>=3.23.1' ]
    }

    def 'adds project dependencies to packageJson'() {
        when:
        addSubproject("foo", """
        dependencies {
            deps project(':bar')
        }
        """.stripIndent())
        addSubproject("bar")

        then:
        runTasksSuccessfully('foo:generatePackageJson')

        def packageJson = loadPackageJson('foo')
        packageJson.name == 'foo'
        packageJson.version == '1.0.0'
        packageJson.dependencies == ['bar': '>=1.0.0']
    }


    def 'adds scoped project dependencies to packageJson'() {
        when:
        addSubproject("foo", """
        dependencies {
            deps project(':bar')
        }
        """.stripIndent())
        addSubproject('bar', "packageJson { scope = 'foundry' }")

        then:
        runTasksSuccessfully('foo:generatePackageJson')

        def packageJson = loadPackageJson('foo')
        packageJson.name == 'foo'
        packageJson.version == '1.0.0'
        packageJson.dependencies == ['@foundry/bar': '>=1.0.0']
    }

    def 'respects fields added through packageJson extension'() {
        when:
        buildFile << """
            version '1.0.0'
            packageJson {
                field 'author', 'forozco'
            }
        """.stripIndent()

        then:
        runTasksSuccessfully('generatePackageJson')

        def packageJson = loadPackageJson()
        packageJson.author == 'forozco'
    }

    private Map<String, Object> loadPackageJson(project = ".") {
        def packageJsonFile = file(project + '/build/tmp/generatePackageJson/package.json')
        return ObjectMappers.MAPPER.readValue(packageJsonFile, new TypeReference<Map<String, Object>>() {})
    }
}
