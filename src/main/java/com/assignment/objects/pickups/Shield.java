package com.assignment.objects.pickups;

// Base for the two shields; while carried it sticks to its holder each frame.
public abstract class Shield extends Pickup {
    public enum Orientation { TOP, FRONT }

    protected Shield(double x, double y, double w, double h) {
        super(x, y, w, h);
    }

    // which beam type this shield blocks
    public abstract Orientation getOrientation();

    // While carried, follow the holder.
    @Override
    public void update(double deltaTime) {
        if (collected && carrier != null) {
            attachToCarrier();
        }
    }

    // Position the shield on its carrier.
    protected abstract void attachToCarrier();
}
