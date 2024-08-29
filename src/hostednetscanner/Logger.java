package hostednetscanner;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

public class Logger {
	private static String logFilePath = System.getProperty("user.dir") + "/logs.html";

	public static void logMessage(String message) {
		log("INFO", message);
	}

	public static void logError(String error, Throwable throwable) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		log("ERROR", error + "\nStack trace:\n" + sw.toString());
	}

	private static void log(String level, String message) {
		try (FileWriter fw = new FileWriter(logFilePath, true)) {
			// Initialize HTML file if empty
			fw.write("<p><pre>" + LocalDateTime.now() + " [<span style=\"color:"
					+ (level.equals("ERROR") ? "red" : "black") + "\">" + level + "</span>] " + message
					+ "</pre></p>\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
