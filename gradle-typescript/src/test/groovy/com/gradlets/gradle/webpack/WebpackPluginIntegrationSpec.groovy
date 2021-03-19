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

package com.gradlets.gradle.webpack

import com.gradlets.gradle.Versions
import nebula.test.IntegrationSpec

class WebpackPluginIntegrationSpec extends IntegrationSpec {


    public static final String WEBPACK_CONFIG = """
        const path = require("path");
        const webpack = require("webpack");

        module.exports = {
            mode: "production",
            devtool: false,
            entry: "./build/scripts/main/index.js",
            resolve: {
                modules: process.env.NODE_PATH.split(":"),
                extensions: [ ".js", ".ts", ".json" ],
            },
            output: {
                path: path.resolve(__dirname, 'build/webpack'),
                filename: "bundle.js"
            },
            target: "node"
        };
        """.stripIndent()

    def setup() {
        buildFile << """
        allprojects {
            apply plugin: 'com.gradlets.npm-base'
            repositories {
                npm { url 'https://registry.npmjs.org' }
            }
            version '1.0.0'
            configurations.all {
                resolutionStrategy {
                    force 'npm:types/eslint:7.2.6'
                    force 'npm:types/node:14.6.2'
                    force 'npm:webpack-sources:2.2.0'
                }
            }
        }
        """.stripIndent()
    }

    def 'bundles typescript'() {
        when:
        buildFile << """
        apply plugin: 'com.gradlets.webpack'

        webpack {
            config 'webpack.config.js'
            outputDir = project.file('build/webpack')
        }   
        dependencies {
            deps 'npm:conjure-client:${Versions.CONJURE_CLIENT}'

            webpack 'npm:webpack:5.21.2'
            webpack 'npm:webpack-cli:4.5.0'
            webpack 'npm:awesome-typescript-loader:5.2.1'
        }
        """.stripIndent()
        file('webpack.config.js') << WEBPACK_CONFIG

        file('src/main/typescript/index.ts') << """
        import { FetchBridge } from "conjure-client";
        import { foo, IFoo } from "./foo";
        
        new FetchBridge({ 
            baseUrl: "my-base-url", 
            token: "my-token" , 
            userAgent: { productName: "my-product", productVersion: "0.0.0"}
        });
        console.log(foo);
        """.stripIndent()
        file('src/main/typescript/foo.ts') << """
        export interface IFoo {
            foo: string
        }
        export const foo: IFoo = { foo: "foo" };
        """.stripIndent()

        then:
        runTasks("bundleWebpack")
        fileExists('build/webpack/bundle.js')
        file('build/webpack/bundle.js').text.contains('FetchBridge')
    }

    def 'handles project dependencies'() {
        when:
        addSubproject('test-project', """
        apply plugin: 'com.gradlets.typescript'
        packageJson {
            scope = "foundry"
        }
        """)
        file('test-project/src/main/typescript/index.ts') << """
        export const foo = 10;
        """.stripIndent()

        buildFile << """
        apply plugin: 'com.gradlets.webpack'

        webpack {
            config 'webpack.config.js'
            outputDir = project.file('build/webpack')
        }   
        
        dependencies {
            deps project(':test-project')

            webpack 'npm:webpack:5.21.2'
            webpack 'npm:webpack-cli:4.5.0'
            webpack 'npm:awesome-typescript-loader:5.2.1'
        }
        """.stripIndent()

        file('webpack.config.js') << WEBPACK_CONFIG

        file('src/main/typescript/index.ts') << """
        import { foo } from "@foundry/test-project";
        const bar = foo;
        console.log(bar);
        """.stripIndent()

        then:
        def result = runTasksSuccessfully("bundleWebpack");
        result.wasExecuted('compileTypeScript')
        result.wasExecuted('test-project:compileTypeScript')
    }

    def 'produces consumable configuration'() {
        when:
        addSubproject("my-library",  """

        apply plugin: 'com.gradlets.webpack'

        webpack {
            config 'webpack.config.js'
            outputDir = project.file('build/webpack')
        }
        dependencies {
            deps 'npm:conjure-client:${Versions.CONJURE_CLIENT}'

            webpack 'npm:webpack:5.21.2'
            webpack 'npm:webpack-cli:4.5.0'
            webpack 'npm:awesome-typescript-loader:5.2.1'
        }
        """)

        file('my-library/webpack.config.js') << WEBPACK_CONFIG
        file('my-library/src/main/typescript/index.ts') << """
        interface IFoo {
            foo: string
        }
        """.stripIndent()

        addSubproject("api-consumer", '''
        configurations {
            webpackOutputConsumer {
                attributes.attribute(
                    Attribute.of('com.palantir.conjure', Usage.class),
                        project.objects.named(Usage.class, "webpack-api"));
            }
        }
        dependencies {
            webpackOutputConsumer project(':my-library')
        }

        task getWebpackOutput(type: Copy) {
            from configurations.webpackOutputConsumer
            into "${project.buildDir}/my-copy"
        }
        '''.stripIndent())

        then:
        def result = runTasksSuccessfully('getWebpackOutput')
        result.wasExecuted(':my-library:bundleWebpack')

        fileExists('api-consumer/build/my-copy/bundle.js')
    }
}
