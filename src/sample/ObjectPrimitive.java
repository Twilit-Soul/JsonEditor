package sample;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Allows easy manipulation of a primitive value contained in a JsonObject.
 * Created by mturlington on 10/24/2016.
 */
public class ObjectPrimitive extends JsonEditorPrimitive {
	final private JsonObject parent;
	final private String key;

	ObjectPrimitive(JsonPrimitive old, JsonObject parent, String key) {
		super(old);
		this.key = key;
		this.parent = parent;
	}

	@Override
	void setNew(String string) {
		parent.add(key, getNewPrimitive(string));
	}
}
