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

import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

public class EslintExtension {

    private static final String NAME = "eslint";
    private static final String DEFAULT_ESLINT_VERSION = "7.20.0";

    static EslintExtension registerExtension(Project project) {
        return project.getExtensions().create(NAME, EslintExtension.class, project);
    }

    private final Property<String> toolVersion;
    private final ListProperty<String> plugins;
    private final ListProperty<String> inheritedConfig;
    private final MapProperty<String, Object> parserOptions;
    private final MapProperty<String, Object> rules;

    public EslintExtension(Project project) {
        this.toolVersion = project.getObjects().property(String.class).value(DEFAULT_ESLINT_VERSION);
        this.plugins = project.getObjects().listProperty(String.class);
        this.inheritedConfig = project.getObjects().listProperty(String.class);
        this.parserOptions = project.getObjects().mapProperty(String.class, Object.class);
        this.rules = project.getObjects().mapProperty(String.class, Object.class);
    }

    public final Property<String> getToolVersion() {
        return toolVersion;
    }

    public final ListProperty<String> getPlugins() {
        return plugins;
    }

    public final ListProperty<String> getInheritedConfig() {
        return inheritedConfig;
    }

    public final MapProperty<String, Object> getParserOptions() {
        return parserOptions;
    }

    public final MapProperty<String, Object> getRules() {
        return rules;
    }
}
