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

import com.gradlets.gradle.typescript.ObjectMappers
import nebula.test.IntegrationSpec

class EmbedProductDependenciesIntegrationSpec extends IntegrationSpec {

    def setup() {
        buildFile << """
        apply plugin: 'com.palantir.recommended-product-dependencies'
        apply plugin: 'com.gradlets.typescript'
        """.strip()
    }

    def 'populates package json'() {
        when:
        buildFile << """
        recommendedProductDependencies {
            productDependency {
                productGroup = 'com.foo.bar.group'
                productName = 'product'
                minimumVersion = '1.0.0'
                maximumVersion = '1.x.x'
            }
        }
        """.stripIndent()

        then:
        runTasksSuccessfully("generatePackageJson")

        def value = ObjectMappers.MAPPER.readValue(file("build/tmp/generatePackageJson/package.json"),
                ProductDependenciesPackageJson)
        value.sls().isPresent()
        value.sls().get().dependencies().get("com.foo.bar.group:product") != null
    }
}
