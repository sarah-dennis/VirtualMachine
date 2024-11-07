package window;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Inner class handles getting input from user by creating a pop-up dialog box.
 *
 */
public class InputTextPrompt {
	private final String result;

	/**
	 * Creates a pop-up window to ask the user for input. 
	 * Pop-up blocks the application processes.
	 * @param owner - the window where this will be appearing
	 * @param prompt - the string to be displayed to tell the user some info about what they're inputting.
	 */
	public InputTextPrompt(Window owner, String prompt) {
		final Stage dialog = new Stage();

		dialog.setTitle("Enter input:");
		dialog.initOwner(owner);
		dialog.initModality(Modality.WINDOW_MODAL);

		final Text promptMessage = new Text(prompt);
		final TextField textField = new TextField();
		final Button submitButton = new Button("Submit");
		submitButton.setDefaultButton(true);
		submitButton.setOnAction(event -> {
			dialog.close();
		});
		textField.setMinHeight(TextField.USE_PREF_SIZE);

		final VBox layout = new VBox(10);
		layout.setAlignment(Pos.CENTER_RIGHT);
		layout.setStyle("-fx-padding: 10;");
		layout.getChildren().setAll(
				promptMessage,
				textField,
				submitButton
				);

		Scene styledScene = new Scene(layout);
		styledScene.getStylesheets().add(GraphicsFunctObj.class.getResource("/resources/run-window.css").toExternalForm());
		dialog.setScene(styledScene);
		dialog.showAndWait();

		result = textField.getText();
	}

	/**
	 * Returns the string input from user
	 * @return the input given by user in string form
	 */
	public String getResult() {
		return result;
	}
}