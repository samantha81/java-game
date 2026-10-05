package com.assignment.hazards;

import com.assignment.objects.Player;
import com.assignment.objects.pickups.Shield;

import java.util.List;

// A vertical (falling) laser; a TOP shield held above the player blocks it.
public class VerticalLaser extends Laser {
    public VerticalLaser(double x, double y, double height) {
        super(x, y, 10, height);
    }

    // Protected when a TOP shield sits above the player, across the beam.
    @Override
    protected boolean isProtected(Player p, List<Shield> shields) {
        for (Shield s : shields) {
            if (s.getOrientation() != Shield.Orientation.TOP) continue;
            boolean horizontallyAbovePlayer =
                    s.getX() + s.getWidth() > p.getX()
                 && s.getX() < p.getX() + p.getWidth();
            boolean above = s.getY() + s.getHeight() <= p.getY();
            if (horizontallyAbovePlayer && above) return true;
        }
        return false;
    }

    // Beam stops at the top of any TOP shield held across it.
    @Override
    protected double[] visibleBeam() {
        double bottom = y + height;
        for (Shield s : shields) {
            if (s.getOrientation() != Shield.Orientation.TOP) continue;
            boolean overlapsX = s.getX() + s.getWidth() > x && s.getX() < x + width;
            if (overlapsX && s.getY() > y && s.getY() < bottom) {
                bottom = s.getY();
            }
        }
        return new double[]{x, y, width, bottom - y};
    }
}
