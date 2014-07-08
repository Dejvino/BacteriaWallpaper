package no.dejvi.android.bacteriawallpaper;

import java.util.Random;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.widget.Toast;

public class BacteriaWallpaper extends WallpaperService {

	public static final String SHARED_PREFS_NAME="bacteriawallpapersettings";
	
    private final Handler mHandler = new Handler();

    private Context mContext;
    
    private Resources resources;
    
    @Override
    public void onCreate() {
        super.onCreate();
        WallpaperManager.getInstance(this).setWallpaperOffsetSteps(1, 1);
        this.resources = this.getResources();
        this.mContext = this.getApplicationContext();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public Engine onCreateEngine() {
        return new CubeEngine();
    }

    class CubeEngine extends Engine
    	implements SharedPreferences.OnSharedPreferenceChangeListener {

    	private int width = 10;
    	private int height = 10;
    	
    	private long timeLast;
    	private long timeSpeed = 1000;
    	private double timeColorSpeed = 100;
    	
    	private boolean gridSticks = true;
    	
    	private CellGrid grid;
    	private CellGrid gridOld;
    	private CellGrid[] grids;
    	private CellGrid[] gridsOld;
    	private CellGridGod[] gridGods;
    	private int gridStep;
    	private int gridSteps = 13;
    	private int gridScreens = 1;
    	private int gridOffset;
    	
    	private int gridScale = 5;
    	
    	private int cellColor = 0x0055ff55;
    	private double cellGlow;
    	private double cellGlowRange = 0.3;
    	private double cellGlowMin = 0.7;
    	private boolean cellGlowing = true;
    	private boolean cellColoring = true;
    	private boolean cell3D = false;
    	private boolean cellBlur = true;
    	
    	private int drawFps = 20;
    	
    	private int touchDirt = 2000;
    	private double touchSize = 40;
    	
    	private Random rand = new Random();
    	
        private final Paint mPaint = new Paint();
        private Bitmap mBmp;
        private Bitmap mBackground;
        private float mOffset;
        private float mTouchX = -1;
        private float mTouchY = -1;
        private long mStartTime;
        private float mCenterX;
        private float mCenterY;

        private final Runnable mDrawGrids = new Runnable() {
            public void run() {
                drawFrame();
            }
        };
        private boolean mVisible;

        private SharedPreferences mPrefs;

        CubeEngine() {
            // Create a Paint to draw the lines for our cube
            final Paint paint = mPaint;
            paint.setColor(0xff000000 + this.cellColor);
            paint.setAntiAlias(true);
            paint.setStrokeWidth(0);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStyle(Paint.Style.STROKE);

            mStartTime = SystemClock.elapsedRealtime();
            
            mPrefs = BacteriaWallpaper.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
            mPrefs.registerOnSharedPreferenceChangeListener(this);
            onSharedPreferenceChanged(mPrefs, null);
        }

        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

        	// GRID
            this.gridSticks = prefs.getBoolean("grid_sticks", false);
            this.gridScale = prefs.getInt("grid_scale", 4);
            this.gridScreens = prefs.getInt("grid_screens", 7);
            if (this.gridScale < 2) {
            	this.gridScale = 2;
            }
            if (this.gridScreens <= 0) {
            	this.gridScreens = 1;
            }
            if (this.gridScreens > 10) {
            	this.gridScreens = 10;
            }
            
            // SIM
            this.gridSteps = prefs.getInt("simulation_steps", 13);
            this.timeSpeed = prefs.getInt("simulation_speed", 1000);
            this.drawFps = prefs.getInt("simulation_fps", 15);
            this.touchSize = prefs.getInt("simulation_touch_size", 40);
            this.touchDirt = prefs.getInt("simulation_touch_dirt", 2000);
            if (this.gridSteps <= 0) {
            	this.gridSteps = 1;
            }
            if (this.timeSpeed <= 0) {
            	this.timeSpeed = 1;
            }
            if (this.drawFps <= 5) {
            	this.drawFps = 5;
            }
            if (this.touchSize < 4) {
            	this.touchSize = 4;
            }
            if (this.touchDirt < 10) {
            	this.touchDirt = 10;
            }
            if (this.touchDirt / this.gridScale <= 1) {
            	this.touchDirt = 5 * this.gridScale;
            }
            
            // CELL COLOR
            this.cell3D = prefs.getBoolean("cell_3d", true);
            this.cellBlur = prefs.getBoolean("cell_blur", true);
            this.cellColoring = prefs.getBoolean("cell_coloring", true);
            this.timeColorSpeed = prefs.getInt("cell_coloring_speed", 100);
            this.cellColor = prefs.getInt("cell_color_r", 0x55) * 0x00010000
            				+ prefs.getInt("cell_color_g", 0xff) * 0x00000100
            				+ prefs.getInt("cell_color_b", 0x55);
            if (this.timeColorSpeed <= 1) {
            	this.timeColorSpeed = 1;
            }
            
            // CELL GLOW
            this.cellGlowing = prefs.getBoolean("cell_glowing", true);
            this.cellGlowMin = (double)prefs.getInt("cell_glowing_min", 70) / 100.0;
            this.cellGlowRange = (double)prefs.getInt("cell_glowing_range", 30) / 100.0;
            if (this.cellGlowMin + this.cellGlowRange > 1.0) {
            	this.cellGlowRange = 1.0 - this.cellGlowMin;
            }
            if (this.cellGlowMin + this.cellGlowRange < 0.4) {
            	this.cellGlowMin = 0.4;
            }
            if (this.cellGlowRange <= 0.0) {
            	this.cellGlowing = false;
            }
            
            this.generateGrid();
        }
        
        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            
            // By default we don't get touch events, so enable them.
            setTouchEventsEnabled(true);
            
            this.generateGrid();
            
            this.mBackground = BitmapFactory.decodeResource(
            		resources, R.drawable.background);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mHandler.removeCallbacks(mDrawGrids);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            mVisible = visible;
            if (visible) {
                drawFrame();
            } else {
                mHandler.removeCallbacks(mDrawGrids);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            // pretvoreni gridu
            this.width = width;
            this.height = height;
            
            try {
            	this.generateGrid();
            	
            	this.mBmp = Bitmap.createBitmap(this.grid.getWidth(), this.grid.getHeight(), Bitmap.Config.ARGB_4444);
            } catch (OutOfMemoryError e) {
            	this.dieOnMemoryLimit();
            }
            // store the center of the surface, so we can draw the cube in the right spot
            mCenterX = width/2.0f;
            mCenterY = height/2.0f;
            drawFrame();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            mHandler.removeCallbacks(mDrawGrids);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                float xStep, float yStep, int xPixels, int yPixels) {
            mOffset = xOffset;
            if (this.gridSticks) {
            	this.gridOffset = 0;
            } else {
            	this.gridOffset = (int)(xOffset / (1.0 / (this.gridScreens+1)) * this.width)  - this.width;
            }
            drawFrame();
        }

        /*
         * Store the position of the touch event so we can use it for drawing later
         */
        @Override
        public void onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
            	// uchovani stare pozice
            	int oldX = (int)mTouchX;
            	int oldY = (int)mTouchY; 
            	// nacteni pozice
                mTouchX = event.getX()/this.gridScale;
                mTouchY = event.getY()/this.gridScale;
                // pokud jde o prvni dotek
                if (oldX < 0 || oldY < 0) {
                	oldX = (int)mTouchX;
                	oldY = (int)mTouchY;
                }
                // vygenerovani bakterii
                for (int i = 0; i < rand.nextInt(this.touchDirt/this.gridScale); i++) {
                	double diam = this.touchSize/this.gridScale;
                	double angle = rand.nextDouble() * 2.0f*Math.PI;
                	double dist = rand.nextDouble() * diam;
                	int x = (int)mTouchX + (int)(Math.cos(angle)*dist)
                			+ (int)(rand.nextFloat() * (oldX-mTouchX));
                	int y = (int)mTouchY + (int)(Math.sin(angle)*dist)
                			+ (int)(rand.nextFloat() * (oldY-mTouchY));
                	if (x < 0 || x >= this.grid.getWidth()) {
                		continue;
                	}
                	if (y < 0 || y >= this.grid.getHeight()) {
                		continue;
                	}
                	int g = 0;
                	int gX = x;
                	if (!this.gridSticks) {
                		g = (this.gridOffset+(x*this.gridScale)) / this.width;
                		gX = ((this.gridOffset - g*this.width)/this.gridScale) + x;
                		if (g < 0 || g >= this.gridScreens || gX < 0 || gX > this.grids[g].getWidth()) {
                    		continue;
                    	}
                	}
                	this.grids[g].revive(gX, y);
                	this.gridsOld[g].revive(gX, y);
                }
                /**/
            } else {
                mTouchX = -1;
                mTouchY = -1;
            }
            super.onTouchEvent(event);
        }

        void drawFrame() {
            final SurfaceHolder holder = getSurfaceHolder();

            // zmena casu
            long timeNow = SystemClock.elapsedRealtime();
            long timeDiff = timeNow - this.timeLast;
            this.timeLast = timeNow;
            
            if (this.cellGlowing) {
            	this.cellGlow = (1.0 + Math.sin(timeNow*(1.0/this.timeSpeed))) * this.cellGlowRange
            		+ this.cellGlowMin;
            //Log.d("DJV", "Glow: " + this.cellGlow);
            }
            if (this.cellGlow < this.cellGlowMin) {
            	this.cellGlow = this.cellGlowMin;
            }
            if (this.cellGlow > 1) {
            	this.cellGlow = 1.0;
            }
            
            if (this.cellColoring) {
            	double time = this.timeSpeed*this.timeColorSpeed;
            	int colR = (int)(((1.0 + Math.sin(timeNow * (1.0/(time/7.0))))/2.0) * 0xff);
            	int colG = (int)(((1.0 + Math.sin(timeNow * (1.0/(time/13.0))))/2.0) * 0xff);
            	int colB = (int)(((1.0 + Math.sin(timeNow * (1.0/(time/17.0))))/2.0) * 0xff);
            	this.cellColor = colR * 0x00010000
            				+ colG * 0x00000100
            				+ colB;
            }
            
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
            		// clear
                	c.drawColor(0xff000000);
                	
                	if (this.gridOffset <= -this.width) {
                		this.gridOffset = 0;
                	}
                	
                	// gridy
                    for (int g = 0; g < this.gridScreens; g++) {
                		// preskoceni vzdalenych ploch
                		int xPos = (g * this.width) - this.gridOffset;
	                    if ((this.gridSticks && g > 0) || Math.abs(xPos) > this.width) {
	                    	continue;
	                    }
	                    // pozadi
	                    c.save();
	                    c.translate(xPos, 0);
	                    c.drawBitmap(mBackground, 0, 0, mPaint);
	                    c.restore();
	                    // provedeni vykresleni a simulace
	                    c.save();
	                    c.translate(xPos, 0);
	                    c.scale(this.gridScale, this.gridScale);
	                    //c.drawColor(0xff000000);
	                    mBmp.eraseColor(0x00000000);
	                    for (int i = this.grids[g].getWidth()*this.grids[g].getHeight()-1; i >= 0 ; i--) {
	                    	if (this.grids[g].isAlive(i)) {
	                    		//c.drawPoint(i % this.grid.getWidth(), i / this.grid.getWidth(), mPaint);
	                    		int alpha;
	                    		if (this.cellBlur) {
	                    			alpha = 0xee;
	                    		} else {
	                    			alpha = 0xff;
	                    		}
	                    		if (this.cell3D) {
	                    			alpha = rand.nextInt((int)(alpha*this.cellGlow));
	                    		} else {
	                    			alpha = (int)(alpha*this.cellGlow); 
	                    		}
	                    		mBmp.setPixel(i % this.grids[g].getWidth(),
	                    				i / this.grids[g].getWidth(),
	                    				0x01000000*alpha + this.cellColor);
	                    	}
	                    }
	                    c.drawBitmap(mBmp, 0, 0, mPaint);
	                    if (this.cellBlur) {
		                    c.drawBitmap(mBmp, 1, 1, mPaint);
		                    c.drawBitmap(mBmp, 0, 1, mPaint);
		                    c.drawBitmap(mBmp, 1, 0, mPaint);
	                    }
	                    c.restore();
	                    //drawTouchPoint(c);
                    	gridGods[g].performStep();
                	}
                    /**/
                } else {
                	Log.d("DJV", "Canvas locking failed");
                }
            } finally {
                if (c != null) holder.unlockCanvasAndPost(c);
            }

            // Reschedule the next redraw
            mHandler.removeCallbacks(mDrawGrids);
            if (mVisible) {
                mHandler.postDelayed(mDrawGrids, 1000 / this.drawFps);
            }
            
        }

        /*
         * Draw a circle around the current touch point, if any.
         */
        void drawTouchPoint(Canvas c) {
            if (mTouchX >=0 && mTouchY >= 0) {
                c.drawCircle(mTouchX, mTouchY, 80, mPaint);
            }
        }

        private void generateGrid() {
        	// free
        	this.grids = null;
        	this.gridsOld = null;
        	this.gridGods = null;
        	this.grid = null;
            this.gridOld = null;
            System.gc();
        	
        	// fixup
        	if (this.gridSticks) {
            	this.gridScreens = 1;
            }
        	
        	// create
        	this.grids = new CellGrid[this.gridScreens];
            this.gridsOld = new CellGrid[this.gridScreens];
            this.gridGods = new CellGridGod[this.gridScreens];
            
            Log.d("DJV", "Create " + this.gridScreens + " grids " + width + "x" + height);
            try {
	            for (int i = 0; i < this.gridScreens; i++) {
	            	Log.d("DJV", "...creating grid " + i);
	            	this.grids[i] = new CellGrid(width/this.gridScale, height/this.gridScale);
	            	this.gridsOld[i] = new CellGrid(width/this.gridScale, height/this.gridScale);
	            	this.gridGods[i] = new CellGridGod(this.grids[i], this.gridsOld[i]);
	            }
	            this.grid = this.grids[0];
	            this.gridOld = this.gridsOld[0];
            } catch (OutOfMemoryError e) {
            	// free
        		this.grids = null;
            	this.gridsOld = null;
            	this.gridGods = null;
            	this.grid = null;
                this.gridOld = null;
                try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
            	System.gc();
            	if (!this.gridSticks) {
            		// try to create a simpler wallpaper
            		this.gridSticks = true;
            		this.generateGrid();
            	} else {
            		// DIE
            		this.dieOnMemoryLimit();
            	}
            }
        }
        
        private void dieOnMemoryLimit() {
        	Toast.makeText(mContext, "Not enough memory", Toast.LENGTH_SHORT).show();
    		System.exit(1);
        }
    }
}
