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
	private PersistentPowerShell psInstance;

	private PersistentPowerShell getPSInstance() throws IOException {
		if (psInstance == null)
			return new PersistentPowerShell();
		else
			return psInstance;
	}

	public static String executePowerShellScript() {
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

		ProcessBuilder processBuilder = new ProcessBuilder("powershell.exe", "-Command", "-");

		// Set up the process environment
		processBuilder.redirectErrorStream(true); // Merge error and output streams

		Process process = null;
		try {
			process = processBuilder.start();

			// Write the script to PowerShell's stdin
			try (BufferedWriter writer = new BufferedWriter(
					new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
				writer.write(ps_script);
				writer.newLine();
				writer.flush();
			}

			// Read the output from PowerShell's stdout
			StringBuilder output = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line).append(System.lineSeparator());
				}
			}

			// Wait for the process to finish
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				return "PowerShell script execution failed with exit code: " + exitCode;
			}

			return output.toString();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return "Exception occurred: " + e.getMessage();
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
	}

	public static Set<Device> getReachableDevices() throws IOException {
		Set<Device> reachableDevices = new HashSet<>();

		// Read the output of the command
		Scanner reader = new Scanner(executePowerShellScript());
		String line;
		String ipAddress = null;
		String macAddress = null;

		while (reader.hasNextLine()) {
			line = reader.nextLine();
			System.out.println(line);
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
}
