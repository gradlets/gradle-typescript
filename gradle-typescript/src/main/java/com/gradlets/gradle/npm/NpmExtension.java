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

import com.gradlets.gradle.typescript.shim.IvyPatterns;
import com.gradlets.gradle.typescript.shim.NpmArtifactoryShim;
import java.nio.file.Paths;
import java.util.Optional;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.IvyArtifactRepository;
import org.gradle.api.internal.artifacts.BaseRepositoryFactory;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.model.ObjectFactory;

public class NpmExtension {
    static final String NPM_REPO_DEFAULT_NAME = "npm";

    private final Gradle gradle;
    private final ObjectFactory objectFactory;
    private final RepositoryHandler repositories;
    private final BaseRepositoryFactory repositoryFactory;

    @Inject
    public NpmExtension(
            Gradle gradle,
            ObjectFactory objectFactory,
            RepositoryHandler repositories,
            BaseRepositoryFactory repositoryFactory) {
        this.gradle = gradle;
        this.objectFactory = objectFactory;
        this.repositories = repositories;
        this.repositoryFactory = repositoryFactory;
    }

    public final NpmArtifactRepository npmLocal() {
        NpmArtifactRepository npmArtifactRepository = objectFactory.newInstance(DefaultNpmArtifactRepository.class);
        npmArtifactRepository.setUrl(Paths.get(System.getProperty("user.home"))
                .resolve(".npm")
                .resolve("repository")
                .toString());
        npmArtifactRepository.setName("npmLocal");

        IvyArtifactRepository ivyRepository = repositoryFactory.createIvyRepository();
        ivyRepository.setName(NPM_REPO_DEFAULT_NAME + "_" + npmArtifactRepository.getName());
        ivyRepository.patternLayout(patternLayout -> {
            patternLayout.artifact(IvyPatterns.IVY_ARTIFACT_PATTERN);
            patternLayout.ivy(IvyPatterns.IVY_DESCRIPTOR_PATTERN);
        });

        ivyRepository.metadataSources(metadataSources -> {
            metadataSources.ivyDescriptor();
            metadataSources.ignoreGradleMetadataRedirection();
        });

        ivyRepository.setUrl(npmArtifactRepository.getUrl());
        repositories.addLast(ivyRepository);
        return npmArtifactRepository;
    }

    public final NpmArtifactRepository npm(Action<? super NpmArtifactRepository> action) {
        NpmArtifactRepository npmArtifactRepository = objectFactory.newInstance(DefaultNpmArtifactRepository.class);
        action.execute(npmArtifactRepository);
        String suggestedName =
                Optional.ofNullable(npmArtifactRepository.getName()).orElse(NPM_REPO_DEFAULT_NAME);
        npmArtifactRepository.setName(suggestedName);

        IvyArtifactRepository ivyRepository = repositoryFactory.createIvyRepository();
        ivyRepository.setName(uniqueName(NPM_REPO_DEFAULT_NAME + "_" + suggestedName));
        ivyRepository.patternLayout(patternLayout -> {
            patternLayout.artifact(IvyPatterns.IVY_ARTIFACT_PATTERN);
            patternLayout.ivy(IvyPatterns.IVY_DESCRIPTOR_PATTERN);
        });

        ivyRepository.metadataSources(metadataSources -> {
            metadataSources.ivyDescriptor();
            metadataSources.ignoreGradleMetadataRedirection();
        });

        npmArtifactRepository
                .getCredentials(PasswordCredentials.class)
                .ifPresent(credentials -> ivyRepository.credentials(
                        org.gradle.api.artifacts.repositories.PasswordCredentials.class, passwordCredentials -> {
                            passwordCredentials.setUsername(credentials.getUsername());
                            passwordCredentials.setPassword(credentials.getPassword());
                        }));
        npmArtifactRepository
                .getCredentials(AuthHeaderCredentials.class)
                .ifPresent(credentials -> ivyRepository.credentials(
                        org.gradle.api.artifacts.repositories.PasswordCredentials.class, passwordCredentials -> {
                            passwordCredentials.setUsername(AuthHeaderCredentials.PASSWORD_CREDS_USERNAME);
                            passwordCredentials.setPassword(credentials.getToken());
                        }));

        NpmArtifactoryShim.ShimServer shimServer = ShimManager.getOrCreateShim(gradle, npmArtifactRepository.getUrl());

        ivyRepository.setUrl(shimServer.getUri());
        ivyRepository.setAllowInsecureProtocol(true);
        repositories.addLast(ivyRepository);
        return npmArtifactRepository;
    }

    private String uniqueName(String suggestedName) {
        if (repositories.findByName(suggestedName) == null) {
            return suggestedName;
        }

        for (int index = 2; true; index++) {
            String candidate = suggestedName + index;
            if (repositories.findByName(candidate) == null) {
                return candidate;
            }
        }
    }
}
