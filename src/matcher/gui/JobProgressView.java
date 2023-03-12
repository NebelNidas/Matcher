package matcher.gui;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import matcher.jobs.Job;
import matcher.jobs.JobManager;

public class JobProgressView extends Control {
	public final ObservableList<Job<?>> tasks = FXCollections.observableArrayList();

    public JobProgressView() {
		getStyleClass().add("task-progress-view");

		JobManager.registerEventListener((task, event) -> Platform.runLater(() -> {
			switch (event) {
			case JOB_STARTED:
				tasks.add(task);
				break;
			case JOB_ENDED:
				tasks.remove(task);
				break;
			}
		}));
    }

	@Override
	protected Skin<?> createDefaultSkin() {
		return new TaskProgressViewSkin(this);
	}

	private class TaskProgressViewSkin extends SkinBase<JobProgressView> {
		TaskProgressViewSkin(JobProgressView progressView) {
			super(progressView);

			// list view
			ListView<Job<?>> listView = new ListView<>();
			listView.setPrefSize(400, 300);
			listView.setPlaceholder(new Label("No tasks running"));
			listView.setCellFactory(param -> new TaskCell());
			listView.setFocusTraversable(false);
			listView.setPadding(new Insets(GuiConstants.padding, GuiConstants.padding, GuiConstants.padding, GuiConstants.padding));

			Bindings.bindContent(listView.getItems(), progressView.tasks);

			getChildren().add(listView);
		}

		class TaskCell extends ListCell<Job<?>> {
			private ProgressBar progressBar;
			private Label titleText;
			private Label messageText;
			private Button cancelButton;

			private Job<?> task;
			private BorderPane borderPane;

			public TaskCell() {
				titleText = new Label();
				titleText.getStyleClass().add("task-title");

				messageText = new Label();
				messageText.getStyleClass().add("task-message");

				progressBar = new ProgressBar();
				progressBar.setMaxWidth(Double.MAX_VALUE);
				progressBar.setPrefHeight(10);
				progressBar.getStyleClass().add("task-progress-bar");

				cancelButton = new Button("Cancel");
				cancelButton.getStyleClass().add("task-cancel-button");
				cancelButton.setTooltip(new Tooltip("Cancel Task"));
				cancelButton.setOnAction(evt -> {
					if (task != null) {
						cancelButton.setDisable(true);
						task.cancel();
					}
				});

				VBox vbox = new VBox();
				vbox.setSpacing(GuiConstants.padding);
				vbox.getChildren().add(titleText);
				vbox.getChildren().add(progressBar);
				vbox.getChildren().add(messageText);

				BorderPane.setAlignment(cancelButton, Pos.CENTER);
				BorderPane.setMargin(cancelButton, new Insets(0, 0, 0, GuiConstants.padding));

				borderPane = new BorderPane();
				borderPane.setCenter(vbox);
				borderPane.setRight(cancelButton);
				setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
			}

			@Override
			public void updateIndex(int index) {
				super.updateIndex(index);

				/*
				 * I have no idea why this is necessary but it won't work without
				 * it. Shouldn't the updateItem method be enough?
				 */
				if (index == -1) {
					setGraphic(null);
					getStyleClass().setAll("task-list-cell-empty");
				}
			}

			@Override
			protected void updateItem(Job<?> task, boolean empty) {
				super.updateItem(task, empty);

				this.task = task;

				if (empty || task == null) {
					getStyleClass().setAll("task-list-cell-empty");
					setGraphic(null);
				} else if (task != null) {
					getStyleClass().setAll("task-list-cell");
					titleText.setText(task.getId());
					task.addProgressListener(progress -> Platform.runLater(() -> {
						progressBar.setProgress(progress);
						messageText.setText(String.format("%.0f%%", progress * 100));
					}));

					// Callback<T, Node> factory = getSkinnable().getGraphicFactory();
					// if (factory != null) {
					// 	Node graphic = factory.call(task);
					// 	if (graphic != null) {
					// 		BorderPane.setAlignment(graphic, Pos.CENTER);
					// 		BorderPane.setMargin(graphic, new Insets(0, 4, 0, 0));
					// 		borderPane.setLeft(graphic);
					// 	}
					// } else {
						/*
						 * Really needed. The application might have used a graphic
						 * factory before and then disabled it. In this case the border
						 * pane might still have an old graphic in the left position.
						 */
						borderPane.setLeft(null);
					// }

					setGraphic(borderPane);
				}
			}
		}
	}
}
