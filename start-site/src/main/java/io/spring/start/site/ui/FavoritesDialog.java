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

import java.util.List;
import java.util.function.Consumer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.spring.start.site.ui.Preferences.Entry;

/**
 * Modal listing user-saved favorites. Click loads; trash deletes.
 *
 * @author Vaadin UI Migration
 */
public class FavoritesDialog extends Dialog {

	private final List<Entry> entries;

	private final Consumer<Entry> onLoad;

	private final Consumer<Entry> onDelete;

	public FavoritesDialog(List<Entry> entries, Consumer<Entry> onLoad, Consumer<Entry> onDelete) {
		this.entries = entries;
		this.onLoad = onLoad;
		this.onDelete = onDelete;
		setHeaderTitle("Favorites");
		setWidth("36rem");
		setHeight("28rem");
		VerticalLayout body = new VerticalLayout();
		body.setPadding(false);
		body.setSpacing(true);
		body.setSizeFull();
		if (entries.isEmpty()) {
			Paragraph empty = new Paragraph("Bookmark a configuration to save it here.");
			empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
			body.add(empty);
		}
		else {
			Grid<Entry> grid = new Grid<>();
			grid.setItems(entries);
			grid.addColumn(Entry::name).setHeader("Name").setAutoWidth(true).setFlexGrow(1);
			grid.addComponentColumn(this::actionsFor).setHeader("");
			grid.addItemClickListener((event) -> {
				this.onLoad.accept(event.getItem());
				close();
			});
			body.add(grid);
		}
		add(body);
		Button close = new Button("Close", (event) -> close());
		getFooter().add(close);
	}

	private HorizontalLayout actionsFor(Entry entry) {
		Button load = new Button("Load", (event) -> {
			this.onLoad.accept(entry);
			close();
		});
		load.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
		Button remove = new Button(new Icon(VaadinIcon.TRASH), (event) -> {
			this.onDelete.accept(entry);
			close();
		});
		remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
		return new HorizontalLayout(load, remove);
	}

}
