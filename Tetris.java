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

					/*
					 * Drop - When pressed, we check to see that the game is not
					 * paused and that there is no drop cooldown, then set the
					 * logic timer to run at a speed of 25 cycles per second.
					 */
					case KeyEvent.VK_S:
						if (!isPaused && dropCooldown == 0) {
							logicTimer.setCyclesPerSecond(25.0f);
						}
						break;

					/*
					 * Move Left - When pressed, we check to see that the game is
					 * not paused and that the position to the left of the current
					 * position is valid. If so, we decrement the current column by 1.
					 */
					case KeyEvent.VK_A:
						if (!isPaused
								&& board.isValidAndEmpty(currentType, currentCol - 1, currentRow, currentRotation)) {
							currentCol--;
						}
						break;

					/*
					 * Move Right - When pressed, we check to see that the game is
					 * not paused and that the position to the right of the current
					 * position is valid. If so, we increment the current column by 1.
					 */
					case KeyEvent.VK_D:
						if (!isPaused
								&& board.isValidAndEmpty(currentType, currentCol + 1, currentRow, currentRotation)) {
							currentCol++;
						}
						break;

					/*
					 * Rotate Anticlockwise - When pressed, check to see that the game is not paused
					 * and then attempt to rotate the piece anticlockwise. Because of the size and
					 * complexity of the rotation code, as well as it's similarity to clockwise
					 * rotation, the code for rotating the piece is handled in another method.
					 */
					case KeyEvent.VK_Q:
						if (!isPaused) {
							rotatePiece((currentRotation == 0) ? 3 : currentRotation - 1);
						}
						break;

					/*
					 * Rotate Clockwise - When pressed, check to see that the game is not paused
					 * and then attempt to rotate the piece clockwise. Because of the size and
					 * complexity of the rotation code, as well as it's similarity to anticlockwise
					 * rotation, the code for rotating the piece is handled in another method.
					 */
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

					/*
					 * Start Game - When pressed, check to see that we're in either a game over or
					 * new
					 * game state. If so, reset the game.
					 */
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

					/*
					 * Drop - When released, we set the speed of the logic timer
					 * back to whatever the current game speed is and clear out
					 * any cycles that might still be elapsed.
					 */
					case KeyEvent.VK_S:
						logicTimer.setCyclesPerSecond(gameSpeed);
						logicTimer.reset();
						break;
				}

			}

		});

		/*
		 * Here we resize the frame to hold the BoardPanel and SidePanel instances,
		 * center the window on the screen, and show it to the user.
		 */
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
		showMenu();

	}

	/**
	 * START GAME!
	 */
	void startGame() {
		/*
		 * Initialize our random number generator, logic timer, and new game variables.
		 */
		this.random = new Random();
		this.isNewGame = true;
		/*
		 * Setup the timer to keep the game from running before the user presses enter
		 * to start it.
		 */

		this.logicTimer = new Clock(gameSpeed);
		logicTimer.setPaused(true);

		Timer gameTimer = new Timer((int) FRAME_TIME, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Update the logic timer.
				logicTimer.update();

				/*
				 * If a cycle has elapsed on the timer, we can update the game and
				 * move our current piece down.
				 */
				if (logicTimer.hasElapsedCycle()) {
					updateGame();
				}

				// Decrement the drop cool down if necessary.
				if (dropCooldown > 0) {
					dropCooldown--;
				}

				// Display the window to the user.
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
		/*
		 * Check to see if the piece's position can move down to the next row.
		 */
		if (board.isValidAndEmpty(currentType, currentCol, currentRow + 1, currentRotation)) {
			// Increment the current row if it's safe to do so.
			currentRow++;
		} else {
			/*
			 * We've either reached the bottom of the board, or landed on another piece, so
			 * we need to add the piece to the board.
			 */
			board.addPiece(currentType, currentCol, currentRow, currentRotation);

			/*
			 * Check to see if adding the new piece resulted in any cleared lines. If so,
			 * increase the player's score. (Up to 4 lines can be cleared in a single go;
			 * [1 = 100pts, 2 = 200pts, 3 = 400pts, 4 = 800pts]).
			 */
			int cleared = board.checkLines();
			if (cleared > 0) {
				score += 50 << cleared;
				gameSpeed += 2;
				playMusic("asset/success.wav");

			}

			/*
			 * Increase the speed slightly for the next piece and update the game's timer
			 * to reflect the increase.
			 */
			gameSpeed += 0.25f;
			logicTimer.setCyclesPerSecond(gameSpeed);
			logicTimer.reset();

			/*
			 * Set the drop cooldown so the next piece doesn't automatically come flying
			 * in from the heavens immediately after this piece hits if we've not reacted
			 * yet. (~0.5 second buffer).
			 */
			dropCooldown = 25;

			/*
			 * Spawn a new piece to control.
			 */
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

	/**
	 * Resets the game variables to their default values at the start
	 * of a new game.
	 */
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

	/**
	 * Spawns a new piece and resets our piece's variables to their default
	 * values.
	 */
	private void spawnPiece() {
		/*
		 * Poll the last piece and reset our position and rotation to
		 * their default variables, then pick the next piece to use.
		 */
		this.currentType = nextType;
		this.currentCol = currentType.getSpawnColumn();
		this.currentRow = currentType.getSpawnRow();
		this.currentRotation = 0;
		this.nextType = TileType.values()[random.nextInt(TYPE_COUNT)];

		/*
		 * If the spawn point is invalid, we need to pause the game and flag that we've
		 * lost
		 * because it means that the pieces on the board have gotten too high.
		 */
		if (!board.isValidAndEmpty(currentType, currentCol, currentRow, currentRotation)) {
			this.isGameOver = true;
			logicTimer.setPaused(true);
		}
	}

	/**
	 * Attempts to set the rotation of the current piece to newRotation.
	 * 
	 * @param newRotation The rotation of the new peice.
	 */
	private void rotatePiece(int newRotation) {
		/*
		 * Sometimes pieces will need to be moved when rotated to avoid clipping
		 * out of the board (the I piece is a good example of this). Here we store
		 * a temporary row and column in case we need to move the tile as well.
		 */
		int newColumn = currentCol;
		int newRow = currentRow;

		/*
		 * Get the insets for each of the sides. These are used to determine how
		 * many empty rows or columns there are on a given side.
		 */
		int left = currentType.getLeftInset(newRotation);
		int right = currentType.getRightInset(newRotation);
		int top = currentType.getTopInset(newRotation);
		int bottom = currentType.getBottomInset(newRotation);

		/*
		 * If the current piece is too far to the left or right, move the piece away
		 * from the edges
		 * so that the piece doesn't clip out of the map and automatically become
		 * invalid.
		 */
		if (currentCol < -left) {
			newColumn -= currentCol - left;
		} else if (currentCol + currentType.getDimension() - right >= BoardPanel.COL_COUNT) {
			newColumn -= (currentCol + currentType.getDimension() - right) - BoardPanel.COL_COUNT + 1;
		}

		/*
		 * If the current piece is too far to the top or bottom, move the piece away
		 * from the edges
		 * so that the piece doesn't clip out of the map and automatically become
		 * invalid.
		 */
		if (currentRow < -top) {
			newRow -= currentRow - top;
		} else if (currentRow + currentType.getDimension() - bottom >= BoardPanel.ROW_COUNT) {
			newRow -= (currentRow + currentType.getDimension() - bottom) - BoardPanel.ROW_COUNT + 1;
		}

		/*
		 * Check to see if the new position is acceptable. If it is, update the rotation
		 * and
		 * position of the piece.
		 */
		if (board.isValidAndEmpty(currentType, newColumn, newRow, newRotation)) {
			currentRotation = newRotation;
			currentRow = newRow;
			currentCol = newColumn;
		}
	}

	/**
	 * Checks to see whether or not the game is paused.
	 * 
	 * @return Whether or not the game is paused.
	 */
	public boolean isPaused() {
		return isPaused;
	}

	/**
	 * Checks to see whether or not the game is over.
	 * 
	 * @return Whether or not the game is over.
	 */
	public boolean isGameOver() {
		return isGameOver;
	}

	/**
	 * Checks to see whether or not we're on a new game.
	 * 
	 * @return Whether or not this is a new game.
	 */
	public boolean isNewGame() {
		return isNewGame;
	}

	/**
	 * Gets the current score.
	 * 
	 * @return The score.
	 */
	public int getScore() {
		return score;
	}

	/**
	 * Gets the current level.
	 * 
	 * @return The level.
	 */
	public int getLevel() {
		return level;
	}

	public float getSpeed() {
		return gameSpeed;
	}

	/**
	 * Gets the current type of piece we're using.
	 * 
	 * @return The piece type.
	 */
	public TileType getPieceType() {
		return currentType;
	}

	/**
	 * Gets the next type of piece we're using.
	 * 
	 * @return The next piece.
	 */
	public TileType getNextPieceType() {
		return nextType;
	}

	/**
	 * Gets the column of the current piece.
	 * 
	 * @return The column.
	 */
	public int getPieceCol() {
		return currentCol;
	}

	/**
	 * Gets the row of the current piece.
	 * 
	 * @return The row.
	 */
	public int getPieceRow() {
		return currentRow;
	}

	public Color getCurrentPieceColor() {
		return currentPieceColor;
	}

	// Method to set the color of the current piece
	public void setCurrentPieceColor(Color color) {
		this.currentPieceColor = color;
	}

	/**
	 * Gets the rotation of the current piece.
	 * 
	 * @return The rotation.
	 */
	public int getPieceRotation() {
		return currentRotation;
	}

	public void playMusic(String filePath) {
		try {
			// Open an audio input stream from the specified file path
			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(filePath));

			// Get a Clip object to play the audio
			Clip clip = AudioSystem.getClip();

			// Open the audio clip
			clip.open(audioInputStream);
			FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			gainControl.setValue(-20.0f);
			clip.start();
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
			// Handle any exceptions gracefully
			e.printStackTrace(); // Consider logging the exception or displaying an error message
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
		menuPanel.setLayout(null); // Use null layout for absolute positioning

		// Add transparent buttons
		JButton startButton = createTransparentButton("START");
		JButton levelButton = createTransparentButton("LEVEL");

		// Set the position and size of the buttons
		startButton.setBounds(540, 250, 200, 100); // x, y, width, height
		levelButton.setBounds(540, 370, 200, 100); // x, y, width, height

		// Add action listeners to the buttons
		startButton.addActionListener(e -> {
			menuFrame.setVisible(false); // Hide the menu window
			menuFrame.dispose(); // Dispose of the menu window resources
			requestFocus(); // Request focus on the main game window
			startGame(); // Start the game
		});

		levelButton.addActionListener(e -> {
			menuPanel.setCurrentState("Level"); // Update the state to "Level"
			menuPanel.removeAll(); // Remove all components from the panel

			// Add level buttons with specific coordinates and size
			menuPanel.add(createLevelButton(1, 525, 200, 250, 60, menuFrame)); // Level 1 button
			menuPanel.add(createLevelButton(2, 555, 300, 150, 60, menuFrame)); // Level 2 button
			menuPanel.add(createLevelButton(3, 450, 415, 400, 60, menuFrame)); // Level 3 button

			menuFrame.revalidate(); // Revalidate the frame to reflect changes
		});

		// Add buttons to the menuPanel
		menuPanel.add(startButton);
		menuPanel.add(levelButton);
		setResizable(false);

		// Add the menuPanel to the menuFrame
		menuFrame.add(menuPanel);
		menuFrame.setSize(1280, 720); // Set a larger size for the menu frame
		menuFrame.setLocationRelativeTo(null);
		menuFrame.setVisible(true);

		// Set default button for the menuFrame
		menuFrame.getRootPane().setDefaultButton(startButton);
		setResizable(false);

	}

	public JButton createLevelButton(int level, int x, int y, int width, int height, JFrame menuFrame) {
		JButton button = new JButton("Level " + level);
		button.setBounds(x, y, width, height); // Set the bounds with specific coordinates and size
		button.setOpaque(false); // Make the button transparent
		button.setContentAreaFilled(false); // Make the content area transparent
		button.setBorderPainted(false); // Remove border
		button.setForeground(new Color(0, 0, 0, 0)); // Set text color to fully transparent
		button.setFocusPainted(false); // Remove focus indication
		button.addActionListener(e -> {
			this.level = level; // Set the selected level
			menuFrame.setVisible(false); // Hide the menu window
			menuFrame.dispose(); // Dispose of the menu window resources
			requestFocus(); // Request focus on the main game window
			startGame(); // Start the game with the selected level

		});
		return button;
	}

	public class MenuPanel extends JPanel {
		private String currentState = "Start"; // Variable to track the current state

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			ImageIcon imageIcon = currentState.equals("Start") ? new ImageIcon("asset/wallpaper_start.png")
					: new ImageIcon("asset/wallpaper_levels.png");
			g.drawImage(imageIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
		}

		// Method to update the current state
		public void setCurrentState(String state) {
			currentState = state;
			repaint(); // Repaint the panel to update the wallpaper
		}
	}

	private JButton createTransparentButton(String label) {
		JButton button = new JButton(label);
		button.setOpaque(false); // Make the button transparent
		button.setContentAreaFilled(false); // Make the content area transparent
		button.setBorderPainted(false); // Remove border
		button.setForeground(new Color(0, 0, 0, 0)); // Set text color to fully transparent
		button.setFocusPainted(false); // Remove focus indication

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