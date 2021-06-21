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

import javax.inject.Inject;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;

public class WebpackExtension {
    private static final String NAME = "webpack";

    static WebpackExtension create(Project project) {
        return project.getExtensions().create(NAME, WebpackExtension.class, project);
    }

    private final Project project;
    private final RegularFileProperty configFile;
    private final RegularFileProperty devServerConfigFile;
    private final DirectoryProperty outputDir;

    @Inject
    public WebpackExtension(Project project) {
        this.project = project;
        this.configFile = project.getObjects().fileProperty();
        this.devServerConfigFile = project.getObjects().fileProperty();
        this.outputDir = project.getObjects()
                .directoryProperty()
                .value(project.getLayout().getBuildDirectory().dir("webpack"));
    }

    public final RegularFileProperty getConfigFile() {
        return configFile;
    }

    public final RegularFileProperty getDevServerConfigFile() {
        return devServerConfigFile;
    }

    public final DirectoryProperty getOutputDir() {
        return outputDir;
    }

    public final void config(String fileName) {
        configFile.set(project.file(fileName));
    }

    public final void devServerConfig(String fileName) {
        devServerConfigFile.set(project.file(fileName));
    }
}
