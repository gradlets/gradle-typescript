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

package com.gradlets.gradle.typescript.pdeps;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class TypeScriptProductDependenciesPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPluginManager().withPlugin("com.palantir.recommended-product-dependencies", _plugin -> {
            Packaging.configureRecommendProductDependencies(project);
        });

        project.getPluginManager().withPlugin("com.palantir.sls-asset-distribution", _plugin -> {
            Packaging.configureDiscoveredProductDependencies(project);
        });
    }
}
