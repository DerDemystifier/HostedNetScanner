import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class MainWindow extends JFrame {
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTable table;
	private JMenuItem mntmStartNetwork;

	/**
	 * Listener that updates the table with the active devices on the network. This
	 * listener is triggered when the network is updated.
	 */
	private NetworkUpdateListener refreshTableListener = new NetworkUpdateListener() {
		@Override
		public void onNetworkUpdated(Set<Device> knownDevices) {
			DefaultTableModel model = (DefaultTableModel) table.getModel();
			model.setRowCount(0);
			for (Device device : knownDevices) {
				model.addRow(new Object[] { device.getStatus(), device.getHostname(), device.getCustomName(),
						device.getMacAddress(), device.getHostAddress(), device.getFormattedConnectionTime(),
						device.getFormattedLastSeen() });
			}
		}
	};

	/**
	 * Create the frame.
	 */
	public MainWindow() {
		initializeComponents();

		HostedNetwork hnet = null;

		if (HostedNetwork.isNetworkRunning()) {
			// If the network is already running, get the HostedNetwork instance

			hnet = HostedNetwork.findHostedNetworkInstance();
			hnet.monitorNetwork();
			mntmStartNetwork.setEnabled(false);
			hnet.addNetworkUpdateListener(refreshTableListener);
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

		mntmStartNetwork = new JMenuItem("Start Hosted Network");
		mntmStartNetwork.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Network hnet = HostedNetwork.startNetwork();

				hnet.addNetworkUpdateListener(refreshTableListener);
			}
		});
		mnServer.add(mntmStartNetwork);

		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);

		JMenuItem mntmAbout = new JMenuItem("About");
		mnHelp.add(mntmAbout);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		JScrollPane scrollPane = new JScrollPane();
		contentPane.add(scrollPane);

		table = new JTable();
		table.setRowHeight(25);
		table.setFont(new Font("SansSerif", Font.PLAIN, 17));
		table.setModel(new DefaultTableModel(new Object[][] { { null, null, null, null, null, null, null }, },
				new String[] { "Status", "Hostname", "Custom name", "MAC Address", "IP Address", "Connection time",
						"Last Seen" }) {
			Class[] columnTypes = new Class[] { Object.class, String.class, String.class, String.class, String.class,
					Object.class, String.class };

			public Class getColumnClass(int columnIndex) {
				return columnTypes[columnIndex];
			}

			boolean[] columnEditables = new boolean[] { false, false, false, false, false, false, false };

			public boolean isCellEditable(int row, int column) {
				return columnEditables[column];
			}
		});
		table.getColumnModel().getColumn(0).setResizable(false);
		table.getColumnModel().getColumn(1).setResizable(false);
		table.getColumnModel().getColumn(2).setResizable(false);
		table.getColumnModel().getColumn(3).setResizable(false);
		table.getColumnModel().getColumn(4).setResizable(false);
		table.getColumnModel().getColumn(5).setResizable(false);
		table.getColumnModel().getColumn(6).setResizable(false);
		scrollPane.setViewportView(table);
	}

}
