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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.gradlets.gradle.typescript.eslint.EslintExtension;
import com.gradlets.gradle.typescript.eslint.EslintPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public final class BaselineLintPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(EslintPlugin.class);
        EslintExtension ext = project.getExtensions().getByType(EslintExtension.class);
        addLintDependencies(project);
        ext.getInheritedConfig().add("plugin:prettier/recommended");
        ext.getRules()
                .put(
                        "prettier/prettier",
                        ImmutableList.of(
                                "error",
                                ImmutableMap.builder()
                                        .put("printWidth", 120)
                                        .put("tabWidth", 4)
                                        .put("trailingComma", "all")
                                        .put("arrowParens", "avoid")
                                        .buildOrThrow()));
    }

    private static void addLintDependencies(Project project) {
        Configuration eslint = project.getConfigurations().getByName("eslint");
        project.getDependencies().add("eslint", "npm:eslint-config-prettier:7.2.0");
        project.getDependencies().add("eslint", "npm:eslint-plugin-prettier:3.3.1");
        project.getDependencies().add("eslint", "npm:prettier:2.2.1");

        eslint.getDependencyConstraints()
                .add(project.getDependencies().getConstraints().create("npm:inherits:2.0.4"));
        eslint.getDependencyConstraints()
                .add(project.getDependencies().getConstraints().create("npm:wrappy:1.0.2"));
    }
}
