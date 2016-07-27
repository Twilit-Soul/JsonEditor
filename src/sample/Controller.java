package sample;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
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
	private IJsonManip jsonManip;
	private Path       filePath;
	private Stage      stage;
	private Map<Integer, TextField> textFields = new HashMap<>();

	//And now I'm beginning to understand how feature creep happens.

	//TODO: make a way to delete a field. This would be super useful in trimming excessive json files.
	//TODO: make tabs. Could have multiple files open at once.
	//TODO: make a way to compare two lines?
	//TODO: add a way to introduce entirely new fields to an object/file
	//TODO: add a way to page through the various lines of a file instead of scrolling to each one
	//TODO: pipe dream -> a small button for each object to copy the object's json to clipboard
	//TODO: extra ridiculous pipe dream -> make an expandable/collapsable tree to make things presented even more super clearly

	/**
	 * Tries to load the json objects from file, and updates the UI for possible failures or for success.
	 * Won't load if there is unsaved modified data.
	 */
	public void retrieveJsonData() {
		setNotification("");

		if (savingRequired()) {
			setNotification("You must save the current file or discard changes before opening a new file.");
			return;
		}

		try {
			filePath = Paths.get(filePathField.getText());
		} catch (InvalidPathException e) {
			setNotification("Invalid path: " + e.getMessage());
			return;
		}

		//Make sure file exists
		if (Files.notExists(filePath)) {
			setNotification("Resource file doesn't exist.");
			return;
		}

		//Make sure we can get the data out
		try {
			jsonManip = new JsonManipGsonImpl(this, filePath);
		} catch (IOException e) {
			setNotification("Failed to retrieve data from file: " + e);
			return;
		}

		//Make sure we actually got something
		if (jsonManip.isEmpty()) {
			setNotification("Didn't find any json objects in file.");
			return;
		}

		//Start working through the fields, and label the lines we're looking at
		boxOfFields.getChildren().clear();
		jsonManip.addElementsToUI();
	}

	/**
	 * Sets the filePathField to have the path of the file you pick, and tries to retrieve data.
	 */
	public void findFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Resource File");
		File file = fileChooser.showOpenDialog(stage);
		if (file != null) {
			String filePath = file.getAbsolutePath();
			filePathField.setText(filePath);
			retrieveJsonData();
		}
	}

	/**
	 * Saves each element to file on its own line, and removes the css highlight
	 * from the modified fields.
	 */
	public void saveObjects() {
		//Save to file
		try {
			jsonManip.saveData(filePath);
		} catch (IOException e) {
			setNotification("Failed to save file: " + e);
			return;
		}

		//Remove css highlight
		for (TextField textField : textFields.values()) {
			textField.getStyleClass().remove("modified");
		}

		validateAllFields();
		retrieveJsonData();
	}

	/**
	 * Reset all fields to their original values.
	 */
	public void discardChanges() {
		for (Map.Entry<Integer, TextField> entry : textFields.entrySet()) {
			String originalText = jsonManip.getOriginalVal(entry.getKey());
			if (originalText == null) {
				originalText = "null";
			}
			entry.getValue().setText(originalText);
		}
		boxOfFields.getChildren().clear();
		jsonManip.discardElementsChanges();
	}

	/**
	 * Inserts a label with bold css class to the UI, so we can help identify where objects begin/end.
	 */
	void addObjectLabel(String labelText) {
		boxOfFields.getChildren().add(makeLabelBox(labelText));
	}

	/**
	 * Inserts a label with bold css class to the UI, so we can help identify where objects begin/end.
	 * Also adds a button to duplicate the object.
	 */
	void addObjectLabelWithDuplicateButton(String labelText, int index) {
		HBox labelBox = makeLabelBox(labelText);
		labelBox.setSpacing(10);
		labelBox.setPadding(new Insets(2, 0, 0, 0));
		Button duplicateButton = new Button("Duplicate Line");
		duplicateButton.setOnAction(e -> {
			//TODO: fix modified fields losing modified status when a line is duplicated
			jsonManip.duplicateAndAddToList(index);
			boxOfFields.getChildren().clear();
			jsonManip.addElementsToUI();
		});
		labelBox.getChildren().add(duplicateButton);
		HBox.setMargin(duplicateButton, new Insets(-2, 0, 0, 0));
		boxOfFields.getChildren().add(labelBox);
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
			checkModified(textField, index);
			validateAllFields();
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
	 * Checks if the text field has been modified, and if it has, gives it the "modified" status.
	 */
	private void checkModified(TextField textField, int index) {
		//Check if values were equal, or if the text is "null" and the original value was a null value
		String currentValue = textField.getText(), originalValue = jsonManip.getOriginalVal(index);
		if (!(currentValue.equals("null") && null == originalValue) && !currentValue.equals(originalValue)) {
			//If the value is indeed changed
			if (!textField.getStyleClass().contains("modified")) {
				textField.getStyleClass().add("modified");
			}
		} else {
			textField.getStyleClass().remove("modified");
		}
		jsonManip.setPairValue(index, currentValue);
	}

	private HBox makeLabelBox(String labelText) {
		HBox labelBox = new HBox();
		Label openObjectLabel = new Label(labelText);
		openObjectLabel.getStyleClass().add("ParentObject");
		labelBox.getChildren().add(openObjectLabel);
		return labelBox;
	}

	/**
	 * Disable/enable buttons/fields based on whether or not saving is required.
	 */
	void validateAllFields() {
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
		return jsonManip != null && jsonManip.haveElementsChanged(); //Gets called during initialization
	}

	void setNotification(String notification) {
		notificationField.setText(notification);
	}
}
