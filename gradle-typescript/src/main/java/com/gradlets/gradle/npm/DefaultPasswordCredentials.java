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

public class DefaultPasswordCredentials implements PasswordCredentials {
    private String username;
    private String password;

    public DefaultPasswordCredentials() {}

    @Override
    public final String getUsername() {
        return username;
    }

    @Override
    public final void setUsername(String username) {
        this.username = username;
    }

    @Override
    public final void username(String newUsername) {
        this.username = newUsername;
    }

    @Override
    public final String getPassword() {
        return password;
    }

    @Override
    public final void setPassword(String password) {
        this.password = password;
    }

    @Override
    public final void password(String newPassword) {
        this.password = newPassword;
    }
}
