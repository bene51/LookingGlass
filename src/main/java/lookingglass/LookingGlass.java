package lookingglass;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Random;

/*
 * Some links:
 * https://lookingglassfactory.com/software/looking-glass-bridge
 * https://docs.lookingglassfactory.com/keyconcepts/quilts
 * https://docs.lookingglassfactory.com/software/looking-glass-bridge-sdk/web-application-integration#rest-api-reference
 * https://github.com/Looking-Glass/bridge.js/blob/bd05e2a46275fe2b40f756d2b8113fe3f7ba652c/src/library/components/endpoints.ts#L103
 * https://github.com/Looking-Glass/bridge.js/blob/main/src/library/components/endpoints.ts
 */
public class LookingGlass {

	public static class LookingGlassException extends Exception {
		public LookingGlassException(String msg) {
			super(msg);
		}

		public LookingGlassException(String msg, Throwable cause) {
			super(msg, cause);
		}
	}

	public static class Response {
		private final StringBuilder stdin = new StringBuilder();
		private final StringBuilder stdout = new StringBuilder();
		private final StringBuilder stderr = new StringBuilder();

		private int status;

		public String getStdIn() {
			return stdin.toString();
		}

		public String getStdOut() {
			return stdout.toString();
		}

		public String getStdErr() {
			return stderr.toString();
		}

		public int getStatus() {
			return status;
		}

		public String toString() {
			return "Response (status: " + status + "):"
					+ "\n  stdin:\n  " + stdin
					+ "\n  stdout:\n  " + stdout
					+ "\n  stderr:\n  " + stderr + "\n";
		}
	}

	public static class Hologram {
		public final String uri;
		public final int rows;
		public final int columns;
		public final double aspect;
		public final double focus;
		public final int viewCount;

		public Hologram(String uri, int rows, int columns, double aspect, double focus, int viewCount) {
			this.uri = uri;
			this.rows = rows;
			this.columns = columns;
			this.aspect = aspect;
			this.focus = focus;
			this.viewCount = viewCount;
		}
	}

	public static class PlaylistItem {
		public final Hologram hologram;
		public final String orchestration;
		public final int id;
		public final int index;
		public final String playlistName;

		public PlaylistItem(Hologram hologram, int id, int index, String playlistName, String orchestration) {
			this.hologram = hologram;
			this.orchestration = orchestration;
			this.id = id;
			this.index = index;
			this.playlistName = playlistName;
		}

	}

	public static class Playlist {
		public final String name;
		public final boolean loop;
		public final ArrayList<PlaylistItem> items;
		public final String orchestration;

		public Playlist(String name, boolean loop, String orchestration) {
			this.name = name;
			this.loop = loop;
			this.orchestration = orchestration;
			this.items = new ArrayList<>();
		}

		public void addItem(Hologram hologram) {
			PlaylistItem item = new PlaylistItem(hologram, this.items.size(), this.items.size(), this.name, this.orchestration);
			this.items.add(item);
		}

		public void play() throws LookingGlassException {
			sendMessage("instance_playlist", "{" +
					"\"orchestration\": \"" + orchestration + "\"," +
					"\"name\": \"" + name + "\"," +
					"\"loop\": " + loop +
					"}");

			for(PlaylistItem item : items) {
				sendMessage("insert_playlist_entry", "{" +
						"\"orchestration\": \"" + item.orchestration + "\"," +
						"\"id\": " + item.id + "," +
						"\"name\": \"" + item.playlistName + "\"," +
						"\"index\": " + item.index + "," +
						"\"uri\": \"" + item.hologram.uri + "\"," +
						"\"rows\": " + item.hologram.rows + "," +
						"\"cols\": " + item.hologram.columns + "," +
						"\"focus\": " + item.hologram.focus + "," +
						"\"zoom\": 1," +
						"\"crop_pos_x\": 0," +
						"\"crop_pos_y\": 0," +
						"\"aspect\": " + item.hologram.aspect + "," +
						"\"view_count\": " + item.hologram.viewCount + "," +
						"\"isRGBD\": 0," +
						"\"tag\": \"\"" +
						"}");
			}

			sendMessage("play_playlist", "{" +
					"\"orchestration\": \"" + orchestration + "\"," +
					"\"name\": \"" + name + "\"," +
					"\"head_index\": -1" +
					"}");
		}
	}

	public static final String BASE_URL = "http://localhost:33334/";

	public static Response sendMessage(String endpoint, String jsonInput) throws LookingGlassException {
		Response response = new Response();
		int responseCode;
		try {
			URL url = new URL(BASE_URL + endpoint);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			// Set request method
			conn.setRequestMethod("PUT");

			response.stdin.append("PUT " + url + "\n");
			response.stdin.append(jsonInput + "\n");
			conn.setRequestProperty("Content-Type", "application/json");

			if(jsonInput != null) {
				conn.setDoOutput(true);
				try (OutputStream os = conn.getOutputStream()) {
					os.write(jsonInput.getBytes(StandardCharsets.UTF_8));
				}
			}

			responseCode = conn.getResponseCode();
			response.status = responseCode;
			readResponse(conn, response);
		} catch (Exception e) {
			throw new LookingGlassException("Error sending message: " + response, e);
		}

		if(responseCode != 201 && responseCode != 200) {
			throw new LookingGlassException("Error sending message " + response);
		}
		System.out.println(response);
		return response;
	}

	private static void readResponse(HttpURLConnection conn, Response response) throws LookingGlassException {
		try {
			String line;
			InputStream stream = null;
			try {
				stream = conn.getInputStream();
			} catch (IOException ignored) {
				// This happens when the HTTP response code indicates an error,
				// in which case we might be able to read from the error stream below
			}
			if(stream != null) {
				try (BufferedReader in = new BufferedReader(
						new InputStreamReader(stream))) {
					while ((line = in.readLine()) != null) {
						response.stdout.append(line + "\n");
					}
				}
			}

			stream = conn.getErrorStream();
			if(stream != null) {
				try (BufferedReader err = new BufferedReader(
						new InputStreamReader(stream))) {
					while ((line = err.readLine()) != null) {
						response.stderr.append(line + "\n");
					}
				}
			}
		} catch(Exception e) {
			throw new LookingGlassException("Error reading HTTP response", e);
		}
	}

	public static String enterOrchestration() throws LookingGlassException {
		Response response = LookingGlass.sendMessage(
				"enter_orchestration",
				"{\"name\": \"default\"}");
		return Json.getString(Json.parse(response.getStdOut()), "payload", "value");
	}

	public static void availableOutputDevices(String orchestration) throws LookingGlassException {
		Response response = LookingGlass.sendMessage(
				"available_output_devices",
				"{\"orchestration\": \"" + orchestration + "\"}");
		System.out.println(response.getStdOut());
	}

	private static String getRandomString(int targetStringLength) {
		int leftLimit = 48; // numeral '0'
		int rightLimit = 122; // letter 'z'
		Random random = new Random();

		return random.ints(leftLimit, rightLimit + 1)
				.filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
				.limit(targetStringLength)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
				.toString();
	}

	private final ArrayList<Playlist> playlists = new ArrayList<>();

	public void cast(String orchestration, Hologram hologram) throws LookingGlassException {
		String name = getRandomString(7);
		Playlist playlist = new Playlist(name, true, orchestration);
		playlists.add(playlist);
		playlist.addItem(hologram);
		playlist.play();
	}

	public static void main(String[] args) throws LookingGlassException {
		String jsonInput = null;
//        String jsonInput = "{"
//                + "\"model\": \"bla\","
//                + "\"from\": \"mistral\","
//                + "\"adapters\": {"
//                + "},"
//                + "\"template\": \"" + "{{ .Prompt }}" + "\","
//                + "\"parameters\": {"
//                +   "\"stop\": [\"</s>\", \"<s>\", \"<unk>\", \"### Response:\", \"### Instruction:\", \"### Input:\"],"
//                +   "\"temperature\": 0.1,"
//                +   "\"top_k\": 50,"
//                +   "\"top_p\": 1.0,"
//                +   "\"repeat_penalty\": 1.0,"
//                +   "\"num_predict\": 4096"
//                + "}"
//                + "}";

		Response response = LookingGlass.sendMessage("bridge_version", "");
		System.out.println(response);

		String orchestration = LookingGlass.enterOrchestration();
		System.out.println(orchestration);

		LookingGlass.availableOutputDevices(orchestration);

		LookingGlass lg = new LookingGlass();
		Hologram hologram = new Hologram("D:/jhuisken/4Benjamin/lookingglass9431599197945292202_quilt_qs8x6a0.75.jpg", 6, 8, 0.75, -0.01, 48);
		// Hologram hologram = new Hologram("D:/jhuisken/4Benjamin/steampunk_quilt_qs8x13a0.75.jpg", 13, 8, 0.75, 124);
		// Hologram hologram = new Hologram("https://s3.amazonaws.com/lkg-blocks/u/9aa4b54a7346471d/steampunk_qs8x13.jpg", 13, 8, 0.75, 104);
		lg.cast(orchestration, hologram);

	}
}
