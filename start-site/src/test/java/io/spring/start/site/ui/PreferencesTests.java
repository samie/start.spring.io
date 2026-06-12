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

import io.spring.start.site.ui.Preferences.Entry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Preferences} snapshot / apply round-trip.
 *
 * @author Vaadin UI Migration
 */
class PreferencesTests {

	@Test
	void snapshotAndApplyRoundtripsAllFields() {
		InitializrFormModel original = new InitializrFormModel();
		original.setType("gradle-project");
		original.setLanguage("kotlin");
		original.setBootVersion("3.5.0");
		original.setGroupId("org.acme");
		original.setArtifactId("widget");
		original.setName("Widget");
		original.setDescription("A widget service");
		original.setPackageName("org.acme.widget");
		original.setPackaging("war");
		original.setJavaVersion("21");
		original.setConfigurationFileFormat("yaml");
		original.setDependencies(new LinkedHashSet<>(java.util.List.of("web", "actuator")));

		Entry entry = Preferences.snapshot("my widget", 1717000000000L, original);

		InitializrFormModel restored = new InitializrFormModel();
		Preferences.apply(entry, restored);

		assertThat(restored.getType()).isEqualTo("gradle-project");
		assertThat(restored.getLanguage()).isEqualTo("kotlin");
		assertThat(restored.getBootVersion()).isEqualTo("3.5.0");
		assertThat(restored.getGroupId()).isEqualTo("org.acme");
		assertThat(restored.getArtifactId()).isEqualTo("widget");
		assertThat(restored.getName()).isEqualTo("Widget");
		assertThat(restored.getDescription()).isEqualTo("A widget service");
		assertThat(restored.getPackageName()).isEqualTo("org.acme.widget");
		assertThat(restored.getPackaging()).isEqualTo("war");
		assertThat(restored.getJavaVersion()).isEqualTo("21");
		assertThat(restored.getConfigurationFileFormat()).isEqualTo("yaml");
		assertThat(restored.getDependencies()).containsExactly("web", "actuator");
		assertThat(entry.name()).isEqualTo("my widget");
		assertThat(entry.timestamp()).isEqualTo(1717000000000L);
	}

}
