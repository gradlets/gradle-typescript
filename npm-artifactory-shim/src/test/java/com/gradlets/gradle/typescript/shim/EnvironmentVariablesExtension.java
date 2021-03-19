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

package com.gradlets.gradle.typescript.shim;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public final class EnvironmentVariablesExtension implements ParameterResolver, AfterEachCallback {
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(EnvironmentVariablesExtension.class);
    private static final String ENVIRONMENT_KEY = "environment";

    @Override
    public void afterEach(ExtensionContext context) {
        EnvironmentVariablesImpl environmentVariables =
                context.getStore(NAMESPACE).get(ENVIRONMENT_KEY, EnvironmentVariablesImpl.class);
        if (environmentVariables != null) {
            environmentVariables.restore();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext _extensionContext)
            throws ParameterResolutionException {
        return EnvironmentVariables.class.isAssignableFrom(
                parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(ParameterContext _parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return extensionContext
                .getStore(NAMESPACE)
                .getOrComputeIfAbsent(
                        ENVIRONMENT_KEY,
                        _unused -> new EnvironmentVariablesImpl(ImmutableMap.copyOf(System.getenv())),
                        EnvironmentVariables.class);
    }
}
