package hostednetscanner;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Network {
	private Device connectedInterface;
	private String subnetMask;
	private String defaultGateway;
	private Set<Device> knownDevices = new HashSet<>();
	private List<NetworkUpdateListener> listeners = new ArrayList<>();

	public void addNetworkUpdateListener(NetworkUpdateListener listener) {
		listeners.add(listener);
	}

	public void notifyListeners(Set<Device> devices) {
		for (NetworkUpdateListener listener : listeners) {
			listener.onNetworkUpdated(devices);
		}
	}

	private static final String KNOWN_PEERS_FILE = System.getProperty("user.dir") + "/knownPeers.txt";
	private static final String STATUS_FILE = System.getProperty("user.dir") + "/networkStatus.txt";

	public Network(Device connectedInterface) {
		super();
		this.connectedInterface = connectedInterface;
	}

	public Device getConnectedInterface() {
		return connectedInterface;
	}

	public void setConnectedInterface(Device connectedInterface) {
		this.connectedInterface = connectedInterface;
	}

	public String getSubnetMask() {
		return subnetMask;
	}

	public void setSubnetMask(String subnetMask) {
		this.subnetMask = subnetMask;
	}

	public String getDefaultGateway() {
		return defaultGateway;
	}

	public void setDefaultGateway(String defaultGateway) {
		this.defaultGateway = defaultGateway;
	}

	public void setKnownDevices(Set<Device> knownDevices) {
		this.knownDevices = knownDevices;
	}

	public Set<Device> getKnownDevices() {
		return knownDevices;
	}

	public Set<Device> addDevice(Device device) {
		knownDevices.add(device);
		return knownDevices;
	}

	public void updateConnectedDevices() {

	}

	public void monitorNetwork() {
	};

	// Resolve MAC to a name (placeholder implementation)
	public static String resolveMac(String mac) {
		return "UnknownClient_" + mac.substring(mac.length() - 4); // Example resolution
	}

	// Load known peers from file
	private static Map<String, String> loadKnownPeers() throws IOException {
		Map<String, String> knownPeers = new HashMap<>();
		File file = new File(KNOWN_PEERS_FILE);
		if (!file.exists())
			return knownPeers;

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split("-");
				if (parts.length == 2) {
					knownPeers.put(parts[0].trim(), parts[1].trim());
				}
			}
		}
		return knownPeers;
	}

	// Recognize client by MAC; resolve if unknown and save to knownPeers.txt
	public static String recognizeClient(String mac) throws IOException {
		Map<String, String> knownPeers = loadKnownPeers();
		if (knownPeers.containsKey(mac)) {
			return knownPeers.get(mac);
		} else {
			String clientName = resolveMac(mac);
			knownPeers.put(mac, clientName);
			saveKnownPeers(knownPeers);
			return clientName;
		}
	}

	// Save known peers to file
	private static void saveKnownPeers(Map<String, String> knownPeers) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(KNOWN_PEERS_FILE))) {
			for (Map.Entry<String, String> entry : knownPeers.entrySet()) {
				writer.write(entry.getKey() + "-" + entry.getValue());
				writer.newLine();
			}
		}
	}

	// Save status of connected and disconnected clients to a file
//	private static void saveStatus(List<Device> connectedDevices) throws IOException {
//		Set<String> connectedMacs = new HashSet<>();
//		for (Device device : connectedDevices) {
//			connectedMacs.add(device.getMac());
//		}
//
//		try (BufferedWriter writer = new BufferedWriter(new FileWriter(STATUS_FILE))) {
//			// Save connected clients
//			writer.write("Connected (" + connectedDevices.size() + "):\n");
//			for (Client client : connectedDevices) {
//				writer.write(client.toString() + "\n");
//			}
//
//			// Save disconnected clients
//			writer.write("\nDisconnected (" + (allMacs.size() - connectedDevices.size()) + "):\n");
//			for (String mac : allMacs) {
//				if (!connectedMacs.contains(mac)) {
//					String name = recognizeClient(mac); // Fetch the name if previously recognized
//					writer.write(name + " (" + mac + ")\n");
//				}
//			}
//		}
//	}

	@Override
	public int hashCode() {
		return Objects.hash(connectedInterface);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Network other = (Network) obj;
		return Objects.equals(connectedInterface.getIpAddress(), other.connectedInterface.getIpAddress());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Network {\n");
		sb.append("  connectedInterface: ").append(connectedInterface).append(",\n");
		sb.append("  subnetMask: ").append(subnetMask).append(",\n");
		sb.append("  defaultGateway: ").append(defaultGateway).append(",\n");
		sb.append("  activeDevices: [\n");
		for (Device device : knownDevices) {
			sb.append("    ").append(device).append(",\n");
		}
		sb.append("  ]\n");
		sb.append("}");
		return sb.toString();
	}

}
