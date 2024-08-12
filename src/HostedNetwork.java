import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

public class HostedNetwork {
	public static Network startNetwork() {
		if (!isNetworkRunning()) {
			try {
				Runtime.getRuntime().exec("netsh wlan start hostednetwork");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		List<Network> allNetworks = IPConfigScanner.scanNetworks();
		for (Network network : allNetworks) {
			if (network.getConnectedInterface().getMacAddress().equals(getHostedNetMac())) {
				return network;
			}
		}

		return null;
	}

	/**
	 * Retrieves the MAC address of the hosted network.
	 *
	 * This method executes the command `netsh wlan show hostednetwork` and parses
	 * the output to find the line containing "BSSID". It then extracts and returns
	 * the MAC address from that line.
	 *
	 * @return the MAC address of the hosted network, or {@code null} if the MAC
	 *         address could not be found or an error occurred.
	 */
	public static String getHostedNetMac() {
		try {
			Process p = Runtime.getRuntime().exec("netsh wlan show hostednetwork");
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains("BSSID")) {
					String mac = line.split(" : ")[1].trim();
					return Device.formatMacAddress(mac);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Checks if there's a hosted network already running.
	 *
	 * @return {@code true} if a hosted network is running, {@code false} otherwise.
	 */
	public static boolean isNetworkRunning() {
		try {
			Process p = Runtime.getRuntime().exec("netsh wlan show hostednetwork");
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains("Status")) {
					return line.split(" : ")[1].trim().equals("Started");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}
}
