package com.valunskii.gameshooter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*;

public class GamePanel extends JPanel implements Runnable, KeyListener {

    // FIELDS
    public static int WIDTH = 800;
    public static int HEIGHT = 800;

    private Thread thread;
    private boolean running;

    private BufferedImage image; 	//our canvas
    private Graphics2D g; 			//our paintbrush
    private int FPS = 30;
    private double averageFPS;

    public static Player player;
    public static ArrayList<Bullet> bullets;
    public static ArrayList<Enemy> enemies;
    public static ArrayList<PowerUp> powerups;
    public static ArrayList<Explosion> explosions;
    public static ArrayList<Text> texts;

    private long waveStartTimer;
    private long waveStartTimerDiff;
    private int waveNumber;
    private int finalWave = 8;
    private boolean waveStart;
    private int waveDelay = 2000;

    private long slowDownTimer;
    private long slowDownTimerDiff;
    private long slowDownLength = 6000;

    // CONSTRUCTOR
    public GamePanel() {
        super();
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        requestFocus();
    }

    // METHODS
    public void addNotify() {
        super.addNotify();
        if(thread == null) {
            thread = new Thread(this);
            thread.start();
        }
        addKeyListener(this);
    }


    public void run() {

        running = true;

        image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        g = (Graphics2D) image.getGraphics();

        // Сглаживание граффики:
        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        // Сглаживание текста:
        g.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        player = new Player();
        bullets = new ArrayList<Bullet>();
        enemies = new ArrayList<Enemy>();
        powerups = new ArrayList<PowerUp>();
        explosions = new ArrayList<Explosion>();
        texts = new ArrayList<Text>();

        waveStartTimer = 0;
        waveStartTimerDiff = 0;
        waveStart = true;
        waveNumber = 0;

        long startTime;
        long URDTimeMillis;
        long waitTime;
        long totalTime = 0;

        int frameCount = 0;
        int maxFrameCount = 30;

        long targetTime = 1000 / FPS;

        // GAME LOOP
        while(running) {

            startTime = System.nanoTime(); //current time in nanosec.

            gameUpdate();
            gameRender();
            gameDraw();

            URDTimeMillis = (System.nanoTime() - startTime) / 1000000;

            waitTime = targetTime - URDTimeMillis;

            try {
                Thread.sleep(waitTime);
            }
            catch(Exception e) {
            }

            totalTime += System.nanoTime() - startTime;
            frameCount++;
            if(frameCount == maxFrameCount) {
                averageFPS = 1000.0 / ((totalTime / frameCount) / 1000000);
                frameCount = 0;
                totalTime = 0;
            }
        }

        g.setColor(new Color(0, 100, 255));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Century Gothic", Font.PLAIN, WIDTH / 20));
        String s = "G  A  M  E    O  V  E  R";
        int length = (int) g.getFontMetrics().getStringBounds(s, g).getWidth();
        g.drawString(s, (WIDTH - length) / 2, HEIGHT / 2);
        s = "Final Score: " + player.getScore();
        length = (int) g.getFontMetrics().getStringBounds(s, g).getWidth();
        g.drawString(s, (WIDTH - length) / 2, HEIGHT / 2 + 40);
        gameDraw();

    }

    // All Game Logic
    private void gameUpdate() {

        // new wave
        if(waveStartTimer == 0 && enemies.size() == 0) {
            waveNumber++;
            waveStart = false;
            waveStartTimer = System.nanoTime();
        }
        else {
            waveStartTimerDiff = (System.nanoTime() - waveStartTimer) / 1000000;
            if(waveStartTimerDiff > waveDelay) {
                waveStart = true;
                waveStartTimer = 0;
                waveStartTimerDiff = 0;
            }
        }

        // create enemies
        if(waveStart && enemies.size() == 0) {
            createNewEnemies();
        }
        // player update
        player.update();

        // bullet update
        for(int i = 0; i < bullets.size(); i++) {
            boolean remove = bullets.get(i).update();
            if(remove) {
                bullets.remove(i);
                i--;
            }
        }

        // enemy update
        for(int i = 0; i < enemies.size(); i++) {
            enemies.get(i).update();
        }

        // powerup update
        for(int i = 0; i < powerups.size(); i++) {
            boolean remove = powerups.get(i).update();
            if(remove) {
                powerups.remove(i);
                i--;
            }
        }

        //explosion update
        for(int i = 0; i < explosions.size(); i++) {
            boolean remove = explosions.get(i).update();
            if(remove) {
                explosions.remove(i);
                i--;
            }
        }

        // text update
        for(int i = 0; i < texts.size(); i++) {
            boolean remove = texts.get(i).update();
            if(remove) {
                texts.remove(i);
                i--;
            }
        }

        // bullet-enemy collision
        for(int i = 0; i < bullets.size(); i++) {

            Bullet b = bullets.get(i);
            double bx = b.getx();
            double by = b.gety();
            double br = b.getr();

            for(int j = 0; j < enemies.size(); j++) {
                Enemy e = enemies.get(j);
                double ex = e.getx();
                double ey = e.gety();
                double er = e.getr();

                double dx = bx - ex;
                double dy = by - ey;
                double dist = Math.sqrt(dx * dx + dy * dy);

                if(dist < br + er) {
                    e.hit();
                    bullets.remove(i);
                    i--;
                    break;
                }
            }
        }

        // check dead enemies
        for(int i = 0; i < enemies.size(); i++) {

            if(enemies.get(i).isDead()) {

                Enemy e = enemies.get(i);

                // chance for powerup
                double rand = Math.random();
                if(rand < 0.001) { powerups.add(new PowerUp(1, e.getx(), e.gety())); }
                else if(rand < 0.020) { powerups.add(new PowerUp(3, e.getx(), e.gety())); }
                else if(rand < 0.120){ powerups.add(new PowerUp(2, e.getx(), e.gety())); }
                else if(rand < 0.130){ powerups.add(new PowerUp(2, e.getx(), e.gety())); }

                player.addScore(e.getType() + e.getRank());
                enemies.remove(i);
                i--;

                e.explode();
                explosions.add(new Explosion(e.getx(), e.gety(), e.getr(), e.getr() + 30));
            }
        }

        // check dead player
        if(player.isDead()) {
            running = false;
        }

        // check player-enemy collision
        if(!player.isRecovering()) {
            int px = player.getx();
            int py = player.gety();
            int pr = player.getr();
            for(int i = 0; i < enemies.size(); i++) {
                Enemy e = enemies.get(i);
                double ex = e.getx();
                double ey = e.gety();
                double er = e.getr();

                double dx = px - ex;
                double dy = py - ey;
                double dist = Math.sqrt(dx * dx + dy * dy);

                if(dist < pr + er) {
                    player.loseLife();
                }
            }
        }

        // player-powerup collision
        int px = player.getx();
        int py = player.gety();
        int pr = player.getr();
        for(int i = 0; i < powerups.size(); i++) {
            PowerUp p = powerups.get(i);
            double x = p.getx();
            double y = p.gety();
            double r = p.getr();
            double dx = px - x;
            double dy = py - y;
            double dist = Math.sqrt(dx * dx + dy * dy);

            //collected powerup
            if(dist < pr + r) {

                int type = p.getType();

                if(type == 1) {
                    player.ganeLife();
                    texts.add(new Text(player.getx(), player.gety(), 2000, "Extra Life")); // 2000 - 2 сек. текст на экране.
                }
                if(type == 2) {
                    player.increasePower(1);
                    texts.add(new Text(player.getx(), player.gety(), 2000, "Power"));
                }
                if(type == 3) {
                    player.increasePower(2);
                    texts.add(new Text(player.getx(), player.gety(), 2000, "Double Power"));
                }
                if(type == 4) {
                    slowDownTimer = System.nanoTime();
                    for(int j = 0; j < enemies.size(); j++) {
                        enemies.get(j).setSlow(true);
                    }
                    texts.add(new Text(player.getx(), player.gety(), 2000, "Slow Down"));
                }

                powerups.remove(i);
                i--;
            }
        }

        // slowdown update
        if(slowDownTimer != 0) {
            slowDownTimerDiff = (System.nanoTime() - slowDownTimer) / 1000000;
            if(slowDownTimerDiff > slowDownLength) {
                slowDownTimer = 0;
                for(int j = 0; j < enemies.size(); j++) {
                    enemies.get(j).setSlow(false);
                }
            }
        }

    }

    //Screen Image: player, enemies, background ... everything
    private void gameRender() {

        // draw background
        g.setColor(new Color(0, 100, 255)); //background color
        g.fillRect(0,0, WIDTH, HEIGHT);

        // draw slowdown screen
        if(slowDownTimer != 0)  {
            g.setColor(new Color(255, 255, 255, 64));
            g.fillRect(0, 0, WIDTH, HEIGHT);
        }

        // FPS count
        //g.setColor(Color.BLACK);
        //g.drawString("FPS: " + (int) averageFPS, 10, 10);

        // draw player
        player.draw(g);

        // draw bullet
        for(int i = 0; i < bullets.size(); i++) {
            bullets.get(i).draw(g);
        }

        // draw enemy
        for(int i = 0; i < enemies.size(); i++) {
            enemies.get(i).draw(g);
        }

        // draw powerups
        for(int i = 0; i < powerups.size(); i++) {
            powerups.get(i).draw(g);
        }

        // draw explosions
        for(int i = 0; i < explosions.size(); i++) {
            explosions.get(i).draw(g);
        }

        // draw text
        for(int i = 0; i < texts.size(); i++) {
            texts.get(i).draw(g);
        }

        //draw wave number
        if(waveStartTimer != 0) {
            g.setFont(new Font("Century Gothic", Font.PLAIN, HEIGHT / 20));

            String s;
            if(waveNumber < finalWave) {
                s = "- W A V E  " + waveNumber + "   -";
            }
            else {
                s = "- F I N A L   W A V E -";
            }
            int length = (int) g.getFontMetrics().getStringBounds(s, g).getWidth();
            int alpha = (int) (255 * Math.sin(3.14 * waveStartTimerDiff / waveDelay));
            if(alpha > 255) alpha = 255;
            g.setColor(new Color(255, 255, 255, alpha));
            g.drawString(s, WIDTH / 2 - length / 2, HEIGHT /2);
        }

        // draw player lives
        for(int i = 0; i < player.getLives(); i++) {
            g.setColor(Color.WHITE);
            g.fillOval(20 +(20 * i), 20, player.getr(), player.getr());
            g.setStroke(new BasicStroke(3));
            g.setColor(Color.WHITE.darker());
            g.drawOval(20 +(20 * i), 20, player.getr(), player.getr());
            g.setStroke(new BasicStroke(1));
        }

        // draw player power
        g.setColor(Color.YELLOW);
        g.fillRect(20, 40, player.getPower() * 8, 8);
        g.setColor(Color.YELLOW.darker());
        g.setStroke(new BasicStroke(2));
        for(int i = 0; i < player.getRequiredPower(); i++) {
            g.drawRect(20 + 8 * i, 40, 8, 8);
        }
        g.setStroke(new BasicStroke(2));

        // draw player score
        g.setColor(Color.WHITE);
        g.setFont(new Font("Century Gothic", Font.PLAIN, 14));
        g.drawString("Score: " + player.getScore(),WIDTH - 100, 30);

        // draw slowdown meter
        if(slowDownTimer != 0) {
            g.setColor(Color.WHITE);
            g.drawRect(20, 60, 100, 8);
            g.fillRect(20, 60,
                    (int) (100 - 100.0 * slowDownTimerDiff / slowDownLength), 8);
        }

    }

    private void gameDraw() {
        Graphics g2 = this.getGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
    }

    private void createNewEnemies() {
        enemies.clear();
        Enemy e;

        if(waveNumber == 1) {
            for(int i = 0; i < 4; i++) {
                enemies.add(new Enemy(1, 1));
            }
        }
        if(waveNumber == 2) {
            for(int i = 0; i < 8; i++) {
                enemies.add(new Enemy(1, 1));
            }
        }
        if(waveNumber == 3) {
            enemies.add(new Enemy(1, 2));
            enemies.add(new Enemy(1, 2));
            for(int i = 0; i < 8; i++) {
                enemies.add(new Enemy(1, 1));
            }
        }
        if(waveNumber == 4) {
            enemies.add(new Enemy(1, 3));
            enemies.add(new Enemy(1, 4));
            for(int i = 0; i < 4; i++) {
                enemies.add(new Enemy(2, 1));
            }
        }
        if(waveNumber == 5) {
            enemies.add(new Enemy(1, 3));
            enemies.add(new Enemy(1, 4));
            enemies.add(new Enemy(2, 3));
            for(int i = 0; i < 4; i++) {
                enemies.add(new Enemy(3, 1));
            }
        }
        if(waveNumber == 6) {
            enemies.add(new Enemy(1, 3));
            for(int i = 0; i < 4; i++) {
                enemies.add(new Enemy(3, 1));
                enemies.add(new Enemy(2, 1));
                enemies.add(new Enemy(1, 1));
            }
        }
        if(waveNumber == 7) {
            enemies.add(new Enemy(3, 3));
            enemies.add(new Enemy(2, 3));
            enemies.add(new Enemy(1, 3));
        }
        if(waveNumber == 8) {
            enemies.add(new Enemy(3, 4));
            enemies.add(new Enemy(2, 4));
            enemies.add(new Enemy(1, 4));
        }
        if(waveNumber == 9) {
            running = false;
        }
    }

    public void keyTyped(KeyEvent key) {} //не нужен,но описан в интерфейсе.

    public void keyPressed(KeyEvent key) {
        int keyCode = key.getKeyCode();
        if(keyCode == KeyEvent.VK_LEFT) {
            player.setLeft(true);
        }
        if(keyCode == KeyEvent.VK_RIGHT) {
            player.setRight(true);
        }
        if(keyCode == KeyEvent.VK_UP) {
            player.setUp(true);
        }
        if(keyCode == KeyEvent.VK_DOWN) {
            player.setDown(true);
        }
        if(keyCode == KeyEvent.VK_Z) {
            player.setFiring(true);
        }
    }
    public void keyReleased (KeyEvent key) {
        int keyCode = key.getKeyCode();
        if(keyCode == KeyEvent.VK_LEFT) {
            player.setLeft(false);
        }
        if(keyCode == KeyEvent.VK_RIGHT) {
            player.setRight(false);
        }
        if(keyCode == KeyEvent.VK_UP) {
            player.setUp(false);
        }
        if(keyCode == KeyEvent.VK_DOWN) {
            player.setDown(false);
        }
        if(keyCode == KeyEvent.VK_Z) {
            player.setFiring (false);
        }
    }
}
