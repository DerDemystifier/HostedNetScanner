package hostednetscanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public class Network {
	private Device connectedInterface;
	private String subnetMask;
	private String defaultGateway;
	private Set<Device> knownDevices = new HashSet<>();
	private Set<NetworkUpdateListener> listeners = new HashSet<>();

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

	/**
	 * Load known peers from file using ConfigManager.
	 */
	public static Map<String, String> loadKnownPeers() throws IOException {
		ConfigManager configManager = new ConfigManager();
		String knownDevicesPath = configManager.getKnownDevicesFilePath();
		Map<String, String> knownPeers = new HashMap<>();
		File file = new File(knownDevicesPath);
		if (!file.exists())
			return null;

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(Pattern.quote("||"));
				if (parts.length == 2) {
					String mac = Device.formatMacAddress(parts[0].trim());
					String name = parts[1].trim();
					knownPeers.put(mac, name);
				}
			}
		}
		return knownPeers;
	}

	// Recognise client by MAC;
	public static String recognizeClient(String mac) throws IOException {
		Map<String, String> knownPeers = loadKnownPeers();
		if (knownPeers.containsKey(mac)) {
			return knownPeers.get(mac);
		}

		return null;
	}

	/**
	 * Registers a new NetworkUpdateListener.
	 *
	 * @param listener the listener to register.
	 */
	public void addNetworkUpdateListener(NetworkUpdateListener listener) {
		listeners.add(listener);
	}

	/**
	 * Removes a NetworkUpdateListener.
	 *
	 * @param listener the listener to remove.
	 */
	public void removeNetworkUpdateListener(NetworkUpdateListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Notifies all registered listeners with the current known devices.
	 *
	 * @param devices the set of known devices to pass to listeners.
	 */
	public void notifyListeners(Set<Device> devices) {
		for (NetworkUpdateListener listener : listeners) {
			listener.onNetworkUpdated(devices);
		}
	}

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
