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

import com.palantir.logsafe.exceptions.SafeRuntimeException;
import java.lang.reflect.Field;
import java.util.Map;

final class EnvironmentVariablesImpl implements EnvironmentVariables {

    private final Map<String, String> originalEnv;

    EnvironmentVariablesImpl(Map<String, String> originalEnv) {
        this.originalEnv = originalEnv;
    }

    void restore() {
        restoreVariables(getEditableMapOfVariables(), originalEnv);
        restoreVariables(getTheCaseInsensitiveEnvironment(), originalEnv);
    }

    @Override
    public EnvironmentVariables set(String name, String value) {
        set(getEditableMapOfVariables(), name, value);
        set(getTheCaseInsensitiveEnvironment(), name, value);
        return this;
    }

    private void set(Map<String, String> variables, String name, String value) {
        if (variables != null) {
            if (value == null) {
                variables.remove(name);
            } else {
                variables.put(name, value);
            }
        }
    }

    private void restoreVariables(Map<String, String> variables, Map<String, String> originalVariables) {
        if (variables != null) { // theCaseInsensitiveEnvironment may be null
            variables.clear();
            variables.putAll(originalVariables);
        }
    }

    private static Map<String, String> getEditableMapOfVariables() {
        Class<?> classOfMap = System.getenv().getClass();
        try {
            return getFieldValue(classOfMap, System.getenv(), "m");
        } catch (IllegalAccessException e) {
            throw new SafeRuntimeException("System Rules cannot access the field 'm' of the map System.getenv().", e);
        } catch (NoSuchFieldException e) {
            throw new SafeRuntimeException(
                    "System Rules expects System.getenv() to have a field 'm' but it has not.", e);
        }
    }

    /*
     * The names of environment variables are case-insensitive in Windows.
     * Therefore it stores the variables in a TreeMap named
     * theCaseInsensitiveEnvironment.
     */
    private static Map<String, String> getTheCaseInsensitiveEnvironment() {
        try {
            Class<?> processEnvironment = Class.forName("java.lang.ProcessEnvironment");
            return getFieldValue(processEnvironment, null, "theCaseInsensitiveEnvironment");
        } catch (ClassNotFoundException e) {
            throw new SafeRuntimeException(
                    "System Rules expects the existence of"
                            + " the class java.lang.ProcessEnvironment but it does not"
                            + " exist.",
                    e);
        } catch (IllegalAccessException e) {
            throw new SafeRuntimeException(
                    "System Rules cannot access the static"
                            + " field 'theCaseInsensitiveEnvironment' of the class"
                            + " java.lang.ProcessEnvironment.",
                    e);
        } catch (NoSuchFieldException e) {
            // this field is only available for Windows
            return null;
        }
    }

    private static Map<String, String> getFieldValue(Class<?> klass, Object object, String name)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = klass.getDeclaredField(name);
        field.setAccessible(true);
        return (Map<String, String>) field.get(object);
    }
}
