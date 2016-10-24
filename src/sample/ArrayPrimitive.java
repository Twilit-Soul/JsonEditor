package sample;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

/**
 * Allows easy manipulation of a primitive value contained in a JsonArray.
 * Created by mturlington on 10/24/2016.
 */
public class ArrayPrimitive extends JsonEditorPrimitive {
	final private JsonArray parent;
	final private int index;

	ArrayPrimitive(JsonPrimitive old, JsonArray parent, int index) {
		super(old);
		this.index = index;
		this.parent = parent;
	}

	@Override
	void setNew(String string) {
		parent.set(index, getNewPrimitive(string));
	}
}
