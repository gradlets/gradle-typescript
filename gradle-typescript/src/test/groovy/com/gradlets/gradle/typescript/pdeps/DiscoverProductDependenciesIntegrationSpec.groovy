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

package com.gradlets.gradle.typescript.pdeps

import nebula.test.IntegrationSpec

class DiscoverProductDependenciesIntegrationSpec extends IntegrationSpec {
    def setup() {
        buildFile << """
        apply plugin: 'com.palantir.sls-asset-distribution'
        allprojects {
            group 'com.palantir.test'
            version '1.0.0'

            apply plugin: 'com.palantir.recommended-product-dependencies'
            apply plugin: 'com.gradlets.typescript'
        }
        """.stripIndent()
    }

    def 'discovers in repo product dependencies'() {
        when:
        buildFile << """
        dependencies {
            deps project(':subproject')
        }
        """.stripIndent()
        addSubproject("subproject", """
        recommendedProductDependencies {
            productDependency {
                productGroup = 'com.foo.bar.group'
                productName = 'product'
                minimumVersion = '1.0.0'
                maximumVersion = '1.x.x'
            }
        } 
        """.stripIndent())

        then:
        def result = runTasksSuccessfully('createManifest', '--write-locks')
        result.wasExecuted('discoverNpmProductDependencies')
        result.wasExecuted('subproject:generatePackageJson')
        !result.wasExecuted('subproject:distNpm')
        file('product-dependencies.lock').text.contains 'product (1.0.0, 1.x.x)'
    }

    def 'discovers product dependencies from external projects'() {
        when:
        buildFile << """
        repositories {
            npm { url 'https://registry.npmjs.org' }
        }
        dependencies {
            deps 'npm:gradlets/test-service-api:0.0.1'
        }
        """.stripIndent()

        then:
        def result = runTasksSuccessfully('createManifest', '--write-locks')
        result.wasExecuted('discoverNpmProductDependencies')
        file('product-dependencies.lock').text.contains 'com.gradlets.test:test-service (0.0.1, 0.x.x)'
    }

}
