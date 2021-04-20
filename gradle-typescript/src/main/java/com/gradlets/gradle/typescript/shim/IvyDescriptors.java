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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.gradlets.gradle.typescript.shim.clients.PackageJson;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeIllegalArgumentException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class IvyDescriptors {
    private static final String VERSION = "\\d+(?:\\.\\d+)?(?:\\.\\d+)?(?:-\\w+(?:\\.\\d+))?";
    private static final Pattern SEMVER = Pattern.compile("^[~^]?(" + VERSION + ")$");
    private static final Pattern GTE_VERSION = Pattern.compile(">?=\\s?(" + VERSION + ")");
    private static final Pattern EQUAL_TO = Pattern.compile("==\\s?(" + VERSION + ")");
    private static final Pattern LENIENT_SEMVER = Pattern.compile("^[~^]?([^>=]+)$");
    private static final ImmutableList<ConstraintAdapter> CONSTRAINT_ADAPTERS = ImmutableList.of(
            new VariableReplacingConstraintAdapter(),
            new RegexConstraintAdapter(SEMVER),
            new RegexConstraintAdapter(GTE_VERSION),
            new RegexConstraintAdapter(EQUAL_TO),
            new RegexConstraintAdapter(LENIENT_SEMVER));

    public static String createDescriptor(String group, PackageJson packageJson) {
        String sanitizedName = sanitizeName(packageJson.name());
        return "<ivy-module version=\"2.0\" xmlns:e=\"http://ant.apache.org/ivy/extra\""
                + " xmlns:m=\"http://ant.apache.org/ivy/maven\">\n"
                + String.format(
                        "<info organisation=\"%s\" module=\"%s\" revision=\"%s\"/>\n",
                        group, sanitizedName, packageJson.version())
                + "<publications>\n"
                + String.format("<artifact name=\"%s\" ext=\"tgz\" conf=\"default\" type=\"tgz\"/>\n", sanitizedName)
                + "</publications>\n"
                + createDependencies(packageJson)
                + "</ivy-module>";
    }

    private static String createDependencies(PackageJson packageJson) {
        return packageJson.dependencies().entrySet().stream()
                .map(entry -> String.format(
                        "<dependency org=\"npm\" name=\"%s\" rev=\"%s\" conf=\"default\"/>",
                        sanitizeName(entry.getKey()), sanitizeConstraint(entry.getValue())))
                .collect(Collectors.joining("\n", "<dependencies>\n", "\n</dependencies>\n"));
    }

    @VisibleForTesting
    static String sanitizeConstraint(String version) {
        return CONSTRAINT_ADAPTERS.stream()
                .flatMap(adapter -> adapter.convert(version).stream())
                .findFirst()
                .orElseThrow(() ->
                        new SafeIllegalArgumentException("Unsupported constraint", SafeArg.of("constraint", version)));
    }

    private static String sanitizeName(String name) {
        return name.startsWith("@") ? name.substring(1) : name;
    }

    private IvyDescriptors() {}

    interface ConstraintAdapter {
        Optional<String> convert(String constraint);
    }

    private static class RegexConstraintAdapter implements ConstraintAdapter {
        private final Pattern pattern;

        RegexConstraintAdapter(Pattern pattern) {
            this.pattern = pattern;
        }

        @Override
        public Optional<String> convert(String constraint) {
            Matcher matcher = pattern.matcher(constraint);
            if (matcher.find()) {
                return Optional.of(matcher.group(1));
            }
            return Optional.empty();
        }
    }

    private static final class VariableReplacingConstraintAdapter implements ConstraintAdapter {
        private static final Pattern PATTERN = Pattern.compile("^[~^]?(\\d+(?:\\.(?:\\d+|x))?(?:\\.(?:\\d+|x))?)$");

        @Override
        public Optional<String> convert(String constraint) {
            Matcher matcher = PATTERN.matcher(constraint);
            if (matcher.matches()) {
                return Optional.of(matcher.group(1).replace("x", "0"));
            }
            return Optional.empty();
        }
    }
}
