import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HostedNetwork extends Network {
	private static HostedNetwork instance;

	private HostedNetwork(Network network) {
		super(network.getConnectedInterface());
	}

	public static HostedNetwork getInstance() {
		return instance;
	}

	public static Network startNetwork() {
		if (!isNetworkRunning()) {
			try {
				Runtime.getRuntime().exec("netsh wlan start hostednetwork");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (instance != null) {
			return getInstance();
		}

		HostedNetwork hnet = findHostedNetworkInstance();
		if (hnet != null) {
			hnet.monitorNetwork();
		}

		return hnet;
	}

	static HostedNetwork findHostedNetworkInstance() {
		List<Network> allNetworks = IPConfigScanner.scanNetworks();
		for (Network network : allNetworks) {
			if (network.getConnectedInterface().getMacAddress().equals(getHostedNetMac())) {
				if (instance == null) {
					instance = new HostedNetwork(network);
				}
				return getInstance();
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

	// Parse connected clients from netsh command
	public Set<Device> getConnectedDevices() throws IOException {
		Set<Device> devices = new HashSet<>();
		Process process = Runtime.getRuntime().exec("netsh wlan show hostednetwork");

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				Matcher matcher = Pattern.compile("([\\dA-Fa-f:]{17})").matcher(line);
				if (matcher.find()) {
					String mac = matcher.group(1);
					String name = recognizeClient(mac);

					Device connectedDevice = new Device(mac);
					connectedDevice.setCustomName(name);

					devices.add(connectedDevice);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return devices;
	}

	@Override
	public void monitorNetwork() {
		try {
			Set<Device> devices = getConnectedDevices();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
