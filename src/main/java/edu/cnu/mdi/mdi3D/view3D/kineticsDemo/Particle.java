package edu.cnu.mdi.mdi3D.view3D.kineticsDemo;

/**
 * Simple record to represent a particle's position in 3D space.
 * This is a UI-agnostic data structure used for rendering and physics calculations.
 *
 * @param x x coordinate
 * @param y y coordinate
 * @param z z coordinate
 */
public record Particle(float x, float y, float z) {

}
