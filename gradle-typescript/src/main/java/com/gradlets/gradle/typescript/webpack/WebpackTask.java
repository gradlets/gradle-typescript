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

package com.gradlets.gradle.typescript.webpack;

import com.gradlets.gradle.npm.NodeExec;
import java.nio.file.Path;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;

public abstract class WebpackTask extends DefaultTask {
    @Classpath
    abstract Property<Configuration> getWebpackClasspath();

    @Classpath
    abstract Property<Configuration> getCompileClasspath();

    @InputFiles
    abstract ConfigurableFileCollection getSourceFiles();

    @InputFile
    abstract RegularFileProperty getWebpackConfigFile();

    @OutputDirectory
    abstract DirectoryProperty getOutputDirectory();

    @Input
    abstract ListProperty<String> getArgs();

    static TaskProvider<WebpackTask> register(
            Project project,
            String name,
            WebpackExtension ext,
            Configuration compileClasspath,
            Configuration webpackClasspath,
            Action<WebpackTask> configure) {
        return project.getTasks().register(name, WebpackTask.class, task -> {
            task.getOutputDirectory().set(ext.getOutputDir());
            task.getWebpackClasspath().set(webpackClasspath);
            task.getCompileClasspath().set(compileClasspath);
            task.getWebpackConfigFile().set(ext.getConfigFile());
            task.getArgs().empty();
            configure.execute(task);
        });
    }

    @TaskAction
    final void bundle() {
        ConfigurableFileCollection unifiedClasspath = getProject().getObjects().fileCollection();
        unifiedClasspath.from(getWebpackClasspath().get(), getCompileClasspath().get());
        NodeExec.exec(getProject(), unifiedClasspath, execSpec -> {
            execSpec.executable(getWebpackExecutable(unifiedClasspath));
            execSpec.args(getArgs().get());
            execSpec.args("--config", getWebpackConfigFile().get().getAsFile().getAbsolutePath());
        });
    }

    private static Path getWebpackExecutable(FileCollection classpath) {
        return classpath.getFiles().stream()
                .filter(file -> file.getName().equals("webpack-cli"))
                .map(file -> file.toPath().resolve("bin/cli.js"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Webpack dependency must exists"));
    }
}
