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

package com.gradlets.gradle.typescript.pdeps;

import com.fasterxml.jackson.core.type.TypeReference;
import com.gradlets.gradle.typescript.ObjectMappers;
import com.gradlets.gradle.typescript.PackageJsonExtension;
import com.gradlets.gradle.typescript.TypeScriptAttributes;
import com.palantir.gradle.dist.ProductDependency;
import com.palantir.gradle.dist.RecommendedProductDependenciesExtension;
import com.palantir.gradle.dist.asset.AssetDistributionExtension;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.internal.artifacts.ArtifactAttributes;
import org.gradle.api.tasks.TaskProvider;

public final class Packaging {
    private static final String FILE_TO_EXTRACT = "package/package.json";

    static void configureRecommendProductDependencies(Project project) {
        PackageJsonExtension packageJson = project.getExtensions().getByType(PackageJsonExtension.class);
        packageJson.setFieldValue(
                "sls",
                project.getExtensions()
                        .getByType(RecommendedProductDependenciesExtension.class)
                        .getRecommendedProductDependenciesProvider()
                        .map(pdeps -> SlsBlock.builder()
                                .putAllDependencies(pdeps.stream()
                                        .collect(Collectors.toMap(
                                                pdep -> pdep.getProductGroup() + ":" + pdep.getProductName(),
                                                pdep -> PackageJsonProductDependency.of(
                                                        pdep.getMinimumVersion(), pdep.getMaximumVersion()))))
                                .build()));
    }

    static void configureDiscoveredProductDependencies(Project project) {
        configureExternalDependencyTransform(project);

        Configuration consumableConfig = createConsumableRuntimeConfiguration(project);
        ArtifactView attributeSpecificArtifactView = consumableConfig
                .getIncoming()
                .artifactView(v -> {
                    v.getAttributes().attribute(ArtifactAttributes.ARTIFACT_FORMAT, "package-json");
                    v.getAttributes()
                            .attribute(
                                    LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                                    project.getObjects().named(LibraryElements.class, "package-json"));
                });

        TaskProvider<DiscoverProductDependenciesTask> discoverNpmProductDependencies = project.getTasks()
                .register("discoverNpmProductDependencies", DiscoverProductDependenciesTask.class, task -> {
                    task.getPackageJsonFiles()
                            .from(attributeSpecificArtifactView.getArtifacts().getArtifactFiles());
                });

        AssetDistributionExtension distExt = project.getExtensions().getByType(AssetDistributionExtension.class);
        project.getTasks().named("createManifest").configure(task -> task.dependsOn(discoverNpmProductDependencies));
        distExt.getAllProductDependencies()
                .addAll(discoverNpmProductDependencies
                        .flatMap(DiscoverProductDependenciesTask::getOutputFile)
                        .map(file -> parseProductDependencies(file.getAsFile())));
    }

    private static Configuration createConsumableRuntimeConfiguration(Project project) {
        return project.getConfigurations().create("depsForPdeps", conf -> {
            Configuration deps = project.getConfigurations().getByName("deps");
            conf.extendsFrom(deps);
            conf.setCanBeConsumed(true);
            conf.setCanBeResolved(true);
            conf.setVisible(false);
            deps.getAttributes().keySet().forEach(attributeKey -> addAttribute(conf, deps, attributeKey));
        });
    }

    private static <T> void addAttribute(Configuration conf, Configuration deps, Attribute<T> attribute) {
        conf.getAttributes().attribute(attribute, deps.getAttributes().getAttribute(attribute));
    }

    private static void configureExternalDependencyTransform(Project project) {
        project.getDependencies().registerTransform(ExtractFileFromModule.class, details -> {
            details.getParameters().getPathToExtract().set(FILE_TO_EXTRACT);

            details.getFrom().attribute(ArtifactAttributes.ARTIFACT_FORMAT, "tgz");
            details.getTo().attribute(ArtifactAttributes.ARTIFACT_FORMAT, TypeScriptAttributes.PACKAGE_JSON);

            details.getFrom()
                    .attribute(
                            LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                            project.getObjects().named(LibraryElements.class, TypeScriptAttributes.MODULE));
            details.getTo()
                    .attribute(
                            LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                            project.getObjects().named(LibraryElements.class, TypeScriptAttributes.PACKAGE_JSON));
        });
    }

    private static Set<ProductDependency> parseProductDependencies(File file) {
        try {
            return ObjectMappers.MAPPER.readValue(file, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse product dependencies", e);
        }
    }

    private Packaging() {}
}
