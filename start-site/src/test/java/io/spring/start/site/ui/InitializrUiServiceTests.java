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

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for {@link InitializrUiService}, the bridge that the Vaadin UI uses to talk
 * to Initializr.
 *
 * @author Vaadin UI Migration
 */
@SpringBootTest
@ActiveProfiles("test")
class InitializrUiServiceTests {

	@Autowired
	private InitializrUiService service;

	@Test
	void defaultFormModelUsesVaadinMavenJava21Defaults() {
		InitializrFormModel model = this.service.defaultFormModel();
		assertThat(model.getType()).isEqualTo("maven-project");
		assertThat(model.getLanguage()).isEqualTo("java");
		assertThat(model.getPackaging()).isEqualTo("jar");
		assertThat(model.getJavaVersion()).isEqualTo("21");
		assertThat(model.getConfigurationFileFormat()).isEqualTo("properties");
		assertThat(model.getGroupId()).isEqualTo("org.vaadin.example");
		assertThat(model.getArtifactId()).isEqualTo("vaadin-demo");
		assertThat(model.getPackageName()).isEqualTo("org.vaadin.example");
		assertThat(model.getBootVersion()).isNotBlank();
		assertThat(model.getDependencies()).contains("vaadin");
	}

	@Test
	void generateMavenZipProducesValidZipWithPomAndApplicationClass() throws Exception {
		InitializrFormModel model = this.service.defaultFormModel();
		model.setType("maven-project");
		byte[] zip = this.service.generateZip(model);
		assertThat(zip).isNotEmpty();
		Set<String> entries = listEntries(zip);
		assertThat(entries).anyMatch((entry) -> entry.endsWith("/pom.xml"));
		assertThat(entries).anyMatch((entry) -> entry.endsWith("Application.java"));
	}

	@Test
	void exploreReturnsTextContentForCommonFiles() {
		InitializrFormModel model = this.service.defaultFormModel();
		model.setType("maven-project");
		Map<String, String> files = this.service.explore(model);
		assertThat(files).isNotEmpty();
		String pom = files.entrySet()
			.stream()
			.filter((entry) -> entry.getKey().endsWith("/pom.xml"))
			.map(Map.Entry::getValue)
			.findFirst()
			.orElseThrow();
		assertThat(pom).contains("<groupId>org.springframework.boot</groupId>");
	}

	@Test
	void dependencyGroupsIncludeAllEntriesAndTagCompatibility() {
		InitializrFormModel model = this.service.defaultFormModel();
		var groups = this.service.dependencyGroups(model.getBootVersion());
		assertThat(groups).isNotEmpty()
			.allSatisfy((group) -> assertThat(group.items()).isNotEmpty()
				.allSatisfy((entry) -> assertThat(entry.dependency()).isNotNull()));
	}

	@Test
	void dependencyGroupsFlagOutOfRangeEntriesAsInvalid() {
		String farFutureBoot = "99.99.99";
		var groups = this.service.dependencyGroups(farFutureBoot);
		boolean anyInvalid = groups.stream()
			.flatMap((group) -> group.items().stream())
			.anyMatch((entry) -> !entry.valid() && entry.invalidMessage() != null
					&& entry.invalidMessage().startsWith("Requires Spring Boot "));
		assertThat(anyInvalid).as("at least one dep should be tagged invalid for boot version %s", farFutureBoot)
			.isTrue();
	}

	@Test
	void incompatibilityHintReturnsNullWhenCompatible() {
		InitializrFormModel model = this.service.defaultFormModel();
		var dep = this.service.getDependency("web").orElseThrow();
		assertThat(this.service.incompatibilityHint(dep, model.getBootVersion())).isNull();
	}

	@Test
	void vaadinIsCompatibleWithSpringBoot41() {
		var vaadin = this.service.getDependency("vaadin").orElseThrow();
		assertThat(this.service.incompatibilityHint(vaadin, "4.1.0")).isNull();
	}

	private Set<String> listEntries(byte[] zip) throws Exception {
		Set<String> entries = new HashSet<>();
		try (ZipInputStream stream = new ZipInputStream(new ByteArrayInputStream(zip))) {
			for (ZipEntry entry = stream.getNextEntry(); entry != null; entry = stream.getNextEntry()) {
				entries.add(entry.getName());
			}
		}
		return entries;
	}

}
