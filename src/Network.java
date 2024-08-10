import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Network {
	private Device connectedInterface;
	private String subnetMask;
	private String defaultGateway;
	private List<Device> activeDevices = new ArrayList<>();
//	private List<StatusListener> statusListener;

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

	public List<Device> getActiveDevices() {
		return activeDevices;
	}

	public List<Device> addActiveDevice(Device device) {
		activeDevices.add(device);
		return activeDevices;
	}

	public void update(Network net) {
		// Update network configuration
		this.subnetMask = net.getSubnetMask();
		this.defaultGateway = net.getDefaultGateway();

		// Mark existing devices as offline if not in new network
		for (Device existingDevice : this.activeDevices) {
			if (existingDevice.equals(this.connectedInterface))
				continue;

			boolean deviceFound = false;
			for (Device newDevice : net.getActiveDevices()) {
				if (existingDevice.equals(newDevice)) {
					deviceFound = true;
					break;
				}
			}
			if (!deviceFound) {
				existingDevice.setStatus("unconfirmed");
			}
		}

		// Add new devices
		for (Device newDevice : net.getActiveDevices()) {
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
			}
		}

		// Remove offline devices
		// this.activeDevices.removeIf(device -> !device.isOnline());
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
		for (Device device : activeDevices) {
			sb.append("    ").append(device).append(",\n");
		}
		sb.append("  ]\n");
		sb.append("}");
		return sb.toString();
	}

}
