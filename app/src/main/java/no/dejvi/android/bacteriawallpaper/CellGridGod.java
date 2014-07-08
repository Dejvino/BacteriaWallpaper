package no.dejvi.android.bacteriawallpaper;

public class CellGridGod {

	private int gridStep;
	private int gridSteps = 3;
	
	private CellGrid grid;
	private CellGrid gridOld;
	
	public CellGridGod(CellGrid grid, CellGrid gridOld) {
		this.grid = grid;
		this.gridOld = gridOld;
	}
	
	public void performStep() {
		// pregenerovani
        for (int i = this.grid.getWidth()*this.grid.getHeight()-this.gridStep-1;
        	i >= 0; i -= this.gridSteps) {
        	int n = this.gridOld.countNeighbours(i);
        	if (this.gridOld.isAlive(i)) {
        		if (n < 2) {
        			// underpopulated
        			this.grid.kill(i);
        		} else if (n <= 3) {
        			// OK
        			this.grid.revive(i);
        		} else {
        			// overpopulated
        			this.grid.kill(i);
        		}
        	} else {
        		if (n == 3) {
        			// born
        			this.grid.revive(i);
        		} else {
        			this.grid.kill(i);
        		}
        	}
        }
        // posun kroku
        this.gridStep = (this.gridStep + 1) % this.gridSteps;
        // prekopirovani gridu
        if (this.gridStep == 0) {
            this.gridOld.importGrid(this.grid);
        }
	}
}
