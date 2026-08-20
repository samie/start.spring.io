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
import java.util.Iterator;
import java.util.List;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.spring.start.site.ui.Preferences.Entry;

/**
 * Root view of the start.spring.io Vaadin UI. Owns {@code GET /} and composes the
 * three-pane layout: {@link ProjectFormSection} on the left, {@link DependenciesSection}
 * + {@link ActionsBar} on the right.
 *
 * @author Vaadin UI Migration
 */
@Route("")
@PageTitle("Spring Initializr")
public class MainView extends AppLayout {

	private final InitializrUiService service;

	private final Preferences preferences;

	private final InitializrFormModel model;

	private final Binder<InitializrFormModel> binder;

	private final ProjectFormSection formSection;

	private final DependenciesSection dependenciesSection;

	private final ActionsBar actionsBar;

	private final Button themeToggle;

	private final List<Entry> histories = new ArrayList<>();

	private final List<Entry> favorites = new ArrayList<>();

	private boolean darkMode = true;

	public MainView(InitializrUiService service, Preferences preferences) {
		this.service = service;
		this.preferences = preferences;
		this.model = service.defaultFormModel();
		this.binder = new Binder<>(InitializrFormModel.class);
		this.binder.setBean(this.model);
		this.formSection = new ProjectFormSection(this.service, this.model, this.binder);
		this.dependenciesSection = new DependenciesSection(this.service, this.model);
		this.actionsBar = new ActionsBar(this.service, this.model);
		// Dark-first initial state; applyTheme reconciles with any saved preference.
		this.themeToggle = new Button(new Icon(VaadinIcon.SUN_O), (event) -> toggleTheme());
		this.themeToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
		this.themeToggle.setAriaLabel("Switch to light mode");
		this.themeToggle.setTooltipText("Switch to light mode");
		this.formSection.onBootVersionChange(this.dependenciesSection::refresh);
		this.actionsBar.onShare(() -> new ShareDialog(this.model).open());
		this.actionsBar.onBookmark(this::openBookmarkPrompt);
		this.actionsBar.afterGenerate(this::recordHistory);
		setPrimarySection(Section.DRAWER);
		buildDrawer();
		// Start collapsed: History/Favorites are occasional actions and the drawer would
		// otherwise steal horizontal space from the form on every load. The navbar
		// DrawerToggle opens it on demand.
		setDrawerOpened(false);
		buildNavbar();
		setContent(buildContent());
		registerShortcuts();
	}

	private void registerShortcuts() {
		// Generate: Cmd/Ctrl + Enter
		bindShortcut(this.actionsBar::generate, Key.ENTER, KeyModifier.META);
		bindShortcut(this.actionsBar::generate, Key.ENTER, KeyModifier.CONTROL);
		// Explore: Ctrl + Space (the React shortcut is Ctrl-only, even on macOS)
		bindShortcut(this.actionsBar::explore, Key.SPACE, KeyModifier.CONTROL);
		// Add dependencies: Cmd/Ctrl + B
		bindShortcut(this.dependenciesSection::openPicker, Key.KEY_B, KeyModifier.META);
		bindShortcut(this.dependenciesSection::openPicker, Key.KEY_B, KeyModifier.CONTROL);
	}

	private ShortcutRegistration bindShortcut(Runnable action, Key key, KeyModifier modifier) {
		return Shortcuts.addShortcutListener(this, action::run, key, modifier).listenOn(this);
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		attachEvent.getUI().getPage().executeJs("return window.location.hash;").then(String.class, this::applyHash);
		this.preferences.loadHistories(this::onHistoriesLoaded);
		this.preferences.loadFavorites(this::onFavoritesLoaded);
		this.preferences.loadTheme(this::applyTheme);
	}

	private void applyHash(String hash) {
		if (!ShareUrlCodec.decodeInto(hash, this.model)) {
			return;
		}
		this.binder.readBean(this.model);
		this.dependenciesSection.refresh();
		getElement().executeJs("history.replaceState(null, '', window.location.pathname);");
	}

	private void onHistoriesLoaded(List<Entry> entries) {
		this.histories.clear();
		this.histories.addAll(entries);
	}

	private void onFavoritesLoaded(List<Entry> entries) {
		this.favorites.clear();
		this.favorites.addAll(entries);
	}

	private void applyTheme(String theme) {
		// Dark-first: a dev tool defaults to dark. Only an explicit saved "light"
		// preference opts out; unset/blank/"dark" all resolve to dark.
		setDarkMode(!"light".equalsIgnoreCase(theme));
	}

	private void recordHistory() {
		String name = (this.model.getArtifactId() != null && !this.model.getArtifactId().isBlank())
				? this.model.getArtifactId() : "demo";
		Entry entry = Preferences.snapshot(name, System.currentTimeMillis(), this.model);
		this.histories.add(0, entry);
		this.preferences.saveHistories(this.histories);
	}

	private void openHistoryDialog() {
		new HistoryDialog(new ArrayList<>(this.histories), this::loadEntry, this::deleteHistoryEntry,
				this::clearHistories)
			.open();
	}

	private void openFavoritesDialog() {
		new FavoritesDialog(new ArrayList<>(this.favorites), this::loadEntry, this::deleteFavoriteEntry).open();
	}

	private void openBookmarkPrompt() {
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("Bookmark configuration");
		TextField nameField = new TextField("Name");
		nameField.setValue((this.model.getArtifactId() != null) ? this.model.getArtifactId() : "demo");
		nameField.setWidthFull();
		dialog.add(new VerticalLayout(nameField));
		Button cancel = new Button("Cancel", (event) -> dialog.close());
		Button save = new Button("Save", (event) -> {
			String name = nameField.getValue();
			if (name == null || name.isBlank()) {
				return;
			}
			Entry entry = Preferences.snapshot(name, System.currentTimeMillis(), this.model);
			this.favorites.add(0, entry);
			this.preferences.saveFavorites(this.favorites);
			dialog.close();
			Notification.show("Saved to favorites");
		});
		save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		dialog.getFooter().add(cancel, save);
		dialog.open();
	}

	private void loadEntry(Entry entry) {
		Preferences.apply(entry, this.model);
		this.binder.readBean(this.model);
		this.dependenciesSection.refresh();
	}

	private void deleteHistoryEntry(Entry entry) {
		removeBy(this.histories, entry);
		this.preferences.saveHistories(this.histories);
	}

	private void deleteFavoriteEntry(Entry entry) {
		removeBy(this.favorites, entry);
		this.preferences.saveFavorites(this.favorites);
	}

	private void clearHistories() {
		ConfirmDialog confirm = new ConfirmDialog();
		confirm.setHeader("Clear history");
		confirm.setText("This removes all entries. Continue?");
		confirm.setCancelable(true);
		confirm.setConfirmText("Clear");
		confirm.addConfirmListener((event) -> {
			this.histories.clear();
			this.preferences.saveHistories(this.histories);
		});
		confirm.open();
	}

	private static void removeBy(List<Entry> list, Entry target) {
		Iterator<Entry> iterator = list.iterator();
		while (iterator.hasNext()) {
			Entry current = iterator.next();
			if (current.timestamp() == target.timestamp() && current.name().equals(target.name())) {
				iterator.remove();
				return;
			}
		}
	}

	private void buildDrawer() {
		Button history = new Button("History", new Icon(VaadinIcon.CLOCK), (event) -> openHistoryDialog());
		history.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		Button favorites = new Button("Favorites", new Icon(VaadinIcon.STAR), (event) -> openFavoritesDialog());
		favorites.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		VerticalLayout drawer = new VerticalLayout(history, favorites);
		drawer.setPadding(true);
		drawer.setSpacing(true);
		addToDrawer(drawer);
	}

	private void buildNavbar() {
		DrawerToggle toggle = new DrawerToggle();
		H1 title = new H1("Spring Initializr");
		title.getStyle().set("font-size", "1.125rem").set("margin", "0");
		HorizontalLayout right = new HorizontalLayout(this.themeToggle);
		right.setSpacing(true);
		right.setAlignItems(FlexComponent.Alignment.CENTER);
		right.getStyle().set("margin-left", "auto").set("margin-right", "var(--vaadin-padding-l, 1rem)");
		addToNavbar(toggle, title, right);
	}

	/**
	 * Responsive two-column content with exactly two states, driven by the
	 * {@code .main-content} rules in {@code styles/app.css} (loaded via
	 * {@link AppShell}): side by side the form (~60%) and the dependencies/actions panel
	 * (~40%) each fill the window height and scroll internally; below {@code 44rem} the
	 * panel stacks under the form at natural height and the page scrolls. The
	 * dependencies panel's tint comes from Aura's stock {@code aura-surface} class (its
	 * border/radius live in the stylesheet); both follow the day/night toggle via CSS
	 * {@code light-dark()}.
	 * @return the content layout to mount as the AppLayout content
	 */
	private Component buildContent() {
		VerticalLayout formColumn = new VerticalLayout(this.formSection);
		formColumn.setPadding(true);
		formColumn.setSpacing(true);
		formColumn.addClassNames("main-column", "main-column-form");
		VerticalLayout rightColumn = new VerticalLayout(this.dependenciesSection, this.actionsBar);
		rightColumn.setPadding(true);
		rightColumn.setSpacing(true);
		rightColumn.addClassNames("main-column", "main-column-deps", "aura-surface");
		// Let the dependencies area take the free space so the action bar sits below it
		// rather than floating directly under the header (gh #5).
		rightColumn.setFlexGrow(1, this.dependenciesSection);
		rightColumn.setFlexGrow(0, this.actionsBar);
		FlexLayout content = new FlexLayout(formColumn, rightColumn);
		content.addClassName("main-content");
		return content;
	}

	private void toggleTheme() {
		setDarkMode(!this.darkMode);
		this.preferences.saveTheme(this.darkMode ? "dark" : "light");
	}

	/**
	 * Switches between the Aura light and dark color schemes. Aura keys its dark variant
	 * off the CSS {@code color-scheme} property rather than a Lumo theme attribute, so
	 * the scheme is applied to the document root to cover overlays (dialogs, menus) that
	 * render outside this view.
	 * @param dark whether the dark color scheme should be active
	 */
	private void setDarkMode(boolean dark) {
		this.darkMode = dark;
		this.themeToggle.setIcon(new Icon(dark ? VaadinIcon.SUN_O : VaadinIcon.MOON));
		String label = dark ? "Switch to light mode" : "Switch to dark mode";
		this.themeToggle.setAriaLabel(label);
		this.themeToggle.setTooltipText(label);
		getElement().executeJs("document.documentElement.style.colorScheme = $0;", dark ? "dark" : "light");
	}

	public InitializrFormModel getModel() {
		return this.model;
	}

	public Binder<InitializrFormModel> getBinder() {
		return this.binder;
	}

	public ProjectFormSection getFormSection() {
		return this.formSection;
	}

	public DependenciesSection getDependenciesSection() {
		return this.dependenciesSection;
	}

	public ActionsBar getActionsBar() {
		return this.actionsBar;
	}

}
