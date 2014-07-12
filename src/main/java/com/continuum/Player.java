package com.continuum;

import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector3f;
import static org.lwjgl.opengl.GL11.*;

/**
 * Player class
 *
 * Created by gil0mendes on 12/07/14.
 */
public class Player extends RenderObject {
	// Viewing direction of the player
	private float yaw = 135.0f;
	private float pitch = 0.0f;

	// Walking speed
	private float walkingSpeed = 1.0f;

	// Accelaration
	private Vector3f acc = new Vector3f(0.0f, 0.0f, 0.0f);

	// Gravity
	private float gravity = 0.0f;

	// The parent world
	private World parent = null;

	/**
	 * Player constructor
	 *
	 * @param parent Parent world
	 */
	public Player(World parent) {
		this.parent = parent;
		this.position = new Vector3f(0f, 128f, 0f);
	}

	/**
	 * Positions the player within the world
	 * and adjusts the players view accordingly.
	 */
	@Override
	public void render() {
		// Rotate
		glRotatef(pitch, 1.0f, 0.0f, 0.0f);
		glRotatef(yaw, 0.0f, 1.0f, 0.0f);
		glTranslatef(-position.x, -position.y, -position.z);
	}

	@Override
	public void update(int delta) {
		Vector3f direction = new Vector3f();

		if (acc.x >= 0) {
			direction.x = 1;
		} else {
			direction.x = -1;
		}

		if (acc.z >= 0) {
			direction.z = 1;
		} else {
			direction.z = -1;
		}

		// Check if player is hitting something
		if (parent.isHitting(new Vector3f(getPosition().x + direction.x * 2, getPosition().y - 1.0f, getPosition().z + direction.z * 2))) {
			acc.x = 0.0f;
			acc.z = 0.0f;
		}

		if (!parent.isHitting(new Vector3f(getPosition().x, getPosition().y - 2.0f, getPosition().z))) {
			if (gravity > -1.0f) {
				//gravity -= 0.1f;
			}
		}

		// Update view direction
		yaw(Mouse.getDX() * 0.1f);
		pitch(-1 * Mouse.getDY() * 0.1f);

		getPosition().y += acc.y + (gravity * 0.1f * delta);
		getPosition().x += acc.x * 0.001f * delta;
		getPosition().z += acc.z * 0.001f * delta;

		// Check if player are in the ground
		if (parent.isHitting(new Vector3f(getPosition().x, getPosition().y - 2.0f, getPosition().z))) {
			gravity = 0.0f;
		}
	}

	public void yaw(float diff) {
		yaw += diff;
	}

	public void pitch(float diff) {
		pitch += diff;
	}

	public void walkForward() {
		this.acc.x -= walkingSpeed * (float) Math.sin(Math.toRadians(yaw));
		this.acc.z += walkingSpeed * (float) Math.cos(Math.toRadians(yaw));
	}

	public void walkBackwards() {
		this.acc.x += walkingSpeed * (float) Math.sin(Math.toRadians(yaw));
		this.acc.z -= walkingSpeed * (float) Math.cos(Math.toRadians(yaw));
	}

	public void strafeLeft() {
		this.acc.x -= walkingSpeed * (float)Math.sin(Math.toRadians(yaw - 90));
		this.acc.z += walkingSpeed * (float)Math.cos(Math.toRadians(yaw - 90));
	}

	public void strafeRight() {
		this.acc.x -= walkingSpeed * (float)Math.sin(Math.toRadians(yaw + 90));
		this.acc.z += walkingSpeed * (float)Math.cos(Math.toRadians(yaw + 90));
	}

	/**
	 * Jump
	 */
	public void jump() {
		// Check if its hitting something
		if (parent.isHitting(new Vector3f(getPosition().x, getPosition().y - 2.0f, getPosition().z))) {
			this.gravity += 2.0f;
		}
	}
}