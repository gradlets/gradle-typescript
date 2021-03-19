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

package com.gradlets.gradle.npm;

import com.palantir.conjure.java.api.config.service.PartialServiceConfiguration;
import com.palantir.conjure.java.api.config.service.ServicesConfigBlock;
import com.palantir.conjure.java.api.config.service.UserAgent;
import com.palantir.conjure.java.api.config.service.UserAgent.Agent;
import com.palantir.conjure.java.api.config.ssl.SslConfiguration;
import com.palantir.conjure.java.api.config.ssl.SslConfiguration.StoreType;
import com.palantir.dialogue.clients.DialogueClients;
import com.palantir.logsafe.exceptions.SafeRuntimeException;
import com.palantir.refreshable.Refreshable;
import com.palantir.tokens.auth.AuthHeader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.repositories.IvyArtifactRepository;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

public abstract class PublishToNpmRepository extends DefaultTask {
    private static final UserAgent USER_AGENT = UserAgent.of(Agent.of(
            "publishToNpmRepo",
            Optional.ofNullable(PublishToNpmRepository.class.getPackage().getImplementationVersion())
                    .orElse("0.0.0")));

    @SuppressWarnings("PublicConstructorForAbstractClass")
    public PublishToNpmRepository() {
        getInputs()
                .files((Callable<FileCollection>)
                        () -> getPublication().get().getPublishableArtifacts().getFiles())
                .withPropertyName("publication.publishableFiles")
                .withPathSensitivity(PathSensitivity.NAME_ONLY);
    }

    @Input
    public abstract Property<NpmPublication> getPublication();

    @Input
    public abstract Property<IvyArtifactRepository> getRepository();

    private NpmPublishService buildNpmClient() {
        return DialogueClients.create(Refreshable.only(ServicesConfigBlock.builder()
                        .putServices(
                                "service",
                                PartialServiceConfiguration.builder()
                                        .addUris(getRepository().get().getUrl().toString())
                                        .security(SslConfiguration.builder()
                                                .trustStorePath(trustStorePath())
                                                .trustStoreType(storeType())
                                                .build())
                                        .build())
                        .build()))
                .withUserAgent(USER_AGENT)
                .get(NpmPublishService.class, "service");
    }

    @TaskAction
    public final void upload() throws IOException {
        Path publicationPath = getPublication()
                .get()
                .getPublishableArtifacts()
                .getFiles()
                .getSingleFile()
                .toPath();
        NpmPackageRoot npmPackageRoot = PackageJsons.packageJsonForPublication(
                PackageJsons.getPackageJson(publicationPath),
                StringUtils.appendIfMissing(
                        getRepository().get().getUrl().toString().replace("https", "http"), "/"),
                Files.readAllBytes(publicationPath));
        org.gradle.api.artifacts.repositories.PasswordCredentials passwordCreds =
                getRepository().get().getCredentials();
        if (AuthHeaderCredentials.PASSWORD_CREDS_USERNAME.equals(passwordCreds.getUsername())
                && passwordCreds.getPassword() != null) {
            buildNpmClient().uploadPublication(AuthHeader.valueOf(passwordCreds.getPassword()), npmPackageRoot);
        } else if (passwordCreds.getUsername() != null && passwordCreds.getPassword() != null) {
            buildNpmClient()
                    .uploadPublication(
                            BasicAuthHeader.of(passwordCreds.getUsername(), passwordCreds.getPassword()),
                            npmPackageRoot);
        } else {
            buildNpmClient().uploadPublication(npmPackageRoot);
        }
    }

    private static StoreType storeType() {
        return Optional.ofNullable(System.getProperty("javax.net.ssl.trustStoreType"))
                .map(StoreType::valueOf)
                .orElse(StoreType.JKS);
    }

    private static Path trustStorePath() {
        return Optional.ofNullable(System.getProperty("javax.net.ssl.trustStore"))
                .map(Paths::get)
                .orElseGet(() -> Optional.ofNullable(System.getProperty("java.home"))
                        .map(javaHome -> Paths.get(javaHome, "lib", "security", "cacerts"))
                        .orElseThrow(() -> new SafeRuntimeException("Unable to find trustStore")));
    }
}
