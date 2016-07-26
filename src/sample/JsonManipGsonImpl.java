package sample;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * Should handle reading/modifying the json data from the file.
 * Created by mturlington on 6/27/2016.
 */
class JsonManipGsonImpl implements IJsonManip {
	/**
	 * Json values are saved so they can be edited at will via the UI.
	 * These are most often primitives, but could be nulls.
	 */
	final private List<JsonPrimitive> primitives  = new ArrayList<>();
	/**
	 * Original value is saved so we can have UI notifications on "modified" values,
	 * and to reset values to their original state.
	 */
	final private List<String>        originalVal = new ArrayList<>();
	/**
	 * So we can discard changes and get back to this if we add/remove any.
	 */
	private List<JsonObject> originalElements;
	/**
	 * Our link back to the UI.
	 */
	final private Controller       controller;
	/**
	 * For labeling on the UI.
	 */
	final private String           fileName;
	/**
	 * We deserialize everything when we save to file.
	 */
	private       List<JsonObject> elements;
	/**
	 * For preservation of comments/spacing.
	 */
	private List<String> originalText = new ArrayList<>();

	//TODO: just let them say "verifyAddWalletWithProperData" and it finds it automatically.
	//Right now we use a custom gson, and that's certainly not ideal.
	//TODO: I might make a custom object that holds the JsonObjects/primitives at some point.
	//It would allow me to solve the issue below, and it would allow me to track "modified" and "original value" status more easily.
	//TODO: save the JsonObjects that hold the primitives (via HashMap I guess) and modify the objects themselves...even though it's bullshit!
	//TODO: creating that hashmap is even more important than I thought, as it would allow me to genuinely change primitives to null.
	//The problem with this is making it clear on the UI as an alternative means of getting what we want...
	//Maybe let them set the parent directory of the project somewhere, and it recursively searches, offering a
	//dropdown of auto-fill file paths to choose? That sounds pretty damn fancy.

	/**
	 * Gets the json java objects from the file, and remembers the UI controller.
	 */
	JsonManipGsonImpl(Controller controller, Path filePath) throws IOException {
		elements = getGson(filePath);
		originalElements = duplicateList(elements);
		this.controller = controller;
		this.fileName = filePath.getFileName().toString();
	}

	/**
	 * If the file didn't return any objects or it only returned an empty object.
	 */
	@Override
	public boolean isEmpty() {
		return elements.isEmpty() || elements.get(0).toString().equals("{}");
	}

	/**
	 * Returns the value this primitive had when it was loaded.
	 */
	@Override
	public String getOriginalVal(int index) {
		return originalVal.get(index);
	}

	/**
	 * Set the value of the primitive object.
	 */
	@Override
	public void setPairValue(int index, String newVal) {
		JsonPrimitive primitive = primitives.get(index);
		if (primitive.isBoolean()) {
			primitive.setValue(newVal.equalsIgnoreCase("true"));
		} else {
			try {
				primitive.setValue(Integer.parseInt(newVal));
			} catch (NumberFormatException e1) {
				primitive.setValue(newVal);
			}
		}
	}

	/**
	 * Deserializes and saves to file.
	 */
	@Override
	public void saveData(Path filePath) throws IOException {
		Files.write(filePath, getJson().getBytes());
		originalElements = duplicateList(elements);
	}

	/**
	 * Go through each Json element and add it, using the file name as a label.
	 */
	@Override
	public void addElementsToUI() {
		for (int i = 0; i < elements.size(); i++) {
			addJson(fileName + "[" + i + "]", elements.get(i));
		}
		controller.validateAllFields();
	}

	/**
	 * Gets JsonOject by index from elements and duplicates it in the list.
	 */
	@Override
	public void duplicateAndAddToList(int index) {
		elements.add(duplicateObject(elements.get(index)));
	}

	/**
	 * If the elements list doesn't match the original elements list size, we've added or removed.
	 * We shouldn't need to compare sub-fields...the modified fields should be enough.
	 * This could change if we ever allow adding/removing sub-objects.
	 */
	@Override
	public boolean haveElementsChanged() {
		return elements.size() != originalElements.size(); //Ehhhh this will do for now
	}

	@Override
	public void discardElementsChanges() {
		if (haveElementsChanged()) {
			elements = duplicateList(originalElements);
		}
		addElementsToUI();
	}

	/**
	 * Returns a copy of the passed in jsonObject.
	 */
	private JsonObject duplicateObject(JsonObject jsonObject) {
		return new Gson().fromJson(jsonObject, JsonObject.class); //Feels like a gross way to copy a JsonObject
	}

	/**
	 * Returns a copy of a jsonObject list.
	 */
	private List<JsonObject> duplicateList(List<JsonObject> list) {
		return list.stream().map(this::duplicateObject).collect(Collectors.toList()); //Creates a wholly separate copy of elements. References are unique, too.
	}

	/**
	 * Get the gson java objects from the given filepath.
	 */
	private List<JsonObject> getGson(Path filePath) throws IOException {
		JsonParser parser = new JsonParser();
		originalText = Files.readAllLines(filePath);
		return originalText.stream().filter(l -> l.startsWith("{")).map(l -> (JsonObject) parser.parse(l)).collect(Collectors.toList());
	}

	/**
	 * Combines all of the java objects into a single json string.
	 */
	private String getJson() {
		String json = "";

		int j = 0;
		for (int i = 0; i < originalText.size(); i++) {
			String originalLine = originalText.get(i);
			json += originalLine.startsWith("{") ? convertToJson(elements.get(j++)) : originalLine;
			if (i < originalText.size() - 1) {
				json += "\n";
			}
		}
		if (j < elements.size()) {
			json += "\n";
			for (; j < elements.size(); j++) {
				json += convertToJson(elements.get(j));
				if (j < elements.size() - 1) {
					json += "\n";
				}
			}
		}

		return json;
	}

	private String convertToJson(JsonElement element) {
		Gson gson = new Gson();
		return gson.toJson(element);
	}

	/**
	 * Determine what type it is and handle it appropriately. If it's none of these,
	 * don't know what it is, just don't do anything I guess?
	 */
	private void addJson(String key, JsonElement element) {
		if (element.isJsonArray()) {
			addArray(key, element.getAsJsonArray());
		} else if (element.isJsonObject()) {
			addObject(key, element.getAsJsonObject());
		} else if (element.isJsonPrimitive()) {
			addPrimitive(key, element.getAsJsonPrimitive());
		} else if (element.isJsonNull()) {
			addNull(key, element.getAsJsonNull());
		} else {
			controller.setNotification("Unknown json type: " + key);
		}
	}

	/**
	 * If it's a JsonArray, handle each object and label them.
	 */
	private void addArray(String key, JsonArray array) {
		for (int i = 0; i < array.size(); i++) {
			addJson(key + "[" + i + "]", array.get(i));
		}
	}

	/**
	 * Add primitive to the UI, and give them the hashcode to identify our primitive with, should they need to change the value.
	 */
	private void addPrimitive(String key, JsonPrimitive primitive) {
		primitives.add(primitive);
		originalVal.add(primitive.getAsString());
		controller.addPair(key, primitive.getAsString(), primitives.size() - 1);
	}

	/**
	 * Add null to the UI, and give them the hashcode to identify our primitive with, should they need to change the value.
	 */
	private void addNull(String key, JsonNull jsonNull) {
		primitives.add(new JsonPrimitive("null"));
		originalVal.add(null);
		controller.addPair(key, "null", primitives.size() - 1);
	}

	/**
	 * Add all sub-objects to the vbox, with labels.
	 */
	private void addObject(String key, JsonObject object) {
		Optional<Integer> index = getIndexOfObject(object);
		String labelText = key + "~{";
		if (index.isPresent()) {
			controller.addObjectLabelWithDuplicateButton(labelText, index.get());
		} else {
			controller.addObjectLabel(labelText); //Probably a sub object. I see no need to copy those
		}
		//If it's not present, it's probably a sub object, which isn't allowed for copies (I see no reason for it)
		for (Map.Entry<String, JsonElement> pair : object.entrySet()) {
			addJson(pair.getKey(), pair.getValue());
		}
		controller.addObjectLabel("}~" + key);
	}

	/**
	 * Gets the index of an object that exists in our elements list.
	 */
	private Optional<Integer> getIndexOfObject(JsonObject object) {
		for (int i = 0; i < elements.size(); i++) {
			if (elements.get(i).equals(object)) {
				return Optional.of(i);
			}
		}
		return Optional.empty();
	}
}
