import java.util.Random;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.sound.sampled.*;

import java.io.File;

import java.io.IOException;

public class Tetris extends JFrame {
	private Clip clip;

	private static final long FRAME_TIME = 1000L / 50L;

	private static final int TYPE_COUNT = TileType.values().length;

	private BoardPanel board;

	private SidePanel side;

	private boolean isPaused;

	private boolean isNewGame;

	private boolean isGameOver;

	private int level;

	private int score;

	private Random random;

	private Clock logicTimer;

	private TileType currentType;

	private TileType nextType;

	private int currentCol;

	private int currentRow;

	private int currentRotation;

	private Color currentPieceColor;

	private int dropCooldown;

	private float gameSpeed;

	public Tetris() {
		super("Tetrimino");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setResizable(false);
		this.board = new BoardPanel(this);
		this.side = new SidePanel(this);
		add(board, BorderLayout.CENTER);
		add(side, BorderLayout.EAST);

		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_S:
						if (!isPaused && dropCooldown == 0) {
							logicTimer.setCyclesPerSecond(25.0f);
						}
						break;
					case KeyEvent.VK_A:
						if (!isPaused
								&& board.isValidAndEmpty(currentType, currentCol - 1, currentRow, currentRotation)) {
							currentCol--;
						}
						break;
					case KeyEvent.VK_D:
						if (!isPaused
								&& board.isValidAndEmpty(currentType, currentCol + 1, currentRow, currentRotation)) {
							currentCol++;
						}
						break;
					case KeyEvent.VK_Q:
						if (!isPaused) {
							rotatePiece((currentRotation == 0) ? 3 : currentRotation - 1);
						}
						break;
					case KeyEvent.VK_E:
						if (!isPaused) {
							rotatePiece((currentRotation == 3) ? 0 : currentRotation + 1);
						}
						break;
					case KeyEvent.VK_P:
						if (!isGameOver && !isNewGame) {
							isPaused = !isPaused;
							logicTimer.setPaused(isPaused);
						}
						break;
					case KeyEvent.VK_ENTER:
						if (isGameOver || isNewGame) {
							resetOrPLAYGame();
							stopMusic();
						}
						break;
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_S:
						logicTimer.setCyclesPerSecond(gameSpeed);
						logicTimer.reset();
						break;
				}
			}
		});
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
		showMenu();

	}

	/**
	 * START GAME!
	 */
	void startGame() {

		this.random = new Random();
		this.isNewGame = true;

		this.logicTimer = new Clock(gameSpeed);
		logicTimer.setPaused(true);

		Timer gameTimer = new Timer((int) FRAME_TIME, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Update the logic timer.
				logicTimer.update();
				if (logicTimer.hasElapsedCycle()) {
					updateGame();
				}
				if (dropCooldown > 0) {
					dropCooldown--;
				}
				renderGame();
			}
		});
		gameTimer.setRepeats(true);
		gameTimer.start();

	}

	/**
	 * Updates the game and handles logic.
	 */
	private void updateGame() {
		if (board.isValidAndEmpty(currentType, currentCol, currentRow + 1, currentRotation)) {
			currentRow++;
		} else {
			board.addPiece(currentType, currentCol, currentRow, currentRotation);

			int cleared = board.checkAllLine();
			if (cleared > 0) {
				score += 50 << cleared;
				gameSpeed += 2;
				// playMusic("asset/success.wav");
			}
			gameSpeed += 0.25f;
			logicTimer.setCyclesPerSecond(gameSpeed);
			logicTimer.reset();
			dropCooldown = 25;
			spawnPiece();
		}
	}

	/**
	 * Forces the BoardPanel and SidePanel to repaint.
	 */
	private void renderGame() {
		board.repaint();
		side.repaint();
	}

	private void resetOrPLAYGame() {
		switch (level) {
			case 0:
				this.gameSpeed = 1;
				break;
			case 1:
				this.gameSpeed = 3;
				break;
			case 2:
				this.gameSpeed = 9;
				break;
			default:
				this.level = 0;
				this.gameSpeed = 1;
				break;
		}
		this.score = 0;
		this.nextType = TileType.values()[random.nextInt(TYPE_COUNT)];
		this.isNewGame = false;
		this.isGameOver = false;
		board.clear();
		logicTimer.reset();
		logicTimer.setCyclesPerSecond(gameSpeed);
		spawnPiece();
	}

	private void spawnPiece() {

		this.currentType = nextType;
		this.currentCol = currentType.getSpawnColumn();
		this.currentRow = currentType.getSpawnRow();
		this.currentRotation = 0;
		this.nextType = TileType.values()[random.nextInt(TYPE_COUNT)];

		if (board.isValidAndEmpty(currentType, currentCol, currentRow, currentRotation) == false) {
			this.isGameOver = true;
			logicTimer.setPaused(true);
		}
	}

	private void rotatePiece(int newRotation) {

		int newColumn = currentCol;
		int newRow = currentRow;

		int left = currentType.getLeftInset(newRotation);
		int right = currentType.getRightInset(newRotation);
		int top = currentType.getTopInset(newRotation);
		int bottom = currentType.getBottomInset(newRotation);

		if (currentCol < -left) {
			newColumn -= currentCol - left;
		} else if (currentCol + currentType.getDimension() - right >= BoardPanel.COL_COUNT) {
			newColumn -= (currentCol + currentType.getDimension() - right) - BoardPanel.COL_COUNT + 1;
		}

		if (currentRow < -top) {
			newRow -= currentRow - top;
		} else if (currentRow + currentType.getDimension() - bottom >= BoardPanel.ROW_COUNT) {
			newRow -= (currentRow + currentType.getDimension() - bottom) - BoardPanel.ROW_COUNT + 1;
		}

		if (board.isValidAndEmpty(currentType, newColumn, newRow, newRotation)) {
			currentRotation = newRotation;
			currentRow = newRow;
			currentCol = newColumn;
		}
	}

	public boolean isPaused() {
		return isPaused;
	}

	public boolean isGameOver() {
		return isGameOver;
	}

	public boolean isNewGame() {
		return isNewGame;
	}

	public int getScore() {
		return score;
	}

	public int getLevel() {
		return level;
	}

	public float getSpeed() {
		return gameSpeed;
	}

	public TileType getPieceType() {
		return currentType;
	}

	public TileType getNextPieceType() {
		return nextType;
	}

	public int getPieceCol() {
		return currentCol;
	}

	public int getPieceRow() {
		return currentRow;
	}

	public Color getCurrentPieceColor() {
		return currentPieceColor;
	}

	public void setCurrentPieceColor(Color color) {
		this.currentPieceColor = color;
	}

	public int getPieceRotation() {
		return currentRotation;
	}

	public void playMusic(String filePath) {
		try {
			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(filePath));

			Clip clip = AudioSystem.getClip();

			clip.open(audioInputStream);
			FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			gainControl.setValue(-20.0f);
			clip.start();
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
			e.printStackTrace();
		}

	}

	public void stopMusic() {
		if (clip != null && clip.isRunning()) {
			clip.stop();
		}
	}

	public void showMenu() {
		playMusic("asset/song.wav");
		JFrame menuFrame = new JFrame("Tetrimino Menu");
		MenuPanel menuPanel = new MenuPanel();
		setResizable(false);
		menuPanel.setLayout(null);

		JButton startButton = createTransparentButton("START");
		JButton levelButton = createTransparentButton("LEVEL");

		startButton.setBounds(540, 250, 200, 100); // x, y, width, height
		levelButton.setBounds(540, 370, 200, 100); // x, y, width, height

		startButton.addActionListener(e -> {
			menuFrame.setVisible(false);
			menuFrame.dispose();
			requestFocus();
			startGame();
		});

		levelButton.addActionListener(e -> {
			menuPanel.setCurrentState("Level");
			menuPanel.removeAll();

			menuPanel.add(createLevelButton(1, 525, 200, 250, 60, menuFrame)); // Level 1 button
			menuPanel.add(createLevelButton(2, 555, 300, 150, 60, menuFrame)); // Level 2 button
			menuPanel.add(createLevelButton(3, 450, 415, 400, 60, menuFrame)); // Level 3 button

			menuFrame.revalidate(); // Revalidate the frame to reflect changes
		});

		menuPanel.add(startButton);
		menuPanel.add(levelButton);
		setResizable(false);

		menuFrame.add(menuPanel);
		menuFrame.setSize(1280, 720);
		menuFrame.setLocationRelativeTo(null);
		menuFrame.setVisible(true);

		menuFrame.getRootPane().setDefaultButton(startButton);
		setResizable(false);

	}

	public JButton createLevelButton(int level, int x, int y, int width, int height, JFrame menuFrame) {
		JButton button = new JButton("Level " + level);
		button.setBounds(x, y, width, height);
		button.setOpaque(false);
		button.setContentAreaFilled(false);
		button.setBorderPainted(false);
		button.setForeground(new Color(0, 0, 0, 0));
		button.setFocusPainted(false); // Remove focus indication
		button.addActionListener(e -> {
			this.level = level; // Set the selected level
			menuFrame.setVisible(false);
			menuFrame.dispose();
			requestFocus();
			startGame();

		});
		return button;
	}

	public class MenuPanel extends JPanel {
		private String currentState = "Start";

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			ImageIcon imageIcon = currentState.equals("Start") ? new ImageIcon("asset/wallpaper_start.png")
					: new ImageIcon("asset/wallpaper_levels.png");
			g.drawImage(imageIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
		}

		public void setCurrentState(String state) {
			currentState = state;
			repaint();
		}
	}

	private JButton createTransparentButton(String label) {
		JButton button = new JButton(label);
		button.setOpaque(false);
		button.setContentAreaFilled(false);
		button.setBorderPainted(false);
		button.setForeground(new Color(0, 0, 0, 0));
		button.setFocusPainted(false);

		return button;
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new Tetris();
			}
		});
	}
}