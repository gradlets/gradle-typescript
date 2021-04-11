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

package com.gradlets.gradle.typescript;

import com.gradlets.gradle.npm.NpmBasePlugin;
import com.gradlets.gradle.typescript.idea.CreateTsConfigTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.Directory;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.artifacts.ArtifactAttributes;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.ReportingBasePlugin;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

public final class TypeScriptBasePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(BasePlugin.class);
        project.getPluginManager().apply(NpmBasePlugin.class);
        project.getPluginManager().apply(ReportingBasePlugin.class);

        TypeScriptPluginExtension typeScriptPluginExtension = addExtensions(project);
        configureSourceSetDefaults(project, typeScriptPluginExtension);
    }

    private TypeScriptPluginExtension addExtensions(Project project) {
        TypeScriptPluginExtension typeScriptPluginExtension = DefaultTypeScriptPluginExtension.register(project);
        project.getExtensions()
                .add(SourceSetContainer.class, "typeScriptSourceSets", typeScriptPluginExtension.getSourceSets());
        return typeScriptPluginExtension;
    }

    private static void configureSourceSetDefaults(
            Project project, TypeScriptPluginExtension typeScriptPluginExtension) {
        typeScriptPluginExtension.getSourceSets().configureEach(sourceSet -> {
            defineConfigurationsForSourceSet(sourceSet, project.getConfigurations(), project.getObjects());
            definePathsForSourceSet(sourceSet);

            configureOutputDirectoryForSourceSet(sourceSet, sourceSet.getSource(), project);
            createCompileTask(project, sourceSet, typeScriptPluginExtension);
            createAssembleTask(sourceSet, project);
            createTsConfigTaskForSourceSet(project, sourceSet, typeScriptPluginExtension.getCompilerOptions());
        });
    }

    private static void definePathsForSourceSet(SourceSet sourceSet) {
        sourceSet.getSource().srcDir("src/" + sourceSet.getName() + "/typescript");
    }

    private static void defineConfigurationsForSourceSet(
            SourceSet sourceSet, ConfigurationContainer configurations, ObjectFactory objectFactory) {
        Configuration compileConfiguration =
                NpmBasePlugin.createConfiguration(configurations, sourceSet.getCompileConfigurationName());
        Configuration compileTypesConfiguration =
                NpmBasePlugin.createConfiguration(configurations, sourceSet.getCompileTypesConfigurationName());
        Configuration apiElements = configurations.create(sourceSet.getApiElementsConfigurationName());

        compileConfiguration.setDescription("Dependencies for " + sourceSet.getName());
        compileConfiguration.setCanBeResolved(true);
        compileConfiguration.setCanBeConsumed(false);
        compileConfiguration
                .getAttributes()
                .attribute(
                        Usage.USAGE_ATTRIBUTE, objectFactory.named(Usage.class, TypeScriptAttributes.TYPESCRIPT_API));
        compileConfiguration
                .getAttributes()
                .attribute(Category.CATEGORY_ATTRIBUTE, objectFactory.named(Category.class, Category.LIBRARY));
        compileConfiguration
                .getAttributes()
                .attribute(
                        LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                        objectFactory.named(LibraryElements.class, TypeScriptAttributes.MODULE));
        // TODO(forozco): propogate type dependencies
        sourceSet.getCompileClasspath().from(compileConfiguration);

        compileTypesConfiguration.setDescription("Type dependencies for " + sourceSet.getName());
        compileTypesConfiguration.setCanBeResolved(true);
        compileTypesConfiguration.setCanBeConsumed(false);
        compileTypesConfiguration
                .getAttributes()
                .attribute(Bundling.BUNDLING_ATTRIBUTE, objectFactory.named(Bundling.class, Bundling.EXTERNAL));

        // TODO(forozco): add consumable configuration which should only have the the produce artifact on it
        // We can configure the artifact in typescriptPlugin like we Java does
        apiElements.setCanBeConsumed(true);
        apiElements.setCanBeResolved(false);
    }

    private static Provider<TypeScriptCompile> createCompileTask(
            Project project, SourceSet sourceSet, TypeScriptPluginExtension typeScriptPluginExtension) {
        return project.getTasks().register(sourceSet.getCompileTypeScriptTaskName(), TypeScriptCompile.class, task -> {
            task.setGroup(LifecycleBasePlugin.BUILD_GROUP);
            task.setDescription("Compiles " + sourceSet.getName());
            task.getClasspath().from(sourceSet.getCompileClasspath());
            task.getOutputDir().set(sourceSet.getSource().getClassesDirectory());
            task.getCompilerOptions().value(typeScriptPluginExtension.getCompilerOptions());
            task.getTypeRoots().from(project.getConfigurations().named(sourceSet.getCompileTypesConfigurationName()));

            task.getTypeScriptVersion().set(typeScriptPluginExtension.getSourceCompatibility());
            task.source(sourceSet.getSource());
        });
    }

    private static void createTsConfigTaskForSourceSet(
            Project project, SourceSet sourceSet, MapProperty<String, Object> compilerOptions) {
        project.getTasks()
                .register(sourceSet.getCreateTsConfigTaskName(), CreateTsConfigTask.class, createTsConfigTask -> {
                    createTsConfigTask.setGroup(LifecycleBasePlugin.BUILD_TASK_NAME);
                    createTsConfigTask.setDescription("Creates vscode tsconfig.json for " + sourceSet.getName());
                    createTsConfigTask
                            .getCompileClasspath()
                            .from(project.getConfigurations()
                                    .getByName(sourceSet.getCompileConfigurationName())
                                    .getIncoming()
                                    .artifactView(v -> v.getAttributes()
                                            .attribute(
                                                    LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                                                    project.getObjects()
                                                            .named(
                                                                    LibraryElements.class,
                                                                    TypeScriptAttributes.SOURCE_SCRIPT_DIRS))
                                            .attribute(ArtifactAttributes.ARTIFACT_FORMAT, TypeScriptAttributes.MODULE))
                                    .getArtifacts()
                                    .getArtifactFiles());
                    createTsConfigTask
                            .getSourceDirectories()
                            .set(sourceSet.getSource().getSrcDirs());
                    createTsConfigTask
                            .getOutputDir()
                            .set(sourceSet.getSource().getClassesDirectory().map(Directory::getAsFile));
                    createTsConfigTask.getTsConfigName().set(sourceSet.getName());
                    createTsConfigTask.getCompilerOptions().value(compilerOptions);
                    createTsConfigTask
                            .getTypeRoots()
                            .from(project.getConfigurations().getByName(sourceSet.getCompileTypesConfigurationName()));
                    createTsConfigTask.getTsConfig().set(project.file("src/" + sourceSet.getName() + "/tsconfig.json"));
                    createTsConfigTask.onlyIf(new Spec<Task>() {
                        @Override
                        public boolean isSatisfiedBy(Task _task) {
                            return !sourceSet.getSource().isEmpty();
                        }
                    });
                });
    }

    private static void createAssembleTask(SourceSet sourceSet, Project project) {
        TaskProvider<Task> assembleTask = project.getTasks().register(sourceSet.getAssembleClassName(), task -> {
            task.setGroup(LifecycleBasePlugin.BUILD_GROUP);
            task.setDescription("Assembles " + sourceSet.getOutput());
            task.dependsOn(sourceSet.getCompileTypeScriptTaskName());
        });
        sourceSet.compiledBy(assembleTask);
    }

    public static void configureOutputDirectoryForSourceSet(
            SourceSet sourceSet, SourceDirectorySet sourceDirectorySet, Project project) {
        String sourceSetChildPath = "scripts/" + sourceSet.getName();
        sourceDirectorySet.setOutputDir(
                project.getLayout().getBuildDirectory().dir(sourceSetChildPath).map(Directory::getAsFile));
        DefaultSourceSetOutput sourceSetOutput = sourceSet.getOutput();
        sourceSetOutput.addScriptsDirs(sourceDirectorySet::getOutputDir);
    }
}
