import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Device {
	private String hostname;
	private String customName;
	private String ipAddress;
	private String macAddress;
	private LocalDateTime connectionTime = LocalDateTime.now(); // timestamp of when the device connected
	private LocalDateTime lastSeen = LocalDateTime.now();;
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
		this.macAddress = macAddress;
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
		if (!status.equals("connected") && !status.equals("disconnected") && !status.equals("unconfirmed")) {
			throw new IllegalArgumentException("Invalid status: " + status);
		}

		this.status = status;
		if (status.equals("disconnected") || status.equals("unconfirmed")) {
			this.setLastSeen(LocalDateTime.now());
		}
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
