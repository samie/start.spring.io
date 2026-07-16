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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;

/**
 * Bottom action row: Generate downloads the project zip; Explore opens the tree-view
 * dialog; the "..." menu hosts Share / Bookmark (wired in Steps 5–6).
 *
 * @author Vaadin UI Migration
 */
public class ActionsBar extends HorizontalLayout {

	private final InitializrUiService service;

	private final InitializrFormModel model;

	private final Button generate;

	private final Button explore;

	private final MenuBar more;

	private Runnable onShare = () -> Notification.show("Share — wired in step 5");

	private Runnable onBookmark = () -> Notification.show("Bookmark — wired in step 6");

	private Runnable afterGenerate = () -> {
	};

	public ActionsBar(InitializrUiService service, InitializrFormModel model) {
		this.service = service;
		this.model = model;
		setSpacing(true);
		setAlignItems(FlexComponent.Alignment.CENTER);
		this.generate = new Button("Generate", (event) -> generate());
		this.generate.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		this.explore = new Button("Explore", (event) -> explore());
		this.more = new MenuBar();
		this.more.addThemeVariants(MenuBarVariant.LUMO_TERTIARY);
		var menu = this.more.addItem("More");
		menu.getSubMenu().addItem("Share", (event) -> this.onShare.run());
		menu.getSubMenu().addItem("Bookmark", (event) -> this.onBookmark.run());
		add(this.generate, this.explore, this.more);
	}

	public Button getGenerateButton() {
		return this.generate;
	}

	public Button getExploreButton() {
		return this.explore;
	}

	public void onShare(Runnable callback) {
		this.onShare = (callback != null) ? callback : () -> {
		};
	}

	public void onBookmark(Runnable callback) {
		this.onBookmark = (callback != null) ? callback : () -> {
		};
	}

	/**
	 * Register a callback to run after a successful Generate (e.g. to record history).
	 * @param callback fires once the zip bytes are queued for download.
	 */
	public void afterGenerate(Runnable callback) {
		this.afterGenerate = (callback != null) ? callback : () -> {
		};
	}

	/** Build the zip in-JVM and trigger a browser download via a transient Anchor. */
	public void generate() {
		try {
			byte[] bytes = this.service.generateZip(this.model);
			String filename = filenameFor(this.model);
			DownloadHandler handler = DownloadHandler
				.fromInputStream((event) -> new DownloadResponse(new ByteArrayInputStream(bytes), filename,
						"application/zip", bytes.length));
			Anchor download = new Anchor(handler, "");
			download.getElement().setAttribute("download", true);
			download.getStyle().set("display", "none");
			UI.getCurrent().add(download);
			download.getElement().executeJs("this.click(); setTimeout(() => this.remove(), 1000);");
			Notification success = Notification.show("Downloading " + filename + "…");
			success.addThemeVariants(com.vaadin.flow.component.notification.NotificationVariant.LUMO_SUCCESS);
			this.afterGenerate.run();
		}
		catch (RuntimeException ex) {
			Notification.show("Generate failed: " + ex.getMessage())
				.addThemeVariants(com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR);
		}
	}

	public void explore() {
		try {
			var files = this.service.explore(this.model);
			new ExploreDialog(files).open();
		}
		catch (RuntimeException ex) {
			Notification.show("Explore failed: " + ex.getMessage())
				.addThemeVariants(com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR);
		}
	}

	private static String filenameFor(InitializrFormModel model) {
		String artifact = (model.getArtifactId() != null && !model.getArtifactId().isBlank()) ? model.getArtifactId()
				: "demo";
		return artifact + ".zip";
	}

}
