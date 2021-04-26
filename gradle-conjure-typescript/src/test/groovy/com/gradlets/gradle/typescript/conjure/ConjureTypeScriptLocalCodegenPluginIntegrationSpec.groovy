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


import com.gradlets.gradle.typescript.ObjectMappers
import com.gradlets.gradle.typescript.conjure.TestVersions
import java.nio.file.Paths
import nebula.test.IntegrationSpec

class ConjureTypeScriptLocalCodegenPluginIntegrationSpec extends IntegrationSpec {

    def setup() {
        buildFile << """
        allprojects {
            apply plugin: 'com.gradlets.npm-base'

            repositories {
                npm {  url 'https://registry.npmjs.org' }
                mavenCentral()
                maven { url 'file://${Paths.get(".").toAbsolutePath()}/src/test/resources/repo' }
            }

            configurations.all {
                resolutionStrategy {
                    force 'com.palantir.conjure:conjure-api:${TestVersions.CONJURE}'
                    force 'com.palantir.conjure.typescript:conjure-typescript:${TestVersions.CONJURE_TYPESCRIPT}'
                    force 'npm:conjure-client:${TestVersions.CONJURE_CLIENT}'
                }
            }
        }
        """.stripIndent()
        addSubproject('conjure-api')
        buildFile << """
        apply plugin: 'com.gradlets.conjure-typescript-local'

        dependencies {
            conjure 'com.palantir.conjure:conjure-api@conjure.json'
        }
        """.stripIndent()
    }

    def 'generates subproject'() {
        expect:
        def result = runTasksSuccessfully('compileTypeScript', '-s')
        result.wasExecuted(':extractConjureIr')
        result.wasExecuted('conjure-api:gitignoreConjure')
        fileExists('conjure-api/src/main/typescript/index.ts')
    }

    def 'configures idea if present'() {
        when:
        buildFile << "allprojects { apply plugin: 'idea' }"

        then:
        def result = runTasksSuccessfully('idea', '-s')
        result.wasExecuted(':extractConjureIr')
        result.wasExecuted('conjure-api:gitignoreConjure')
        fileExists('conjure-api/src/main/typescript/index.ts')
    }

    def 'injects product dependencies'() {
        when:
        buildFile << """
        dependencies {
            conjure 'com.gradlets.test:test-service-api:0.0.1@conjure.json'
        }
        """.stripIndent()
        addSubproject('test-service-api')

        then:
        def result = runTasksSuccessfully('generatePackageJson')

        result.wasExecuted(':extractConjureIr')
        def packageJson = ObjectMappers.MAPPER.readValue(file('test-service-api/build/tmp/generatePackageJson/package.json'), Map)
        !packageJson['sls']['dependencies'].containsKey('com.gradlets.test:test-service')
    }
}
