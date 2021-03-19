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

import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

public class DefaultTypeScriptPluginExtension implements TypeScriptPluginExtension {
    private static final String DEFAULT_TYPESCRIPT_VERSION = "4.2.3";
    private static final String EXTENSION_NAME = "typeScript";

    private final SourceSetContainer sourceSets;
    private final Property<String> sourceCompat;
    private final MapProperty<String, Object> compilerOptions;

    static TypeScriptPluginExtension register(Project project) {
        return project.getExtensions()
                .create(
                        TypeScriptPluginExtension.class,
                        EXTENSION_NAME,
                        DefaultTypeScriptPluginExtension.class,
                        project.getObjects());
    }

    public DefaultTypeScriptPluginExtension(ObjectFactory objects) {
        sourceSets = objects.newInstance(DefaultSourceSetContainer.class);
        sourceCompat = objects.property(String.class).convention(DEFAULT_TYPESCRIPT_VERSION);
        compilerOptions = objects.mapProperty(String.class, Object.class);
    }

    @Override
    public final SourceSetContainer getSourceSets() {
        return sourceSets;
    }

    @Override
    public final Property<String> getSourceCompatibility() {
        return sourceCompat;
    }

    @Override
    public final MapProperty<String, Object> getCompilerOptions() {
        return compilerOptions;
    }
}
