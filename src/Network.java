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

			}
		}

		long onlineDevices = this.getKnownDevices().stream().filter(device -> "connected".equals(device.getStatus()))
				.count();
		if (newDevices.size() != onlineDevices) {
			// If there are new devices not currently on active Devices
				networkModified = true;
		}

		if (networkModified) {
			System.out.println("WILL MODIFY NETWORK");
			// carry out .connectionTime from knownDevices to newDevices
			for (Device newDevice : newDevices) {
				for (Device knownDevice : this.getKnownDevices()) {
					if (newDevice.equals(knownDevice)) {
						if (knownDevice.getHostname() != null) {
							newDevice.setHostname(knownDevice.getHostname());
							System.out.println("KNOWN HOSTNAME");
						}
						newDevice.setConnectionTime(knownDevice.getConnectionTime());
						break;
					}
				}
			}

			for (Device newDevice : newDevices) {
				if (newDevice.getHostname() == null) {
					System.out.println("Will try to find hostname");
					ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
					executor.schedule(() -> {
						String hostName = newDevice.getIpAddress().getHostName();
						System.out.println("FOUND" + newDevice.getIpAddress().getHostName());
						newDevice.setHostname(hostName);
						notifyListeners(this.getKnownDevices());
					}, 1, TimeUnit.SECONDS);
				}
			}

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
		for (Device device : knownDevices) {
			sb.append("    ").append(device).append(",\n");
		}
		sb.append("  ]\n");
		sb.append("}");
		return sb.toString();
	}

}
