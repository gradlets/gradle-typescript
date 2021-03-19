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
import java.util.regex.Pattern;

public final class ArtifactLayout {
    @VisibleForTesting
    static final Pattern HEX_CHARS = Pattern.compile("^[0-9a-fA-F]+$");

    public static boolean isChecksumDirOrTransformed(String dirName) {
        return HEX_CHARS.matcher(dirName).matches() || dirName.equals("transformed");
    }

    private ArtifactLayout() {}
}
