import java.util.Random;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

public class Tetris extends JFrame {

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

	private int dropCooldown;

	private float gameSpeed;

	public Tetris() {
		super("Tetris");
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
							resetGame();
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
		switch (level) {
			case 1:
				this.gameSpeed = 1.0f;
				break;
			case 2:
				this.gameSpeed = 1.5f;
				break;
			case 3:
				this.gameSpeed = 2.0f;
				break;
			default:
				this.gameSpeed = 1.0f; // Default to level 1 if level is not recognized
				break;
		}
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
		playMusic("/Users/ink_project/Desktop/Source Code/asset/song.wav");
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
			}

			/*
			 * Increase the speed slightly for the next piece and update the game's timer
			 * to reflect the increase.
			 */
			gameSpeed += 0.035f;
			logicTimer.setCyclesPerSecond(gameSpeed);
			logicTimer.reset();

			/*
			 * Set the drop cooldown so the next piece doesn't automatically come flying
			 * in from the heavens immediately after this piece hits if we've not reacted
			 * yet. (~0.5 second buffer).
			 */
			dropCooldown = 25;

			/*
			 * Update the difficulty level. This has no effect on the game, and is only
			 * used in the "Level" string in the SidePanel.
			 */
			level = (int) (gameSpeed * 1.70f);

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
	private void resetGame() {
		switch (level) {
			case 1:
				this.gameSpeed = 1.0f;
				break;
			case 2:
				this.gameSpeed = 1.5f;
				break;
			case 3:
				this.gameSpeed = 2.0f;
				break;
			default:
				this.gameSpeed = 1.0f; // Default to level 1 if level is not recognized
				break;
		}
		this.score = 0;
		switch (level) {
			case 1:
				this.gameSpeed = 1.0f;
				break;
			case 2:
				this.gameSpeed = 1.5f;
				break;
			case 3:
				this.gameSpeed = 2.0f;
				break;
			default:
				this.gameSpeed = 1.0f; // Default to level 1 if level is not recognized
				break;
		}
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
			// Open an audio input stream
			AudioInputStream audioInputStream = AudioSystem
					.getAudioInputStream(new File("/Users/ink_project/Desktop/Source Code/asset/song.wav"));

			// Get a Clip object to play the audio
			Clip clip = AudioSystem.getClip();

			// Open the audio clip
			clip.open(audioInputStream);

			// Start playing the audio clip
			clip.start();
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
			e.printStackTrace();
		}
	}

	public void showMenu() {
		playMusic("/Users/ink_project/Desktop/Source Code/asset/song.wav");

		JFrame menuFrame = new JFrame("Tetris Menu");
		JPanel menuPanel = new JPanel();
		menuPanel.setLayout(new GridBagLayout());

		JLabel titleLabel = new JLabel("Tetris");
		titleLabel.setFont(new Font("Arial", Font.BOLD, 24));

		JButton startButton = new JButton("START");
		startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				menuFrame.setVisible(false); // Hide the menu window
				menuFrame.dispose(); // Dispose of the menu window resources
				requestFocus(); // Request focus on the main game window
				startGame(); // Start the game
			}
		});

		JButton optionButton = new JButton("OPTION");
		optionButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Open a dialog for setting options
				int selectedOption = JOptionPane.showOptionDialog(menuFrame, "Set Level:",
						"Option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
						new String[] { "Level 1", "Level 2", "Level 3" }, "Level 1");

				// Update the level based on user selection and adjust game parameters
				switch (selectedOption) {
					case 0:
						level = 1;
						gameSpeed = 1.0f;
						break;
					case 1:
						level = 2;
						gameSpeed = 1.5f;
						break;
					case 2:
						level = 3;
						gameSpeed = 2.0f;
						break;
				}
			}
		});

		JButton creditButton = new JButton("CREDIT");
		creditButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Add action for credit button (to be implemented)
				JOptionPane.showMessageDialog(menuFrame, "Credit menu is not yet implemented.");
			}
		});

		JButton exitButton = new JButton("EXIT");
		exitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0); // Close the program
			}
		});

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(10, 10, 10, 10); // Add some padding
		menuPanel.add(titleLabel, gbc);

		gbc.gridy = 1;
		menuPanel.add(startButton, gbc);

		gbc.gridy = 2;
		menuPanel.add(optionButton, gbc);

		gbc.gridy = 3;
		menuPanel.add(creditButton, gbc);

		gbc.gridy = 4;
		menuPanel.add(exitButton, gbc);

		menuFrame.add(menuPanel);
		menuFrame.setSize(400, 300); // Set a larger size for the menu frame
		menuFrame.setLocationRelativeTo(null);
		menuFrame.setVisible(true);

		// รอ USER กด ENTER
		menuFrame.getRootPane().setDefaultButton(startButton);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new Tetris();
			}
		});
	}
}