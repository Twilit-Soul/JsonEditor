package sample;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Should primarily handle UI logic.
 */
public class Controller {

	public  VBox       boxOfFields;
	public  ScrollPane scrollFields;
	public  TextField  filePathField;
	public  Label      notificationField;
	public  Button     saveButton;
	public  Button     discardButton;
	private JsonManip  jsonManip;
	private Path       filePath;
	private Stage      stage;
	private Map<Integer, TextField> textFields = new HashMap<>();

	/**
	 * Tries to load the json objects from file, and updates the UI for possible failures or for success.
	 * Won't load if there is unsaved modified data.
	 */
	public void retrieveJsonData() {
		notificationField.setText("");

		if (savingRequired()) {
			notificationField.setText("You must save the current file or discard changes before opening a new file.");
			return;
		}

		try {
			filePath = Paths.get(filePathField.getText());
		} catch (InvalidPathException e) {
			notificationField.setText("Invalid path: " + e.getMessage());
			return;
		}

		//Make sure file exists
		if (Files.notExists(filePath)) {
			notificationField.setText("Resource file doesn't exist.");
			return;
		}

		//Make sure we can get the data out
		try {
			jsonManip = new JsonManip(this, filePath);
		} catch (IOException e) {
			notificationField.setText("Failed to retrieve data from file: " + e);
			return;
		}

		//Make sure we actually got something
		if (jsonManip.isEmpty()) {
			notificationField.setText("Didn't find any json objects in file.");
			return;
		}

		//Start working through the fields, and label the lines we're looking at
		boxOfFields.getChildren().clear();
		jsonManip.addElementsToUI();
		validateFields();
	}

	/**
	 * Sets the filePathField to have the path of the file you pick, and tries to retrieve data.
	 */
	public void findFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Resource File");
		filePathField.setText(fileChooser.showOpenDialog(stage).getAbsolutePath());
		retrieveJsonData();
	}

	/**
	 * Saves each element to file on its own line, and removes the css highlight
	 * from the modified fields.
	 */
	public void saveObjects() {
		//Combine to individual lines in one string
		String json = jsonManip.getJson();

		//Save to file
		try {
			Files.write(filePath, json.getBytes());
		} catch (IOException e) {
			notificationField.setText("Failed to save file: " + e);
			return;
		}

		//Remove css highlight
		for (TextField textField : textFields.values()) {
			textField.getStyleClass().remove("modified");
		}

		validateFields();
		retrieveJsonData();
	}

	/**
	 * Reset all fields to their original values.
	 */
	public void discardChanges() {
		for (Map.Entry<Integer, TextField> entry : textFields.entrySet()) {
			String originalText = jsonManip.getOriginalVal(entry.getKey());
			entry.getValue().setText(originalText);
		}
		validateFields();
	}

	//TODO: add a better way to visually group things in general? Sub-objects, different top-level objects

	void addBoldLabel(String label) {
		Label openObjectLabel = new Label(label);
		openObjectLabel.getStyleClass().add("ParentObject");
		boxOfFields.getChildren().add(openObjectLabel);
	}

	/**
	 * Add each value and set the onAction so we can save to file, and make the value-changes save
	 * to java object whenever they're changed. We also make modified fields blue until they're saved to file.
	 */
	void addPair(String key, String value, int index) {
		VBox pairBox = new VBox();
		pairBox.setSpacing(4);
		Label fieldLabel = new Label(key);
		TextField textField = new TextField(value);
		textField.setMinWidth(890);
		textField.setOnAction(e -> saveObjects()); //If you hit enter, save
		textField.textProperty().addListener((observable, oldValue, newValue) -> {
			if (!jsonManip.getOriginalVal(index).equals(newValue)) {
				if (!textField.getStyleClass().contains("modified")) {
					textField.getStyleClass().add("modified");
				}
				jsonManip.setPairValue(index, newValue);
			} else {
				textField.getStyleClass().remove("modified");
				jsonManip.setPairValue(index, newValue);
			}
			validateFields();
		});
		textFields.put(index, textField);
		pairBox.getChildren().addAll(fieldLabel, textField);
		boxOfFields.getChildren().add(pairBox);
	}

	/**
	 * Used for showing the find-file dialog.
	 */
	void setStage(Stage stage) {
		this.stage = stage;
	}

	/**
	 * Disable/enable buttons/fields based on whether or not saving is required.
	 */
	private void validateFields() {
		boolean dataRetrieved = !jsonManip.isEmpty();
		saveButton.setVisible(dataRetrieved);
		discardButton.setVisible(dataRetrieved);
		boolean savingRequired = savingRequired();
		saveButton.setDisable(!savingRequired); //Note the !
		discardButton.setDisable(!savingRequired); //Note the !
	}

	/**
	 * Returns true if any fields have been edited but not saved.
	 */
	private boolean savingRequired() {
		for (TextField textField : textFields.values()) {
			if (textField.getStyleClass().contains("modified")) {
				return true;
			}
		}
		return false;
	}
}
