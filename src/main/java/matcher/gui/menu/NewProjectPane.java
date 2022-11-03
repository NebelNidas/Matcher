package matcher.gui.menu;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;

import matcher.config.ProjectConfig;
import matcher.gui.Gui;
import matcher.gui.Gui.SelectedFile;
import matcher.gui.GuiConstants;
import matcher.gui.GuiUtil;

public class NewProjectPane extends ScrollPane {
	NewProjectPane(ProjectConfig config, Window window, Node okButton) {
		this.window = window;
		this.okButton = okButton;

		gridPane = new GridPane();
		pathsA = FXCollections.observableArrayList(config.getPathsA());
		pathsB = FXCollections.observableArrayList(config.getPathsB());
		classPathA = FXCollections.observableArrayList(config.getClassPathA());
		classPathB = FXCollections.observableArrayList(config.getClassPathB());
		sharedClassPath = FXCollections.observableArrayList(config.getSharedClassPath());
		inputsBeforeClassPath = config.hasInputsBeforeClassPath();
		nonObfuscatedClassPatternA = new TextField(config.getNonObfuscatedClassPatternA());
		nonObfuscatedClassPatternB = new TextField(config.getNonObfuscatedClassPatternB());
		nonObfuscatedMemberPatternA = new TextField(config.getNonObfuscatedMemberPatternA());
		nonObfuscatedMemberPatternB = new TextField(config.getNonObfuscatedMemberPatternB());
		ignoredClassPatternA = new TextField(config.getIgnoredClassPatternA());
		ignoredClassPatternB = new TextField(config.getIgnoredClassPatternB());
		ignoredMemberPatternA = new TextField(config.getIgnoredMemberPatternA());
		ignoredMemberPatternB = new TextField(config.getIgnoredMemberPatternB());
		ignoreUnmappedA = new CheckBox("Ignore unmapped elements on side A") {{
			setSelected(config.isIgnoreUnmappedA());
		}};
		ignoreUnmappedB = new CheckBox("Ignore unmapped elements on side B") {{
			setSelected(config.isIgnoreUnmappedB());
		}};
		dontAnalyzeIgnoredClasses = new CheckBox("Don't analyze ignored classes at all (leads to worse results!)") {{
			setSelected(!config.isAnalyzeIgnoredClasses());
		}};
		dontAnalyzeIgnoredMembers = new CheckBox("Don't analyze ignored members at all (leads to worse results!)") {{
			setSelected(!config.isAnalyzeIgnoredMembers());
		}};

		init();
	}

	private void init() {
		gridPane.setHgap(4 * GuiConstants.padding);
		gridPane.setVgap(4 * GuiConstants.padding);

		ColumnConstraints colConstraint = new ColumnConstraints();
		colConstraint.setPercentWidth(50);
		gridPane.getColumnConstraints().addAll(colConstraint, colConstraint);

		RowConstraints rowConstraintInput = new RowConstraints();
		RowConstraints rowConstraintClassPath = new RowConstraints();
		rowConstraintClassPath.setVgrow(Priority.SOMETIMES);
		RowConstraints rowConstraintButtons = new RowConstraints();
		RowConstraints rowConstraintShared = new RowConstraints();
		rowConstraintShared.setVgrow(Priority.SOMETIMES);
		gridPane.getRowConstraints().addAll(rowConstraintInput, rowConstraintClassPath, rowConstraintButtons, rowConstraintShared);

		gridPane.add(createFilesSelectionPane("Inputs A", pathsA, window, false, false), 0, 0);
		gridPane.add(createFilesSelectionPane("Inputs B", pathsB, window, false, false), 1, 0);
		gridPane.add(createFilesSelectionPane("Class path A", classPathA, window, true, false), 0, 1);
		gridPane.add(createFilesSelectionPane("Class path B", classPathB, window, true, false), 1, 1);

		HBox hbox = new HBox(GuiConstants.padding);
		Button swapButton = new Button("swap A ⇄ B");
		hbox.getChildren().add(swapButton);
		swapButton.setOnAction(event -> {
			List<Path> paths = new ArrayList<>(pathsA);
			pathsA.clear();
			pathsA.addAll(pathsB);
			pathsB.setAll(paths);

			paths.clear();
			paths.addAll(classPathA);
			classPathA.clear();
			classPathA.addAll(classPathB);
			classPathB.setAll(paths);

			String tmpS = nonObfuscatedClassPatternA.getText();
			nonObfuscatedClassPatternA.setText(nonObfuscatedClassPatternB.getText());
			nonObfuscatedClassPatternB.setText(tmpS);

			tmpS = nonObfuscatedMemberPatternA.getText();
			nonObfuscatedMemberPatternA.setText(nonObfuscatedMemberPatternB.getText());
			nonObfuscatedMemberPatternB.setText(tmpS);

			tmpS = ignoredClassPatternA.getText();
			ignoredClassPatternA.setText(ignoredClassPatternB.getText());
			ignoredClassPatternB.setText(tmpS);

			tmpS = ignoredMemberPatternA.getText();
			ignoredMemberPatternA.setText(ignoredMemberPatternB.getText());
			ignoredMemberPatternB.setText(tmpS);

			boolean tmpB = ignoreUnmappedA.isSelected();
			ignoreUnmappedA.setSelected(ignoreUnmappedB.isSelected());
			ignoreUnmappedB.setSelected(tmpB);
		});
		gridPane.add(hbox, 0, 2, 2, 1);

		gridPane.add(createFilesSelectionPane("Shared class path", sharedClassPath, window, true, true), 0, 3, 2, 1);
		// TODO: config.inputsBeforeClassPath

		gridPane.add(createMiscPane(), 0, 4, 2, 1);

		ListChangeListener<Path> listChangeListener = change -> okButton.setDisable(!createConfig().isValid());
		InvalidationListener invalidationListener = change -> okButton.setDisable(!createConfig().isValid());

		pathsA.addListener(listChangeListener);
		pathsB.addListener(listChangeListener);
		classPathA.addListener(listChangeListener);
		classPathB.addListener(listChangeListener);
		sharedClassPath.addListener(listChangeListener);
		nonObfuscatedClassPatternA.textProperty().addListener(invalidationListener);
		nonObfuscatedClassPatternB.textProperty().addListener(invalidationListener);
		nonObfuscatedMemberPatternA.textProperty().addListener(invalidationListener);
		nonObfuscatedMemberPatternB.textProperty().addListener(invalidationListener);
		ignoredClassPatternA.textProperty().addListener(invalidationListener);
		ignoredClassPatternB.textProperty().addListener(invalidationListener);
		ignoredMemberPatternA.textProperty().addListener(invalidationListener);
		ignoredMemberPatternB.textProperty().addListener(invalidationListener);
		listChangeListener.onChanged(null);

		setContent(gridPane);
		setFitToWidth(true);
		// setPrefHeight(Screen.getPrimary().getBounds().getHeight() * 0.8);
	}

	private Node createFilesSelectionPane(String name, ObservableList<Path> entries, Window window, boolean isClassPath, boolean isShared) {
		VBox ret = new VBox(GuiConstants.padding);

		ret.getChildren().add(new Label(name+":"));

		ListView<Path> list = new ListView<>(entries);
		ret.getChildren().add(list);

		list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		list.setPrefWidth(320);
		list.setPrefHeight(isShared ? 200 : 100);
		VBox.setVgrow(list, Priority.ALWAYS);

		HBox footer = new HBox(GuiConstants.padding);
		ret.getChildren().add(footer);

		footer.setAlignment(Pos.CENTER_RIGHT);

		final Button moveToAButton, moveToBButton, splitButton;

		if (isClassPath) {
			if (isShared) {
				moveToAButton = new Button("to A");
				footer.getChildren().add(moveToAButton);
				moveToAButton.setOnAction(event -> {
					MultipleSelectionModel<Path> selection = list.getSelectionModel();
					List<Path> selected = new ArrayList<>(selection.getSelectedItems());

					for (Path path : selected) {
						list.getItems().remove(path);
						classPathA.add(path);
					}
				});

				moveToBButton = new Button("to B");
				footer.getChildren().add(moveToBButton);
				moveToBButton.setOnAction(event -> {
					MultipleSelectionModel<Path> selection = list.getSelectionModel();
					List<Path> selected = new ArrayList<>(selection.getSelectedItems());

					for (Path path : selected) {
						list.getItems().remove(path);
						classPathB.add(path);
					}
				});

				splitButton = new Button("split");
				footer.getChildren().add(splitButton);
				splitButton.setOnAction(event -> {
					MultipleSelectionModel<Path> selection = list.getSelectionModel();
					List<Path> selected = new ArrayList<>(selection.getSelectedItems());

					for (Path path : selected) {
						list.getItems().remove(path);
						classPathA.add(path);
						classPathB.add(path);
					}
				});
			} else {
				moveToAButton = new Button("to shared");
				footer.getChildren().add(moveToAButton);
				moveToAButton.setOnAction(event -> {
					MultipleSelectionModel<Path> selection = list.getSelectionModel();
					List<Path> selected = new ArrayList<>(selection.getSelectedItems());
					ObservableList<Path> localList = list.getItems();
					ObservableList<Path> otherList = localList == classPathA ? classPathB : classPathA;

					for (Path path : selected) {
						localList.remove(path);
						otherList.remove(path);
						sharedClassPath.add(path);
					}
				});

				moveToBButton = null;
				splitButton = null;
			}
		} else {
			moveToAButton = moveToBButton = splitButton = null;
		}

		Button button = new Button("add");
		footer.getChildren().add(button);
		button.setOnAction(event -> {
			List<SelectedFile> res = Gui.requestFiles("Select file to add", window, getInputLoadExtensionFilters());
			for (SelectedFile each : res) {
				if (!list.getItems().contains(each.path)) list.getItems().add(each.path);
			}
		});

		Button addDirecotyButton = new Button("add dir");
		footer.getChildren().add(addDirecotyButton);
		addDirecotyButton.setOnAction(event -> {
			Path res = Gui.requestDir("Select directory to add", window);
			try (Stream<Path> stream = Files.walk(res, 128)) {
				stream.filter(Files::isRegularFile)
				.filter(getInputLoadExtensionMatcher()::matches)
				.forEach(path -> {
					if (!list.getItems().contains(path)) {
						list.getItems().add(path);
					}
				});
			} catch (IOException ignored) {
			}
		});

		Button removeButton = new Button("remove");
		footer.getChildren().add(removeButton);
		removeButton.setOnAction(event -> {
			Set<Path> selected = new HashSet<>(list.getSelectionModel().getSelectedItems());
			list.getItems().removeIf(selected::contains);
		});

		Button upButton = new Button("up");
		footer.getChildren().add(upButton);
		upButton.setOnAction(event -> GuiUtil.moveSelectionUp(list));

		Button downButton = new Button("down");
		footer.getChildren().add(downButton);
		downButton.setOnAction(event -> GuiUtil.moveSelectionDown(list));

		ListChangeListener<Path> changeListener = change -> {
			List<Integer> selectedIndices = list.getSelectionModel().getSelectedIndices();
			boolean empty = selectedIndices.isEmpty();

			removeButton.setDisable(empty);
			if (moveToAButton != null) moveToAButton.setDisable(empty);
			if (moveToBButton != null) moveToBButton.setDisable(empty);

			boolean disableUp = true;
			boolean disableDown = true;

			if (!empty) {
				Set<Integer> selected = new HashSet<>(selectedIndices);

				for (int idx : selectedIndices) {
					if (disableUp && idx > 0 && !selected.contains(idx - 1)) {
						disableUp = false;
						if (!disableDown) break;
					}

					if (disableDown && idx < list.getItems().size() - 1 && !selected.contains(idx + 1)) {
						disableDown = false;
						if (!disableUp) break;
					}
				}
			}

			upButton.setDisable(disableUp);
			downButton.setDisable(disableDown);
		};

		list.getSelectionModel().getSelectedItems().addListener(changeListener);
		changeListener.onChanged(null);

		return ret;
	}

	private static List<ExtensionFilter> getInputLoadExtensionFilters() {
		return Arrays.asList(new FileChooser.ExtensionFilter("Java archive", "*.jar"));
	}

	private static PathMatcher getInputLoadExtensionMatcher() {
		return FileSystems.getDefault().getPathMatcher("glob:**.jar");
	}

	private Node createMiscPane() {
		VBox ret = new VBox(2 * GuiConstants.padding);

		TitledPane nonObfPatternPane = new TitledPane();
		VBox nonObfPatternBox = new VBox(GuiConstants.padding);
		nonObfPatternPane.setText("Non-obfuscated class and member patterns");
		nonObfPatternBox.getChildren().add(new Label("Non-obfuscated class name pattern A (regex):"));
		nonObfPatternBox.getChildren().add(nonObfuscatedClassPatternA);
		nonObfPatternBox.getChildren().add(new Label("Non-obfuscated class name pattern B (regex):"));
		nonObfPatternBox.getChildren().add(nonObfuscatedClassPatternB);
		nonObfPatternBox.getChildren().add(new Label("Non-obfuscated member name pattern A (regex):"));
		nonObfPatternBox.getChildren().add(nonObfuscatedMemberPatternA);
		nonObfPatternBox.getChildren().add(new Label("Non-obfuscated member name pattern B (regex):"));
		nonObfPatternBox.getChildren().add(nonObfuscatedMemberPatternB);
		nonObfPatternPane.setContent(nonObfPatternBox);
		nonObfPatternPane.setExpanded(false);
		ret.getChildren().add(nonObfPatternPane);

		TitledPane ignoredPatternPane = new TitledPane();
		ignoredPatternPane.setText("Ignored class and member patterns");
		VBox ignoredPatternBox = new VBox(GuiConstants.padding);
		ignoredPatternBox.getChildren().add(new Label("Ignored class name pattern A (regex):"));
		ignoredPatternBox.getChildren().add(ignoredClassPatternA);
		ignoredPatternBox.getChildren().add(new Label("Ignored class name pattern B (regex):"));
		ignoredPatternBox.getChildren().add(ignoredClassPatternB);
		ignoredPatternBox.getChildren().add(new Label("Ignored member name pattern A (regex):"));
		ignoredPatternBox.getChildren().add(ignoredMemberPatternA);
		ignoredPatternBox.getChildren().add(new Label("Ignored member name pattern B (regex):"));
		ignoredPatternBox.getChildren().add(ignoredMemberPatternB);
		ignoredPatternBox.getChildren().add(ignoreUnmappedA);
		ignoredPatternBox.getChildren().add(ignoreUnmappedB);
		ignoredPatternBox.getChildren().add(dontAnalyzeIgnoredClasses);
		ignoredPatternBox.getChildren().add(dontAnalyzeIgnoredMembers);

		ignoredPatternPane.setContent(ignoredPatternBox);
		ignoredPatternPane.setExpanded(false);
		ret.getChildren().add(ignoredPatternPane);

		return ret;
	}

	public ProjectConfig createConfig() {
		return new ProjectConfig(new ArrayList<>(pathsA),
				new ArrayList<>(pathsB),
				new ArrayList<>(classPathA),
				new ArrayList<>(classPathB),
				new ArrayList<>(sharedClassPath),
				inputsBeforeClassPath,
				nonObfuscatedClassPatternA.getText(),
				nonObfuscatedClassPatternB.getText(),
				nonObfuscatedMemberPatternA.getText(),
				nonObfuscatedMemberPatternB.getText(),
				ignoredClassPatternA.getText(),
				ignoredClassPatternB.getText(),
				ignoredMemberPatternA.getText(),
				ignoredMemberPatternB.getText(),
				ignoreUnmappedA.isSelected(),
				ignoreUnmappedB.isSelected(),
				!dontAnalyzeIgnoredClasses.isSelected(),
				!dontAnalyzeIgnoredMembers.isSelected());
	}

	private final Window window;
	private final Node okButton;

	private final GridPane gridPane;
	private final ObservableList<Path> pathsA;
	private final ObservableList<Path> pathsB;
	private final ObservableList<Path> classPathA;
	private final ObservableList<Path> classPathB;
	private final ObservableList<Path> sharedClassPath;
	private final boolean inputsBeforeClassPath;
	private final TextField nonObfuscatedClassPatternA;
	private final TextField nonObfuscatedClassPatternB;
	private final TextField nonObfuscatedMemberPatternA;
	private final TextField nonObfuscatedMemberPatternB;
	private final TextField ignoredClassPatternA;
	private final TextField ignoredClassPatternB;
	private final TextField ignoredMemberPatternA;
	private final TextField ignoredMemberPatternB;
	private final CheckBox ignoreUnmappedA;
	private final CheckBox ignoreUnmappedB;
	private final CheckBox dontAnalyzeIgnoredClasses;
	private final CheckBox dontAnalyzeIgnoredMembers;
}
