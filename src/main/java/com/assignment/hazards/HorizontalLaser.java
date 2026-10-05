package com.assignment.hazards;

import com.assignment.objects.Player;
import com.assignment.objects.pickups.Shield;

import java.util.List;

// A horizontal laser fired from one wall; a FRONT shield between player and emitter blocks it.
public class HorizontalLaser extends Laser {
    private final boolean sourceOnRight;   // which wall the beam fires from

    public HorizontalLaser(double x, double y, double width, double height, boolean sourceOnRight) {
        super(x, y, width, height);
        this.sourceOnRight = sourceOnRight;
    }

    // emitter sits on the source side
    @Override
    protected boolean emitterOnRight() {
        return sourceOnRight;
    }

    // Protected when a FRONT shield sits between the player and the emitter.
    @Override
    protected boolean isProtected(Player p, List<Shield> shields) {
        for (Shield s : shields) {
            if (s.getOrientation() != Shield.Orientation.FRONT) continue;
            boolean verticallyOverlapsPlayer =
                    s.getY() + s.getHeight() > p.getY()
                 && s.getY() < p.getY() + p.getHeight();
            if (!verticallyOverlapsPlayer) continue;

            if (sourceOnRight) {
                if (s.getX() >= p.getX() + p.getWidth()) return true;
            } else {
                if (s.getX() + s.getWidth() <= p.getX()) return true;
            }
        }
        return false;
    }

    // Beam runs from the source side and stops at a FRONT shield in its path.
    @Override
    protected double[] visibleBeam() {
        double left = x;
        double right = x + width;
        for (Shield s : shields) {
            if (s.getOrientation() != Shield.Orientation.FRONT) continue;
            boolean overlapsY = s.getY() + s.getHeight() > y && s.getY() < y + height;
            if (!overlapsY) continue;
            if (sourceOnRight) {
                double sRight = s.getX() + s.getWidth();
                if (sRight > left && sRight < right) left = sRight; // hide left of shield
            } else {
                double sLeft = s.getX();
                if (sLeft < right && sLeft > left) right = sLeft;   // hide right of shield
            }
        }
        return new double[]{left, y, right - left, height};
    }
}
