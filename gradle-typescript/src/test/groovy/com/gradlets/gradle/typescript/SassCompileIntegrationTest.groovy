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
import spock.lang.Ignore

class SassCompileIntegrationTest extends IntegrationSpec {
    @Ignore
    def "it compiles sassy stuff in the root"() {
        when:
        buildFile << applyPlugin(TypeScriptPlugin)
        file("src/foo.scss") << '''
            .foo {
                color: red;
            } '''.stripIndent()

        then:
        def result = runTasks("compileSass")
        result.success
        fileExists("build/css/foo.css")
    }

    @Ignore
    def "it compiles nested sassys stuff"() {
        when:
        buildFile << applyPlugin(TypeScriptPlugin)
        file("src/foo.scss") << '''
            .foo {
                color: blue;
            }
        '''.stripIndent()
        file("src/nested/bar.scss") << '''
            .nested_bar {
                color: red;
            }
        '''.stripIndent()

        then:
        def result = runTasks("compileSass")
        result.success
        fileExists("build/css/foo.css")
        fileExists("build/css/nested/bar.css")
    }

    @Ignore
    def "it caches previous compilation"() {
        when:
        buildFile << applyPlugin(TypeScriptPlugin)
        file("src/foo.scss") << '''
            .foo {
                color: red;
            } '''.stripIndent()

        then:
        def result1 = runTasks("compileSass")
        !result1.wasUpToDate("compileSass")
        def result2 = runTasks("compileSass")
        result2.wasUpToDate("compileSass")
    }
}
