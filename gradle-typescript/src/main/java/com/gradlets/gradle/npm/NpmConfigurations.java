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

package com.gradlets.gradle.npm;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.palantir.logsafe.exceptions.SafeIllegalStateException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NpmConfigurations {
    private static final Logger log = LoggerFactory.getLogger(NpmConfigurations.class);
    private static final Pattern VERSION_PATTERN =
            Pattern.compile("^(?<baseName>.*?)-(\\d+(?:\\.\\d+)*(?:-\\d+)?(?:-g[0-9a-f]+)?(?:\\.dirty)?)$");
    private static final Splitter TYPES_SPLITTER = Splitter.on("__");

    @VisibleForTesting
    static String extractBaseName(String fileName) {
        Matcher matcher = VERSION_PATTERN.matcher(fileName);

        String baseName;
        if (matcher.matches()) {
            baseName = matcher.group("baseName");
        } else {
            log.debug("Couldn't match version through regex, falling back to basic handler. File: {}", fileName);

            baseName = fileName.substring(0, fileName.lastIndexOf("-"));
        }

        return baseName;
    }

    public static Optional<String> getTypesPackageNameFromArtifact(Path artifact) {
        if (artifact.getParent().getFileName().toString().equals("@types")) {
            List<String> components =
                    TYPES_SPLITTER.splitToList(artifact.getFileName().toString());
            if (components.size() == 2) {
                return Optional.of("@" + String.join("/", components));
            } else if (components.size() == 1) {
                return Optional.of(Iterables.getOnlyElement(components));
            }
            throw new SafeIllegalStateException("This should never happen");
        }
        return Optional.empty();
    }

    public static String getPackageNameFromArtifact(Path artifact) {
        Path maybeSha = artifact.getParent().getParent();
        if (ArtifactLayout.isChecksumDirOrTransformed(maybeSha.getFileName().toString())) {
            return artifact.getFileName().toString();
        }

        return artifact.getParent().getFileName() + "/" + artifact.getFileName();
    }

    private NpmConfigurations() {}
}
