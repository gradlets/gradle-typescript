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

package com.gradlets.baseline.typescript;

import com.gradlets.gradle.typescript.TypeScriptPluginExtension;
import com.gradlets.gradle.typescript.webpack.WebpackExtension;
import com.gradlets.gradle.typescript.webpack.WebpackPlugin;
import com.gradlets.gradle.typescript.webpack.WebpackTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

public final class BaselineWebpack implements Plugin<Project> {
    @Override
    @SuppressWarnings("rawtypes")
    public void apply(Project project) {
        project.getPlugins().withId("com.gradlets.webpack", _plugin -> {
            applyInternal(project);
        });
    }

    private static void applyInternal(Project project) {
        TypeScriptPluginExtension tsExt = project.getExtensions().getByType(TypeScriptPluginExtension.class);
        Provider<RegularFile> configFile =
                project.getLayout().getBuildDirectory().file("webpack.config.js");

        WebpackExtension webpackExt = project.getExtensions().getByType(WebpackExtension.class);
        webpackExt.getConfigFile().set(configFile);

        TaskProvider<GenerateWebpackConfig> generateWebpackConfig = project.getTasks()
                .register("generateWebpackConfig", GenerateWebpackConfig.class, task -> {
                    task.getOutputFile().set(configFile);
                    task.getWebpackOutputDir().set(webpackExt.getOutputDir().map(dir -> dir.getAsFile()
                            .getAbsolutePath()));
                    task.getEntryPoint()
                            .set(tsExt.getSourceSets()
                                    .getByName("main")
                                    .getSource()
                                    .getDestinationDirectory()
                                    .file("index.js")
                                    .get()
                                    .getAsFile()
                                    .toPath()
                                    .toAbsolutePath()
                                    .toString());
                });

        project.getTasks().named(WebpackPlugin.BUNDLE_WEBPACK_TASK_NAME, WebpackTask.class, bundleWebpack -> {
            bundleWebpack.dependsOn(generateWebpackConfig);
        });

        configureWebpackConfiguration(project, tsExt);
    }

    private static void configureWebpackConfiguration(Project project, TypeScriptPluginExtension tsExt) {
        Configuration webpack = project.getConfigurations().getByName(WebpackPlugin.WEBPACK_CONFIGURATION_NAME);
        project.getDependencies().add(WebpackPlugin.WEBPACK_CONFIGURATION_NAME, "npm:source-map-loader:4.0.0");
        project.getDependencies().add(WebpackPlugin.WEBPACK_CONFIGURATION_NAME, "npm:webpack:5.74.0");
        project.getDependencies().add(WebpackPlugin.WEBPACK_CONFIGURATION_NAME, "npm:webpack-cli:4.7.0");

        webpack.getDependencyConstraints()
                .add(project.getDependencies().getConstraints().create("npm:types/eslint:8.4.5"));
        webpack.getDependencyConstraints()
                .add(project.getDependencies().getConstraints().create("npm:types/node:18.6.2"));
        project.afterEvaluate(_unused -> {
            project.getDependencies()
                    .add(
                            WebpackPlugin.WEBPACK_CONFIGURATION_NAME,
                            "npm:typescript:" + tsExt.getSourceCompatibility().get());
        });
    }
}
