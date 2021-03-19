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

import com.google.common.collect.ImmutableSet;
import com.palantir.logsafe.Preconditions;
import com.palantir.logsafe.SafeArg;
import java.util.Set;
import javax.inject.Inject;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

public class PackageJsonExtension {
    public static final String EXTENSION_NAME = "packageJson";
    private static final Set<String> reservedKeys = ImmutableSet.of("name", "version", "main", "types", "dependencies");
    private final Property<String> scope;
    private final MapProperty<String, Object> fields;

    @Inject
    public PackageJsonExtension(ObjectFactory objectFactory) {
        scope = objectFactory.property(String.class).value("");
        fields = objectFactory.mapProperty(String.class, Object.class);
    }

    public final Property<String> getScope() {
        return scope;
    }

    public final MapProperty<String, Object> getFields() {
        return fields;
    }

    public final void field(String key, Object value) {
        checkPreconditions(key);
        fields.put(key, value);
    }

    public final void setFieldValue(String key, Provider<Object> value) {
        checkPreconditions(key);
        fields.put(key, value);
    }

    public final Provider<Object> getField(String key) {
        checkPreconditions(key);
        return fields.getting(key);
    }

    private static void checkPreconditions(String key) {
        Preconditions.checkArgument(
                !reservedKeys.contains(key), "Can not specify value for key", SafeArg.of("key", key));
    }
}
