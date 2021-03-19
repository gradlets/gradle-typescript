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

package com.gradlets.gradle.typescript.eslint;

import com.gradlets.gradle.npm.NpmBasePlugin;
import com.gradlets.gradle.typescript.SourceSet;
import com.gradlets.gradle.typescript.TypeScriptPlugin;
import com.gradlets.gradle.typescript.TypeScriptPluginExtension;
import java.util.Optional;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.util.GUtil;

public class EslintPlugin implements Plugin<Project> {

    private static final String ESLINT_CONFIGURATION_NAME = "eslint";

    @Override
    public final void apply(Project project) {
        project.getPluginManager().apply(TypeScriptPlugin.class);

        EslintExtension ext = EslintExtension.registerExtension(project);
        TypeScriptPluginExtension tsExt = project.getExtensions().getByType(TypeScriptPluginExtension.class);
        Configuration eslintConfiguration = createEslintConfiguration(project, tsExt, ext);

        // Indicator task which is used to determine whether lint failures should be automatically fixed or not
        TaskProvider<Task> format = Optional.ofNullable(project.getTasks().findByName("format"))
                .map(task -> project.getTasks().named(task.getName()))
                .orElseGet(() -> project.getTasks().register("format"));

        tsExt.getSourceSets().configureEach(sourceSet -> {
            TaskProvider<EslintTask> eslintTask = createEslintTask(project, sourceSet, eslintConfiguration, ext);
            format.configure(task -> task.dependsOn(eslintTask));
            project.getTasks()
                    .named(LifecycleBasePlugin.CHECK_TASK_NAME)
                    .configure(check -> check.dependsOn(eslintTask));
        });
    }

    private static Configuration createEslintConfiguration(
            Project project, TypeScriptPluginExtension tsExt, EslintExtension ext) {
        Configuration configuration =
                NpmBasePlugin.createConfiguration(project.getConfigurations(), ESLINT_CONFIGURATION_NAME);
        configuration.setVisible(false);
        configuration.setCanBeConsumed(false);
        project.getGradle().projectsEvaluated(_gradle -> {
            project.getDependencies()
                    .add(
                            ESLINT_CONFIGURATION_NAME,
                            "npm:eslint:" + ext.getToolVersion().get());
            project.getDependencies().add(ESLINT_CONFIGURATION_NAME, "npm:typescript-eslint/parser:4.15.0");
            project.getDependencies()
                    .add(
                            ESLINT_CONFIGURATION_NAME,
                            "npm:typescript:" + tsExt.getSourceCompatibility().get());
        });
        return configuration;
    }

    private static TaskProvider<EslintTask> createEslintTask(
            Project project, SourceSet sourceSet, Configuration eslintConfiguration, EslintExtension ext) {
        String name = GUtil.toLowerCamelCase("eslint " + sourceSet.getName());
        return project.getTasks().register(name, EslintTask.class, task -> {
            task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);

            task.getShouldFix().set(project.provider(() -> project.getGradle()
                    .getTaskGraph()
                    .hasTask(project.getTasks().getByName("format"))));
            task.getParserOptions().set(ext.getParserOptions());
            task.getPlugins().set(ext.getPlugins());
            task.getInheritedConfig().set(ext.getInheritedConfig());
            task.getRules().set(ext.getRules());

            task.getPluginClasspath().from(eslintConfiguration);
            task.getCompileClasspath().from(sourceSet.getCompileClasspath());
            task.source(sourceSet.getSource());
        });
    }
}
