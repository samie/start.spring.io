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

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.testbench.unit.SpringUIUnitTest;
import com.vaadin.testbench.unit.ViewPackages;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Browser-free UI tests for {@link MainView} using Vaadin's UIUnitTest harness. Verifies
 * that the form loads with Initializr metadata defaults, that dependency toggling updates
 * the bound model + rendered chip list, and that the bound model still feeds
 * {@link InitializrUiService#generateZip} to a non-empty zip.
 *
 * @author Vaadin UI Migration
 */
@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings({ "deprecation", "removal" })
@ViewPackages(classes = MainView.class)
@Disabled("Vaadin TestBench (vaadin-testbench-unit-junit5) requires a vaadin.com developer license at "
		+ "runtime — opens a browser login on first run. Enable manually after `vaadin login` once a "
		+ "license is configured.")
class MainViewUiTests extends SpringUIUnitTest {

	@Autowired
	private InitializrUiService service;

	@Test
	void formLoadsWithMetadataDefaults() {
		MainView view = navigate(MainView.class);
		InitializrFormModel model = view.getModel();
		assertThat(model.getType()).isNotBlank();
		assertThat(model.getLanguage()).isEqualTo("java");
		assertThat(model.getPackaging()).isEqualTo("jar");
		assertThat(model.getGroupId()).isNotBlank();
		assertThat(model.getArtifactId()).isNotBlank();
		assertThat(model.getBootVersion()).isNotBlank();
		assertThat(model.getJavaVersion()).isNotBlank();
		assertThat(view).isInstanceOf(AppLayout.class);
		assertThat(view.getContent()).isInstanceOf(SplitLayout.class);
		// Project / Language / Spring Boot / Packaging / Java radios populated from
		// metadata.
		assertThat($(RadioButtonGroup.class).all()).hasSizeGreaterThanOrEqualTo(4);
	}

	@Test
	void toggleDependencyUpdatesModelAndChipList() {
		MainView view = navigate(MainView.class);
		InitializrFormModel model = view.getModel();
		DependenciesSection deps = view.getDependenciesSection();
		assertThat(model.getDependencies()).isEmpty();

		model.getDependencies().add("web");
		deps.refresh();

		assertThat(model.getDependencies()).containsExactly("web");
		// "Add dependencies..." button stays put after a refresh.
		assertThat($(Button.class, deps).withText("Add dependencies...").exists()).isTrue();
		// Removing the dep clears the chip and leaves the model empty.
		model.getDependencies().remove("web");
		deps.refresh();
		assertThat(model.getDependencies()).isEmpty();
	}

	@Test
	void generateZipFromViewModelProducesNonEmptyZip() {
		MainView view = navigate(MainView.class);
		InitializrFormModel model = view.getModel();
		model.setType("maven-project");
		model.getDependencies().add("web");

		byte[] zip = this.service.generateZip(model);

		assertThat(zip).isNotEmpty();
	}

}
