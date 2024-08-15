import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
		} else {
			System.out.println("Hosted network not found.");
		}

		return hnet;
	}

	/**
	 * Attempts to find and return an instance of HostedNetwork by scanning
	 * available networks. The method will make up to 6 attempts, with a 500ms delay
	 * between each attempt. If a network with a matching MAC address is found, it
	 * will return the HostedNetwork instance. If the instance does not already
	 * exist, it will instatiate a new one.
	 *
	 * @return the HostedNetwork instance if found, or null if not found after 6
	 *         attempts or if interrupted.
	 */
	static HostedNetwork findHostedNetworkInstance() {
		int attempts = 6;
		while (attempts > 0) {
			// Scan all networks
			List<Network> allNetworks = IPConfigScanner.scanNetworks();

			for (Network network : allNetworks) {
				if (network.getConnectedInterface().getMacAddress().equals(getHostedNetMac())) {
					// If the network's MAC address (from IPConfig) matches the hosted network's MAC
					// address

					if (instance == null) {
						instance = new HostedNetwork(network);
					}
					return getInstance();
				}
			}

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			}

			attempts--;
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

	/**
	 * Monitors the network by periodically checking for connected devices.
	 * This method schedules a task to run at a fixed rate of every 2 seconds.
	 * The task retrieves the currently connected devices and updates the internal state.
	 *
	 * <p>Note: This method uses a single-threaded scheduled executor service to perform the monitoring task.</p>
	 *
	 * @throws IOException if an I/O error occurs while retrieving connected devices.
	 */
	@Override
	public void monitorNetwork() {
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(() -> {
			try {
				System.out.println("Monitoring ....");
				Set<Device> devices = getConnectedDevices();
				this.updateConnectedDevices(devices);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}, 0, 2, TimeUnit.SECONDS);
	}
}
