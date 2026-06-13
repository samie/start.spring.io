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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;

/**
 * Modal showing the generated project as a file tree plus a read-only view of the
 * selected file's text. No syntax highlighting — stock Vaadin components only.
 *
 * @author Vaadin UI Migration
 */
public class ExploreDialog extends Dialog {

	private final Pre fileContent = new Pre();

	public ExploreDialog(Map<String, String> files) {
		setHeaderTitle("Explore");
		setWidth("60rem");
		setHeight("40rem");
		TreeGrid<Node> tree = new TreeGrid<>();
		tree.addHierarchyColumn(Node::name).setHeader("File").setAutoWidth(true).setFlexGrow(0);
		tree.setHeightFull();
		// Size the tree to its longest visible item rather than stretching to half the
		// dialog; the content pane (below) takes the remaining width.
		tree.getStyle().set("flex", "0 0 auto").set("width", "fit-content").set("max-width", "22rem");
		TreeData<Node> data = buildTreeData(files);
		tree.setDataProvider(new TreeDataProvider<>(data));
		tree.addSelectionListener((event) -> event.getFirstSelectedItem().ifPresent((node) -> {
			if (node.content() != null) {
				this.fileContent.setText(node.content());
			}
		}));
		tree.expandRecursively(data.getRootItems(), 3);
		this.fileContent.getStyle()
			.set("margin", "0")
			.set("overflow", "auto")
			.set("white-space", "pre")
			.set("font-family", "var(--lumo-font-family-monospace, monospace)")
			.set("font-size", "0.85rem")
			.set("padding", "var(--lumo-space-m)")
			.set("background", "var(--lumo-contrast-5pct)")
			.set("height", "100%");
		VerticalLayout right = new VerticalLayout(this.fileContent);
		right.setPadding(false);
		right.setSpacing(false);
		right.setSizeFull();
		right.getStyle().set("flex-grow", "1");
		HorizontalLayout body = new HorizontalLayout(tree, right);
		body.setSizeFull();
		body.setSpacing(true);
		add(body);
		Button close = new Button("Close", (event) -> close());
		getFooter().add(close);
	}

	private TreeData<Node> buildTreeData(Map<String, String> files) {
		TreeData<Node> data = new TreeData<>();
		Map<String, Node> directories = new HashMap<>();
		Map<String, String> sorted = new LinkedHashMap<>();
		files.keySet().stream().sorted().forEach((key) -> sorted.put(key, files.get(key)));
		for (Map.Entry<String, String> entry : sorted.entrySet()) {
			String[] segments = entry.getKey().split("/");
			Node parent = null;
			StringBuilder accumulator = new StringBuilder();
			for (int i = 0; i < segments.length - 1; i++) {
				if (!accumulator.isEmpty()) {
					accumulator.append('/');
				}
				accumulator.append(segments[i]);
				String key = accumulator.toString();
				Node directory = directories.get(key);
				if (directory == null) {
					directory = new Node(segments[i], null, key);
					directories.put(key, directory);
					data.addItem(parent, directory);
				}
				parent = directory;
			}
			Node file = new Node(segments[segments.length - 1], entry.getValue(), entry.getKey());
			data.addItem(parent, file);
		}
		return data;
	}

	/**
	 * A tree node. The {@code path} is included so two nodes that share a display
	 * {@code name} and {@code content} (e.g. a project's root {@code demo} directory and
	 * a {@code demo} package directory) remain distinct items in the {@link TreeData}.
	 */
	private record Node(String name, String content, String path) {

	}

}
