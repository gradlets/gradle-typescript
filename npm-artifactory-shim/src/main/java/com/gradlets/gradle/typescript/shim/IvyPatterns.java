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
import com.google.common.base.Splitter;
import com.palantir.logsafe.Preconditions;
import java.util.List;
import java.util.Optional;

public final class IvyPatterns {
    public static final String IVY_ARTIFACT_PATTERN = "[module]/-/[module]-[revision](.[ext])";
    public static final String IVY_DESCRIPTOR_PATTERN = "[module]/[revision]/descriptor.ivy";
    public static final String IVY_DESCRIPTOR_TEMPLATE =
            "/{packageScope}/{packageName}/{packageVersion}/descriptor.ivy";
    public static final String PACKAGE_NAME = "packageName";
    public static final String PACKAGE_VERSION = "packageVersion";

    @VisibleForTesting
    static Optional<ModuleIdentifier> parseArtifactPath(String relativePath) {
        List<String> segments = Splitter.on("/-/").splitToList(stripLeadingSlash(relativePath));
        if (segments.size() != 2) {
            return Optional.empty();
        }

        Preconditions.checkArgument(
                segments.size() == 2, "Expected request pattern '/[module]/-/[module]-[revision](.[ext])");
        String artifactName = segments.get(0);
        String version = segments.get(1)
                .substring(artifactName.length() + 1, segments.get(1).lastIndexOf("."));
        return Optional.of(
                ModuleIdentifier.of(artifactName.contains("/") ? "@" + artifactName : artifactName, version));
    }

    private static String stripLeadingSlash(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private IvyPatterns() {}
}
