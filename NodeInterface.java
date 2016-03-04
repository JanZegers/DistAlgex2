import java.rmi.Remote;

public interface NodeInterface extends Remote {
	public void passMessage(int level, int number) throws Exception;

	public void newNodeJoined() throws Exception;
}
