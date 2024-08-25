import java.awt.EventQueue;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import HostedNetScannerUI.MainWindow;

public class App {
	public static void main(String[] args) {
		if (!isAdmin()) {
			JOptionPane.showMessageDialog(null, "This application requires administrator privileges to run.");
			System.exit(1); // Exit with an error code
		}

		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Throwable e) {
			e.printStackTrace();
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow frame = new MainWindow();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private static boolean isAdmin() {
		try {
			// Attempt to execute a command that requires admin rights
			Process process = Runtime.getRuntime().exec("net session");
			int exitCode = process.waitFor();
			return exitCode == 0; // Exit code 0 usually indicates success, meaning admin rights
		} catch (IOException | InterruptedException e) {
			// An exception likely means we don't have admin rights
			return false;
		}
	}
}
