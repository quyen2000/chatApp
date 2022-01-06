package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import model.Message;
import model.Status;

public class ClientHandler extends Thread {
	private final Socket socket;
	private final ObjectInputStream serverInput;
	private final ObjectOutputStream serverOutput;
	private volatile boolean running = true;
	private List<String> rejected = new ArrayList<>();
	private String username;
	private String clientInfo;

	public ClientHandler(Socket socket, ObjectInputStream serverInput, ObjectOutputStream serverOutput) {
		this.socket = socket;
		this.serverInput = serverInput;
		this.serverOutput = serverOutput;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPeerInfo() {
		return clientInfo;
	}

	public void setPeerInfo(String peerInfo) {
		this.clientInfo = peerInfo;
	}

	@Override
	public void run() {
		Message received;
		try (socket; serverInput; serverOutput;) {
			while (running) {
				received = (Message) serverInput.readObject();
				System.out.println(received);
				switch (received.getStatus()) {
				// sau ghép đôi có status CHAT, 
					case CHAT: {
						if (clientInfo != null && !clientInfo.isEmpty()) {
							Server.listMatched.get(clientInfo).sendMessage(received);
						}
						break;
					}
					case OK: {
						break;
					}
					// nếu 1 trong 2 thằng thoát thì thằng thoát gửi status EXIT và hủy ghép đôi thằng còn lại
					case EXIT:
						if (clientInfo == null) {
							Server.listWait.remove(username);
						} else {
							ClientHandler b;
							if (Server.listMatched.get(clientInfo) != null && Server.listMatched.get(clientInfo).getPeerInfo().equals(username)) {
								b = Server.listMatched.get(clientInfo);
								Message unmatch = new Message(username, null, Status.EXIT);
								b.sendMessage(unmatch);
							}
							removeMatched();
							Server.listMatched.remove(username);
							Server.listWait.remove(username);
						}
						running = false;
						break;
					case REFUSE: {
						ClientHandler b;
						rejected.add(clientInfo);
						if (Server.listMatched.get(clientInfo) != null) {
							b = Server.listMatched.get(clientInfo);
						} else {
							b = Server.listWait.get(clientInfo);
						}
						Message unmatch = new Message(username, null, Status.UNMATCH);
						b.sendMessage(unmatch);
						removeMatched();
						if (Server.listWait.size() - 1 > rejected.size() && Server.lastEnter.equals(username)) {
							matching();
						}
						break;
					}
					case MATCH: {
						matching();
						break;
					}
					case DISCONNECT:
						ClientHandler bx;
						if (Server.listMatched.get(clientInfo) != null
								&& Server.listMatched.get(clientInfo).getPeerInfo().equals(username)) {
							bx = Server.listMatched.get(clientInfo);
							Message exit = new Message(username, null, Status.EXIT);
							bx.sendMessage(exit);
						}
						removeMatched();
						Server.listMatched.remove(username);
						break;
					// luông 2 if đầu tiền kiểm tra trùng tên không, else trường hợp không trùng tên cho thằng đó vô hàng đợi
					case CONNECT: {
						if (Server.listMatched.get(received.getName()) != null
								|| Server.listWait.get(received.getName()) != null) {
							Message mess = new Message(username, null, Status.EXIST);
							sendMessage(mess);
						} else if (username == null) {
							username = received.getName();
							Server.listWait.put(username, this);
							Message welcome = new Message(username, null, Status.CONNECTED);
							System.out.println(welcome);
							sendMessage(welcome);
							Server.lastEnter = username;
							// chạy ghép đôi
							matching();
						}
					}
				}
			}
			System.out.println("ClientHanlder :" + username + " đã tắt !");
		} catch (IOException | ClassNotFoundException | InterruptedException e) {
			e.printStackTrace();
		}

	}

	public void sendMessage(Message mess) throws IOException {
		serverOutput.writeObject(mess);
		serverOutput.flush();
	}

	public void matching() throws IOException, InterruptedException {
		System.out.println(username);
		synchronized (this) {
			if (Server.listWait.size() > 1) {
				Server.listWait.remove(username);
				ArrayList<ClientHandler> list = new ArrayList<>(Server.listWait.values());
				list.removeIf(x -> rejected.contains(x.getUsername()));
				Collections.shuffle(list);
				ClientHandler benB = list.get(0);
				setPeerInfo(benB.getUsername());
				benB.setPeerInfo(username);
				Server.listMatched.put(username, this);
				Server.listMatched.put(benB.getUsername(), benB);
				Message mess = new Message(clientInfo, null, Status.MATCH);
				sendMessage(mess);
				mess.setName(username);
				benB.sendMessage(mess);
				Server.listWait.remove(benB.getUsername());
			}
		}
	}

	public void removeMatched() {
		if (Server.listMatched.get(clientInfo) != null) {
			ClientHandler benB = Server.listMatched.get(clientInfo);
			Server.listMatched.remove(clientInfo);
			Server.listWait.put(clientInfo, benB);
			Server.listMatched.remove(username);
			Server.listWait.put(username, this);
		}
		setPeerInfo(null);
	}

}
