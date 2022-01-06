package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.LineBorder;

import model.Client;
import model.Message;
import model.Status;

import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JList;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.SystemColor;

public class UserFrame extends JFrame {

	public int i = 0;
	public JPanel mainPanel;
	public JPanel insideCenter;
	public String ipAddress;
	public int port;
	private JScrollPane scroll;
	private JButton btnSend;
	private JButton btnConnect;
	private JTextArea displayArea;
	private JTextField txMessage;
	private JTextField txName;
	private Client client;
	private JButton btnEnd;
	private volatile boolean running = true;
	Thread t;

	public UserFrame() throws UnknownHostException, IOException {
		setBackground(Color.WHITE);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (client != null) {
					try {
						running = false;
						Message send = new Message(null, null, Status.EXIT);
						System.out.println(send);
						client.sendMessage(send);
						client.closeAll();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});

		setResizable(false);
		setSize(649, 508);

		setLocationRelativeTo(null);
		setTitle("Chat App");
		mainPanel = new JPanel();
		mainPanel.setBackground(Color.WHITE);
		mainPanel.setLayout(new BorderLayout(0, 0));
		setContentPane(mainPanel);

		ipAddress = "localhost";
		port = 999;

		insideCenter = new JPanel();
		insideCenter.setBorder(new LineBorder(Color.WHITE, 0, true));
		insideCenter.setPreferredSize(new Dimension(180, 650));
		insideCenter.setBackground(SystemColor.activeCaption);
		insideCenter.setLayout(null);
		mainPanel.add(insideCenter, BorderLayout.CENTER);

		displayArea = new JTextArea();
		displayArea.setEditable(false);

		scroll = new JScrollPane(displayArea);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.setBounds(29, 70, 580, 292);
		insideCenter.add(scroll);

		txMessage = new JTextField();
		txMessage.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (txMessage.getText().length() != 0) {
					btnSend.setEnabled(true);
				} else {
					btnSend.setEnabled(false);
				}
			}
		});
		txMessage.setBounds(29, 384, 482, 55);
		insideCenter.add(txMessage);
		txMessage.setColumns(10);

		btnSend = new JButton("Gửi");
		btnSend.setEnabled(false);
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					if(client == null) {
						JOptionPane.showMessageDialog(null,
								"Chưa kết nối mà send !",
								"Thông báo", JOptionPane.ERROR_MESSAGE);
					}else {
						if (client.isMatched()) {
							if(!txMessage.getText().isEmpty()) {
								Message send = new Message(client.getName(), txMessage.getText(), Status.CHAT);
								client.sendMessage(send);
								displayArea.append(client.getName() + " : " + txMessage.getText() + "\n");
								txMessage.setText("");
								btnSend.setEnabled(false);
							} else {
								JOptionPane.showMessageDialog(null,
										"Tin nhắn không được để trống !",
										"Thông báo", JOptionPane.ERROR_MESSAGE);
							}
							
						} else {
							System.out.println("Chưa ghép đôi mà đòi send");
						}
					}
					

				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		btnSend.setBounds(539, 384, 70, 55);
		insideCenter.add(btnSend);

		txName = new JTextField();
		txName.setBounds(29, 21, 351, 28);
		insideCenter.add(txName);
		txName.setColumns(10);

		btnConnect = new JButton("Kết nối");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (txName.getText().isEmpty()) {
					JOptionPane.showMessageDialog(null,
							"Không được để trống tên !",
							"Thông báo", JOptionPane.ERROR_MESSAGE);
				} else {
					try {
						if (client == null) {
							client = new Client(new Socket(ipAddress, port), txName.getText());
							Message welcome = new Message(txName.getText(), null, Status.CONNECT);
							client.sendMessage(welcome);
						} else {
							Message welcome = new Message(txName.getText(), null, Status.CONNECT);
							client.sendMessage(welcome);
						}
					
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				if (i == 0) {
					t = new Thread(new Runnable() {
						@Override
						public void run() {
							while (running) {
								// TODO Auto-generated method stub
								try {
									Message receivedMessage = client.receiveMessage();
									System.out.println(receivedMessage);
									switch (receivedMessage.getStatus()) {
									case MATCH:
										int action = JOptionPane.showConfirmDialog(null,
												"Bạn có muốn ghép đôi với " + receivedMessage.getName() + "?",
												"Ghép đôi thành công", JOptionPane.YES_NO_OPTION);
										if (action == JOptionPane.OK_OPTION) {
											Message accept = new Message(client.getName(), null, Status.OK);
											client.sendMessage(accept);
											client.setMatched(true);
											btnEnd.setEnabled(true);
											displayArea.setText("");
										} else {
											Message refuse = new Message(client.getName(), null, Status.REFUSE);
											client.sendMessage(refuse);
											btnEnd.setEnabled(false);
										}
										break;
									case CHAT:
										displayArea.append(
												receivedMessage.getName() + " : " + receivedMessage.getData() + "\n");
										break;
									case EXIST:
										JOptionPane.showMessageDialog(null, "Tên trùng với người khác !", "Thông báo",
												JOptionPane.ERROR_MESSAGE);
										break;
									case UNMATCH:
										JOptionPane.showMessageDialog(null,
												"Người kia đã từ chối ghép đôi, bạn sẽ quay lại hàng chờ !",
												"Thông báo", JOptionPane.ERROR_MESSAGE);
										client.setMatched(false);
										btnEnd.setEnabled(true);
										break;
									case EXIT:
										JOptionPane.showMessageDialog(null,
												"Người kia đã thoát khỏi phòng chat, bạn sẽ quay lại hàng chờ !",
												"Thông báo", JOptionPane.ERROR_MESSAGE);
										client.setMatched(false);
										btnEnd.setEnabled(false);
										displayArea.setText("");
										break;
									case CONNECTED:
										JOptionPane.showMessageDialog(null,
												"Đăng nhập thành công",
												"Thông báo", JOptionPane.INFORMATION_MESSAGE );
										btnConnect.setEnabled(false);
										txName.setEditable(false);
										break;
									default:
									}

								} catch (IOException | ClassNotFoundException e) {
									System.out.println();
								}
							}
						}
					});
					t.start();
					i++;
				}
				}
			}
		});
		btnConnect.setBounds(411, 20, 85, 30);
		insideCenter.add(btnConnect);

		btnEnd = new JButton("Thoát");
		btnEnd.setForeground(Color.BLACK);
		btnEnd.setFont(new Font("Arial", Font.PLAIN, 10));
		btnEnd.setEnabled(false);
		btnEnd.setBounds(524, 20, 85, 30);
		btnEnd.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
					try {
						Message end = new Message(null, null, Status.DISCONNECT);
						client.sendMessage(end);
						client.setMatched(false);
						btnEnd.setEnabled(false);
						displayArea.setText("");
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
		});
		insideCenter.add(btnEnd);
		
	}
	
}
