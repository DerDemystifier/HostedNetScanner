package HostedNetScannerUI;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import hostednetscanner.ConfigManager;

public class ConfigWindow extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField tfDevicesLog;
	private JTextField textField_1;
	private ConfigManager config = new ConfigManager();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Throwable e) {
			e.printStackTrace();
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ConfigWindow frame = new ConfigWindow();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public ConfigWindow() {
		initializeComponents();
	}

	private void initializeComponents() {
		setTitle("Configuration Window");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 745, 104);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);

		JLabel lblNewLabel = new JLabel("Devices Log location :");
		lblNewLabel.setBounds(8, 5, 139, 25);

		tfDevicesLog = new JTextField(config.getDeviceLogFilePath());
		tfDevicesLog.setEditable(false);
		tfDevicesLog.setBounds(149, 5, 443, 25);
		tfDevicesLog.setColumns(10);

		JButton btnDevicesLog = new JButton("Choose directory");
		btnDevicesLog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File path = ConfigManager.showSaveDialog(ConfigWindow.this, "Choose save location", "Log file path",
						System.getProperty("user.dir"), "Text Dpcs", new String[] { "txt", "log" },
						"devicesStatus.txt");

				// Save a custom key
				if (path != null) {
					config.saveDeviceLogFilePath(path.getAbsolutePath());
					tfDevicesLog.setText(path.getAbsolutePath());
				}
			}
		});
		btnDevicesLog.setBounds(600, 5, 121, 25);

		JLabel lblNewLabel_1 = new JLabel("New label");
		lblNewLabel_1.setBounds(8, 35, 139, 25);
		contentPane.setLayout(null);
		contentPane.add(lblNewLabel);
		contentPane.add(tfDevicesLog);
		contentPane.add(btnDevicesLog);
		contentPane.add(lblNewLabel_1);

		textField_1 = new JTextField();
		textField_1.setBounds(149, 35, 443, 25);
		contentPane.add(textField_1);
		textField_1.setColumns(10);

		JButton btnNewButton_1 = new JButton("New button");
		btnNewButton_1.setBounds(600, 35, 121, 25);
		contentPane.add(btnNewButton_1);

	}
}
