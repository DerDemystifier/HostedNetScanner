package HostedNetScannerUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import hostednetscanner.ConfigManager;
import hostednetscanner.Device;
import hostednetscanner.HostedNetwork;
import hostednetscanner.Network;
import hostednetscanner.NetworkUpdateListener;

public class MainWindow extends JFrame {
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTable table;
	private JMenuItem mntmStartNetwork;
	private JMenuItem mntmStopNetwork; // New menu item
	private JMenuItem mntmRefresh; // Declare the new menu item
	private ConfigManager config = new ConfigManager();

	// Add status icons
	private final ImageIcon statusGreen = new ImageIcon("images/green16.png");
	private final ImageIcon statusYellow = new ImageIcon("images/yellow16.png");
	private final ImageIcon statusRed = new ImageIcon("images/red16.png");

	/**
	 * Listener that updates the table with the active devices on the network. This
	 * listener is triggered when the network is updated.
	 */
	private NetworkUpdateListener refreshTableListener = new NetworkUpdateListener() {
		@Override
		public synchronized void onNetworkUpdated(Set<Device> knownDevices) {
			DefaultTableModel model = (DefaultTableModel) table.getModel();
			model.setRowCount(0);
			for (Device device : knownDevices) {
				// Convert status to appropriate icon
				ImageIcon statusIcon;
				switch (device.getStatus().toLowerCase()) {
				case "online":
					statusIcon = statusGreen;
					break;
				case "unconfirmed":
					statusIcon = statusYellow;
					break;
				case "offline":
					statusIcon = statusRed;
					break;
				default:
					statusIcon = statusRed;
				}

				model.addRow(new Object[] { statusIcon, device.getHostname(), device.getCustomName(),
						device.getMacAddress(), device.getHostAddress(), device.getFormattedConnectionTime(),
						device.getFormattedLastSeen() });
			}
		}
	};

	/**
	 * Listener that updates the table with the active devices on the network. This
	 * listener is triggered when the network is updated.
	 */
	private NetworkUpdateListener saveDevicesLog = new NetworkUpdateListener() {
		@Override
		public synchronized void onNetworkUpdated(Set<Device> knownDevices) {
			String logPath = config.getDeviceLogFilePath();
			if (logPath == null || logPath.isEmpty()) {
				return;
			}

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(logPath, false))) {
				LocalDateTime now = LocalDateTime.now();
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

				// First write online devices
				writer.write(now.format(formatter));
				writer.newLine();
				writer.write("Online devices:");
				writer.newLine();
				knownDevices.stream().filter(d -> "online".equals(d.getStatus()))
						.forEach(device -> writeDeviceLog(writer, device));

				// Then write offline/unconfirmed devices
				writer.write("\nOffline/Unconfirmed devices:");
				writer.newLine();
				knownDevices.stream().filter(d -> !"online".equals(d.getStatus()))
						.forEach(device -> writeDeviceLog(writer, device));

				writer.flush();
			} catch (IOException e) {
				System.err.println("Error writing to device log file: " + e.getMessage());
			}
		}

		private void writeDeviceLog(BufferedWriter writer, Device device) {
			try {
				String line = String.format("%-17s||%-15s||%s", device.getMacAddress(), device.getHostAddress(),
						device.getCustomName() != null ? device.getCustomName() : device.getHostname());
				writer.write(line);
				writer.newLine();
			} catch (IOException e) {
				System.err.println("Error writing device entry: " + e.getMessage());
			}
		}
	};

	/**
	 * Listener that updates the table with the active devices on the network. This
	 * listener is triggered when the network is updated.
	 */
	private NetworkUpdateListener saveKnownDevices = new NetworkUpdateListener() {
		private Map<String, String> cachedDevices;

		@Override
		public synchronized void onNetworkUpdated(Set<Device> knownDevices) {
			saveKnownDevices(knownDevices);
		}

		/**
		 * Saves known devices to the specified file if there are changes.
		 *
		 * @param devices the set of current known devices.
		 */
		public void saveKnownDevices(Set<Device> devices) {
			// Get current devices
			Map<String, String> currentDevices = new HashMap<>();
			for (Device device : devices) {
				currentDevices.put(device.getMacAddress(), device.getCustomName());
			}

			// Load or use cached known devices
			Map<String, String> knownDevices;

			if (cachedDevices == null) {
				Map<String, String> loadedDevices = Network.loadKnownPeers();
				if (loadedDevices != null) {
					cachedDevices = loadedDevices; // Update the cached version if loading was successful
					knownDevices = new HashMap<>(cachedDevices); // Use the newly loaded devices
				} else {
					// Handle the case where loadKnownPeers returns null (e.g., empty data source)
					knownDevices = new HashMap<>(); // Initialize an empty map
					System.err.println("Warning: Network.loadKnownPeers returned null.");
				}
			} else {
				knownDevices = new HashMap<>(cachedDevices); // Use the cached version
			}

			// Track if any changes were made
			boolean hasChanges = false;

			// Update known devices with new/modified entries
			for (Map.Entry<String, String> entry : currentDevices.entrySet()) {
				String mac = entry.getKey();
				String newName = entry.getValue();

				if (!knownDevices.containsKey(mac) || !knownDevices.get(mac).equals(newName)) {
					knownDevices.put(mac, newName);
					hasChanges = true;
				}
			}

			// Save if there were changes
			if (hasChanges) {
				String filePath = config.getKnownDevicesFilePath();
				try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
					for (Map.Entry<String, String> entry : knownDevices.entrySet()) {
						writer.write(entry.getKey() + "||" + entry.getValue());
						writer.newLine();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			cachedDevices = new HashMap<>(knownDevices);
		}
	};

	/**
	 * Create the frame.
	 */
	public MainWindow() {
		initializeComponents();
		initializeHostedNetwork();
	}

	void initializeHostedNetwork() {
		if (HostedNetwork.isNetworkRunning()) {
			// If the network is already running, get the HostedNetwork instance

			HostedNetwork hnet = HostedNetwork.findHostedNetworkInstance();
			hnet.addNetworkUpdateListener(refreshTableListener);
			hnet.addNetworkUpdateListener(saveDevicesLog);
			hnet.addNetworkUpdateListener(saveKnownDevices);
			hnet.monitorNetwork();
			mntmStartNetwork.setEnabled(false);
			mntmStopNetwork.setEnabled(true); // Enable Stop when network is running
		}
	}

	void initializeComponents() {
		setTitle("HostedNetScanner");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1200, 500);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		JMenuItem mntmQuit = new JMenuItem("Quit");
		mntmQuit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		mnFile.add(mntmQuit);

		JMenu mnServer = new JMenu("Server");
		menuBar.add(mnServer);

		JMenuItem mntmNetworkSettings = new JMenuItem("Network Settings"); // New menu item
		mnServer.add(mntmNetworkSettings);
		mntmNetworkSettings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new NetworkSettingsWindow().setVisible(true);
			}
		});

		JSeparator separator_1 = new JSeparator();
		mnServer.add(separator_1);

		mntmStartNetwork = new JMenuItem("Start Hosted Network");
		mnServer.add(mntmStartNetwork);

		mntmStopNetwork = new JMenuItem("Stop Hosted Network"); // New menu item
		mnServer.add(mntmStopNetwork);
		mntmStopNetwork.addActionListener(new ActionListener() { // ActionListener for stopping
			public void actionPerformed(ActionEvent e) {
				HostedNetwork.stopNetwork();

				// Clear the table
				DefaultTableModel model = (DefaultTableModel) table.getModel();
				model.setRowCount(0);

				mntmStartNetwork.setEnabled(true);
				mntmStopNetwork.setEnabled(false);
			}
		});
		mntmStopNetwork.setEnabled(false); // Initially disabled

		JSeparator separator = new JSeparator();
		mnServer.add(separator);

		// Add "Refresh" menu item to the Options menu
		mntmRefresh = new JMenuItem("Refresh");
		mnServer.add(mntmRefresh);
		mntmRefresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				HostedNetwork.getInstance().refreshData();
			}
		});
		mntmStartNetwork.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				HostedNetwork.startNetwork();

				initializeHostedNetwork();
			}
		});

		JMenu mnOptions = new JMenu("Options");
		menuBar.add(mnOptions);

		JMenuItem mntmConfigManager = new JMenuItem("Open Config");
		mntmConfigManager.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new ConfigWindow();
			}
		});
		mnOptions.add(mntmConfigManager);

		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);

		JMenuItem mntmAbout = new JMenuItem("About");
		mnHelp.add(mntmAbout);
		contentPane = new JPanel();
		contentPane.setBackground(Color.WHITE);
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBackground(new Color(0, 128, 0));
		contentPane.add(scrollPane);

		table = new JTable();
		table.setRowHeight(25);
		table.setFont(new Font("SansSerif", Font.PLAIN, 17));
		table.setModel(new DefaultTableModel(new Object[][] { { null, null, null, null, null, null, null }, },
				new String[] { "Status", "Hostname", "Custom name", "MAC Address", "IP Address", "Connection time",
						"Last Seen" }) {
			Class[] columnTypes = new Class[] { Icon.class, String.class, String.class, String.class, String.class,
					Object.class, String.class };
			boolean[] columnEditable = new boolean[] { false, false, true, false, false, false, false };

			public Class getColumnClass(int columnIndex) {
				return columnTypes[columnIndex];
			}

			public boolean isCellEditable(int row, int column) {
				return columnEditable[column];
			}
		});

		table.getModel().addTableModelListener(e -> {
			if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
				int row = e.getFirstRow();
				int column = e.getColumn();
				if (column == 2) { // 'Custom name' column index
					String newCustomName = (String) table.getValueAt(row, column);
					String macAddress = (String) table.getValueAt(row, 3);

					// Find and update the corresponding Device
					for (Device device : HostedNetwork.getInstance().getKnownDevices()) {
						if (device.getMacAddress().equals(macAddress)) {
							device.setCustomName(newCustomName);
							break;
						}
					}

					// Notify listeners about the update
					HostedNetwork.getInstance().notifyListeners(HostedNetwork.getInstance().getKnownDevices());
				}
			}
		});

		table.getColumnModel().getColumn(0).setResizable(false);
		table.getColumnModel().getColumn(0).setMaxWidth(46);
		table.getColumnModel().getColumn(1).setResizable(false);
		table.getColumnModel().getColumn(2).setResizable(false);
		table.getColumnModel().getColumn(3).setResizable(false);
		table.getColumnModel().getColumn(4).setResizable(false);
		table.getColumnModel().getColumn(4).setMinWidth(130);
		table.getColumnModel().getColumn(4).setMaxWidth(130);
		table.getColumnModel().getColumn(5).setResizable(false);
		table.getColumnModel().getColumn(6).setResizable(false);

		scrollPane.setViewportView(table);
	}
}
