package com.continuum.rendering.particles;

import com.continuum.rendering.RenderableObject;
import com.continuum.utilities.FastRandom;
import org.lwjgl.util.vector.Vector3f;

import static org.lwjgl.opengl.GL11.*;

public abstract class Particle implements RenderableObject {
	protected ParticleEmitter _parent;

	protected final Vector3f _targetVelocity = new Vector3f(0.0f, -0.04f, 0.0f);
	protected final Vector3f _velDecSpeed = new Vector3f(0.002f, 0.002f, 0.002f);

	protected final Vector3f _position = new Vector3f();
	protected final Vector3f _velocity = new Vector3f();
	protected int _orientation = 0;

	protected static final FastRandom _rand = new FastRandom();

	protected int _lifeTime;

	public Particle(int lifeTime, Vector3f position, ParticleEmitter parent) {
		_position.set(position);
		_velocity.set((float) _rand.randomDouble() / 15f, (float) _rand.randomDouble() / 15f, (float) _rand.randomDouble() / 15f);
		_lifeTime = lifeTime;
		_orientation = _rand.randomInt() % 360;
		_parent = parent;
	}

	protected abstract void renderParticle();

	public void render() {
		if (isAlive()) {
			glPushMatrix();
			glTranslatef(_position.x, _position.y, _position.z);
			glRotatef(_orientation, 0, 1, 0);
			renderParticle();
			glPopMatrix();
		}
	}

	public void update() {
		updateVelocity();
		updatePosition();
		decLifetime();
	}

	protected void updateVelocity() {
		if (_velocity.x > _targetVelocity.x)
			_velocity.x -= _velDecSpeed.x;
		if (_velocity.x < _targetVelocity.x)
			_velocity.x += _velDecSpeed.x;
		if (_velocity.y > _targetVelocity.y)
			_velocity.y -= _velDecSpeed.y;
		if (_velocity.y < _targetVelocity.y)
			_velocity.y += _velDecSpeed.y;
		if (_velocity.z > _targetVelocity.z)
			_velocity.z -= _velDecSpeed.z;
		if (_velocity.z < _targetVelocity.z)
			_velocity.z += _velDecSpeed.z;
	}

	protected boolean canMove() {
		return true;
	}

	protected void updatePosition() {
		if (!canMove())
			return;

		Vector3f.add(_position, _velocity, _position);
	}

	protected void decLifetime() {
		if (_lifeTime > 0)
			_lifeTime--;
	}

	public boolean isAlive() {
		return _lifeTime > 0;
	}

	protected ParticleEmitter getParent() {
		return _parent;
	}
}