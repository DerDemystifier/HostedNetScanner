import java.util.Set;

public interface NetworkUpdateListener {
	void onNetworkUpdated(Set<Device> devices);
}
