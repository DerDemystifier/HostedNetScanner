package hostednetscanner;

import java.net.InetAddress;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Device {
	private String hostname;
	private String customName;
	private InetAddress ipAddress;
	private String macAddress;
	private LocalDateTime connectionTime = LocalDateTime.now(); // timestamp of when the device connected
	private LocalDateTime lastSeen = LocalDateTime.now(); // Remove extra semicolon
	private String status = "online"; // online/offline/unconfirmed
	private Network network; // Network this device interface belongs to

	// Constructors
	public Device(InetAddress ipAddress) {
		this(ipAddress, ARPScanner.getMacAddress(ipAddress));
	}

	public Device(InetAddress ipAddress, String macAddress) {
		this.ipAddress = ipAddress;
		this.macAddress = macAddress;
		this.setConnectionTime(LocalDateTime.now());
	}

	public Device(InetAddress ipAddress, String macAddress, Network network) {
		this(ipAddress, macAddress);
		this.setNetwork(network);
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getCustomName() {
		return customName;
	}

	public void setCustomName(String customName) {
		this.customName = customName;
	}

	public InetAddress getIpAddress() {
		return ipAddress;
	}

	public String getHostAddress() {
		if (this.getIpAddress() != null)
			return this.getIpAddress().getHostAddress();
		return null;
	}

	public void setIpAddress(InetAddress ipAddress) {
		this.ipAddress = ipAddress;
	}

	/**
	 * Formats a MAC address by converting it to uppercase and replacing colons with
	 * hyphens.
	 *
	 * @param oldMac the original MAC address string
	 * @return the formatted MAC address string
	 */
	public static String formatMacAddress(String oldMac) {
		String mac = oldMac.toUpperCase();
		mac = mac.replace(":", "-");
		return mac;
	}

	public String getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(String macAddress) {
		if (macAddress == null || !macAddress.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")) {
			throw new IllegalArgumentException("Invalid MAC address format");
		}
		this.macAddress = formatMacAddress(macAddress);
	}

	public LocalDateTime getConnectionTime() {
		return connectionTime;
	}

	public String getFormattedConnectionTime() {
		// format the connection time to a readable format
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		return connectionTime.format(formatter);
	}

	public void setConnectionTime(LocalDateTime localDateTime) {
		this.connectionTime = localDateTime;
	}

	public LocalDateTime getLastSeen() {
		return lastSeen;
	}

	public String getFormattedLastSeen() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		return lastSeen.format(formatter);
	}

	public void setLastSeen(LocalDateTime lastSeen) {
		this.lastSeen = lastSeen;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		// Verify status is valid
		if (!status.equals("online") && !status.equals("offline") && !status.equals("unconfirmed")) {
			throw new IllegalArgumentException("Invalid status: " + status);
		}

		this.status = status;
		if (status.equals("offline") || status.equals("unconfirmed")) {
			this.setLastSeen(LocalDateTime.now());
		}
	}

	public Network getNetwork() {
		return network;
	}

	public void setNetwork(Network network) {
		this.network = network;
	}

	public boolean isConnected() {
		return "online".equals(this.status);
	}

	@Override
	public int hashCode() {
		return Objects.hash(macAddress);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Device other = (Device) obj;
		return Objects.equals(macAddress, other.macAddress);
	}

	@Override
	public String toString() {
		return MessageFormat.format("""
				Device [
				  hostname={0}
				  customName={1}
				  ipAddress={2}
				  macAddress={3}
				  connectionTime={4}
				  lastSeen={5}
				  status={6}
				]""", hostname, customName, ipAddress, macAddress, connectionTime, lastSeen, status);
	}
}
