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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.value.ValueChangeMode;
import io.spring.initializr.metadata.Dependency;
import io.spring.start.site.ui.InitializrUiService.DependencyEntry;
import io.spring.start.site.ui.InitializrUiService.DependencyGroupEntry;

/**
 * Modal for picking dependencies. Search filters across name, id and description.
 * Clicking a row toggles the dependency's membership in the form model.
 *
 * @author Vaadin UI Migration
 */
public class DependencyPickerDialog extends Dialog {

	private final InitializrUiService service;

	private final InitializrFormModel model;

	private final Consumer<String> onToggle;

	private final VirtualList<Row> list;

	private List<Row> allRows = List.of();

	public DependencyPickerDialog(InitializrUiService service, InitializrFormModel model, Consumer<String> onToggle) {
		this.service = service;
		this.model = model;
		this.onToggle = onToggle;
		setHeaderTitle("Add dependencies");
		setWidth("36rem");
		setHeight("32rem");
		TextField search = new TextField();
		search.setPlaceholder("Web, Security, JPA, Actuator, Devtools...");
		search.setWidthFull();
		search.setValueChangeMode(ValueChangeMode.EAGER);
		search.addValueChangeListener((event) -> refresh(event.getValue()));
		this.list = new VirtualList<>();
		this.list.setSizeFull();
		this.list.setRenderer(new com.vaadin.flow.data.renderer.ComponentRenderer<>(this::renderRow));
		VerticalLayout body = new VerticalLayout(search, this.list);
		body.setSizeFull();
		body.setPadding(false);
		body.setSpacing(true);
		add(body);
		Button close = new Button("Close", (event) -> close());
		getFooter().add(close);
		reload();
	}

	/** Called by the section when boot version changes — recompute available rows. */
	public void reload() {
		this.allRows = new ArrayList<>();
		List<Row> invalid = new ArrayList<>();
		for (DependencyGroupEntry group : this.service.dependencyGroups(this.model.getBootVersion())) {
			for (DependencyEntry entry : group.items()) {
				Row row = new Row(group.name(), entry);
				if (entry.valid()) {
					this.allRows.add(row);
				}
				else {
					invalid.add(row);
				}
			}
		}
		this.allRows.addAll(invalid);
		refresh("");
	}

	private void refresh(String query) {
		String needle = (query != null) ? query.trim().toLowerCase(Locale.ROOT) : "";
		List<Row> filtered = needle.isEmpty() ? this.allRows
				: this.allRows.stream().filter((row) -> row.matches(needle)).toList();
		this.list.setItems(filtered);
	}

	private com.vaadin.flow.component.Component renderRow(Row row) {
		Dependency dependency = row.entry().dependency();
		boolean valid = row.entry().valid();
		Span name = new Span(dependency.getName());
		name.getStyle().set("font-weight", "600");
		Span groupLabel = new Span(row.groupName());
		groupLabel.getElement().getThemeList().add("badge");
		groupLabel.getStyle().set("margin-left", "auto");
		HorizontalLayout header = new HorizontalLayout(name, groupLabel);
		header.setWidthFull();
		header.setAlignItems(FlexComponent.Alignment.CENTER);
		Span description = new Span((dependency.getDescription() != null) ? dependency.getDescription() : "");
		description.getStyle()
			.set("color", "var(--lumo-secondary-text-color)")
			.set("font-size", "var(--lumo-font-size-s)");
		VerticalLayout content = new VerticalLayout(header, description);
		content.setPadding(false);
		content.setSpacing(false);
		content.getStyle().set("padding", "var(--lumo-space-s) var(--lumo-space-m)");
		if (valid) {
			content.getStyle().set("cursor", "pointer");
		}
		else {
			content.getStyle().set("opacity", "0.55").set("cursor", "not-allowed");
			Span invalid = new Span(row.entry().invalidMessage());
			invalid.getStyle()
				.set("color", "var(--lumo-error-text-color)")
				.set("font-size", "var(--lumo-font-size-xs)");
			content.add(invalid);
		}
		boolean selected = this.model.getDependencies().contains(dependency.getId());
		if (valid && selected) {
			content.getStyle().set("background", "var(--lumo-primary-color-10pct)");
		}
		if (valid) {
			content.addClickListener((event) -> {
				this.onToggle.accept(dependency.getId());
				refresh("");
			});
		}
		Button add = new Button(selected ? "Remove" : "Add", (event) -> {
			this.onToggle.accept(dependency.getId());
			refresh("");
		});
		add.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
		add.setEnabled(valid);
		header.add(add);
		return content;
	}

	private record Row(String groupName, DependencyEntry entry) {

		boolean matches(String needle) {
			Dependency dependency = this.entry.dependency();
			return contains(dependency.getName(), needle) || contains(dependency.getId(), needle)
					|| contains(dependency.getDescription(), needle) || contains(this.groupName, needle);
		}

		private static boolean contains(String haystack, String needle) {
			return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
		}

	}

}
