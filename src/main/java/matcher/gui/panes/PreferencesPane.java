package matcher.gui.panes;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import job4j.JobManager;

import matcher.Matcher;
import matcher.gui.GuiConstants;

public class PreferencesPane extends VBox {
	public PreferencesPane(Button okButton) {
		super(GuiConstants.padding);

		this.okButton = okButton;

		init();
	}

	private void init() {
		int minSliderValue = 1;
		int sliderLabelStep = 2;
		int targetedMaxSliderValue = Math.max(Runtime.getRuntime().availableProcessors(), Matcher.getMatchingThreadPoolSize());
		int alignedMaxSliderValue = getAlignedMaxSliderValue(targetedMaxSliderValue, sliderLabelStep, minSliderValue);

		Label label = newLabel("Amount of threads used for matching (applies once the current matching subtask is finished):");
		getChildren().add(label);

		matchingThreadsSlider = newSlider(minSliderValue, alignedMaxSliderValue, sliderLabelStep, Matcher.getMatchingThreadPoolSize());
		matchingThreadsSlider.valueProperty().addListener((newValue) -> showWarningIfNecessary());
		getChildren().add(matchingThreadsSlider);

		label = newLabel("Amount of threads used for job execution:");
		getChildren().add(label);

		jobExecutorsSlider = newSlider(minSliderValue, alignedMaxSliderValue, sliderLabelStep, JobManager.get().getMaxJobExecutorThreads());
		jobExecutorsSlider.valueProperty().addListener((newValue) -> showWarningIfNecessary());
		getChildren().add(jobExecutorsSlider);

		warningText = newLabel(null);
		warningText.setStyle("-fx-fill: firebrick;");
		VBox.setMargin(warningText, new Insets(GuiConstants.padding, 0, GuiConstants.padding, 0));
		getChildren().add(warningText);
		showWarningIfNecessary();

		widthProperty().addListener((observable, oldWidth, newWidth) -> {
			for (Node child : getChildren()) {
				if (child instanceof Text) {
					((Text) child).setWrappingWidth(newWidth.intValue() - getSpacing() * 5);
				}
			}
		});
		okButton.setOnAction(event -> save());
		setWidth(600);
	}

	@SuppressWarnings("UnnecessaryLocalVariable")
	private static int getAlignedMaxSliderValue(int targetedMaxSliderValue, int sliderLabelStep, int minSliderValue) {
		int alignedMaxSliderValue = ((int) Math.floor((float) targetedMaxSliderValue / sliderLabelStep)) * sliderLabelStep;
		int normalizedShift = minSliderValue - ((int) Math.floor((float) minSliderValue / sliderLabelStep) * sliderLabelStep);
		int distanceRight = normalizedShift;
		int distanceLeft = sliderLabelStep - distanceRight;

		if (distanceLeft >= distanceRight) {
			alignedMaxSliderValue += distanceRight;
		} else {
			alignedMaxSliderValue -= distanceLeft;
		}

		alignedMaxSliderValue = Math.max(sliderLabelStep, alignedMaxSliderValue);
		return alignedMaxSliderValue;
	}

	private Label newLabel(String text) {
		Label label = new Label(text);
		label.setWrapText(true);
		return label;
	}

	private Slider newSlider(int min, int max, int labelDistance, int value) {
		Slider slider = new Slider(min, max, value);
		slider.setShowTickMarks(true);
		slider.setShowTickLabels(true);
		slider.setMajorTickUnit(labelDistance);
		slider.setMinorTickCount(labelDistance - 1);
		slider.setBlockIncrement(1);
		slider.setSnapToTicks(true);
		return slider;
	}

	private void showWarningIfNecessary() {
		StringBuilder warning = new StringBuilder();

		int allocatedMegabytes = (int) (Runtime.getRuntime().maxMemory() / 1024 / 1024);
		int workerThreadCount = (int) matchingThreadsSlider.getValue();
		int minBaseMegabytes = 5200;
		int minMegabytesPerThread = 70;
		int minTotalRequiredMegabytes = minBaseMegabytes + workerThreadCount * minMegabytesPerThread;

		if (allocatedMegabytes < minBaseMegabytes) {
			warning.append("The amount of allocated RAM (");
			warning.append(allocatedMegabytes);
			warning.append(" MB) is insufficient! Matcher requires at least ");
			warning.append(minBaseMegabytes);
			warning.append(" MB to work correctly.");
		} else if (allocatedMegabytes < minTotalRequiredMegabytes) {
			warning.append("The amount of allocated RAM (");
			warning.append(allocatedMegabytes);
			warning.append(" MB) is most likely insufficient for the amount of allocated worker threads! ");
			warning.append("Please increase the RAM limit to at least ");
			warning.append((int) Math.ceil(minTotalRequiredMegabytes / 100f) * 100);
			warning.append(" MB (via the '-Xmx' startup arg)!");
		}

		warningText.setText(warning.toString());

		if (!warningText.isVisible() && !warning.isEmpty()) {
			warningText.setVisible(true);
			requestLayout();
			requestParentLayout();
		} else if (warningText.isVisible() && warning.isEmpty()) {
			warningText.setVisible(false);
			requestLayout();
			requestParentLayout();
		}
	}

	private void save() {
		Matcher.setMatchingThreadPoolSize((int) matchingThreadsSlider.getValue());
		JobManager.get().setMaxJobExecutorThreads((int) jobExecutorsSlider.getValue());
	}

	private final Button okButton;
	private Slider matchingThreadsSlider;
	private Slider jobExecutorsSlider;
	private Label warningText;
}
