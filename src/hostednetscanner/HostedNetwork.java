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

	/**
	 * Starts the hosted network if it is not already running.
	 *
	 * @return The instance of the hosted network if started successfully, otherwise null.
	 *
	 * This method initializes the ConfigManager to retrieve the network SSID and password.
	 * It attempts to start the hosted network up to a maximum of 3 times, with a delay of 500 milliseconds between attempts.
	 * If the SSID is null or empty, the method returns null.
	 * If an exception occurs during the process, it logs the error and returns null.
	 *
	 * @throws InterruptedException If the thread is interrupted while sleeping between retry attempts.
	 */
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
				// Set SSID and Password
				if (ssid == null || ssid.isEmpty()) {
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
				Logger.logError("Error starting hosted network: ", e);
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
			Logger.logError("Error stopping hosted network: ", e);
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
			Logger.logError("Error getting hosted network MAC address: ", e);
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
			Logger.logError("Error checking if hosted network is running: ", e);
		}

		return false;
	}

	/**
	 * Retrieves the set of devices currently connected to the hosted network.
	 *
	 * @return a set of {@link Device} objects representing the connected devices.
	 * @throws IOException if an I/O error occurs while executing the command or reading the output.
	 */
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
			Logger.logError("Error getting connected devices: ", e);
		}

		return devices;
	}

	/**
	 * Updates the list of connected devices and their statuses.
	 *
	 * This method performs the following steps:
	 * 1. Retrieves the currently connected devices.
	 * 2. Determines the reachable devices based on the known devices.
	 * 3. Updates the status of existing known devices based on their connectivity and reachability.
	 * 4. Adds any new connected devices to the network's known devices list.
	 * 5. Performs hostname lookups for new devices asynchronously.
	 * 6. Notifies listeners if there are any changes in the network.
	 * 7. Rechecks custom names and notifies listeners if necessary.
	 *
	 * Exceptions:
	 * - IOException: If there is an error retrieving connected or reachable devices.
	 */
	@Override
	public void updateConnectedDevices() {
		Set<Device> connectedDevices = null;
		try {
			connectedDevices = getConnectedDevices();
		} catch (IOException e) {
			e.printStackTrace();
			Logger.logError("Error getting connected devices: ", e);
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

		// Mac, Device map for quick lookup by MAC address
		Map<String, Device> reachableDevicesMap = reachableDevices.stream()
				.collect(Collectors.toMap(Device::getMacAddress, d -> d));
		Map<String, Device> connectedDevicesMap = connectedDevices.stream()
				.collect(Collectors.toMap(Device::getMacAddress, d -> d));

		// Mark existing devices as offline if not in new network
		for (Device existingDevice : this.getKnownDevices()) {
			if (existingDevice.equals(this.getConnectedInterface()))
				continue;

			Device reachableDevice = reachableDevicesMap.get(existingDevice.getMacAddress());
			boolean isReachable = reachableDevice != null;
			boolean isConnected = connectedDevicesMap.containsKey(existingDevice.getMacAddress());

			if (isReachable) {
				existingDevice.setIpAddress(reachableDevice.getIpAddress());
			}

			String newStatus;
			if (isConnected && isReachable) {
				newStatus = "online";
			} else if (!isConnected) {
				newStatus = "offline";
			} else {
				newStatus = "unconfirmed";
			}

			if (!newStatus.equals(existingDevice.getStatus())) {
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

	/**
	 * Rechecks and updates the custom names of known devices based on a predefined map of known peers.
	 *
	 * @return {@code true} if any device's custom name was changed, {@code false} otherwise.
	 * @throws IOException if an I/O error occurs while loading the known peers.
	 */
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

	/**
	 * Refreshes the network data by re-scanning network interfaces to get updated
	 * information. It updates the connected interface with new information if the
	 * MAC address matches the hosted network's MAC address. Additionally, it clears
	 * the current known devices to force a full refresh and forces an immediate
	 * update of connected devices.
	 */
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
