package sample;

import java.io.IOException;
import java.nio.file.Path;

import com.google.gson.JsonObject;

/**
 * Is this how dependency inversion works?
 * Created by mturlington on 6/29/2016.
 */
interface IJsonManip {
	/**
	 * If the file didn't return any objects or it only returned an empty object.
	 */
	boolean isEmpty();

	/**
	 * Go through each Json element and add it, using the file name as a label.
	 */
	void addElementsToUI();

	/**
	 * Returns the value this primitive had when it was loaded.
	 */
	String getOriginalVal(int index);

	/**
	 * Set the value of the primitive object.
	 */
	void setPairValue(int index, String newVal);

	/**
	 * Deserializes and saves to file.
	 */
	void saveData(Path filePath) throws IOException;

	/**
	 * Gets object by index and duplicates it in our elements list.
	 */
	void duplicateAndAddToList(int index);

	/**
	 * Determines whether or not elements have changed from what they originally were.
	 */
	boolean haveElementsChanged();

	/**
	 * Undoes any changes in elements.
	 */
	void discardElementsChanges();
}
