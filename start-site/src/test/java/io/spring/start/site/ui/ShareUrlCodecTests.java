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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures the {@code #!} share-URL contract matches what the React UI documents in
 * {@code USING.adoc}.
 *
 * @author Vaadin UI Migration
 */
class ShareUrlCodecTests {

	@Test
	void encodeProducesHashBangFormat() {
		InitializrFormModel model = new InitializrFormModel();
		model.setType("maven-project");
		model.setLanguage("kotlin");
		model.setBootVersion("3.5.0");
		model.setGroupId("org.acme");
		model.setArtifactId("widget");
		model.setDependencies(new LinkedHashSet<>(java.util.List.of("web", "devtools")));
		String fragment = ShareUrlCodec.encode(model);
		assertThat(fragment).startsWith("#!");
		assertThat(fragment).contains("type=maven-project");
		assertThat(fragment).contains("language=kotlin");
		assertThat(fragment).contains("bootVersion=3.5.0");
		assertThat(fragment).contains("groupId=org.acme");
		assertThat(fragment).contains("artifactId=widget");
		assertThat(fragment).contains("dependencies=web%2Cdevtools");
	}

	@Test
	void decodeRoundtripsAllFields() {
		InitializrFormModel original = new InitializrFormModel();
		original.setType("gradle-project");
		original.setLanguage("java");
		original.setBootVersion("3.4.5");
		original.setGroupId("com.example");
		original.setArtifactId("demo");
		original.setName("Demo");
		original.setDescription("A test");
		original.setPackageName("com.example.demo");
		original.setPackaging("war");
		original.setJavaVersion("21");
		original.setConfigurationFileFormat("yaml");
		original.setDependencies(new LinkedHashSet<>(java.util.List.of("web", "actuator", "data-jpa")));

		String fragment = ShareUrlCodec.encode(original);
		InitializrFormModel restored = new InitializrFormModel();
		boolean applied = ShareUrlCodec.decodeInto(fragment, restored);
		assertThat(applied).isTrue();
		assertThat(restored.getType()).isEqualTo("gradle-project");
		assertThat(restored.getLanguage()).isEqualTo("java");
		assertThat(restored.getBootVersion()).isEqualTo("3.4.5");
		assertThat(restored.getGroupId()).isEqualTo("com.example");
		assertThat(restored.getArtifactId()).isEqualTo("demo");
		assertThat(restored.getName()).isEqualTo("Demo");
		assertThat(restored.getDescription()).isEqualTo("A test");
		assertThat(restored.getPackageName()).isEqualTo("com.example.demo");
		assertThat(restored.getPackaging()).isEqualTo("war");
		assertThat(restored.getJavaVersion()).isEqualTo("21");
		assertThat(restored.getConfigurationFileFormat()).isEqualTo("yaml");
		assertThat(restored.getDependencies()).containsExactly("web", "actuator", "data-jpa");
	}

	@Test
	void decodeReturnsFalseForBlankAndPlainHash() {
		InitializrFormModel model = new InitializrFormModel();
		assertThat(ShareUrlCodec.decodeInto(null, model)).isFalse();
		assertThat(ShareUrlCodec.decodeInto("", model)).isFalse();
		assertThat(ShareUrlCodec.decodeInto("#", model)).isFalse();
		assertThat(ShareUrlCodec.decodeInto("#!", model)).isFalse();
	}

	@Test
	void decodeIgnoresUnknownKeys() {
		InitializrFormModel model = new InitializrFormModel();
		model.setGroupId("untouched");
		boolean applied = ShareUrlCodec.decodeInto("#!unknown=garbage&type=maven-project", model);
		assertThat(applied).isTrue();
		assertThat(model.getType()).isEqualTo("maven-project");
		assertThat(model.getGroupId()).isEqualTo("untouched");
	}

}
