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

import com.google.common.collect.ImmutableMap;
import com.gradlets.gradle.typescript.TypeScriptPluginExtension;
import java.util.List;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class BaselineTsc implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        TypeScriptPluginExtension ext = project.getExtensions().getByType(TypeScriptPluginExtension.class);
        ext.getCompilerOptions()
                .putAll(ImmutableMap.<String, Object>builder()
                        .put("lib", List.of("ES2015", "DOM"))
                        .put("sourceMap", true)
                        .put("declarationMap", true)
                        .put("forceConsistentCasingInFileNames", true)
                        .put("moduleResolution", "node")
                        .put("noImplicitAny", true)
                        .put("noImplicitReturns", true)
                        .put("noImplicitThis", true)
                        .put("noUnusedLocals", true)
                        .put("preserveConstEnums", true)
                        .put("strict", true)
                        .put("strictNullChecks", true)
                        .put("suppressImplicitAnyIndexErrors", true)
                        .put("module", "es6")
                        .put("target", "es6")
                        .buildOrThrow());
    }
}
