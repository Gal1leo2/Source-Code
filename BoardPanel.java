import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.JPanel;

public class BoardPanel extends JPanel {

	public static final int COLOR_MIN = 35;

	public static final int COLOR_MAX = 255 - COLOR_MIN;

	private static final int BORDER_WIDTH = 5;

	public static final int COL_COUNT = 10;

	private static final int VISIBLE_ROW_COUNT = 20;

	private static final int HIDDEN_ROW_COUNT = 2;

	public static final int ROW_COUNT = VISIBLE_ROW_COUNT + HIDDEN_ROW_COUNT;

	public static final int TILE_SIZE = 24;

	public static final int SHADE_WIDTH = 4;

	private static final int CENTER_X = COL_COUNT * TILE_SIZE / 2;

	private static final int CENTER_Y = VISIBLE_ROW_COUNT * TILE_SIZE / 2;

	public static final int PANEL_WIDTH = COL_COUNT * TILE_SIZE + BORDER_WIDTH * 2;

	public static final int PANEL_HEIGHT = VISIBLE_ROW_COUNT * TILE_SIZE + BORDER_WIDTH * 2;

	private static final Font LARGE_FONT = new Font("Tahoma", Font.BOLD, 16);

	private static final Font SMALL_FONT = new Font("Tahoma", Font.BOLD, 12);

	private Tetris tetris;

	private TileType[][] tiles;

	public BoardPanel(Tetris tetris) {
		this.tetris = tetris;
		this.tiles = new TileType[ROW_COUNT][COL_COUNT];

		setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
		setBackground(Color.WHITE);
	}

	public void clear() {
		/*
		 * Loop through every tile index and set it's value
		 * to null to clear the board.
		 */
		for (int i = 0; i < ROW_COUNT; i++) {
			for (int j = 0; j < COL_COUNT; j++) {
				tiles[i][j] = null;
			}
		}
	}

	public boolean isValidAndEmpty(TileType type, int x, int y, int rotation) {

		if (x < -type.getLeftInset(rotation) || x + type.getDimension() - type.getRightInset(rotation) >= COL_COUNT) {
			return false;
		}

		if (y < -type.getTopInset(rotation) || y + type.getDimension() - type.getBottomInset(rotation) >= ROW_COUNT) {
			return false;
		}

		for (int col = 0; col < type.getDimension(); col++) {
			for (int row = 0; row < type.getDimension(); row++) {
				if (type.isTile(col, row, rotation) && isOccupied(x + col, y + row)) {
					return false;
				}
			}
		}
		return true;
	}

	public void addPiece(TileType type, int x, int y, int rotation) {
		/*
		 * Loop through every tile within the piece and add it
		 * to the board only if the boolean that represents that
		 * tile is set to true.
		 */
		for (int col = 0; col < type.getDimension(); col++) {
			for (int row = 0; row < type.getDimension(); row++) {
				if (type.isTile(col, row, rotation)) {
					setTile(col + x, row + y, type);
				}
			}
		}
	}

	public int checkAllLine() {
		int completedLines = 0;

		for (int row = 0; row < ROW_COUNT; row++) {
			if (checkspecificLine(row)) {
				completedLines++;
			}
		}
		return completedLines;
	}

	private boolean checkspecificLine(int line) {

		for (int col = 0; col < COL_COUNT; col++) {
			if (isOccupied(col, line) == false) {
				return false;
			}
		}

		for (int row = line - 1; row >= 0; row--) {
			for (int col = 0; col < COL_COUNT; col++) {
				setTile(col, row + 1, getTile(col, row));
			}
		}
		return true;
	}

	private boolean isOccupied(int x, int y) {
		return tiles[y][x] != null;
	}

	private void setTile(int x, int y, TileType type) {
		tiles[y][x] = type;
	}

	TileType getTile(int x, int y) {
		return tiles[y][x];
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		g.translate(BORDER_WIDTH, BORDER_WIDTH);

		if (tetris.isPaused()) {
			g.setFont(LARGE_FONT);
			g.setColor(Color.WHITE);
			String msg = "PAUSED";
			g.drawString(msg, CENTER_X - g.getFontMetrics().stringWidth(msg) / 2, CENTER_Y);
		} else if (tetris.isNewGame() || tetris.isGameOver()) {
			g.setFont(LARGE_FONT);
			g.setColor(Color.BLACK);

			String msg = tetris.isNewGame() ? "TETRIS" : "GAME OVER";
			g.drawString(msg, CENTER_X - g.getFontMetrics().stringWidth(msg) / 2, 150);
			g.setFont(SMALL_FONT);
			msg = "Press Enter to Play" + (tetris.isNewGame() ? "" : " Again");
			g.drawString(msg, CENTER_X - g.getFontMetrics().stringWidth(msg) / 2, 300);
		} else {

			/*
			 * Draw the tiles onto the board.
			 */
			for (int x = 0; x < COL_COUNT; x++) {
				for (int y = HIDDEN_ROW_COUNT; y < ROW_COUNT; y++) {
					TileType tile = getTile(x, y);
					if (tile != null) {

						drawTile(tile, x * TILE_SIZE, (y - HIDDEN_ROW_COUNT) * TILE_SIZE, g);
					}
				}
			}

			/*
			 * Draw the current piece
			 */
			TileType type = tetris.getPieceType();
			int pieceCol = tetris.getPieceCol();
			int pieceRow = tetris.getPieceRow();
			int rotation = tetris.getPieceRotation();

			// Draw the piece onto the board.
			for (int col = 0; col < type.getDimension(); col++) {
				for (int row = 0; row < type.getDimension(); row++) {
					if (pieceRow + row >= 2 && type.isTile(col, row, rotation)) {
						drawTile(type, (pieceCol + col) * TILE_SIZE, (pieceRow + row - HIDDEN_ROW_COUNT) * TILE_SIZE,
								g);
					}
				}
			}
			/*
			 * Draw Ghost
			 */
			Color base = type.getBaseColor();
			base = new Color(base.getRed(), base.getGreen(), base.getBlue(), 20);
			for (int lowest = pieceRow; lowest < ROW_COUNT; lowest++) {
				if (isValidAndEmpty(type, pieceCol, lowest, rotation)) {
					continue;
				}

				lowest -= 1;

				for (int col = 0; col < type.getDimension(); col++) {
					for (int row = 0; row < type.getDimension(); row++) {
						if (lowest + row >= 2 && type.isTile(col, row, rotation)) {
							drawTile(base, base.brighter(), base.darker(), (pieceCol + col) * TILE_SIZE,
									(lowest + row - HIDDEN_ROW_COUNT) * TILE_SIZE, g);
						}
					}
				}

				break;
			}
			g.setColor(Color.BLUE);
			for (int x = 0; x < COL_COUNT; x++) {
				for (int y = 0; y < VISIBLE_ROW_COUNT; y++) {
					g.drawLine(0, y * TILE_SIZE, COL_COUNT * TILE_SIZE, y * TILE_SIZE);
					g.drawLine(x * TILE_SIZE, 0, x * TILE_SIZE, VISIBLE_ROW_COUNT * TILE_SIZE);
				}
			}
		}
		g.setColor(Color.BLACK);
		g.drawRect(0, 0, TILE_SIZE * COL_COUNT, TILE_SIZE * VISIBLE_ROW_COUNT);
	}

	private void drawTile(TileType type, int x, int y, Graphics g) {
		drawTile(type.getBaseColor(), type.getLightColor(), type.getDarkColor(), x, y, g);
	}

	private void drawTile(Color base, Color light, Color dark, int x, int y, Graphics g) {

		g.setColor(base);
		g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
	}

}