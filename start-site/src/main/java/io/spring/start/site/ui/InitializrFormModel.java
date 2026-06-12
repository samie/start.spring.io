/*
 * Copyright 2012 - present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.start.site.ui;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Mutable POJO bound to the Vaadin form by {@code Binder}. Mirrors the React
 * {@code Initializr.js} reducer's {@code values} shape.
 *
 * @author Vaadin UI Migration
 */
public class InitializrFormModel {

	private String type;

	private String language;

	private String bootVersion;

	private String groupId;

	private String artifactId;

	private String name;

	private String description;

	private String packageName;

	private String packaging;

	private String javaVersion;

	private String configurationFileFormat;

	private Set<String> dependencies = new LinkedHashSet<>();

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLanguage() {
		return this.language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getBootVersion() {
		return this.bootVersion;
	}

	public void setBootVersion(String bootVersion) {
		this.bootVersion = bootVersion;
	}

	public String getGroupId() {
		return this.groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getArtifactId() {
		return this.artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPackageName() {
		return this.packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getPackaging() {
		return this.packaging;
	}

	public void setPackaging(String packaging) {
		this.packaging = packaging;
	}

	public String getJavaVersion() {
		return this.javaVersion;
	}

	public void setJavaVersion(String javaVersion) {
		this.javaVersion = javaVersion;
	}

	public String getConfigurationFileFormat() {
		return this.configurationFileFormat;
	}

	public void setConfigurationFileFormat(String configurationFileFormat) {
		this.configurationFileFormat = configurationFileFormat;
	}

	public Set<String> getDependencies() {
		return this.dependencies;
	}

	public void setDependencies(Set<String> dependencies) {
		this.dependencies = (dependencies != null) ? new LinkedHashSet<>(dependencies) : new LinkedHashSet<>();
	}

}
