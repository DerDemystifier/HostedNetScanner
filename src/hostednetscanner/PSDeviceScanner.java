package hostednetscanner;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class PSDeviceScanner {

	private static Process powerShellProcess;
	private static BufferedWriter writer;
	private static BufferedReader reader;
	private static boolean initialized = false;

	public PSDeviceScanner() throws IOException {
		initialize();
	}

	private static synchronized void initialize() throws IOException {
		if (!initialized) {
			ProcessBuilder processBuilder = new ProcessBuilder("powershell.exe", "-NoExit", "-Command", "-");
			processBuilder.redirectErrorStream(true);

			powerShellProcess = processBuilder.start();

			writer = new BufferedWriter(
					new OutputStreamWriter(powerShellProcess.getOutputStream(), StandardCharsets.UTF_8));
			reader = new BufferedReader(
					new InputStreamReader(powerShellProcess.getInputStream(), StandardCharsets.UTF_8));
			initialized = true;
		}
	}

	public String executePowerShellScript(String ps_script) {
		try {
			// Write the script to PowerShell's stdin
			writer.write(ps_script);
			writer.newLine();
			writer.flush();
			writer.write("Write-Output \"EndOfScript\"\n");
			writer.flush();

			// Read the output from PowerShell's stdout
			StringBuilder output = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.equals("EndOfScript")) {
					break;
				}
				output.append(line).append(System.lineSeparator());
			}
			return output.toString();
		} catch (IOException e) {
			e.printStackTrace();
			Logger.logError("Error executing PowerShell script: ", e);
			return "Exception occurred: " + e.getMessage();
		}
	}

	public String executePowerShellCommand(String command) {
		try {
			writer.write(command);
			writer.newLine();
			writer.flush();
			writer.write("Write-Output \"EndOfScript\"\n");
			writer.flush();

			StringBuilder output = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.equals("EndOfScript")) {
					break;
				}
				output.append(line).append(System.lineSeparator());
			}
			return output.toString();
		} catch (IOException e) {
			e.printStackTrace();
			Logger.logError("Error executing PowerShell command: ", e);
			return "Exception occurred: " + e.getMessage();
		}
	}

	public Set<Device> getReachableDevices() throws IOException {
		String ps_script = """
				try {
				    # Get network adapter information
				    $adapter = Get-NetAdapter | Where-Object {$_.Status -eq "Up" -and $_.Name -like "*HostedNetwork*"}

				    if ($adapter) {
				        # Get connected devices using Get-NetNeighbor
				        $devices = Get-NetNeighbor -AddressFamily IPv4 -LinkLayerAddress "??-??-??-??-??-??" -InterfaceIndex $adapter.ifIndex |
				            Where-Object {$_.State -eq "Reachable"}

				        if ($devices) {
				            Write-Host "`nConnected Devices:"
				            Write-Host "-------------------"
				            foreach ($device in $devices) {
				                Write-Host "IP Address: $($device.IPAddress)"
				                Write-Host "MAC Address: $($device.LinkLayerAddress)"
				                Write-Host "State: $($device.State)"
				                Write-Host "-------------------"
				            }
				        }
				        else {
				            Write-Host "No devices currently connected."
				        }
				    }
				    else {
				        Write-Host "No active WiFi adapter found."
				    }
				}
				catch {
				    Write-Error "Error getting connected devices: $_"
				}
				""";

		Set<Device> reachableDevices = new HashSet<>();

		// Read the output of the command
		Scanner reader = new Scanner(executePowerShellScript(ps_script));
		String line;
		String ipAddress = null;
		String macAddress = null;

		while (reader.hasNextLine()) {
			line = reader.nextLine();
			if (line.startsWith("IP Address")) {
				ipAddress = line.split(":\\s+")[1].trim();
			} else if (line.startsWith("MAC Address")) {
				macAddress = line.split(":\\s+")[1].trim();
			}

			if (ipAddress != null && macAddress != null) {
				try {
					InetAddress inetAddress = InetAddress.getByName(ipAddress);
					Device device = new Device(inetAddress, macAddress);
					reachableDevices.add(device);
				} catch (UnknownHostException e) {
					System.err.println("Error: Unknown host - " + ipAddress);
				}
				ipAddress = null;
				macAddress = null;
			}
		}

		reader.close();

		return reachableDevices;
	}

	public static void close() {
		try {
			if (writer != null) {
				writer.close();
			}
			if (reader != null) {
				reader.close();
			}
			if (powerShellProcess != null) {
				powerShellProcess.destroy();
			}
			initialized = false;
		} catch (IOException e) {
			e.printStackTrace();
			Logger.logError("Error closing PowerShell process: ", e);
		}
	}

}
