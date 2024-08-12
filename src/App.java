import java.awt.EventQueue;

import javax.swing.UIManager;

public class App {
	public static void main(String[] args) {
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

		// Check for admin rights
		// if (!System.getProperty("user.name").equals("Administrator")) {
		// try {
		// ProcessBuilder pb = new ProcessBuilder("runas", "/user:Administrator", "cmd",
		// "/c",
		// "java -jar " + new File("HostedNetwork.jar").getAbsolutePath());
		// pb.start();

		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// System.exit(0);
		// }
	}
}
