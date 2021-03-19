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

import java.util.Optional;
import javax.inject.Inject;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.util.GUtil;

@SuppressWarnings("DesignForExtension")
public abstract class DefaultTypeScriptSourceSet implements SourceSet {

    private final SourceDirectorySet source;
    private final String baseName;
    private final String name;
    private final String displayName;
    private final ConfigurableFileCollection compileClasspath;
    private DefaultSourceSetOutput output;

    @Inject
    @SuppressWarnings({"InjectOnConstructorOfAbstractClass", "PublicConstructorForAbstractClass"})
    public DefaultTypeScriptSourceSet(String name, ObjectFactory objectFactory) {
        this.name = name;
        this.displayName = GUtil.toWords(name);
        this.source = objectFactory.sourceDirectorySet(name, "TypeScript Source Set");
        this.source.getFilter().include("**/*.ts", "**/*.tsx");
        this.source.getFilter().exclude("**/__tests__/**/*.ts", "**/__tests__/**/*.tsx");
        this.compileClasspath = objectFactory.fileCollection();

        this.baseName = name.equals("main") ? "" : GUtil.toCamelCase(name);
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public SourceDirectorySet getSource() {
        return source;
    }

    @Override
    public ConfigurableFileCollection getCompileClasspath() {
        return compileClasspath;
    }

    @Override
    public final String getCompileConfigurationName() {
        return this.configurationNameOf("deps");
    }

    @Override
    public final String getCompileTypesConfigurationName() {
        return this.configurationNameOf("types");
    }

    @Override
    public String getApiElementsConfigurationName() {
        return this.configurationNameOf("typesScriptApiElements");
    }

    @Override
    public final String getCompileTypeScriptTaskName() {
        return getTaskName(Optional.of("compile"), Optional.of("typeScript"));
    }

    @Override
    public String getCreateTsConfigTaskName() {
        return getTaskName(Optional.of("createTsConfig"), Optional.empty());
    }

    @Override
    public final String getAssembleClassName() {
        return getTaskName(Optional.empty(), Optional.of("assembleTs"));
    }

    @Override
    public final DefaultSourceSetOutput getOutput() {
        return this.output;
    }

    public void setOutput(DefaultSourceSetOutput output) {
        this.output = output;
    }

    @Override
    public void compiledBy(Object... tasks) {
        output.builtBy(tasks);
    }

    private String configurationNameOf(String configurationName) {
        return GUtil.toLowerCamelCase(baseName + " " + configurationName);
    }

    private String getTaskName(Optional<String> verb, Optional<String> target) {
        return GUtil.toLowerCamelCase(verb.orElse("") + " " + baseName + " " + target.orElse(""));
    }
}
