package hostednetscanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HostedNetwork extends Network {
	private static HostedNetwork instance;
	private WatchService watchService;
	private Thread watchThread;

	private HostedNetwork(Network network) {
		super(network.getConnectedInterface());
	}

	public static HostedNetwork getInstance() {
		return instance;
	}

	public static Network startNetwork() {
		if (instance != null) {
			return getInstance();
		}

		ConfigManager config = new ConfigManager(); // Initialize ConfigManager
		String password = config.getNetworkPassword(); // Retrieve configured Password
		String ssid = config.getSSID(); // Retrieve configured SSID

		final int MAX_RETRY_ATTEMPTS = 3;
		final int RETRY_DELAY_MS = 500;

		int attempts = 0;
		while (!isNetworkRunning() && attempts < MAX_RETRY_ATTEMPTS) {
			try {
				System.out.println("Attempting to start network, attempt " + (attempts + 1));

				// Set SSID and Password
				if (ssid == null || ssid.isEmpty()) {
					System.out.println("No SSID.");
					return null;
				}

				String setCommand = String.format(
						"netsh wlan set hostednetwork mode=allow ssid=\"%s\" key=\"%s\" keyUsage=persistent", ssid,
						password);

				// Start Hosted Network
				Runtime.getRuntime().exec(setCommand);

				// Start Hosted Network
				Runtime.getRuntime().exec("netsh wlan start hostednetwork");
				Thread.sleep(RETRY_DELAY_MS);
				attempts++;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		HostedNetwork hnet = findHostedNetworkInstance();
		return hnet;
	}

	public static void stopNetwork() {
		try {
			Runtime.getRuntime().exec("netsh wlan stop hostednetwork");
			if (instance != null) {
				instance.getKnownDevices().clear();
				instance.notifyListeners(instance.getKnownDevices());

				// Stop the watch service
				if (instance.watchService != null) {
					instance.watchService.close();
				}
				if (instance.watchThread != null) {
					instance.watchThread.interrupt();
				}
			}
			instance = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
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
	public static HostedNetwork findHostedNetworkInstance() {
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
					String mac = Device.formatMacAddress(matcher.group(1));
					String name = recognizeClient(mac);
					InetAddress ipAddr;
					if (this.getConnectedInterface().getMacAddress().equals(mac)) {
						ipAddr = this.getConnectedInterface().getIpAddress();
					} else {
						ipAddr = ARPScanner.getIpAddress(this, mac);
					}

					Device connectedDevice = new Device(ipAddr, mac);
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
	public void updateConnectedDevices() {
		Set<Device> connectedDevices = null;
		try {
			connectedDevices = getConnectedDevices();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Set<Device> reachableDevices = null;
		if (this.getKnownDevices().size() < 2) {
			// If there are less than 2 known devices, use the connected devices as
			// reachable
			// This is because PSDeviceScanner takes times to start up, so I don't want the
			// initial scan to take too long
			reachableDevices = connectedDevices;
		} else {
			try {
				reachableDevices = new PSDeviceScanner().getReachableDevices();
			} catch (IOException e) {
				System.err.println("Error executing PowerShell command: " + e.getMessage());
			}
		}

		boolean networkModified = false;

		// Mark existing devices as offline if not in new network
		for (Device existingDevice : this.getKnownDevices()) {
			if (existingDevice.equals(this.getConnectedInterface()))
				continue;

			String newStatus;
			if (connectedDevices.contains(existingDevice) && reachableDevices.contains(existingDevice)) {
				newStatus = "online";
			} else if (connectedDevices.contains(existingDevice) && !reachableDevices.contains(existingDevice)
					|| !connectedDevices.contains(existingDevice) && reachableDevices.contains(existingDevice)) {
				newStatus = "unconfirmed";
			} else {
				newStatus = "offline";
			}

			if (newStatus != existingDevice.getStatus()) {
				existingDevice.setStatus(newStatus);
				networkModified = true;
			}
		}

		Set<Device> newDevices = connectedDevices.stream().filter(device -> !this.getKnownDevices().contains(device))
				.collect(Collectors.toSet());
		if (!newDevices.isEmpty()) {
			// If there are new devices not currently on known Devices
			this.getKnownDevices().addAll(newDevices);
			networkModified = true;
		}

		if (networkModified) {
			List<CompletableFuture<Void>> HN_lookupTasks = new ArrayList<>();

			for (Device newDevice : newDevices) {
				if (newDevice.getHostname() == null) {
					CompletableFuture<Void> lookupTask = CompletableFuture.runAsync(() -> {
						String hostName = newDevice.getIpAddress().getHostName();
						newDevice.setHostname(hostName);
					});
					HN_lookupTasks.add(lookupTask);
				}
			}

			notifyListeners(getKnownDevices());

			// Wait for all hostname lookups to complete then notify
			CompletableFuture.allOf(HN_lookupTasks.toArray(new CompletableFuture[0])).thenRun(() -> {
				notifyListeners(getKnownDevices());
			});
		}

		try {
			if (recheckCustomNames())
				notifyListeners(getKnownDevices());
		} catch (IOException e) {
		}
	}

	/**
	 * Monitors the network by periodically checking for connected devices. This
	 * method schedules a task to run at a fixed rate of every 2 seconds. The task
	 * retrieves the currently connected devices and updates the internal state.
	 *
	 * <p>
	 * Note: This method uses a single-threaded scheduled executor service to
	 * perform the monitoring task.
	 * </p>
	 *
	 * @throws IOException if an I/O error occurs while retrieving connected
	 *                     devices.
	 */
	@Override
	public void monitorNetwork() {
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(() -> {
			this.updateConnectedDevices();
		}, 0, 2, TimeUnit.SECONDS);
	}

	private boolean recheckCustomNames() throws IOException {
		boolean changed = false;
		Map<String, String> knownDevicesMap = loadKnownPeers();
		for (Device device : getKnownDevices()) {
			String customName = knownDevicesMap.get(device.getMacAddress());
			if (customName != null && !customName.equals(device.getCustomName())) {
				device.setCustomName(customName);
				changed = true;
			}
		}
		return changed;
	}

	public void refreshData() {
		// Re-scan network interfaces to get updated information
		List<Network> allNetworks = IPConfigScanner.scanNetworks();

		// Find our network interface with updated information
		for (Network network : allNetworks) {
			if (network.getConnectedInterface().getMacAddress().equals(getHostedNetMac())) {
				// Update the connected interface with new information
				this.setConnectedInterface(network.getConnectedInterface());
				break;
			}
		}

		// Clear current devices to force a full refresh
		this.getKnownDevices().clear();

		// Force an immediate update of connected devices
		updateConnectedDevices();
	}
}
