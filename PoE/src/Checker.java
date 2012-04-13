import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JWindow;

public class Checker extends JWindow {
	private static final long serialVersionUID = 223454302234815228L;
	private Checker window = this;
	private TrayIcon tray;
	private int x, y;

	private JLabel nextName, lastName, time;
	private boolean canMove = true;

	public static void main(String[] args) {
		new Checker();
	}

	public Checker() {
		nextName = new JLabel();
		lastName = new JLabel();
		time = new JLabel();

		JPanel content = new JPanel();

		getData();
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				x = e.getX();
				y = e.getY();
			}
		});
		this.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (canMove)
					window.setLocation(e.getXOnScreen() - x, e.getYOnScreen() - y);
			}

			@Override
			public void mouseMoved(MouseEvent arg0) {
				// System.out.println("Mouse Moving");
				// TODO check if I need to do anything with this.
			}
		});
		add(content);
		content.setLayout(null);
		content.setBounds(0, 0, 250, 297);

		JLayeredPane layeredPane = new JLayeredPane();
		content.add(layeredPane);
		layeredPane.setBounds(0, 0, 250, 297);
		layeredPane.setLayout(null);
		
		JLabel lblClose = new JLabel();
		lblClose.setBounds(233, 3, 12, 14);
		layeredPane.add(lblClose);
		lblClose.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				//TODO
			}
		});
		
		JLabel lblMini = new JLabel();
		lblMini.setBounds(218, 3, 12, 14);
		layeredPane.add(lblMini);
		lblMini.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				tray.setState(1);
			}
		});
		
		lastName.setBounds(54, 51, 124, 14);
		layeredPane.add(lastName);
		
		nextName.setBounds(54, 81, 124, 14);
		layeredPane.add(nextName);
		
		time.setBounds(101, 243, 124, 14);
		layeredPane.add(time);
		
		JLabel lblNewLabel = new JLabel();
		lblNewLabel.setBounds(0, 0, 251, 297);
		layeredPane.add(lblNewLabel);
		try {
			lblNewLabel.setIcon(new ImageIcon(ImageIO.read(new URL("http://l3eta.nightphoenix13.info/projects/poe/bg.png"))));
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.setSize(250, 297);
		this.setVisible(true);
		tray = new TrayIcon();
	}

	public void getData() {
		new SiteReader();
	}

	public void updateData(String last, String next, int seconds) {
		lastName.setText(last);
		lastName.setSize(last.length() * 10, lastName.getHeight());
		nextName.setText(next);
		nextName.setSize(next.length() * 10, nextName.getHeight());
		new TimeThread(seconds).start();
	}

	public void updateTime(int seconds) {
		time.setText(formatSeconds(seconds));
	}

	private String formatSeconds(int sec) {
		return sec / 60 % 60 + ":"
				+ (sec % 60 < 10 ? "0" + sec % 60 : sec % 60);
	}

	public class TimeThread extends Thread {
		private int seconds;

		public TimeThread(int seconds) {
			if (seconds == -1)
				return;
			this.seconds = seconds;
		}

		public void run() {
			while (seconds > 0) {
				updateTime(seconds--);
				try {
					Thread.sleep(995);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			getData(); // Pulls update
		}
	}

	public class SiteReader {
		private Socket sock;
		private BufferedReader in;
		private BufferedWriter out;
		private Reader reader;
		private String userAgent = "Mozilla/5.0 (Windows NT 6.2; rv:9.0.1) Gecko/20100101 Firefox/9.0.1";
		private boolean finished = false;

		public SiteReader() {
			init("www.pathofexile.com", 80,
					"scripts/beta-invite-query.php?mode=next");
		}

		public final void init(String address, int port, Object page) {
			try {
				sock = new Socket(address, port);
				in = new BufferedReader(new InputStreamReader(
						sock.getInputStream()));
				out = new BufferedWriter(new OutputStreamWriter(
						sock.getOutputStream()));
				sendLine("GET /" + page.toString() + " HTTP/1.0");
				sendLine("HOST: " + address);
				sendLine("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*\\/*;q=0.8");
				sendLine("Accept-Language: en-us,en;q=0.5");
				sendLine("Referer: " + address);
				sendLine("User-Agent: " + userAgent);
				sendLine("Connection: close\r\n\r\n");
				reader = new Reader();
				reader.start();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		public final boolean isAlive() {
			return reader.isAlive() && !finished;
		}

		public final void close() {
			try {
				sock.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private class Reader extends Thread {
			public void run() {
				try {
					String line;
					while ((line = in.readLine()) != null) {
						onReadLine(line);
					}
					finished = true;
				} catch (Exception ex) {
					finished = true;
					ex.printStackTrace();
				}
			}
		}

		public final void sendLine(String data) {
			try {
				out.write(data + "\r\n");
				out.flush();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		public void onReadLine(String line) {
			if (line.startsWith("{\"action\":\"update\"")) {
				try {
					String[] data = line.split("\\{");
					String last = data[3], next = data[4];
					Matcher m = Pattern.compile(
							"(?:.+)\\\\\">((.+))<\\\\/a>(?:.+)").matcher(last);
					m.find();
					last = m.group(1);
					m = Pattern
							.compile(
									"(?:.+)\\\\\">(.+)<\\\\/a>(?:.+)next_s\":(\\d+)(?:.+)")
							.matcher(next);
					m.find();
					updateData(last, m.group(1), Integer.parseInt(m.group(2)));
				} catch (Exception ex) {
					ex.printStackTrace();
					updateData("Failed to get data.", "Failed to get data.", -1);
				}
			}
		}
	}
	
	public class TrayIcon extends JFrame {
		private static final long serialVersionUID = -6491847398907164018L;

		public TrayIcon() {
			this.setTitle("PoE Beta Timer");
			this.setSize(0, 0);
			this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			this.setUndecorated(true);
			this.setOpacity(0.0f);
			try {
				this.setIconImage(ImageIO.read(new URL("http://l3eta.nightphoenix13.info/projects/poe/icon.png")));
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.addWindowListener(new WindowAdapter() {				
				public void windowActivated(WindowEvent e) {
					window.toFront();
				}
				
				public void windowDeiconified(WindowEvent arg0) {
					window.setVisible(true);					
				}

				public void windowIconified(WindowEvent arg0) {
					window.setVisible(false);					
				}				
			});
			
			this.setVisible(true);
		}
		
		
		
	}
}
