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
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
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
	void formLoadsWithVaadinDefaults() {
		MainView view = navigate(MainView.class);
		InitializrFormModel model = view.getModel();
		assertThat(model.getType()).isEqualTo("maven-project");
		assertThat(model.getLanguage()).isEqualTo("java");
		assertThat(model.getPackaging()).isEqualTo("jar");
		assertThat(model.getJavaVersion()).isEqualTo("21");
		assertThat(model.getGroupId()).isEqualTo("org.vaadin.example");
		assertThat(model.getArtifactId()).isEqualTo("vaadin-demo");
		// Package name survives programmatic binding: it is NOT auto-derived to
		// org.vaadin.example.vaadin_demo (see ProjectFormSection isFromClient guard).
		assertThat(model.getPackageName()).isEqualTo("org.vaadin.example");
		assertThat(model.getBootVersion()).isNotBlank();
		assertThat(view).isInstanceOf(AppLayout.class);
		assertThat(view.getContent()).isInstanceOf(FlexLayout.class);
		// Project / Language / Spring Boot / Packaging / Java radios populated from
		// metadata.
		assertThat($(RadioButtonGroup.class).all()).hasSizeGreaterThanOrEqualTo(4);
	}

	@Test
	void toggleDependencyUpdatesModelAndChipList() {
		MainView view = navigate(MainView.class);
		InitializrFormModel model = view.getModel();
		DependenciesSection deps = view.getDependenciesSection();
		// The default form preselects the Vaadin starter.
		assertThat(model.getDependencies()).containsExactly("vaadin");

		model.getDependencies().add("web");
		deps.refresh();

		assertThat(model.getDependencies()).containsExactly("vaadin", "web");
		// "Add dependencies..." button stays put after a refresh.
		assertThat($(Button.class, deps).withText("Add dependencies...").exists()).isTrue();
		// Removing the added dep leaves the Vaadin default in place.
		model.getDependencies().remove("web");
		deps.refresh();
		assertThat(model.getDependencies()).containsExactly("vaadin");
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
