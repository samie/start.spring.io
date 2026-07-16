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

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Right column header: "Add dependencies" button + chip list of selected dependencies.
 * The picker dialog is owned by this section.
 *
 * @author Vaadin UI Migration
 */
public class DependenciesSection extends VerticalLayout {

	private final InitializrUiService service;

	private final InitializrFormModel model;

	private final VerticalLayout chipList;

	private final Paragraph emptyState;

	private final DependencyPickerDialog dialog;

	private final Button addButton;

	public DependenciesSection(InitializrUiService service, InitializrFormModel model) {
		this.service = service;
		this.model = model;
		setPadding(false);
		setSpacing(true);
		setWidthFull();
		add(new H2("Dependencies"));
		this.addButton = new Button("Add dependencies...", new Icon(VaadinIcon.PLUS), (event) -> openPicker());
		this.addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		add(this.addButton);
		this.emptyState = new Paragraph("No dependency selected.");
		this.emptyState.getStyle().set("color", "var(--lumo-secondary-text-color)");
		this.chipList = new VerticalLayout();
		this.chipList.setPadding(false);
		this.chipList.setSpacing(true);
		add(this.emptyState, this.chipList);
		this.dialog = new DependencyPickerDialog(service, model, this::toggle);
		refresh();
	}

	/** Open the picker programmatically (used by the Cmd/Ctrl+B shortcut in Step 7). */
	public void openPicker() {
		this.dialog.reload();
		this.dialog.open();
	}

	/**
	 * Recompute chip list + reload picker after external changes (e.g. share-URL load).
	 */
	public void refresh() {
		this.chipList.removeAll();
		boolean empty = this.model.getDependencies().isEmpty();
		this.emptyState.setVisible(empty);
		for (String id : this.model.getDependencies()) {
			this.service.getDependency(id).ifPresent((dependency) -> {
				String invalid = this.service.incompatibilityHint(dependency, this.model.getBootVersion());
				this.chipList.add(chip(dependency.getId(), dependency.getName(), dependency.getDescription(), invalid));
			});
		}
		this.dialog.reload();
	}

	private void toggle(String id) {
		if (this.model.getDependencies().contains(id)) {
			this.model.getDependencies().remove(id);
		}
		else {
			this.model.getDependencies().add(id);
		}
		refresh();
	}

	private HorizontalLayout chip(String id, String name, String description, String invalidMessage) {
		boolean invalid = (invalidMessage != null);
		Span title = new Span(name);
		title.getStyle().set("font-weight", "600");
		Span detail = new Span(invalid ? invalidMessage : ((description != null) ? description : ""));
		detail.getStyle()
			.set("color", invalid ? "var(--lumo-error-text-color)" : "var(--lumo-secondary-text-color)")
			.set("font-size", "var(--lumo-font-size-s)");
		VerticalLayout text = new VerticalLayout(title, detail);
		text.setPadding(false);
		text.setSpacing(false);
		Button remove = new Button(new Icon(VaadinIcon.CLOSE_SMALL), (event) -> toggle(id));
		remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
		remove.setAriaLabel("Remove " + name);
		remove.setTooltipText("Remove " + name);
		HorizontalLayout chip = new HorizontalLayout(text, remove);
		chip.setWidthFull();
		chip.setAlignItems(FlexComponent.Alignment.CENTER);
		chip.getStyle()
			.set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
			.set("background", "var(--lumo-contrast-5pct)")
			.set("border-radius", "var(--lumo-border-radius-m)");
		if (invalid) {
			chip.getStyle().set("opacity", "0.6");
		}
		text.getStyle().set("flex-grow", "1");
		return chip;
	}

}
