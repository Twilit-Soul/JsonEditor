package sample;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * To help hold together old values and new.
 * Created by mturlington on 7/27/2016.
 */
class JsonEditorPrimitive {
	final private JsonPrimitive oldPrimitive;
	final private JsonObject    parent;
	final private String        key;

	JsonEditorPrimitive(JsonPrimitive old, JsonObject parent, String key) {
		oldPrimitive = old;
		this.parent = parent;
		this.key = key;
	}

	JsonPrimitive getOld() {
		return oldPrimitive;
	}

	void setNew(String string) {
		parent.add(key, getNewPrimitive(string));
	}

	/**
	 * Returns a new JsonPrimitive of an appropriate type, which might actually be JsonNull instead of
	 * JsonPrimitive.
	 */
	private JsonElement getNewPrimitive(String newVal) {
		if (newVal.equals("null")) {
			return JsonNull.INSTANCE;
		}
		if (getOld().isBoolean()) {
			return new JsonPrimitive(newVal.equalsIgnoreCase("true"));
		} else if (getOld().isNumber()) {
			try {
				return new JsonPrimitive(Integer.parseInt(newVal));
			} catch (NumberFormatException e) {
				//We'll assign it as a string later, then.
			}
		}

		return new JsonPrimitive(newVal);
	}
}
