package hostednetscanner;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PersistentPowerShell {

	public Process process;
	private BufferedWriter writer;
	private BufferedReader reader;
	private ExecutorService outputReaderThread;

	public PersistentPowerShell() throws IOException {
		ProcessBuilder processBuilder = new ProcessBuilder("powershell.exe", "-NoExit", "-Command", "-");
		processBuilder.redirectErrorStream(true); // Merge error and output streams
		process = processBuilder.start();

		writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
		reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

		// Use a separate thread to continuously read output
		outputReaderThread = Executors.newSingleThreadExecutor();
		outputReaderThread.submit(() -> {
			try {
				String line;
				while ((line = reader.readLine()) != null) {
					System.out.println("PowerShell Output: " + line); // Or store it somewhere
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	public void executeCommand(String command) throws IOException {
		writer.write(command);
		writer.newLine();
		writer.write("Write-Host 'EndOfCommand'\n"); // Marker to identify the end of command output
		writer.flush();
	}

	public String executeCommandSync(String command) throws IOException, InterruptedException {
		StringBuilder output = new StringBuilder();
		executeCommand(command);

		String line;
		while (true) {
			line = reader.readLine();
			if (line == null) {
				// Handle unexpected PowerShell process termination
				throw new IOException("PowerShell process terminated unexpectedly.");
			}
			if (line.equals("EndOfCommand")) {
				break;
			}
			output.append(line).append(System.lineSeparator());
		}

		return output.toString().trim();
	}

	public void close() {
		try {
			writer.write("exit\n"); // Gracefully exit PowerShell
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			process.destroy();
			outputReaderThread.shutdown();
		}
	}
}