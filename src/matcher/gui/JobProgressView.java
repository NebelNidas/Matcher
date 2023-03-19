package matcher.gui;

import java.util.List;

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
import matcher.jobs.JobState;

public class JobProgressView extends Control {
	public final ObservableList<Job<?>> jobs = FXCollections.observableArrayList();

    public JobProgressView() {
		getStyleClass().add("task-progress-view");

		JobManager.get().registerEventListener((job, event) -> Platform.runLater(() -> {
			List<Job<?>> subJobs = job.getSubJobs();

			switch (event) {
			case JOB_QUEUED:
				jobs.add(job);

				synchronized (subJobs) {
					subJobs.forEach(jobs::add);
				}

				break;

			case JOB_FINISHED:
				jobs.remove(job);

				synchronized (subJobs) {
					subJobs.forEach(jobs::remove);
				}

				break;
			}
		}));
    }

	@Override
	protected Skin<?> createDefaultSkin() {
		return new JobProgressViewSkin(this);
	}

	private class JobProgressViewSkin extends SkinBase<JobProgressView> {
		JobProgressViewSkin(JobProgressView progressView) {
			super(progressView);

			// list view
			ListView<Job<?>> listView = new ListView<>();
			listView.setPrefSize(400, 300);
			listView.setPlaceholder(new Label("No tasks running"));
			listView.setCellFactory(param -> new TaskCell());
			listView.setFocusTraversable(false);
			listView.setPadding(new Insets(GuiConstants.padding, GuiConstants.padding, GuiConstants.padding, GuiConstants.padding));

			Bindings.bindContent(listView.getItems(), progressView.jobs);

			getChildren().add(listView);
		}

		class TaskCell extends ListCell<Job<?>> {
			private ProgressBar progressBar;
			private Label titleText;
			private Label messageText;
			private Button cancelButton;

			private Job<?> job;
			private BorderPane borderPane;
			private VBox vbox;

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
				cancelButton.setOnAction(event -> {
					if (this.job != null) {
						cancelButton.setDisable(true);
						this.job.cancel();
					}
				});

				vbox = new VBox();
				vbox.setPadding(new Insets(GuiConstants.padding, 0, 0, GuiConstants.padding));
				vbox.setSpacing(GuiConstants.padding * 0.7f);
				vbox.getChildren().add(titleText);
				vbox.getChildren().add(progressBar);
				vbox.getChildren().add(messageText);

				BorderPane.setAlignment(cancelButton, Pos.CENTER);
				BorderPane.setMargin(cancelButton, new Insets(0, GuiConstants.padding, 0, GuiConstants.padding));

				borderPane = new BorderPane();
				borderPane.setCenter(vbox);
				borderPane.setRight(cancelButton);
				setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
			}

			private void resetProperties() {
				titleText.setText(null);
				messageText.setText(null);
				progressBar.setProgress(-1);
				progressBar.setStyle(null);
				cancelButton.setText("Cancel");
				cancelButton.setDisable(false);
			}

			private void update() {
				if (job == null) return;

				if (job.getProgress() <= 0) {
					progressBar.setProgress(-1);
				} else {
					messageText.setText(String.format("%.0f%%", job.getProgress() * 100));
					progressBar.setProgress(job.getProgress());
				}

				if (job.getState().isFinished()) {
					cancelButton.setDisable(true);
				}

				if (job.getState() == JobState.CANCELING) {
					cancelButton.setText("Canceling...");
					cancelButton.setDisable(true);
				}

				if (job.getState() == JobState.CANCELED || job.getState() == JobState.ERRORED) {
					progressBar.setStyle("-fx-accent: darkred");
				}

				if (job.getState() == JobState.CANCELED) {
					cancelButton.setText("Canceled");
				}
			}

			@Override
			protected void updateItem(Job<?> job, boolean empty) {
				super.updateItem(job, empty);
				this.job = job;

				if (empty || job == null) {
					resetProperties();
					getStyleClass().setAll("task-list-cell-empty");
					setGraphic(null);
				} else if (job != null) {
					job.addCancelListener(() -> Platform.runLater(() -> update()));
					job.addProgressListener((progress) -> Platform.runLater(() -> update()));
					job.addCompletionListener((result, error) -> Platform.runLater(() -> update()));

					update();
					getStyleClass().setAll("task-list-cell");
					titleText.setText(job.getId());

					int nestLevel = 0;
					Job<?> currentJob = job;

					while (currentJob.getParent() != null) {
						nestLevel++;
						currentJob = currentJob.getParent();
					}

					BorderPane.setMargin(vbox, new Insets(0, 0, GuiConstants.padding, GuiConstants.padding * (nestLevel * 5)));
					setGraphic(borderPane);
				}
			}
		}
	}
}
