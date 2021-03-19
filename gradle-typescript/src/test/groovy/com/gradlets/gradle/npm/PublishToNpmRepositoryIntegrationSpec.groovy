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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo
import static com.github.tomakehurst.wiremock.client.WireMock.put
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options

import com.github.tomakehurst.wiremock.WireMockServer
import nebula.test.IntegrationSpec

class PublishToNpmRepositoryIntegrationSpec extends IntegrationSpec {
    public WireMockServer wiremock = new WireMockServer(options().dynamicPort())

    def cleanup() {
        wiremock.stop()
    }

    def setup() {
        wiremock.stubFor(put(urlMatching("/artifactory/api/npm/internal-npm-sandbox/publish-to-remote-repository"))
                .withBasicAuth('me', 'changeit')
                .willReturn(aResponse().withStatus(200)))
        wiremock.stubFor(put(urlMatching("/npm/publish-to-remote-repository"))
                .withHeader("Authorization", equalTo("Bearer notatoken"))
                .willReturn(aResponse().withStatus(200)))
        wiremock.start()
        buildFile << """
        import com.gradlets.gradle.npm.NpmPublication
        import com.gradlets.gradle.npm.PasswordCredentials
        import com.gradlets.gradle.npm.AuthHeaderCredentials
        allprojects {
            apply plugin: "com.gradlets.typescript"
            apply plugin: "com.gradlets.npm-publish"
            repositories {
                npm { url 'https://registry.npmjs.org' }
            }

            version '1.0.1'

            publishing {
                publications {
                    typescript(NpmPublication) {
                        artifact distNpm
                    }
                }
                repositories {
                    npm {
                        url 'http://localhost:${wiremock.port()}/artifactory/api/npm/internal-npm-sandbox'
                        credentials(PasswordCredentials) {
                            username 'me'
                            password 'changeit'
                        }
                    }
                    npm {
                        name 'second'
                        url 'http://localhost:${wiremock.port()}/npm'
                        credentials(AuthHeaderCredentials) {
                            token 'notatoken'
                        }
                    }
                }
            }
        }
        """.stripIndent()
    }

    def 'publish to remote repository'() {
        when:
        file("src/main/typescript/foo.ts") << '''
            const foo = 10;
        '''.stripIndent()

        then:
        def result = runTasksSuccessfully("publish")
        result.wasExecuted("publishTypescriptPublicationToNpmRepository")
        result.wasExecuted("publishTypescriptPublicationToSecondRepository")
    }
}
