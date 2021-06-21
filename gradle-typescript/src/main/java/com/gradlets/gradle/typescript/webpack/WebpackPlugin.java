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

import com.gradlets.gradle.npm.NpmBasePlugin;
import com.gradlets.gradle.typescript.SourceSet;
import com.gradlets.gradle.typescript.TypeScriptAttributes;
import com.gradlets.gradle.typescript.TypeScriptPlugin;
import com.gradlets.gradle.typescript.TypeScriptPluginExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Usage;
import org.gradle.api.internal.artifacts.ArtifactAttributes;
import org.gradle.api.tasks.TaskProvider;

public final class WebpackPlugin implements Plugin<Project> {

    public static final String WEBPACK_CONFIGURATION_NAME = "webpack";
    public static final String WEBPACK_OUTGOING_CONFIGURATION_NAME = "webpackOutgoing";
    public static final String BUNDLE_WEBPACK_TASK_NAME = "bundleWebpack";
    public static final String WEBPACK_DEV_SERVER = "webpackDevServer";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(TypeScriptPlugin.class);
        SourceSet mainSourceSet = project.getExtensions()
                .getByType(TypeScriptPluginExtension.class)
                .getSourceSets()
                .getByName("main");

        Configuration webpackClasspath = createWebpackClasspath(project);
        Configuration compileClasspath =
                project.getConfigurations().getByName(mainSourceSet.getCompileConfigurationName());
        WebpackExtension webpackExtension = WebpackExtension.create(project);

        TaskProvider<WebpackTask> bundleWebpackTask = WebpackTask.register(
                project,
                BUNDLE_WEBPACK_TASK_NAME,
                webpackExtension.getOutputDir(),
                webpackExtension.getConfigFile(),
                compileClasspath,
                webpackClasspath,
                task -> {
                    task.dependsOn(mainSourceSet.getCompileTypeScriptTaskName());
                    task.getSourceFiles()
                            .from(project.getTasks().getByName(mainSourceSet.getCompileTypeScriptTaskName()));
                });

        WebpackTask.register(
                project,
                WEBPACK_DEV_SERVER,
                webpackExtension.getOutputDir(),
                webpackExtension.getDevServerConfigFile(),
                compileClasspath,
                webpackClasspath,
                task -> {
                    task.getArgs().add("serve");
                });

        createWebpackOutgoingConfiguration(project, bundleWebpackTask);
    }

    private static Configuration createWebpackClasspath(Project project) {
        Configuration webpackClasspath =
                NpmBasePlugin.createConfiguration(project.getConfigurations(), WEBPACK_CONFIGURATION_NAME);
        webpackClasspath.setVisible(false);
        webpackClasspath.setCanBeConsumed(false);
        webpackClasspath.setCanBeResolved(true);
        webpackClasspath.getAttributes().attribute(ArtifactAttributes.ARTIFACT_FORMAT, TypeScriptAttributes.MODULE);

        return webpackClasspath;
    }

    private static void createWebpackOutgoingConfiguration(
            Project project, TaskProvider<WebpackTask> bundleWebpackTask) {
        Configuration webpackOutgoingConfiguration = project.getConfigurations()
                .create(WEBPACK_OUTGOING_CONFIGURATION_NAME, conf -> {
                    conf.setCanBeResolved(false);
                    conf.setCanBeConsumed(true);
                    conf.setVisible(true);
                    conf.getAttributes()
                            .attribute(
                                    Usage.USAGE_ATTRIBUTE,
                                    project.getObjects().named(Usage.class, WebpackAttributes.WEBPACK_API));
                });
        project.getArtifacts()
                .add(
                        webpackOutgoingConfiguration.getName(),
                        bundleWebpackTask.flatMap(WebpackTask::getOutputDirectory),
                        artifact -> artifact.builtBy(bundleWebpackTask));
    }
}
