
public class App {
	public static void main(String[] args) {
		System.out.println(HostedNetwork.startNetwork().getDefaultGateway());

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
