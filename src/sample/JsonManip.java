package sample;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * Should handle reading/modifying the json data from the file.
 * Created by mturlington on 6/27/2016.
 */
class JsonManip {
	/**
	 * JsonPrimitive values are saved so they can be edited at will via the UI.
	 */
	private List<JsonPrimitive> primitives  = new ArrayList<>();
	/**
	 * Original value is saved so we can have UI notifications on "modified" values,
	 * and to reset values to their original state.
	 */
	private List<String>        originalVal = new ArrayList<>();

	/**
	 * We deserialize everything when we save to file.
	 */
	private List<JsonObject> elements;
	/**
	 * Our link back to the UI.
	 */
	private Controller       controller;

	/**
	 * Gets the json java objects from the file, and remembers the UI controller.
	 */
	JsonManip(Controller controller, Path filePath) throws IOException {
		elements = getGson(filePath);
		this.controller = controller;
	}

	/**
	 * If the file didn't return any objects or it only returned an empty object.
	 */
	boolean isEmpty() {
		return elements.isEmpty() || elements.get(0).toString().equals("{}");
	}

	/**
	 * Combines all of the java objects into a single json string.
	 */
	String getJson() {
		String json = "";
		Gson gson = new Gson();
		for (int i = 0; i < elements.size(); i++) {
			json += gson.toJson(elements.get(i));
			if (i < elements.size() - 1) {
				json += "\n";
			}
		}
		return json;
	}

	/**
	 * Go through each Json element and add it, using the line number as a label.
	 * Note: it's the line of the object, not the line in the file. Comments can distort this.
	 */
	void addElementsToUI() {
		for (int i = 0; i < elements.size(); i++) {
			addJson("Line[" + i + "]", elements.get(i));
		}
	}

	/**
	 * Get the json java objects from the given filepath.
	 */
	private List<JsonObject> getGson(Path filePath) throws IOException {
		JsonParser parser = new JsonParser();
		List<String> fileContents = Files.readAllLines(filePath);
		return fileContents.stream().filter(l -> l.startsWith("{")).map(l -> (JsonObject) parser.parse(l)).collect(Collectors.toList());
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
		} else {
			System.out.println("Unknown json type: " + key);
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
	 * Returns the value this primitive had when it was loaded.
	 */
	String getOriginalVal(int index) {
		return originalVal.get(index);
	}

	/**
	 * Set the value of the primitive object.
	 */
	void setPairValue(int index, String newVal) {
		JsonPrimitive primitive = primitives.get(index);
		if (primitive.isBoolean()) {
			primitive.setValue(newVal.equalsIgnoreCase("true"));
			return;
		}
		try {
			primitive.setValue(Integer.parseInt(newVal));
		} catch (NumberFormatException e1) {
			primitive.setValue(newVal);
		}
	}

	/**
	 * Add all sub-objects to the vbox, with labels.
	 */
	private void addObject(String key, JsonObject object) {
		controller.addBoldLabel(key + "~{");
		for (Map.Entry<String, JsonElement> pair : object.entrySet()) {
			addJson(pair.getKey(), pair.getValue());
		}
		controller.addBoldLabel("}~" + key);
	}
}
