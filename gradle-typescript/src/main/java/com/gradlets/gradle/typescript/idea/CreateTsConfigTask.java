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

package com.gradlets.gradle.typescript.idea;

import com.gradlets.gradle.typescript.ObjectMappers;
import com.gradlets.gradle.typescript.PackageJsonExtension;
import com.gradlets.gradle.typescript.TypeScriptConfigs;
import com.gradlets.gradle.typescript.TypeScriptPluginExtension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ComponentResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.GFileUtils;

public abstract class CreateTsConfigTask extends DefaultTask {
    private final Property<String> tsConfigName =
            getProject().getObjects().property(String.class).value("main");
    private final RegularFileProperty tsConfig = getProject()
            .getObjects()
            .fileProperty()
            .value(getProject()
                    .getLayout()
                    .getBuildDirectory()
                    .dir("tsconfigs")
                    .map(dir -> dir.file(String.format("tsconfig-%s.json", tsConfigName.get()))));

    @Internal
    public abstract Property<Configuration> getCompileClasspath();

    @InputFiles
    public abstract SetProperty<File> getSourceDirectories();

    @Input
    public abstract Property<File> getOutputDir();

    @Input
    public abstract MapProperty<String, Object> getCompilerOptions();

    @Input
    public final Property<String> getTsConfigName() {
        return tsConfigName;
    }

    @InputFiles
    @PathSensitive(PathSensitivity.ABSOLUTE)
    public abstract ConfigurableFileCollection getTypeRoots();

    @Input
    final Set<String> getRuntimeClasspathConfig() {
        // serializable way of representing all dependencies without forcing compilation to occur in dependency projects
        return getCompileClasspath().get().getIncoming().getResolutionResult().getAllComponents().stream()
                .map(ComponentResult::getId)
                .map(ComponentIdentifier::getDisplayName)
                .collect(Collectors.toSet());
    }

    @OutputFile
    public final RegularFileProperty getTsConfig() {
        return tsConfig;
    }

    @TaskAction
    public final void generateTsConfig() throws IOException {
        Configuration classpath = getCompileClasspath().get();
        Set<File> dependenciesFiles = classpath.getIncoming().getArtifacts().getArtifacts().stream()
                .filter(resolvedArtifact ->
                        !(resolvedArtifact.getId().getComponentIdentifier() instanceof ProjectComponentIdentifier))
                .map(ResolvedArtifactResult::getFile)
                .collect(Collectors.toUnmodifiableSet());
        Map<String, List<Path>> projectDeps = classpath.getIncoming().getResolutionResult().getAllComponents().stream()
                .map(ComponentResult::getId)
                .filter(ProjectComponentIdentifier.class::isInstance)
                .map(ProjectComponentIdentifier.class::cast)
                .map(ProjectComponentIdentifier::getProjectPath)
                .map(projectPath -> getProject().findProject(projectPath))
                .filter(Objects::nonNull)
                .filter(projectDep -> projectDep.getExtensions().findByType(TypeScriptPluginExtension.class) != null
                        && projectDep.getExtensions().findByType(PackageJsonExtension.class) != null)
                .collect(Collectors.toMap(
                        projectDep -> {
                            PackageJsonExtension packageJsonExtension =
                                    projectDep.getExtensions().findByType(PackageJsonExtension.class);
                            return !packageJsonExtension.getScope().get().equals("")
                                    ? "@" + packageJsonExtension.getScope().get() + "/" + projectDep.getName()
                                    : projectDep.getName();
                        },
                        projectDep -> projectDep
                                .getExtensions()
                                .findByType(TypeScriptPluginExtension.class)
                                .getSourceSets()
                                .getByName("main")
                                .getSource()
                                .getSrcDirs()
                                .stream()
                                .map(File::toPath)
                                .collect(Collectors.toUnmodifiableList())));

        GFileUtils.parentMkdirs(tsConfig.get().getAsFile());
        File tsConfigFile = tsConfig.getAsFile().get();
        ObjectMappers.MAPPER.writeValue(
                tsConfigFile,
                TypeScriptConfigs.createTsConfigForIntellij(
                        getSourceDirectories().get().stream()
                                .map(sourceDir ->
                                        tsConfigFile.toPath().getParent().relativize(sourceDir.toPath()))
                                .collect(Collectors.toUnmodifiableSet()),
                        getOutputDir().get().toPath(),
                        projectDeps,
                        dependenciesFiles,
                        getTypeRoots().getFiles(),
                        getCompilerOptions().get()));
    }
}
