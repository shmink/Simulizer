package simulizer.ui.components.settings;

import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import simulizer.settings.types.IntegerSetting;

/**
 * Component to edit a IntegerSetting
 * 
 * @author Michael
 *
 */
public class IntegerControl extends GridPane {

	public IntegerControl(IntegerSetting setting) {
		// Option Name
		Label title = new Label(setting.getHumanName());
		GridPane.setHgrow(title, Priority.SOMETIMES);
		title.getStyleClass().add("title");
		add(title, 0, 0);

		// Option Value
		TextField value = new TextField();
		value.setEditable(true);
		value.setText("" + setting.getValue());
		GridPane.setRowSpan(value, 2);
		GridPane.setVgrow(value, Priority.SOMETIMES);
		GridPane.setValignment(value, VPos.CENTER);
		value.getStyleClass().add("value");
		value.textProperty().addListener(e -> {
			boolean valid = false;
			int newValue = 0;
			try {
				newValue = Integer.parseInt(value.getText());
				valid = setting.isValid(newValue);
			} catch (NumberFormatException ex) {
				valid = false;
			}

			if (valid)
				setting.setValue(newValue);
			else
				value.setText("" + setting.getValue());
		});
		add(value, 1, 0);

		// Tooltip
		Tooltip tooltip = new Tooltip(setting.getDescription());
		Tooltip.install(title, tooltip);
		Tooltip.install(value, tooltip);
	}
}
