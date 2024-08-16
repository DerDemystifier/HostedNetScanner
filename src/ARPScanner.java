import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ARPScanner {

	public static String runARPScan() {
		try {
			String command = System.getProperty("os.name").toLowerCase().contains("win") ? "arp -a" : "arp -an";
			Process process = Runtime.getRuntime().exec(command);

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder output = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append("\n");
			}

			return output.toString();
		} catch (Exception e) {
			System.err.println("Error executing ARP scan: " + e.getMessage());
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Scans the network using the ARP (Address Resolution Protocol) command and
	 * returns a list of networks. The command executed depends on the operating
	 * system: - For Windows: "arp -a" - For Unix-based systems: "arp -an"
	 *
	 * @return a list of Network objects representing the scanned networks. If an
	 *         error occurs, an empty list is returned.
	 */
	public static Set<Network> scanAll() {
		return parse(runARPScan());
	}

	/**
	 * Parses the given ARP output and extracts network and device information.
	 *
	 * @param arpOutput the ARP output as a string
	 * @return a list of Network objects, each containing devices found in the ARP
	 *         output
	 */
	private static Set<Network> parse(String arpOutput) {
		Set<Network> networks = new HashSet<>();
		Network currentNetwork = null;
		Pattern interfacePattern = Pattern.compile("Interface: (\\d+\\.\\d+\\.\\d+\\.\\d+)");

		String[] lines = arpOutput.split("\n");
		boolean headerPassed = false;

		for (String line : lines) {
			line = line.trim();
			if (line.isEmpty())
				continue;

			// Check for new interface
			Matcher interfaceMatcher = interfacePattern.matcher(line);
			if (interfaceMatcher.find()) {
				try {
					InetAddress interfaceIp = InetAddress.getByName(interfaceMatcher.group(1));
					currentNetwork = new Network(new Device(interfaceIp));
					networks.add(currentNetwork);
					headerPassed = false;
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				continue;
			}

			// Skip header line
			if (line.contains("Internet Address") && line.contains("Physical Address")) {
				headerPassed = true;
				continue;
			}

			// Process device entries
			if (headerPassed && currentNetwork != null) {
				String[] parts = line.trim().split("\\s+");
				if (parts.length >= 3) {
					try {
						InetAddress ipAddress = InetAddress.getByName(parts[0]);
						String macAddress = parts[1];
						String type = parts[2];

						if (type.equals("dynamic") && ipAddress.getHostAddress().matches("\\d+\\.\\d+\\.\\d+\\.\\d+")
								&& macAddress.matches("([0-9a-fA-F]{2}[-:]){5}[0-9a-fA-F]{2}")) {

							Device device = new Device(ipAddress, macAddress, currentNetwork);
							currentNetwork.addDevice(device);
						}
					} catch (UnknownHostException e) {
						e.printStackTrace();
					}
				}
			}
		}

		return networks;
	}

	public static Set<Device> parseNetworkDevices(Network network, String arpOutput) {
		Set<Device> devices = new HashSet<>();
		String interfaceIPString = network.getConnectedInterface().getIpAddress().getHostAddress();
		String[] sections = arpOutput.split("Interface:");

		for (String section : sections) {
			if (section.contains(interfaceIPString)) {
				Pattern pattern = Pattern.compile("([\\d.]+)\\s+([\\w-]+)\\s+dynamic");
				Matcher matcher = pattern.matcher(section);

				while (matcher.find()) {
					try {
						InetAddress ipAddress = InetAddress.getByName(matcher.group(1));
						String macAddress = matcher.group(2);
						devices.add(new Device(ipAddress, macAddress));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				break;
			}
		}

		return devices;
	}

	/**
	 * Scans the network and returns the network object associated with the
	 * specified interface IP address.
	 *
	 * @param interfaceIP the IP address of the network interface to scan for
	 * @return the Network object associated with the specified interface IP
	 *         address, or null if no matching network is found
	 */
	public static Network scan(InetAddress interfaceIP) {
		return scanAll().stream().filter(network -> network.getConnectedInterface().getIpAddress().equals(interfaceIP))
				.findFirst().orElse(null);
	}

	public static InetAddress getIpAddress(Network network, String macAddress) {
		// Run ARP scan on the specific network
		String arpScanOutput = runARPScan();

		// Parse the network devices from the ARP scan output
		Set<Device> devices = parseNetworkDevices(network, arpScanOutput);

		// Find the device with the matching MAC address and return its IP address
		return devices.stream().filter(device -> device.getMacAddress().equals(macAddress)).findFirst()
				.map(Device::getIpAddress).orElse(null);
	}

	public static String getMacAddress(InetAddress ipAddress) {
		return scanAll().stream().map(Network::getKnownDevices).flatMap(Set::stream)
				.filter(device -> device.getIpAddress().equals(ipAddress)).findFirst().map(Device::getMacAddress)
				.orElse(null);
	}

	/**
	 * Scans the given network and returns a list of active devices.
	 *
	 * @param net the network to scan
	 * @return a list of active devices in the specified network, or null if the
	 *         network is not found
	 */
	public static Set<Device> getDiscoveredDevices(Network net) {
		String arpOutput = runARPScan();
		return parseNetworkDevices(net, arpOutput);
	}
}
