/*
 * Copyright 2014-2017 Gil Mendes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.continuum.rendering.particles;

import org.continuum.rendering.RenderableObject;
import org.continuum.utilities.FastRandom;
import org.lwjgl.util.vector.Vector3f;

import static org.lwjgl.opengl.GL11.*;

public abstract class Particle implements RenderableObject {
    protected ParticleEmitter _parent;

    protected final Vector3f _targetVelocity = new Vector3f(0.0f, -0.03f, 0.0f);
    protected final Vector3f _velDecSpeed = new Vector3f(0.003f, 0.003f, 0.003f);

    protected final Vector3f _position = new Vector3f();
    protected final Vector3f _velocity = new Vector3f();
    protected int _orientation = 0;

    protected static final FastRandom _rand = new FastRandom();

    protected int _lifetime;

    public Particle(int lifeTime, Vector3f position, ParticleEmitter parent) {
        _position.set(position);
        _velocity.set((float) _rand.randomDouble() / 16f, (float) _rand.randomDouble() / 16f, (float) _rand.randomDouble() / 16f);
        _lifetime = lifeTime;
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
        if (_lifetime > 0)
            _lifetime--;
    }

    public boolean isAlive() {
        return _lifetime > 0;
    }

    protected ParticleEmitter getParent() {
        return _parent;
    }
}
