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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Splitter;
import com.gradlets.gradle.ImmutablesStyle;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@ImmutablesStyle
public abstract class BasicAuthHeader {
    private static final Splitter COLON_SPLITTER = Splitter.on(":");

    @Value.Parameter
    public abstract String username();

    @Value.Parameter
    public abstract String password();

    /**
     * Takes the string form: "Basic [token]" and creates a new {@link BasicAuthHeader}.
     */
    @JsonCreator
    public static BasicAuthHeader of(String basicAuth) {
        String encodedAuth = new String(
                Base64.getDecoder().decode(basicAuth.startsWith("Basic ") ? basicAuth.substring(6) : basicAuth),
                StandardCharsets.UTF_8);
        List<String> splitAuth = COLON_SPLITTER.splitToList(encodedAuth);
        return ImmutableBasicAuthHeader.of(splitAuth.get(0), splitAuth.get(1));
    }

    public static BasicAuthHeader of(String username, String password) {
        return ImmutableBasicAuthHeader.of(username, password);
    }

    /**
     * Gets the string form: "Basic [token]".
     */
    @JsonValue
    @Override
    public String toString() {
        return "Basic "
                + Base64.getEncoder().encodeToString((username() + ":" + password()).getBytes(StandardCharsets.UTF_8));
    }
}
