package no.dejvi.android.bacteriawallpaper;

public class CellGrid {

	private boolean[] alive;
	private int[] neighbours;
	private int[] family;
	
	private int width;
	private int height;
	
	public CellGrid(int width, int height) {
		if (width <= 0) {
			throw new IllegalArgumentException("width " + width + " <= 0");
		}
		if (height <= 0) {
			throw new IllegalArgumentException("height " + height + " <= 0");
		}
		this.alive = new boolean[width * height];
		this.neighbours = new int[width * height];
		this.width = width;
		this.height = height;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public void importGrid(CellGrid grid) {
		System.arraycopy(grid.alive, 0, this.alive, 0, this.width*this.height);
		System.arraycopy(grid.neighbours, 0, this.neighbours, 0, this.width*this.height);
	}
	
	public boolean isAlive(int x, int y) {
		this.checkBounds(x, y);
		return this.isAlive(x + y * this.width);
	}
	public boolean isAlive(int i) {
		return this.alive[i];
	}
	
	public int countNeighbours(int i) {
		return this.neighbours[i];
	}
	
	public int countNeighbours(int x, int y) {
		this.checkBounds(x, y);
		return this.countNeighbours(x + y * this.width);
	}
	
	public void kill(int x, int y) {
		this.checkBounds(x, y);
		this.kill(x + y * this.width);
	}
	public void kill(int i) {
		if (this.alive[i]) {
			this.updateNeighbours(i % this.width, i / this.width, -1);
		}
		this.alive[i] = false;
	}
	
	public void revive(int x, int y) {
		this.checkBounds(x, y);
		this.revive(x + y * this.width);
	}
	public void revive(int i) {
		if (!this.alive[i]) {
			this.updateNeighbours(i % this.width, i / this.width, 1);
		}
		this.alive[i] = true;
	}
	
	// ================================================================

	/**
	 * Provede kontrolu hodnot pozice X, Y.
	 * Pri chybe vyhodi vyjimku.
	 */
	private void checkBounds(int x, int y) {
		if (x < 0 || x > this.width) {
			throw new IllegalArgumentException("x too small / large: " + x);
		}
		if (y < 0 || y > this.height) {
			throw new IllegalArgumentException("y too small / large: " + y);
		}
	}
	
	private void changeNeighbours(int x, int y, int change) {
		this.neighbours[x + y * this.width] += change;
	}
	
	private void updateNeighbours(int x, int y, int change) {
		// top left
		if (x > 0 && y > 0) {
			this.changeNeighbours(x-1, y-1, change);
		}
		// left
		if (x > 0) {
			this.changeNeighbours(x-1, y, change);
		}
		// bottom left
		if (x > 0 && y < this.height-1) {
			this.changeNeighbours(x-1, y+1, change);
		}
		// top
		if (y > 0) {
			this.changeNeighbours(x, y-1, change);
		}
		// bottom
		if (y < this.height-1) {
			this.changeNeighbours(x, y+1, change);
		}
		// top right
		if (x < this.width-1 && y > 0) {
			this.changeNeighbours(x+1, y-1, change);
		}
		// right
		if (x < this.width-1) {
			this.changeNeighbours(x+1, y, change);
		}
		// bottom right
		if (x < this.width-1 && y < this.height-1) {
			this.changeNeighbours(x+1, y+1, change);
		}
	}
}
