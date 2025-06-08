import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

class InvalidKeyException extends Exception {
    public InvalidKeyException(String message) {
        super(message);
    }
}

class Raindrop {
    int x, y, size, speed;
    Color color;

    public Raindrop(int x, int y, Color color, int speed) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.size = 20;
        this.speed = speed;
    }
}

public class RainDotsGame extends JPanel implements ActionListener, KeyListener {
    Timer timer;
    ArrayList<Raindrop> raindrops;
    Random random;
    int gameTime, elapsedTime, score;
    long startTime;
    Color rectangleColor;
    final Color[] COLORS = {Color.RED, Color.BLUE};
    String errorMessage = "";
    int dropSpeed;

    public RainDotsGame(int speed) {
        this.setPreferredSize(new Dimension(800, 600));
        this.setBackground(Color.PINK);
        this.setFocusable(true);
        this.requestFocusInWindow();
        this.addKeyListener(this);

        raindrops = new ArrayList<>();
        random = new Random();
        gameTime = 60;
        startTime = System.currentTimeMillis();
        rectangleColor = COLORS[0];
        score = 0;
        dropSpeed = speed;

        timer = new Timer(1000 / 40, this);
        timer.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (Raindrop drop : raindrops) {
            g.setColor(drop.color);
            g.fillOval(drop.x, drop.y, drop.size, drop.size);
        }
        g.setColor(rectangleColor);
        g.fillRect(0, 550, 800, 50);

        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Time Left: " + Math.max(0, gameTime - elapsedTime), 10, 20);
        g.drawString("Score: " + score, 650, 20);

        if (!errorMessage.isEmpty()) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 18));
            g.drawString(errorMessage, 200, 500);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        elapsedTime = (int) ((System.currentTimeMillis() - startTime) / 1000);
        if (elapsedTime >= gameTime) {
            endGame("Time's up!");
        }

        if (random.nextDouble() < 0.02) {
            raindrops.add(new Raindrop(random.nextInt(getWidth()), 0, COLORS[random.nextInt(COLORS.length)], dropSpeed));
        }

        Iterator<Raindrop> iterator = raindrops.iterator();
        while (iterator.hasNext()) {
            Raindrop drop = iterator.next();
            drop.y += drop.speed;
            if (drop.y + drop.size >= 550) {
                if (!drop.color.equals(rectangleColor)) {
                    endGame("Game Over! Wrong color match. Final score: " + score);
                } else {
                    score++;
                }
                iterator.remove();
            }
        }
        repaint();
    }

    void endGame(String message) {
        timer.stop();
        saveScoreToDatabase("Player1", score);
        displayScores();
        JOptionPane.showMessageDialog(this, message);
        System.exit(0);
    }

    void saveScoreToDatabase(String playerName, int score) {
        String url = "jdbc:mysql://localhost:3306/RainDotsGamedb";
        String username = "root";
        String password = "nishii2024";
        String query = "INSERT INTO sco (player_name, score) VALUES (?, ?)";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, playerName);
                stmt.setInt(2, score);
                stmt.executeUpdate();
                
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    void displayScores() {
        String url = "jdbc:mysql://localhost:3306/RainDotsGamedb";
        String username = "root";
        String password = "nishii2024";
        String queryHighScore = "SELECT player_name, score FROM sco ORDER BY score DESC LIMIT 1";

        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rsHigh = stmt.executeQuery(queryHighScore)) {
            String highScoreMessage = "No high score yet.";
            if (rsHigh.next()) {
                highScoreMessage = "High Score: " + rsHigh.getString("player_name") + " - " + rsHigh.getInt("score");
            }
            JOptionPane.showMessageDialog(this, highScoreMessage + "\nYour Score: " + score);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        try {
            char pressedKey = e.getKeyChar();
            if (pressedKey == 'r' || pressedKey == 'R') {
                rectangleColor = Color.RED;
                errorMessage = "";
            } else if (pressedKey == 'b' || pressedKey == 'B') {
                rectangleColor = Color.BLUE;
                errorMessage = "";
            } else {
                throw new InvalidKeyException("Invalid key pressed: " + pressedKey + ". Press 'R' or 'B'.");
            }
        } catch (InvalidKeyException ex) {
            errorMessage = ex.getMessage();
        }
        repaint();
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Rain Dots Game");
            JPanel panel = new JPanel();
            panel.setLayout(null); // Set null layout for manual positioning
            panel.setBackground(Color.PINK);

            JLabel label = new JLabel("Select Difficulty:");
            label.setFont(new Font("Arial", Font.BOLD, 24));
            label.setBounds(120, 30, 200, 30);
            panel.add(label);

            JButton easy = new JButton("Easy");
            JButton medium = new JButton("Medium");
            JButton hard = new JButton("Hard");

            easy.setBounds(150, 80, 100, 40);
            medium.setBounds(150, 130, 100, 40);
            hard.setBounds(150, 180, 100, 40);

            easy.addActionListener(e -> startGame(frame, 3));
            medium.addActionListener(e -> startGame(frame, 5));
            hard.addActionListener(e -> startGame(frame, 7));

            panel.add(easy);
            panel.add(medium);
            panel.add(hard);

            frame.add(panel);
            frame.setSize(400, 300);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null); // Center the window
            frame.setVisible(true);
        });
    }

    private static void startGame(JFrame frame, int speed) {
        frame.dispose();
        JFrame gameFrame = new JFrame("Rain Dots Game");
        gameFrame.add(new RainDotsGame(speed));
        gameFrame.pack();
        gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gameFrame.setLocationRelativeTo(null);
        gameFrame.setVisible(true);
    }
}