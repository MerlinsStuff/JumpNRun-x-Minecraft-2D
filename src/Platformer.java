import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Platformer extends JFrame {
	public static final String BasePath = "./assets/";
	@Serial
	private static final long serialVersionUID = 5736902251450559962L;

	private Player p = null;
	private Level l = null;
	private boolean isFullScreen = false;
	BufferStrategy bufferStrategy;

	Timer gameStateUpdateTrigger;

	public Platformer() {
		//exit program when window is closed
		this.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e){
					System.exit(0);
			}
		});

		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new File("./"));
		fc.setDialogTitle("Select input image");
		FileFilter filter = new FileNameExtensionFilter("Level image (.bmp)","bmp");
		fc.setFileFilter(filter);
		int result = fc.showOpenDialog(this);
		File selectedFile = new File("");
		addKeyListener(new AL(this));
		this.setVisible(true);
		createBufferStrategy(2);
		bufferStrategy = this.getBufferStrategy();

		if (result == JFileChooser.APPROVE_OPTION) {
			selectedFile = fc.getSelectedFile();
		} else { dispose(); System.exit(0); }

		if (result == JFileChooser.APPROVE_OPTION) {
			selectedFile = fc.getSelectedFile();
			System.out.println("Selected file: " + selectedFile.getAbsolutePath());
		} else {
			dispose();
			System.exit(0);
		}

		try {
			l = new Level(selectedFile.getAbsolutePath(), BasePath + "background0.png");
			p = new Player(l);
			l.player = p;

			this.setBounds(0, 0, 1000, 12 * 70);
			
			gameStateUpdateTrigger = new Timer();
			gameStateUpdateTrigger.scheduleAtFixedRate(new TimerTask() {

				@Override
				public void run() {
					updateGameStateAndRepaint();
				}

			}, 0, 10);
			//playSound(BasePath + "Sound/soundtrack.wav");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void restart() throws IOException {
		p.pos.x = 0;
		p.pos.y = 0;
		p.posLastFrame.x = 0;
		p.posLastFrame.y = 0;
		p.lastValidPosition.x = 0;
		p.lastValidPosition.y = 0;
		p.numberOfLifes = 3;
		p.points = 0;
		p.currentState = Player.PlayerState.IDLE;
		p.displayedAnimationState = 0;
		p.updateBoundingBox();
		p.brokenTileCount = 0;

		l.offsetX = 0;
		l.creepers.clear();
		l.initLevel();
			
		}

	private void updateGameStateAndRepaint() {
		l.update();
		p.update();
		for(Creeper c : new ArrayList<>(l.creepers)) c.update(p);
		checkCollision();
		repaint();
	}

	private void checkCollision() {
		// Reset player collision flags
		p.collidesDown = false;
		p.collidesLeft = false;
		p.collidesRight = false;
		p.collidesTop = false;
		p.collides = false;

		// ===== Tile Collisions =====
		for (Tile tile : new ArrayList<>(l.tiles)) {
			Vec2 overlapSize = tile.bb.OverlapSize(p.boundingBox);
			if (overlapSize.x > 0 && overlapSize.y > 0) {
				if (tile.hasRigidCollision) {
					boolean resolveY = overlapSize.y < overlapSize.x; // resolve smallest penetration first
					if (resolveY) {
						float centerP = (p.boundingBox.min.y + p.boundingBox.max.y) * 0.5f;
						float centerT = (tile.bb.min.y + tile.bb.max.y) * 0.5f;
						if (centerP > centerT) { // from below
							p.pos.y += overlapSize.y;
							p.posLastFrame.y = p.pos.y;
							p.collidesTop = true;
						} else { // from above
							p.pos.y -= overlapSize.y;
							p.posLastFrame.y = p.pos.y;
							p.collidesDown = true;
						}
					} else {
						float centerP = (p.boundingBox.min.x + p.boundingBox.max.x) * 0.5f;
						float centerT = (tile.bb.min.x + tile.bb.max.x) * 0.5f;
						if (centerP > centerT) { // from right
							p.pos.x += overlapSize.x;
							p.posLastFrame.x = p.pos.x;
							p.collidesLeft = true;
						} else { // from left
							p.pos.x -= overlapSize.x;
							p.posLastFrame.x = p.pos.x;
							p.collidesRight = true;
						}
					}
				}
				p.collides = true;
				tile.onCollision(p);
				p.updateBoundingBox();
				if (p.numberOfLifes == 0) {
					try { gameOver(); } catch (IOException e) { e.printStackTrace(); }
				}
			}
		}

		// ===== Creeper Collisions =====
		for (Creeper c : new ArrayList<>(l.creepers)) {
			Vec2 overlap = c.bb.OverlapSize(p.boundingBox);
			if (overlap.x > 0 && overlap.y > 0) {
				// Player attacking creeper
				BoundingBox attackBox = p.getAttackBox(); // let player define attack box
				int damage = p.getAttackDamage();

				if (attackBox != null && c.bb.intersect(attackBox)) {
					boolean fromLeft = p.pos.x < c.pos.x;
					c.hurt(p, damage, fromLeft);
				}
			}
		}
	}


	private void gameOver() throws IOException {
		restart();
	}


	@Override
	public void paint(Graphics g) {
		Graphics2D g2 = null;

		try {
			g2 = (Graphics2D) bufferStrategy.getDrawGraphics();
			draw(g2);

		} finally {
			g2.dispose();
		}
		bufferStrategy.show();
	}

	private void draw(Graphics2D g2d) {
		if (l == null || l.getResultingImage() == null) return;
		BufferedImage level = (BufferedImage) l.getResultingImage();
		if (l.offsetX > level.getWidth() - 1000)
			l.offsetX = level.getWidth() - 1000;
			int width = Math.min(1000, level.getWidth() - (int)l.offsetX);
			BufferedImage bi = level.getSubimage((int) l.offsetX, 0, width, level.getHeight());
		g2d.drawImage(l.backgroundImage, 0, 0, this);
		g2d.drawImage(bi, 0, 0, this);

		for (int i = 0; i< l.tiles.size(); i++) {
			l.tiles.get(i).draw(g2d,l.offsetX,0);
		}
		for(Creeper c : l.creepers) c.draw(g2d, l.offsetX, 0);

		if (p.brokenTileCount > 0) {

			try {
				g2d.drawImage(ImageIO.read(new File("./assets/Tiles/grassMid/GrassMidBreaking0.png")), 50, 50, 40, 40, null);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // small icon in top-left
			g2d.setColor(Color.WHITE);
			g2d.setFont(new Font("Arial", Font.BOLD, 20));
			g2d.drawString(String.valueOf(p.brokenTileCount), 80, 85);
	}
		g2d.drawImage(getPlayer().getPlayerImage(), (int) (getPlayer().pos.x-l.offsetX), (int) getPlayer().pos.y, this);

		
		g2d.drawString(new String(p.points + ""), 500, 50);
	}

	public Player getPlayer() {
		return this.p;
	}

	public Level getLevel() {
		return this.l;
	}

	public void setFullScreenMode(boolean b) {
		this.isFullScreen = b;
	}

	public boolean getFullScreenMode() {
		return this.isFullScreen;
	}

	public class AL extends KeyAdapter {
		Platformer p;

		public AL(Platformer p) {
			super();
			this.p = p;
		}

		@Override
		public void keyPressed(KeyEvent event) {
			int keyCode = event.getKeyCode();
			Player player = p.getPlayer();

			switch (keyCode) {
				case KeyEvent.VK_ESCAPE -> p.dispose();

				case KeyEvent.VK_LEFT -> player.walkingLeft = true;
				case KeyEvent.VK_RIGHT -> player.walkingRight = true;

				case KeyEvent.VK_SPACE -> player.jump = true;

				case KeyEvent.VK_W -> player.facingDirection = Player.FacingDirection.UP;
        		case KeyEvent.VK_S -> player.facingDirection = Player.FacingDirection.DOWN;

				case KeyEvent.VK_D -> player.placeTile(l);


				case KeyEvent.VK_R -> {
					try { p.restart(); } 
					catch (IOException e) { e.printStackTrace(); }
				}

				case KeyEvent.VK_F -> player.startPunch();
				case KeyEvent.VK_E -> player.startSwordAttack();
				case KeyEvent.VK_P -> player.changeSoundEnabled();
			}
		}

		@Override
		public void keyReleased(KeyEvent event) {
			int keyCode = event.getKeyCode();
			Player player = p.getPlayer();

			switch (keyCode) {
				case KeyEvent.VK_LEFT -> player.walkingLeft = false;
				case KeyEvent.VK_RIGHT -> player.walkingRight = false;

				case KeyEvent.VK_SPACE -> player.jump = false;

				case KeyEvent.VK_W, KeyEvent.VK_S -> {
					// When W/S is released, reset facing direction back to horizontal
					if (player.walkingLeft) player.facingDirection = Player.FacingDirection.LEFT;
					else if (player.walkingRight) player.facingDirection = Player.FacingDirection.RIGHT;
					else player.facingDirection = Player.FacingDirection.RIGHT; // default
				}

				case KeyEvent.VK_F, KeyEvent.VK_E -> {
					// Attacke losgelassen → zurück zu WALK oder IDLE
					if (player.walkingLeft || player.walkingRight) {
						player.currentState = Player.PlayerState.WALK;
					} else {
						player.currentState = Player.PlayerState.IDLE;
					}
					player.displayedAnimationState = 0; // Animation zurücksetzen
				}
			}
		}
	}


/* 
	boolean soundEnabled = false;

	public void playSound(String path) {
		if (!soundEnabled) return;  // Sound ausgeschaltet

		try {
			File soundFile = new File(path);
			Clip clip = AudioSystem.getClip();
			clip.open(AudioSystem.getAudioInputStream(soundFile));
			clip.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setSoundEnabled(boolean enabled) {
		soundEnabled = enabled;
	}
		*/
}