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

public final class JestTestSupport {

    private JestTestSupport() {}

    static void addDependencyConstraints(Project project, SourceSet testSourceSet) {
        project.getConfigurations()
                .named(testSourceSet.getCompileConfigurationName())
                .configure(conf -> {
                    conf.getDependencyConstraints()
                            .add(project.getDependencies().getConstraints().create("npm:types/node:14.6.2"));
                    conf.getDependencyConstraints()
                            .add(project.getDependencies()
                                    .getConstraints()
                                    .create("npm:types/istanbul-lib-report:3.0.0"));
                    conf.getDependencyConstraints()
                            .add(project.getDependencies().getConstraints().create("npm:types/yargs-parser:20.2.0"));
                    conf.getDependencyConstraints()
                            .add(project.getDependencies().getConstraints().create("npm:types/babel__generator:7.6.2"));
                    conf.getDependencyConstraints()
                            .add(project.getDependencies().getConstraints().create("npm:types/babel__template:7.4.0"));
                    conf.getDependencyConstraints()
                            .add(project.getDependencies().getConstraints().create("npm:inherits:2.0.4"));
                    conf.getDependencyConstraints()
                            .add(project.getDependencies().getConstraints().create("npm:wrappy:1.0.2"));
                    conf.getDependencyConstraints()
                            .add(project.getDependencies().getConstraints().create("npm:source-map-support:0.5.19"));
                    conf.getDependencyConstraints()
                            .add(project.getDependencies().getConstraints().create("npm:bs-logger:0.2.6"));
                    conf.getDependencyConstraints()
                            .add(project.getDependencies()
                                    .getConstraints()
                                    .create("npm:fast-json-stable-stringify:2.1.0"));
                    conf.getDependencyConstraints()
                            .add(project.getDependencies().getConstraints().create("npm:semver:7.3.5"));
                    conf.getDependencyConstraints()
                            .add(project.getDependencies().getConstraints().create("npm:json5:2.2.0"));
                    conf.getDependencyConstraints()
                            .add(project.getDependencies().getConstraints().create("npm:make-error:1.3.6"));
                    conf.getDependencyConstraints()
                            .add(project.getDependencies().getConstraints().create("npm:mkdirp:1.0.4"));
                    conf.getDependencyConstraints()
                            .add(project.getDependencies().getConstraints().create("npm:yargs-parser:20.2.7"));
                    conf.getDependencyConstraints()
                            .add(project.getDependencies().getConstraints().create("npm:lodash:4.17.21"));
                    conf.getDependencyConstraints()
                            .add(project.getDependencies().getConstraints().create("npm:buffer-from:1.1.1"));
                });
        project.getConfigurations()
                .named(testSourceSet.getCompileTypesConfigurationName())
                .configure(conf -> {
                    conf.getDependencyConstraints()
                            .add(project.getDependencies().getConstraints().create("npm:types/yargs-parser:20.2.0"));
                    conf.getDependencyConstraints()
                            .add(project.getDependencies()
                                    .getConstraints()
                                    .create("npm:types/istanbul-lib-report:3.0.0"));
                });
    }
}
