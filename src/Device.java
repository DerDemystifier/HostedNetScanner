import java.time.LocalDateTime;

public class Device {
	private String hostname;
	private String customName;
	private String ipAddress;
	private String macAddress;
	private LocalDateTime connectionTime; // timestamp of when the device connected
	private LocalDateTime lastSeen;
	private String status; // connected/disconnected/unconfirmed

	// Constructors
	public Device(String macAddress) {
		this.setMacAddress(macAddress);
		this.setConnectionTime(LocalDateTime.now());
	}

	public Device(String ipAddress, String macAddress) {
		this.ipAddress = ipAddress;
		this.macAddress = macAddress;
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

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	/**
	 * Formats a MAC address by converting it to uppercase and replacing colons with hyphens.
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
		this.macAddress = macAddress;
	}

	public LocalDateTime getConnectionTime() {
		return connectionTime;
	}

	public void setConnectionTime(LocalDateTime localDateTime) {
		this.connectionTime = localDateTime;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		// Verify status is valid
		if (!status.equals("connected") && !status.equals("disconnected") && !status.equals("unconfirmed")) {
			throw new IllegalArgumentException("Invalid status: " + status);
		}

		this.status = status;
		if (status.equals("disconnected") || status.equals("unconfirmed")) {
			this.lastSeen = LocalDateTime.now();
		}
	}
}
