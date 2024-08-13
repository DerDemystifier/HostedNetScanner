import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IPConfigScanner {
	public static List<Network> scanNetworks() {
		try {
			Process process = Runtime.getRuntime().exec("ipconfig /all");
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder output = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append("\n");
			}

			List<Network> networks = new ArrayList<>();
			List<IPConfigEntry> ipConfigEntries = parse(output.toString());
			for (IPConfigEntry entry : ipConfigEntries) {
				Device connectedInterface = entry._interface;
				Network network = new Network(connectedInterface);
				network.setSubnetMask(entry.subnetMask);
				network.setDefaultGateway(entry.defaultGateway);
				network.addActiveDevice(connectedInterface);
				networks.add(network);
			}

			return networks;
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	private static List<IPConfigEntry> parse(String rawOutput) {
		List<IPConfigEntry> ipConfigEntries = new ArrayList<>();
		String[] sections = rawOutput.split("(?=\\r?\\n\\r?\\n[^\\s].+:)");

		for (String section : sections) {
			if (section.contains("adapter") && !section.contains("Media disconnected")) {
				// Extract description/name
				Pattern descPattern = Pattern.compile("Description[^:]+:(.+)");
				Matcher descMatcher = descPattern.matcher(section);

				// Extract Physical Address (MAC)
				Pattern macPattern = Pattern.compile("Physical Address[^:]+:(.+)");
				Matcher macMatcher = macPattern.matcher(section);

				// Extract IPv4 Address
				Pattern ipPattern = Pattern.compile("IPv4 Address[^:]+:([^\\(]+)");
				Matcher ipMatcher = ipPattern.matcher(section);

				// Extract Subnet Mask
				Pattern maskPattern = Pattern.compile("Subnet Mask[^:]+:(.+)");
				Matcher maskMatcher = maskPattern.matcher(section);

				// Extract Default Gateway
				Pattern gatewayPattern = Pattern.compile("Default Gateway[^:]+:*(.+)");
				Matcher gatewayMatcher = gatewayPattern.matcher(section);

				if (descMatcher.find() && macMatcher.find() && ipMatcher.find()) {
					String description = descMatcher.group(1).trim();
					String mac = macMatcher.group(1).trim();
					String ip = ipMatcher.group(1).trim();
					InetAddress ipAddress = null;
					try {
						ipAddress = InetAddress.getByName(ip);
					} catch (UnknownHostException e) {
						e.printStackTrace();
					}

					Device device = new Device(ipAddress, mac);
					device.setStatus("connected");

					String subnetMask = null;
					String defaultGateway = null;
					if (maskMatcher.find()) {
						subnetMask = maskMatcher.group(1).trim();
					}

					if (gatewayMatcher.find()) {
						defaultGateway = gatewayMatcher.group(1).trim();
					}
					System.out.println(defaultGateway);

					ipConfigEntries.add(new IPConfigEntry(device, subnetMask, defaultGateway));
				}
			}
		}

		return ipConfigEntries;
	}

	static class IPConfigEntry {
		public Device _interface;
		public String subnetMask;
		public String defaultGateway;

		public IPConfigEntry(Device _interface, String subnetMask, String defaultGateway) {
			this._interface = _interface;
			this.subnetMask = subnetMask;
			this.defaultGateway = defaultGateway;
		}
	}
}
