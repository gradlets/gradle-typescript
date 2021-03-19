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

class BaselineWebpackIntegrationSpec extends IntegrationSpec {

    def setup() {
        buildFile << """
        apply plugin: 'com.gradlets.baseline-typescript'
        apply plugin: 'com.gradlets.webpack'
        
        repositories {
            npm { url 'https://registry.npmjs.org' }
        }
        version '1.0.0'
        """.stripIndent()

        file('src/main/typescript/index.ts') << 'console.log("foo");'
    }

    def 'correctly configure webpack'() {
        expect:
        def result = runTasksSuccessfully('bundleWebpack')
        result.wasExecuted('generateWebpackConfig')

        fileExists('build/webpack/bundle.js')
        fileExists('build/webpack/bundle.js.map')
        fileExists('build/webpack.config.js')
    }

    def 'tasks are cached correctly'() {
        expect:
        def result1 = runTasksSuccessfully('bundleWebpack')
        result1.wasExecuted('generateWebpackConfig')
        def result2 = runTasksSuccessfully('bundleWebpack')
        result2.wasUpToDate('bundleWebpack')
        result2.wasUpToDate('generateWebpackConfig')
    }
}
