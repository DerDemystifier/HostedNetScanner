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

	private void notifyListeners(Set<Device> devices) {
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

	public Set<Device> getKnownDevices() {
		return knownDevices;
	}

	public Set<Device> addDevice(Device device) {
		knownDevices.add(device);
		return knownDevices;
	}

	public void updateConnectedDevices(Set<Device> newDevices) {
		boolean networkModified = true;

		// Mark existing devices as offline if not in new network
		for (Device existingDevice : this.getKnownDevices()) {
			if (existingDevice.equals(this.connectedInterface))
				continue;

			if (!newDevices.contains(existingDevice)) {
				existingDevice.setStatus("unconfirmed");
				newDevices.add(existingDevice); // Add the missing device, marked as missing

				networkModified = true;
			}
		}

		long onlineDevices = this.getKnownDevices().stream().filter(device -> "connected".equals(device.getStatus()))
				.count();
		if (newDevices.size() != onlineDevices) {
			// If there are new devices not currently on active Devices
				networkModified = true;
		}

		if (networkModified) {
			this.knownDevices = newDevices;
			notifyListeners(this.getKnownDevices());
		}

		// Remove offline devices
		// this.activeDevices.removeIf(device -> !device.isOnline());
	}

	public void monitorNetwork() {
	};

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
		for (Device device : activeDevices) {
			sb.append("    ").append(device).append(",\n");
		}
		sb.append("  ]\n");
		sb.append("}");
		return sb.toString();
	}

}
