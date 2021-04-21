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
import spock.lang.Unroll

class TypeScriptCompileIntegrationTest extends IntegrationSpec {
    private static final List<String> GRADLE_TEST_VERSIONS = ['6.8', '6.7.1']

    def setup() {
        buildFile << """
        allprojects {
            apply plugin: 'com.gradlets.typescript'
            repositories {
                npm { url 'https://registry.npmjs.org' }
            }
            version '1.0.0'
        }
        """.stripIndent()
    }

    def 'Compiles typescript'() {
        when:
        file("src/main/typescript/foo.ts") << '''
            const foo = 10;
        '''.stripIndent()

        then:
        runTasksSuccessfully("compileTypeScript")
        fileExists("build/scripts/main/foo.js")
        fileExists("build/scripts/main/foo.d.ts")
    }

    def 'compiles typescript not up to date if files change'() {
        when:
        file("src/main/typescript/foo.ts") << '''
            const foo = 10;
        '''.stripIndent()
        runTasksSuccessfully(':compileTypeScript')
        file("src/main/typescript/foo.ts").text = '''
            const foo = 20;
        '''.stripIndent()

        then:
        def result = runTasksSuccessfully(':compileTypeScript')
        !result.wasUpToDate(':compileTypeScript')
        fileExists('build/scripts/main/foo.js')
        fileExists('build/scripts/main/foo.d.ts')
    }

    def 'compiles test code with dependency on main'() {
        when:
        addSubproject('foo')
        file("foo/src/main/typescript/index.ts") << '''
        export interface IMainInterface {
            main: number;
        }
        '''.stripIndent()

        file("foo/src/test/typescript/index.ts") << '''
            import { IMainInterface } from "foo";
            const bar: IMainInterface = { main: 10 };
        '''.stripIndent()

        then:
        def result = runTasksSuccessfully("compileTestTypeScript")
        result.wasExecuted(':foo:compileTypeScript')
        result.wasExecuted(':foo:compileTestTypeScript')
    }

    def 'test code inherits main dependencies'() {
        when:
        addSubproject('foo', """
        dependencies {
            deps 'npm:conjure-client:${Versions.CONJURE_CLIENT}'
        }
        """.stripIndent())
        file("foo/src/test/typescript/index.ts") << '''
            import { IHttpApiBridge } from "conjure-client";
            const foo = 10;
        '''.stripIndent()

        then:
        def result = runTasksSuccessfully("compileTestTypeScript")
        result.wasExecuted(':foo:compileTestTypeScript')
    }

    @Unroll
    def '#gradleTestVersion compiles typescript with external dependencies'() {
        setup:
        gradleVersion = gradleTestVersion

        when:
        buildFile << """
        dependencies {
            deps 'npm:conjure-client:${Versions.CONJURE_CLIENT}'
        }
        """.stripIndent()
        file("src/main/typescript/foo.ts") << '''
            import { IHttpApiBridge } from "conjure-client";
            const foo = 10;
        '''.stripIndent()

        then:
        runTasksSuccessfully("compileTypeScript")
        fileExists("build/scripts/main/foo.js")
        fileExists("build/scripts/main/foo.d.ts")

        where:
        gradleTestVersion << GRADLE_TEST_VERSIONS
    }

    @Unroll
    def '#gradleTestVersion compiles typescript with scoped external dependencies'() {
        setup:
        gradleVersion = gradleTestVersion

        when:
        buildFile << """
        dependencies {
            deps 'npm:gradlets/test-service-api:0.0.1'
            deps 'npm:conjure-client:${Versions.CONJURE_CLIENT}'
        }
        """.stripIndent()
        file("src/main/typescript/foo.ts") << '''
            import { IHttpApiBridge } from "conjure-client";
            import { TestServiceService } from "@gradlets/test-service-api"
            const foo = 10;
        '''.stripIndent()

        then:
        runTasksSuccessfully("compileTypeScript")
        fileExists("build/scripts/main/foo.js")
        fileExists("build/scripts/main/foo.d.ts")

        where:
        gradleTestVersion << GRADLE_TEST_VERSIONS
    }

    def "caches previous compilation"() {
        when:
        file("src/main/typescript/foo.ts") << '''
            const foo = 10;
        '''.stripIndent()

        then:
        def result1 = runTasks("compileTypeScript")
        !result1.wasUpToDate(":compileTypeScript")
        def result2 = runTasks("compileTypeScript")
        result2.wasUpToDate(":compileTypeScript")
    }

    def "outputs compiler errors"() {
        when:
        file("src/main/typescript/foo.ts") << '''
            const foo: string = 10;
        '''.stripIndent()

        then:
        def result = runTasksWithFailure("compileTypeScript")
        result.getStandardError().contains("Type 'number' is not assignable to type 'string'")
    }

    def "compiles dependant projects"() {
        when:
        addSubproject("foo", """
        dependencies {
             deps project(':bar')
        }
        """.stripIndent())

        file("foo/src/main/typescript/index.ts") << '''
        import { IBarInterface } from "bar";
        const foo: IBarInterface = { bar: 10 };
        console.log(foo);
        '''.stripIndent()

        addSubproject("bar");
        file("bar/src/main/typescript/index.ts") << '''
        export interface IBarInterface {
            bar: number;
        }
        '''.stripIndent()

        then:
        def result = runTasksSuccessfully(":foo:compileTypeScript")
        result.wasExecuted(':foo:compileTypeScript')
        result.wasExecuted(':bar:compileTypeScript')
    }

    def 'compiles scoped dependant projects'() {
        when:
        addSubproject("foo", """
        dependencies {
             deps project(':bar')
        }
        """.stripIndent())

        file("foo/src/main/typescript/index.ts") << '''
        import { IBarInterface } from "@foundry/bar";
        const foo: IBarInterface = { bar: 10 };
        console.log(foo);
        '''.stripIndent()

        addSubproject("bar", """
        packageJson { scope = 'foundry' }
        """)
        file("bar/src/main/typescript/index.ts") << '''
        export interface IBarInterface {
            bar: number;
        }
        '''.stripIndent()

        then:
        def result = runTasksSuccessfully(":foo:compileTypeScript")
        result.wasExecuted(':foo:compileTypeScript')
        result.wasExecuted(':bar:compileTypeScript')
    }

    def 'supports @types packages'() {
        when:
        buildFile << """
        typeScript {
            compilerOptions.put("noImplicitAny", true)
        }

        dependencies {
            deps 'npm:react:${Versions.REACT}'

            types 'npm:types/react:${Versions.REACT}'
            types 'npm:types/prop-types:${Versions.PROP_TYPES}'
        }
        """.stripIndent()
        file("src/main/typescript/index.ts") << '''
            import * as React from "react";
        '''.stripIndent()

        then:
        def result = runTasksSuccessfully("compileTypeScript")
    }

    def 'handles user provided compilerOptions'() {
        when:
        buildFile << '''
        typeScript {
            compilerOptions.putAll([
                'module': 'es6',
                'target': 'es6',
                'moduleResolution': 'node',
                'strict': true,
            ])
        }
        '''.stripIndent()

        file("src/main/typescript/foo.ts") << '''
            const foo = 10;
        '''.stripIndent()

        then:
        runTasksSuccessfully("compileTypeScript")
        fileExists("build/scripts/main/foo.js")
        fileExists("build/scripts/main/foo.d.ts")
        file('build/tmp/compileTypeScript/tsconfig.json').text.contains '"module" : "es6"'
    }

    def 'supports submodule imports'() {
        when:
        addSubproject("foo", """
        dependencies {
             deps project(':bar')
        }
        """.stripIndent())

        file("foo/src/main/typescript/index.ts") << '''
        import { IBarInterface } from "@foundry/bar";
        import { ISub } from "@foundry/bar/sub";
        import { dom } from "@foundry/bar";
        import IDom = dom.IDom
        const foo: IBarInterface = { bar: 10 };
        console.log(foo);
        const fooSub: ISub = { sub: 10 };
        console.log(fooSub);
        const fooDom: IDom = { dom: 10 };
        console.log(fooDom);
        '''.stripIndent()

        addSubproject("bar", """
        packageJson { scope = 'foundry' }
        """)
        file("bar/src/main/typescript/index.ts") << '''
        import * as dom from "./dom"
        export { dom };
        export * from "./sub";
        export interface IBarInterface {
            bar: number;
        }
        '''.stripIndent()

        file("bar/src/main/typescript/sub/index.ts") << '''
        export interface ISub {
            sub: number;
        }
        '''.stripIndent()

        file("bar/src/main/typescript/dom/index.ts") << '''
        export interface IDom {
            dom: number;
        }
        '''.stripIndent()


        then:
        def result = runTasksSuccessfully(":foo:compileTypeScript")
        result.wasExecuted(':foo:compileTypeScript')
        result.wasExecuted(':bar:compileTypeScript')
    }
}
