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

public interface TypeScriptAttributes {
    /** Used with {@link org.gradle.api.attributes.Usage}. */
    String TYPESCRIPT_API = "typescript-api";

    /**
     * Used with {@link org.gradle.api.internal.artifacts.ArtifactAttributes}
     * and {@link org.gradle.api.attributes.LibraryElements}.
     **/
    String MODULE = "module";

    /**
     * Ued with {@link org.gradle.api.attributes.LibraryElements}.
     */
    String PACKAGE_JSON = "package-json";

    /**
     * Ued with {@link org.gradle.api.attributes.LibraryElements}.
     */
    String SOURCE_SCRIPT_DIRS = "source-script-dirs";
}
