package hostednetscanner;

import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * The ConfigManager class is responsible for managing configuration settings
 * for the HostedNetScanner application. It uses the Preferences API to store
 * and retrieve configuration values such as file paths, network credentials,
 * and other settings.
 *
 * Configuration settings are saved in the Windows registry under:
 * Computer\HKEY_USERS\S-1-5-21-ID\SOFTWARE\JavaSoft\Prefs
 *
 * The following configuration keys are used:
 * - deviceLogFilePath: Path to the device log file.
 * - knownDevicesFilePath: Path to the known devices file.
 * - bssidKey: BSSID of the hosted network.
 * - networkPasswordKey: Password of the hosted network.
 * - ssidKey: SSID of the hosted network.
 *
 * Methods:
 * - ConfigManager(): Constructor that initializes the preferences node.
 * - saveDeviceLogFilePath(String key): Saves the device log file path.
 * - getDeviceLogFilePath(): Retrieves the device log file path.
 * - saveKnownDevicesFilePath(String key): Saves the known devices file path.
 * - getKnownDevicesFilePath(): Retrieves the known devices file path.
 * - saveNetworkPassword(String password): Saves the network password.
 * - getNetworkPassword(): Retrieves the network password.
 * - saveSSID(String ssid): Saves the SSID of the hosted network.
 * - getSSID(): Retrieves the SSID of the hosted network.
 * - showSaveDialog(Component parentComponent, String dialogTitle, String approveButtonText,
 *   String initialDirectory, String fileDescription, String[] fileExtensions, String defaultFileName):
 *   Opens a save dialog and returns the full path of the selected file.
 */
public class ConfigManager {
	// Config saved in Computer\HKEY_USERS\S-1-5-21-ID\SOFTWARE\JavaSoft\Prefs
	private static final String deviceLogFilePath = "device_log_file_path";
	private static final String knownDevicesFilePath = "known_devices_file_path";
	private static final String bssidKey = "hosted_network_bssid";
	private static final String networkPasswordKey = "hosted_network_password";
	private static final String ssidKey = "hosted_network_ssid"; // Add this constant
	private Preferences prefs;

	public ConfigManager() {
		// Get the preferences node for your application/class
		prefs = Preferences.userNodeForPackage(this.getClass());
	}

	public void saveDeviceLogFilePath(String key) {
		prefs.put(deviceLogFilePath, key);
	}

	public String getDeviceLogFilePath() {
		// Provide a default value if the key doesn't exist
		return prefs.get(deviceLogFilePath, System.getProperty("user.dir") + "/devicesStatus.txt");
	}

	public void saveKnownDevicesFilePath(String key) {
		prefs.put(knownDevicesFilePath, key);
	}

	public String getKnownDevicesFilePath() {
		return prefs.get(knownDevicesFilePath, System.getProperty("user.dir") + "/knownDevices.txt");
	}

	public void saveNetworkPassword(String password) {
		prefs.put(networkPasswordKey, password);
	}

	public String getNetworkPassword() {
		return prefs.get(networkPasswordKey, "");
	}

	public void saveSSID(String ssid) { // Add this method
		prefs.put(ssidKey, ssid);
	}

	public String getSSID() { // Add this method
		return prefs.get(ssidKey, "HostedNetScanner"); // Default SSID if not set
	}

	/**
	 * Opens a save dialog and returns the full path of the selected file.
	 *
	 *
	 * <pre>
	 * // Example usage:
	 * File savedFilePath = showSaveDialog(null, // Parent component (can be null)
	 * 		"Save Your Document", // Dialog title
	 * 		"Save File", // Approve button text
	 * 		System.getProperty("user.home"), // Initial directory (user's home)
	 * 		"Text Documents", // File description for filter
	 * 		new String[] { "txt", "log" }, // Allowed file extensions
	 * 		"my_document.txt" // Default file name
	 * );
	 *
	 * if (savedFilePath != null) {
	 * 	JOptionPane.showMessageDialog(null, "File saved to: " + savedFilePath.getAbsolutePath());
	 * } else {
	 * 	JOptionPane.showMessageDialog(null, "Save operation cancelled.");
	 * }
	 * </pre>
	 *
	 * @param parentComponent   The parent component for the dialog (can be null).
	 * @param dialogTitle       The title to display on the save dialog.
	 * @param approveButtonText The text to display on the "Save" button.
	 * @param initialDirectory  The initial directory to open the dialog in (can be
	 *                          null for default).
	 * @param fileDescription   The description for the file type filter (e.g.,
	 *                          "Text Files").
	 * @param fileExtensions    An array of allowed file extensions (e.g., {"txt",
	 *                          "log"}).
	 * @param defaultFileName   The initially suggested file name (can be null).
	 * @return The selected file if the user approves, otherwise null.
	 *
	 */
	public static File showSaveDialog(java.awt.Component parentComponent, String dialogTitle, String approveButtonText,
			String initialDirectory, String fileDescription, String[] fileExtensions, String defaultFileName) {

		JFileChooser fileChooser = new JFileChooser();

		// Set the dialog title
		fileChooser.setDialogTitle(dialogTitle);

		// Set the approve button text
		if (approveButtonText != null && !approveButtonText.trim().isEmpty()) {
			fileChooser.setApproveButtonText(approveButtonText);
		}

		// Set the initial directory
		if (initialDirectory != null && !initialDirectory.trim().isEmpty()) {
			File initialDir = new File(initialDirectory);
			if (initialDir.exists() && initialDir.isDirectory()) {
				fileChooser.setCurrentDirectory(initialDir);
			}
		}

		// Set the file filter
		if (fileDescription != null && fileExtensions != null && fileExtensions.length > 0) {
			FileNameExtensionFilter filter = new FileNameExtensionFilter(fileDescription, fileExtensions);
			fileChooser.setFileFilter(filter);
		}

		// Set the default file name
		if (defaultFileName != null && !defaultFileName.trim().isEmpty()) {
			fileChooser.setSelectedFile(new File(defaultFileName));
		}

		int userSelection = fileChooser.showSaveDialog(parentComponent);

		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			return selectedFile;
		}

		return null; // User cancelled or an error occurred
	}
}
