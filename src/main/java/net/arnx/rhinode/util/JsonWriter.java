/*
 * Copyright 2014 Hidekatsu Izuno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.arnx.rhinode.util;

import java.io.Flushable;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

public class JsonWriter {
	private static final int[] ESCAPE_CHARS = new int[128];

	static {
		for (int i = 0; i < 32; i++) {
			ESCAPE_CHARS[i] = -1;
		}
		ESCAPE_CHARS['\b'] = 'b';
		ESCAPE_CHARS['\t'] = 't';
		ESCAPE_CHARS['\n'] = 'n';
		ESCAPE_CHARS['\f'] = 'f';
		ESCAPE_CHARS['\r'] = 'r';
		ESCAPE_CHARS['"'] = '"';
		ESCAPE_CHARS['\\'] = '\\';
		ESCAPE_CHARS[0x7F] = -1;
	}

	private Appendable out;
	private boolean prettyPrint;
	private int initialIndent;

	private int depth = -1;
	private Stack stack = new Stack();

	public JsonWriter(Appendable out) {
		this.out = out;
	}

	public JsonWriter prettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
		return this;
	}

	public boolean prettyPrint() {
		return prettyPrint;
	}

	public JsonWriter initialIndent(int initialIndent) {
		this.initialIndent = initialIndent;
		return this;
	}

	public int initialIndent() {
		return initialIndent;
	}

	public JsonWriter beginObject() throws IOException {
		State state = stack.peek();
		if(state == null) {
			if (prettyPrint) {
				appendIndent(out, 0);
			}
		} else if (state.type == JSONDataType.OBJECT) {
			if (state.name == null) {
				throw new IllegalStateException("You should not call beginObject in this context.");
			}
			state.index++;
		} else if (state.type == JSONDataType.ARRAY) {
			if (state.index > 0) {
				out.append(',');
			}
			if (prettyPrint) {
				out.append('\n');
				appendIndent(out, depth + 1);
			}
			state.index++;
		} else {
			throw new IllegalStateException();
		}

		depth++;
		stack.push(JSONDataType.OBJECT);
		out.append('{');
		return this;
	}

	public JsonWriter endObject() throws IOException {
		State state = stack.peek();
		if(state == null) {
			throw new IllegalStateException("You should not call endObject in this context.");
		} else if (state.type == JSONDataType.OBJECT) {
			if (prettyPrint && state.index > 0) {
				out.append('\n');
				appendIndent(out, depth);
			}
		} else {
			throw new IllegalStateException("Array is not closed.");
		}

		out.append('}');
		stack.pop();
		depth--;

		if (stack.size() == 0) {
			flush();
		}
		return this;
	}

	public JsonWriter beginArray() throws IOException {
		State state = stack.peek();
		if(state == null) {
			if (prettyPrint) {
				appendIndent(out, 0);
			}
		} else if (state.type == JSONDataType.OBJECT) {
			if (state.name == null) {
				throw new IllegalStateException("You should not call beginArray in this context.");
			}
			state.index++;
		} else if (state.type == JSONDataType.ARRAY) {
			if (state.index > 0) {
				out.append(',');
			}
			if (prettyPrint) {
				out.append('\n');
				appendIndent(out, depth + 1);
			}
			state.index++;
		} else {
			throw new IllegalStateException();
		}

		depth++;
		stack.push(JSONDataType.ARRAY);
		out.append('[');
		return this;
	}

	public JsonWriter endArray() throws IOException {
		State state = stack.peek();
		if(state == null) {
			throw new IllegalStateException("You should not call endArray in this context.");
		} else if (state.type == JSONDataType.ARRAY) {
			if (prettyPrint && state.index > 0) {
				out.append('\n');
				appendIndent(out, depth);
			}
		} else {
			throw new IllegalStateException("Object is not closed.");
		}

		out.append(']');
		stack.pop();
		depth--;

		if (stack.size() == 0) {
			flush();
		}
		return this;
	}

	public JsonWriter name(String name) throws IOException {
		State state = stack.peek();
		if (state == null) {
			throw new IllegalStateException("You should not call name in this context.");
		} else if (state.type == JSONDataType.OBJECT) {
			state.name = name;

			if (state.index > 0) {
				out.append(',');
			}
			if (prettyPrint) {
				out.append('\n');
				appendIndent(out, depth + 1);
			}
		} else {
			throw new IllegalStateException("You should not call name in this context.");
		}

		formatString(name, out);

		out.append(':');
		if (prettyPrint) {
			out.append(' ');
		}

		return this;
	}

	public JsonWriter value(Object value) throws IOException {
		if (value instanceof Iterable<?>) {
			beginArray();
			for (Object child : (Iterable<?>)value) {
				value(child);
			}
			endArray();
			return this;
		} else if (value instanceof Map<?, ?>) {
			beginObject();
			for (Map.Entry<?, ?> entry : ((Map<?,?>)value).entrySet()) {
				Object key = entry.getKey();
				name(key != null ? key.toString() : "null");
				value(entry.getValue());
			}
			endObject();
			return this;
		}

		State state = stack.peek();
		if(state == null) {
			if (prettyPrint) {
				appendIndent(out, 0);
			}
		} else if (state.type == JSONDataType.OBJECT) {
			if (state.name == null) {
				throw new IllegalStateException("You should not call value in this context.");
			}
			state.index++;
		} else if (state.type == JSONDataType.ARRAY) {
			if (state.index > 0) {
				out.append(',');
			}
			if (prettyPrint) {
				out.append('\n');
				appendIndent(out, depth + 1);
			}
			state.index++;
		} else {
			throw new IllegalStateException();
		}

		if (value == null) {
			out.append("null");
		} else if (value instanceof BigDecimal) {
			out.append(((BigDecimal)value).toPlainString());
		} else if (value instanceof Boolean || value instanceof Number) {
			out.append(value.toString());
		} else {
			formatString(value.toString(), out);
		}

		if (stack.size() == 0) {
			flush();
		}
		return this;
	}

	public JsonWriter flush() throws IOException {
		if (out instanceof Flushable) {
			((Flushable)out).flush();
		}
		return this;
	}

	private void appendIndent(Appendable out, int depth) throws IOException {
		int indent = initialIndent() + depth;
		for (int i = 0; i < indent; i++) {
			out.append('\t');
		}
	}

	private static void formatString(String s, Appendable out) throws IOException {
		out.append('"');
		int start = 0;
		final int length = s.length();
		for (int i = 0; i < length; i++) {
			int c = s.charAt(i);
			if (c < ESCAPE_CHARS.length) {
				int x = ESCAPE_CHARS[c];
				if (x == 0) {
					// no handle
				} else if (x > 0) {
					if (start < i) out.append(s, start, i);
					out.append('\\');
					out.append((char)x);
					start = i + 1;
				} else if (x == -1) {
					if (start < i) out.append(s, start, i);
					out.append("\\u00");
					out.append("0123456789ABCDEF".charAt(c / 16));
					out.append("0123456789ABCDEF".charAt(c % 16));
					start = i + 1;
				}
			} else if (c == '\u2028') {
				if (start < i) out.append(s, start, i);
				out.append("\\u2028");
				start = i + 1;
			} else if (c == '\u2029') {
				if (start < i) out.append(s, start, i);
				out.append("\\u2029");
				start = i + 1;
			}
		}
		if (start < length) out.append(s, start, length);
		out.append('"');
	}

	private static enum JSONDataType {
		OBJECT,
		ARRAY,
		STRING,
		NUMBER,
		BOOLEAN,
		NULL
	}

	private static class Stack {
		private int size = 0;
		private State[] list = new State[8];

		public State push(JSONDataType type) {
			size++;
			if (size >= list.length) {
				State[] newList = new State[Math.max(size, list.length) * 2];
				System.arraycopy(list, 0, newList, 0, list.length);
				list = newList;
			}
			State state;
			if (list[size] != null) {
				state = list[size];
				state.name = null;
				state.index = 0;
			} else {
				state = new State();
				list[size] = state;
			}
			state.type = type;
			return state;
		}

		public State peek() {
			if (size < list.length) {
				return list[size];
			} else {
				return null;
			}
		}

		public State pop() {
			if (size >= 0 && size < list.length) {
				return list[size--];
			} else {
				return null;
			}
		}

		public int size() {
			return size;
		}
	}

	static final class State {
		public JSONDataType type;
		public String name;
		public int index = 0;
	}
}
