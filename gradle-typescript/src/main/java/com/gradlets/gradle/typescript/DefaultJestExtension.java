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
import org.gradle.api.provider.Property;

public class DefaultJestExtension implements JestExtension {
    private static final String EXTENSION_NAME = "jest";

    private final Property<String> preset;

    public DefaultJestExtension(ObjectFactory objects) {
        preset = objects.property(String.class).value("ts-jest");
    }

    static JestExtension register(Project project) {
        return project.getExtensions()
                .create(JestExtension.class, EXTENSION_NAME, DefaultJestExtension.class, project.getObjects());
    }

    @Override
    public final Property<String> getPreset() {
        return preset;
    }
}
