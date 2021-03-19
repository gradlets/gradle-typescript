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


import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import nebula.test.IntegrationSpec

class PublishToNpmLocalIntegrationSpec extends IntegrationSpec {
    def setup() {
        buildFile << """
        import com.gradlets.gradle.npm.NpmPublication
        allprojects {
            apply plugin: "com.gradlets.typescript"
            apply plugin: "com.gradlets.npm-publish"
            repositories {
                npmLocal()
                npm { url 'https://registry.npmjs.org' }
            }

            version '1.0.0'

            publishing {
                publications {
                    typescript(NpmPublication) {
                        artifact distNpm
                    }
                    notTypescript(NpmPublication) {
                        artifact distNpm
                    }
                }
            }
        }
        """.stripIndent()
    }

    def 'publish to npm local'() {
        when:
        file("src/main/typescript/foo.ts") << '''
            const foo = 10;
        '''.stripIndent()

        then:
        def result = runTasksSuccessfully("publishToNpmLocal")
        result.wasExecuted("compileTypeScript")
        result.wasExecuted("distNpm")
        Files.exists(PublishToNpmLocal.LOCAL_NPM_REPOSITORY.resolve('publish-to-npm-local').resolve('-').resolve(
                'publish-to-npm-local-1.0.0.tgz'))
        Files.exists(PublishToNpmLocal.LOCAL_NPM_REPOSITORY.resolve('publish-to-npm-local').resolve('1.0.0').resolve(
                'descriptor.ivy'))
    }

    def 'resolve from npm local'() {
        def artifactDirectory = PublishToNpmLocal.LOCAL_NPM_REPOSITORY.resolve('foo-artifact').resolve('-')
        Files.createDirectories(artifactDirectory)
        def descriptorDirectory = PublishToNpmLocal.LOCAL_NPM_REPOSITORY.resolve('foo-artifact').resolve('1.0.0')
        Files.createDirectories(descriptorDirectory)

        when:
        Files.copy(Paths.get('src/test/foo-artifact-1.0.0.tgz'),
                artifactDirectory.resolve('foo-artifact-1.0.0.tgz'),
                StandardCopyOption.REPLACE_EXISTING)
        Files.copy(Paths.get('src/test/foo-artifact-1.0.0-descriptor.ivy'),
                descriptorDirectory.resolve('descriptor.ivy'),
                StandardCopyOption.REPLACE_EXISTING)
        addSubproject("subproject", """
        dependencies {
            deps 'npm:foo-artifact:1.0.0'
        }
        """.stripIndent())
        file("subproject/src/main/typescript/bar.ts") << '''
            const bar = 10;
        '''.stripIndent()

        then:
        runTasksSuccessfully("compileTypeScript")
    }

    def 'publishes all publications'() {
        when:
        file("src/main/typescript/foo.ts") << '''
            const foo = 10;
        '''.stripIndent()

        then:
        def result = runTasksSuccessfully("publishToNpmLocal")
        result.wasExecuted('publishTypescriptPublicationToNpmLocal')
        result.wasExecuted('publishNotTypescriptPublicationToNpmLocal')
    }
}
