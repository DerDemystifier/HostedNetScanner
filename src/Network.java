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
	private Set<Device> activeDevices = new HashSet<>();
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

	public Set<Device> getActiveDevices() {
		return activeDevices;
	}

	public Set<Device> addActiveDevice(Device device) {
		activeDevices.add(device);
		return activeDevices;
	}

	public void updateConnectedDevices(Set<Device> newDevices) {
		boolean networkModified = false;

		// Mark existing devices as offline if not in new network
		for (Device existingDevice : this.activeDevices) {
			if (existingDevice.equals(this.connectedInterface))
				continue;

			boolean deviceFound = false;
			for (Device newDevice : newDevices) {
				if (existingDevice.equals(newDevice)) {
					deviceFound = true;
					break;
				}
			}
			if (!deviceFound) {
				existingDevice.setStatus("unconfirmed");
				networkModified = true;
			}
		}

		// Add new devices
		for (Device newDevice : newDevices) {
			boolean deviceExists = false;
			for (Device existingDevice : this.activeDevices) {
				if (newDevice.equals(existingDevice)) {
					deviceExists = true;
					break;
				}
			}
			if (!deviceExists) {
				newDevice.setStatus("connected");
				this.activeDevices.add(newDevice);

				networkModified = true;
			}
		}

		if (networkModified) {
			notifyListeners(this.getActiveDevices());
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
