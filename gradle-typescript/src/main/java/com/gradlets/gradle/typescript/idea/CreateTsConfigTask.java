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
import com.gradlets.gradle.typescript.TypeScriptConfigs;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
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
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getTypeRoots();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getCompileClasspath();

    @OutputFile
    public final RegularFileProperty getTsConfig() {
        return tsConfig;
    }

    @TaskAction
    public final void generateTsConfig() throws IOException {
        Set<File> externalDependencyFiles = getCompileClasspath()
                .filter(element -> !isProjectDependency(element))
                .getFiles();

        Map<String, List<Path>> projectDepsFiles = getCompileClasspath().getFiles().stream()
                .filter(this::isProjectDependency)
                .map(File::toPath)
                .collect(Collectors.groupingBy(
                        this::getProjectName,
                        Collectors.mapping(Path::toAbsolutePath, Collectors.toUnmodifiableList())));

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
                        projectDepsFiles,
                        externalDependencyFiles,
                        getTypeRoots().getFiles(),
                        getCompilerOptions().get()));
    }

    private String getProjectName(Path scriptPath) {
        String projectPath = getRootProjectDir()
                .relativize(scriptPath.getParent().getParent().getParent())
                .toString();
        return Optional.ofNullable(getProject().getRootProject().findProject(projectPath.replace("/", ":")))
                .map(Project::getName)
                .orElseThrow();
    }

    private boolean isProjectDependency(File file) {
        return file.toPath().startsWith(getRootProjectDir());
    }

    private Path getRootProjectDir() {
        return getProject().getRootProject().getProjectDir().toPath();
    }
}
