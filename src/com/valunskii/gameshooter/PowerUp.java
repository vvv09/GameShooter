package com.valunskii.gameshooter;

import java.awt.*;

public class PowerUp {

    // FIELDS
    private double x;
    private double y;
    private int r;

    private int type;
    private Color color1;

    // 1 -- +1 life
    // 2 -- +1 power
    // 3 -- +2 power
    // 4 -- slow down time

    // CONSTRUCTOR
    public PowerUp(int type, double x, double y) {
        this.type = type;
        this.x = x;
        this.y = y;

        if(type == 1) {
            color1 = Color.PINK;
            r = 6;

        }
        if(type == 2) {
            color1 = Color.YELLOW;
            r = 8;
        }
        if(type == 3) {
            color1 = Color.YELLOW;
            r = 12;
        }
        if(type == 4) {
            color1 = Color.WHITE;
            r = 10;
        }
    }

    // METHODS
    public double getx() { return x; }
    public double gety() { return y; }
    public double getr() { return r; }

    public int getType() { return type;}

    public boolean update(){

        y += 2;

        if(y > GamePanel.HEIGHT + r) {
            return true;
        }

        return false;

    }

    public void draw(Graphics2D g) {

        g.setColor(color1);
        g.fillRect((int) (x - r), (int) (y - r), 2 * r, 2 * r);

        g.setStroke(new BasicStroke(3));
        g.setColor(color1.darker());
        g.drawRect((int) (x - r), (int) (y - r), 2 * r, 2 * r);
        g.setStroke(new BasicStroke(1));

    }

}