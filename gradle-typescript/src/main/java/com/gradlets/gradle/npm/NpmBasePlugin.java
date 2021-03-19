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

import com.gradlets.gradle.typescript.TypeScriptAttributes;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.IvyArtifactRepository;
import org.gradle.api.internal.artifacts.ArtifactAttributes;
import org.gradle.api.internal.plugins.DslObject;

public class NpmBasePlugin implements Plugin<Project> {
    @Override
    public final void apply(Project project) {
        NpmExtension npmExtension = project.getObjects().newInstance(NpmExtension.class);
        project.getDependencies().registerTransform(NpmArtifactTransformAction.class, variantTransform -> {
            variantTransform.getFrom().attribute(ArtifactAttributes.ARTIFACT_FORMAT, "tgz");
            variantTransform.getTo().attribute(ArtifactAttributes.ARTIFACT_FORMAT, TypeScriptAttributes.MODULE);
        });
        new DslObject(project.getRepositories()).getConvention().getPlugins().put("npm", npmExtension);

        project.getRepositories().forEach(NpmBasePlugin::configureRepository);
        project.getRepositories().whenObjectAdded(NpmBasePlugin::configureRepository);
    }

    private static void configureRepository(ArtifactRepository repository) {
        if (!((repository instanceof IvyArtifactRepository)
                && repository.getName().startsWith(NpmExtension.NPM_REPO_DEFAULT_NAME))) {
            repository.content(content -> content.excludeGroup("npm"));
        }
    }

    public static Configuration createConfiguration(ConfigurationContainer configurations, String configurationName) {
        return configurations.create(configurationName, conf -> {
            // Specify we consume modules so that that we properly apply the artifact transformer from tgz -> module
            conf.attributes(attributeContainer ->
                    attributeContainer.attribute(ArtifactAttributes.ARTIFACT_FORMAT, TypeScriptAttributes.MODULE));

            // TODO(forozco): instead of adding dependencies with "dependencies" create an extension that performs the
            conf.getDependencies().whenObjectAdded(dependency -> {
                if (dependency instanceof ExternalModuleDependency) {
                    ((ExternalModuleDependency) dependency).artifact(art -> {
                        art.setName(dependency.getName());
                        art.setExtension("tgz");
                        art.setType("tgz");
                    });
                }
            });
        });
    }
}
