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

class GradleTypeScriptRootIdeaPluginTest extends IntegrationSpec {

    def setup() {
        buildFile << '''
        apply plugin: 'com.gradlets.typescript'
        apply plugin: 'idea'
        repositories {
            npm { url 'https://registry.npmjs.org' }
        }
        version '1.0.0'
        '''.stripIndent()
    }

    def "intellij configures typescript compiler"() {
        when:
        file("src/main/typescript/index.ts") << """
        const foo = 10;
        """.stripIndent()

        then:
        runTasksSuccessfully('idea')
        file("intellij-configures-typescript-compiler.ipr").text.contains(
                "<option name=\"versionType\" value=\"SERVICE_DIRECTORY\"/>")
        file("intellij-configures-typescript-compiler.ipr").text =~
                /<option name="typeScriptServiceDirectory" value=".*\\/typescript"\\/>/
    }

}
