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

package com.gradlets.gradle.typescript.conjure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.gradlets.gradle.typescript.SourceSet;
import com.gradlets.gradle.typescript.TypeScriptPlugin;
import com.gradlets.gradle.typescript.TypeScriptPluginExtension;
import com.gradlets.gradle.typescript.pdeps.TypeScriptProductDependenciesPlugin;
import com.palantir.conjure.java.serialization.ObjectMappers;
import com.palantir.gradle.dist.ProductDependency;
import com.palantir.gradle.dist.RecommendedProductDependenciesExtension;
import com.palantir.gradle.dist.RecommendedProductDependenciesPlugin;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.TaskProvider;

public final class ConjureTypeScriptLocalCodegenPlugin implements Plugin<Project> {
    private static final ObjectMapper OBJECT_MAPPER = ObjectMappers.newClientObjectMapper();
    private static final String CONJURE_CONFIGURATION = "conjure";
    private static final Pattern DEFINITION_NAME =
            Pattern.compile("(.*)-([0-9]+\\.[0-9]+\\.[0-9]+(?:-rc[0-9]+)?(?:-[0-9]+-g[a-f0-9]+)?)(\\.conjure)?\\.json");

    private static final String CONJURE_TYPESCRIPT_CONFIGURATION = "conjureTypeScript";
    private static final String CONJURE_TYPESCRIPT_BINARY = "com.palantir.conjure.typescript:conjure-typescript";
    private static final String TYPESCRIPT_GIT_IGNORE = "/src/main/typescript/";

    @Override
    public void apply(Project project) {
        Configuration conjureIrConfiguration = project.getConfigurations().create(CONJURE_CONFIGURATION);
        TaskProvider<Copy> extractConjureIr = project.getTasks().register("extractConjureIr", Copy.class, task -> {
            task.rename(DEFINITION_NAME, "$1.conjure.json");
            task.from(conjureIrConfiguration);
            task.into(project.getLayout().getBuildDirectory().dir("conjure-ir"));
        });

        TaskProvider<ExtractExecutableTask> extractConjureTypeScript = createExtractConjureTypeScript(project);
        setupSubprojects(project, extractConjureTypeScript, extractConjureIr, conjureIrConfiguration);
    }

    private static TaskProvider<ExtractExecutableTask> createExtractConjureTypeScript(Project project) {
        Configuration conjureTypeScriptConfig = project.getConfigurations()
                .create(CONJURE_TYPESCRIPT_CONFIGURATION, config -> {
                    config.setVisible(false);
                    config.setCanBeConsumed(false);
                    config.setCanBeResolved(true);
                });
        project.getDependencies().add(CONJURE_TYPESCRIPT_CONFIGURATION, CONJURE_TYPESCRIPT_BINARY);
        return project.getTasks().register("extractConjureTypeScript", ExtractExecutableTask.class, task -> {
            task.getInputFiles().from(conjureTypeScriptConfig);
            task.setOutputDirectory(new File(project.getBuildDir(), CONJURE_TYPESCRIPT_CONFIGURATION));
            task.getExecutableName().set("conjure-typescript");
        });
    }

    private static void setupSubprojects(
            Project project,
            TaskProvider<ExtractExecutableTask> extractConjureTypeScript,
            TaskProvider<Copy> extractConjureIr,
            Configuration conjureIrConfiguration) {

        // Validating that each subproject has a corresponding definition and vice versa.
        // We do this in afterEvaluate to ensure the configuration is populated.
        project.getGradle().projectsEvaluated(_gradle -> {
            Set<String> apis = conjureIrConfiguration.getAllDependencies().stream()
                    .map(Dependency::getName)
                    .collect(ImmutableSet.toImmutableSet());

            Set<String> missingProjects =
                    Sets.difference(apis, project.getChildProjects().keySet());
            if (!missingProjects.isEmpty()) {
                throw new RuntimeException(String.format(
                        "Discovered dependencies %s without corresponding subprojects.", missingProjects));
            }
            Set<String> missingApis = Sets.difference(project.getChildProjects().keySet(), apis);
            if (!missingApis.isEmpty()) {
                throw new RuntimeException(
                        String.format("Discovered subprojects %s without corresponding dependencies.", missingApis));
            }
        });

        project.getChildProjects()
                .forEach((_name, subproject) ->
                        configureSubproject(subproject, extractConjureTypeScript, extractConjureIr));
    }

    private static void configureSubproject(
            Project subproject,
            TaskProvider<ExtractExecutableTask> extractConjureTypeScript,
            TaskProvider<Copy> extractConjureIr) {
        subproject.getPluginManager().apply(TypeScriptPlugin.class);
        subproject.getPluginManager().apply(RecommendedProductDependenciesPlugin.class);
        subproject.getPluginManager().apply(TypeScriptProductDependenciesPlugin.class);

        // TODO(forozco): generate into "generated" source set
        SourceSet sourceSet = subproject
                .getExtensions()
                .getByType(TypeScriptPluginExtension.class)
                .getSourceSets()
                .getByName("main");

        subproject.getDependencies().add(sourceSet.getCompileConfigurationName(), "npm:conjure-client");

        TaskProvider<?> generateGitIgnore = subproject
                .getTasks()
                .register("gitignoreConjure", WriteGitignoreTask.class, task -> {
                    task.getOutputFile().set(subproject.file(".gitignore"));
                    task.getContents().set(TYPESCRIPT_GIT_IGNORE);
                });

        Provider<RegularFile> conjureIrFile = extractConjureIr.map(irTask -> subproject
                .getLayout()
                .getProjectDirectory()
                .file(new File(irTask.getDestinationDir(), subproject.getName() + ".conjure.json").getAbsolutePath()));

        subproject
                .getExtensions()
                .getByType(RecommendedProductDependenciesExtension.class)
                .getRecommendedProductDependenciesProvider()
                .addAll(conjureIrFile.map(ConjureTypeScriptLocalCodegenPlugin::extractProductDependencies));

        subproject.getTasks().named("generatePackageJson", task -> {
            task.dependsOn(extractConjureIr);
        });

        TaskProvider<ConjureTypeScriptLocalGeneratorTask> generateConjureTypeScript = subproject
                .getTasks()
                .register("generateConjureTypeScript", ConjureTypeScriptLocalGeneratorTask.class, task -> {
                    task.getInputFile().set(conjureIrFile);
                    task.getExecutablePath()
                            .set(extractConjureTypeScript
                                    .flatMap(ExtractExecutableTask::getExecutable)
                                    .map(OsUtils::appendDotBatIfWindows));
                    task.getOutputDirectory().set(subproject.file("src/main/typescript"));
                    task.dependsOn(extractConjureTypeScript, extractConjureIr, generateGitIgnore);
                });

        subproject
                .getTasks()
                .named(sourceSet.getCompileTypeScriptTaskName())
                .configure(compileTypeScript -> compileTypeScript.dependsOn(generateConjureTypeScript));

        subproject.getTasks().named("createTsConfig").configure(t -> t.dependsOn(generateConjureTypeScript));

        subproject.getPluginManager().withPlugin("idea", _plugin -> {
            subproject.getTasks().named("ideaModule", task -> {
                task.dependsOn(generateConjureTypeScript);
            });
        });

        subproject.getPluginManager().withPlugin("com.gradlets.eslint", _plugin -> {
            subproject.getTasks().named("eslintMain", task -> {
                task.setEnabled(false);
            });
        });
    }

    private static Set<ProductDependency> extractProductDependencies(RegularFile irFile) {
        try {
            MinimalConjureDefinition conjureDefinition =
                    OBJECT_MAPPER.readValue(irFile.getAsFile(), MinimalConjureDefinition.class);
            return conjureDefinition
                    .extensions()
                    .map(MinimalConjureDefinition.Extensions::productDependencies)
                    .orElseGet(Collections::emptySet);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse conjure definition", e);
        }
    }
}
