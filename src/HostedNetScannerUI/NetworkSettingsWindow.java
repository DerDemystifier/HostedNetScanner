package HostedNetScannerUI;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import hostednetscanner.ConfigManager;

public class NetworkSettingsWindow extends JFrame {
	private JTextField pwdPassword;
	private ConfigManager config;
	private JTextField txtSSID; // Add this field

	public NetworkSettingsWindow() {
		config = new ConfigManager();

		setTitle("Network Settings");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setSize(400, 150);
		setLocationRelativeTo(null);

		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
		setContentPane(contentPane);

		JPanel formPanel = new JPanel(new GridLayout(2, 2, 10, 10)); // Change to 3 rows for SSID

		JLabel lblSSID = new JLabel("SSID:"); // Add SSID label
		txtSSID = new JTextField(config.getSSID()); // Initialize SSID field

		JLabel lblPassword = new JLabel("Password:");
		pwdPassword = new JTextField(config.getNetworkPassword());

		formPanel.add(lblSSID); // Add SSID components
		formPanel.add(txtSSID);
		formPanel.add(lblPassword);
		formPanel.add(pwdPassword);

		contentPane.add(formPanel, BorderLayout.CENTER);

		JButton btnSave = new JButton("Save");
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveSettings();
			}
		});

		contentPane.add(btnSave, BorderLayout.SOUTH);
	}

	private void saveSettings() {
		String ssid = txtSSID.getText().trim(); // Get SSID
		String password = new String(pwdPassword.getText()).trim();

		if (password.length() < 8 || password.length() > 63) {
			// Show error message and do not save
			JOptionPane.showMessageDialog(this, "Password must be between 8 and 63 characters.", "Invalid Password", JOptionPane.ERROR_MESSAGE);
			return;
		}

		config.saveSSID(ssid); // Save SSID
		config.saveNetworkPassword(password);

		dispose();
	}
}
