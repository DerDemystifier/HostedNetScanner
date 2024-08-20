package HostedNetScannerUI;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import hostednetscanner.ConfigManager;

public class ConfigWindow extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField tfDevicesLog;
	private JTextField textField_1;
	private JTextField tfKnownDevices;
	private ConfigManager config = new ConfigManager();

	/**
	 * Create the frame.
	 */
	public ConfigWindow() {
		setResizable(false);
		initializeComponents();
	}

	private void initializeComponents() {
		setTitle("Configuration Window");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 745, 104);
		setLocationRelativeTo(null);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);

		JLabel lblNewLabel = new JLabel("Devices Log Location :");
		lblNewLabel.setBounds(8, 5, 139, 25);

		tfDevicesLog = new JTextField(config.getDeviceLogFilePath());
		tfDevicesLog.setEditable(false);
		tfDevicesLog.setBounds(149, 5, 408, 25);
		tfDevicesLog.setColumns(10);

		JButton btnDevicesLog = new JButton("Change Location");
		btnDevicesLog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File path = ConfigManager.showSaveDialog(ConfigWindow.this, "Choose save location", "Log file path",
						System.getProperty("user.dir"), "Text Docs", new String[] { "txt", "log" },
						"devicesStatus.txt");

				// Save a custom key
				if (path != null) {
					config.saveDeviceLogFilePath(path.getAbsolutePath());
					tfDevicesLog.setText(path.getAbsolutePath());
				}
			}
		});
		btnDevicesLog.setBounds(569, 5, 152, 25);

		JLabel lblNewLabel_1 = new JLabel("Devices Data Location :");
		lblNewLabel_1.setBounds(8, 35, 139, 25);
		contentPane.setLayout(null);
		contentPane.add(lblNewLabel);
		contentPane.add(tfDevicesLog);
		contentPane.add(btnDevicesLog);
		contentPane.add(lblNewLabel_1);

		textField_1 = new JTextField();
		textField_1.setBounds(149, 35, 408, 25);
		contentPane.add(textField_1);
		textField_1.setColumns(10);

		JButton btnNewButton_1 = new JButton("Change Location");
		btnNewButton_1.setBounds(569, 35, 152, 25);
		contentPane.add(btnNewButton_1);

		JLabel lblKnownDevices = new JLabel("Known Devices Data Location :");
		lblKnownDevices.setBounds(8, 65, 170, 25);
		contentPane.add(lblKnownDevices);

		tfKnownDevices = new JTextField(config.getKnownDevicesFilePath());
		tfKnownDevices.setEditable(false);
		tfKnownDevices.setBounds(180, 65, 408, 25);
		tfKnownDevices.setColumns(10);
		contentPane.add(tfKnownDevices);

		JButton btnKnownDevicesLog = new JButton("Change Location");
		btnKnownDevicesLog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File path = ConfigManager.showSaveDialog(ConfigWindow.this, "Choose save location", "Log file path",
						System.getProperty("user.dir"), "Text Docs", new String[] { "txt", "log" },
						"knownDevices.txt");

				if (path != null) {
					config.saveKnownDevicesFilePath(path.getAbsolutePath());
					tfKnownDevices.setText(path.getAbsolutePath());
				}
			}
		});
		btnKnownDevicesLog.setBounds(600, 65, 152, 25);
		contentPane.add(btnKnownDevicesLog);

		setVisible(true);
	}
}
