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

import java.util.Set;
import org.gradle.api.Project;

public final class JestTestSupport {
    private static final Set<String> COORDINATES_TO_CONSTRAINT = Set.of(
            "npm:types/node:14.6.2",
            "npm:types/istanbul-lib-report:3.0.0",
            "npm:types/yargs-parser:20.2.0",
            "npm:types/babel__generator:7.6.2",
            "npm:types/babel__template:7.4.0",
            "npm:inherits:2.0.4",
            "npm:wrappy:1.0.2",
            "npm:source-map-support:0.5.19",
            "npm:bs-logger:0.2.6",
            "npm:fast-json-stable-stringify:2.1.0",
            "npm:semver:7.3.5",
            "npm:json5:2.2.0",
            "npm:make-error:1.3.6",
            "npm:mkdirp:1.0.4",
            "npm:yargs-parser:20.2.7",
            "npm:lodash:4.17.21",
            "npm:buffer-from:1.1.1");

    private static final Set<String> TYPES_COORDINATES_TO_CONSTRAINT =
            Set.of("npm:types/yargs-parser:20.2.0", "npm:types/istanbul-lib-report:3.0.0");

    private JestTestSupport() {}

    static void addDependencyConstraints(Project project, SourceSet testSourceSet) {
        project.getConfigurations()
                .named(testSourceSet.getCompileConfigurationName())
                .configure(conf -> {
                    COORDINATES_TO_CONSTRAINT.forEach(coord -> conf.getDependencyConstraints()
                            .add(project.getDependencies().getConstraints().create(coord)));
                });
        project.getConfigurations()
                .named(testSourceSet.getCompileTypesConfigurationName())
                .configure(conf -> {
                    TYPES_COORDINATES_TO_CONSTRAINT.forEach(coord -> conf.getDependencyConstraints()
                            .add(project.getDependencies().getConstraints().create(coord)));
                });
    }
}
