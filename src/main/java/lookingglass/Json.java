package lookingglass;

import java.util.*;

public class Json {

	public static Object parse(String json) {
		return new Parser(json).parseValue();
	}

	public static String stringify(Object obj) {
		if (obj instanceof Map<?, ?>) {
			StringBuilder sb = new StringBuilder("{");
			boolean first = true;
			for (Map.Entry<?, ?> e : ((Map<?, ?>) obj).entrySet()) {
				if (!first) sb.append(",");
				sb.append("\"").append(e.getKey()).append("\":").append(stringify(e.getValue()));
				first = false;
			}
			return sb.append("}").toString();
		} else if (obj instanceof List<?>) {
			StringBuilder sb = new StringBuilder("[");
			boolean first = true;
			for (Object v : (List<?>) obj) {
				if (!first) sb.append(",");
				sb.append(stringify(v));
				first = false;
			}
			return sb.append("]").toString();
		} else if (obj instanceof String) {
			return "\"" + ((String) obj).replace("\"", "\\\"") + "\"";
		} else if (obj instanceof Boolean || obj instanceof Number) {
			return obj.toString();
		} else if (obj == null) {
			return "null";
		}
		throw new RuntimeException("Unsupported type: " + obj.getClass());
	}

	private static class Parser {
		private final String s;
		private int pos = 0;

		Parser(String s) {
			this.s = s.trim();
		}

		Object parseValue() {
			skipWhite();
			char c = peek();
			if (c == '{') return parseObject();
			if (c == '[') return parseArray();
			if (c == '"') return parseString();
			if (Character.isDigit(c) || c == '-') return parseNumber();
			if (s.startsWith("true", pos)) { pos += 4; return true; }
			if (s.startsWith("false", pos)) { pos += 5; return false; }
			if (s.startsWith("null", pos)) { pos += 4; return null; }
			throw new RuntimeException("Unexpected char at pos " + pos);
		}

		Map<String, Object> parseObject() {
			expect('{');
			Map<String, Object> obj = new LinkedHashMap<>();
			skipWhite();
			if (peek() == '}') { pos++; return obj; }
			while (true) {
				skipWhite();
				String key = parseString();
				skipWhite(); expect(':'); skipWhite();
				Object val = parseValue();
				obj.put(key, val);
				skipWhite();
				if (peek() == '}') { pos++; break; }
				expect(',');
			}
			return obj;
		}

		List<Object> parseArray() {
			expect('[');
			List<Object> arr = new ArrayList<>();
			skipWhite();
			if (peek() == ']') { pos++; return arr; }
			while (true) {
				skipWhite();
				arr.add(parseValue());
				skipWhite();
				if (peek() == ']') { pos++; break; }
				expect(',');
			}
			return arr;
		}

		String parseString() {
			expect('"');
			StringBuilder sb = new StringBuilder();
			while (true) {
				char c = s.charAt(pos++);
				if (c == '"') break;
				if (c == '\\') {
					c = s.charAt(pos++);
					if (c == 'n') sb.append('\n');
					else if (c == 't') sb.append('\t');
					else if (c == 'b') sb.append('\b');
					else if (c == 'r') sb.append('\r');
					else if (c == 'f') sb.append('\f');
					else if (c == '"' || c == '\\' || c == '/') sb.append(c);
					else throw new RuntimeException("Invalid escape at " + pos);
				} else {
					sb.append(c);
				}
			}
			return sb.toString();
		}

		Number parseNumber() {
			int start = pos;
			while (pos < s.length() && "-0123456789.eE".indexOf(s.charAt(pos)) != -1) pos++;
			String num = s.substring(start, pos);
			return num.contains(".") ? Double.parseDouble(num) : Long.parseLong(num);
		}

		void skipWhite() {
			while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
		}

		char peek() {
			if (pos >= s.length()) throw new RuntimeException("Unexpected end");
			return s.charAt(pos);
		}

		void expect(char c) {
			if (peek() != c) throw new RuntimeException("Expected '" + c + "' at pos " + pos);
			pos++;
		}
	}

	// Optional helpers
	public static Map<String, Object> asObject(Object o) {
		return (Map<String, Object>) o;
	}

	public static List<Object> asArray(Object o) {
		return (List<Object>) o;
	}

	public static void main2(String[] args) {
		String json = "{\"name\":\"Alice\",\"age\":30,\"skills\":[\"Java\",\"JSON\"]}";
		Map<String, Object> obj = Json.asObject(Json.parse(json));
		System.out.println("Name: " + obj.get("name"));
		System.out.println("Age: " + obj.get("age"));
		System.out.println("Skills: " + obj.get("skills"));

		String out = Json.stringify(obj);
		System.out.println("Back to JSON: " + out);
	}

	public static String getString(Object object, String... keys) {
		Map<String, Object> map = Json.asObject(object);

		for(int k = 0; k < keys.length - 1; k++)
			map = Json.asObject(map.get(keys[k]));

		return (String) map.get(keys[keys.length - 1]);
	}

	public static void main(String[] args) {
		String json = "{\"name\":\"value\",\"orchestration\":{\"name\":\"orchestration\",\"type\":\"WSTRING\",\"value\":\"default\"},\"payload\":{\"name\":\"payload\",\"type\":\"WSTRING\",\"value\":\"{2434D6B3-DF0A-4471-8FDC-3A917F490E16}\"},\"status\":{\"name\":\"status\",\"type\":\"WSTRING\",\"value\":\"Completion\"}}";
		Object parsed = Json.parse(json);
		System.out.println(parsed);
		String orchestration = Json.getString(parsed, "payload", "value");
		System.out.println(orchestration);

//        Map<String, Object> obj = Json.asObject(Json.parse(json));
//
//        Map<String, Object> payload = Json.asObject(obj.get("payload"));
//
//        String value = (String) payload.get("value");
//        System.out.println(value);

	}
}
