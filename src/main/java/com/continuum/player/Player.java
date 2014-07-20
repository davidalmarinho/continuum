package com.continuum.player;

import java.util.Collections;

import com.continuum.Configuration;
import com.continuum.RenderableObject;
import com.continuum.datastructures.AABB;
import com.continuum.datastructures.BlockPosition;
import com.continuum.world.World;
import com.continuum.blocks.Block;
import com.continuum.utilities.*;
import javolution.util.FastList;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector3f;

import static org.lwjgl.opengl.GL11.*;

/**
 *  This class contains all functions regarding the player's actions,
 * movement and the orientation of the camera.
 */
public final class Player extends RenderableObject {

	private boolean _jump = false;
	private byte _selectedBlockType = 1;
	private double _wSpeed = Configuration.getSettingNumeric("WALKING_SPEED");
	private double _yaw = 135d;
	private double _pitch;
	private final Vector3f _movement = VectorPool.getVector(0, 0, 0);
	private final Vector3f _acc = VectorPool.getVector(0, 0, 0);
	private float _gravity = 0.0f;
	private World _parent = null;
	private final PerlinNoise _pGen = new PerlinNoise((int) Helper.getInstance().getTime());
	private final Vector3f _viewingDirection = VectorPool.getVector();
	private boolean _playerIsTouchingGround = false;

	/**
	 *
	 */
	public Player() {
		resetPlayer();
	}

	/**
	 * Positions the player within the world and adjusts the player's view accordingly.
	 */
	@Override
	public void render() {
		RayBoxIntersection is = calcSelectedBlock();

		// Display the block the player is aiming at
		if (Configuration.getSettingBoolean("PLACING_BOX")) {
			if (is != null) {

				if (Block.getBlockForType(_parent.getBlockAtPosition(is.getBlockPos())).renderBoundingBox()) {
					Block.AABBForBlockAt((int) is.getBlockPos().x, (int) is.getBlockPos().y, (int) is.getBlockPos().z).render();
				}
			}
		}
	}

	public void applyPlayerModelViewMatrix() {

		glMatrixMode(GL11.GL_MODELVIEW);
		glLoadIdentity();

		float bobbing1 = 0.0f;

		if (Configuration.getSettingBoolean("BOBBING") && !Configuration.getSettingBoolean("GOD_MODE") && _playerIsTouchingGround) {
			bobbing1 = (float) _pGen.noise(_position.x * 2f, _position.z * 2f, 0f) * 0.05f;
		}

		float newPosY = _position.y + getAABB().getDimensions().y / 1.2f + bobbing1;
		GLU.gluLookAt(_position.x, newPosY, _position.z, _position.x + _viewingDirection.x, newPosY + _viewingDirection.y, _position.z + _viewingDirection.z, 0, 1, 0);
	}

	public void applyNormalizedModelViewMatrix() {

		glMatrixMode(GL11.GL_MODELVIEW);
		glLoadIdentity();

		GLU.gluLookAt(0, 0, 0, _viewingDirection.x, _viewingDirection.y, _viewingDirection.z, 0, 1, 0);
	}

	/**
	 * Updates the player.
	 */
	@Override
	public void update() {
		float dx = Mouse.getDX();
		float dy = Mouse.getDY();

		yaw(dx  * Configuration.MOUSE_SENS);
		pitch(dy * Configuration.MOUSE_SENS);

		processMovement();
		updatePlayerPosition();

		// Update the viewing direction
		_viewingDirection.set((float) Math.sin(Math.toRadians(_yaw)) * (float) Math.cos(Math.toRadians(_pitch)), -1f * (float) Math.sin(Math.toRadians(_pitch)), -1 * (float) Math.cos(Math.toRadians(_pitch)) * (float) Math.cos(Math.toRadians(_yaw)));
		_viewingDirection.normalise();

		_movement.set(0, 0, 0);
	}

	/**
	 * Yaws the player's point of view.
	 *
	 * @param diff Amount of yawing to be applied.
	 */
	void yaw(float diff) {
		double nYaw = (_yaw + diff) % 360;
		if (nYaw < 0) {
			nYaw += 360;
		}
		_yaw = nYaw;
	}

	/**
	 * Pitches the player's point of view.
	 *
	 * @param diff Amount of pitching to be applied.
	 */
	void pitch(float diff) {
		double nPitch = (_pitch - diff) % 360;
		if (nPitch < 0) {
			nPitch += 360;
		}
		// Do not allow the player to "look on his back" :-)
		// TODO: Problematic if the mouse movement is very fast
		if ((nPitch > 0 && nPitch < 90) || (nPitch < 360 && nPitch > 270)) {
			_pitch = nPitch;
		}
	}

	/**
	 * Moves the player forward.
	 */
	void walkForward() {
		_movement.x += _wSpeed * Math.sin(Math.toRadians(_yaw)) * Math.cos(Math.toRadians(_pitch));

		if (Configuration.getSettingBoolean("GOD_MODE")) {
			_movement.y -= _wSpeed * Math.sin(Math.toRadians(_pitch));
		}

		_movement.z -= _wSpeed * Math.cos(Math.toRadians(_yaw)) * Math.cos(Math.toRadians(_pitch));
	}

	/**
	 * Moves the player backward.
	 */
	void walkBackwards() {
		_movement.x -= _wSpeed * Math.sin(Math.toRadians(_yaw)) * Math.cos(Math.toRadians(_pitch));

		if (Configuration.getSettingBoolean("GOD_MODE")) {
			_movement.y += _wSpeed * Math.sin(Math.toRadians(_pitch));
		}

		_movement.z += _wSpeed * Math.cos(Math.toRadians(_yaw)) * Math.cos(Math.toRadians(_pitch));
	}

	/**
	 * Lets the player strafe left.
	 */
	void strafeLeft() {
		_movement.x += _wSpeed * Math.sin(Math.toRadians(_yaw - 90));
		_movement.z -= _wSpeed * Math.cos(Math.toRadians(_yaw - 90));
	}

	/**
	 * Lets the player strafe right.
	 */
	void strafeRight() {
		_movement.x += _wSpeed * Math.sin(Math.toRadians(_yaw + 90));
		_movement.z -= _wSpeed * Math.cos(Math.toRadians(_yaw + 90));
	}

	/**
	 * Lets the player jump.
	 */
	void jump() {
		if (_playerIsTouchingGround) {
			_jump = true;
		}
	}

	/**
	 * Calculates the currently looked at block in front of the player.
	 *
	 * @return Intersection point of the looked at block
	 */
	RayBoxIntersection calcSelectedBlock() {
		FastList<RayBoxIntersection> inters = new FastList<RayBoxIntersection>();
		for (int x = -4; x < 4; x++) {
			for (int y = -4; y < 4; y++) {
				for (int z = -4; z < 4; z++) {
					if (x != 0 || y != 0 || z != 0) {
						// The ray originates from the "player's eye"
						FastList<RayBoxIntersection> iss = RayBoxIntersectionHelper.rayBlockIntersection(_parent, (int) _position.x + x, (int) _position.y + y, (int) _position.z + z, VectorPool.getVector(_position.x, _position.y + getAABB().getDimensions().y / 1.2f, _position.z), _viewingDirection);

						if (iss != null) {
							inters.addAll(iss);
						}
					}
				}
			}
		}

		/**
		 * Calculated the closest intersection.
		 */
		if (inters.size() > 0) {
			Collections.sort(inters);
			return inters.get(0);
		}

		return null;
	}

	/**
	 * @return
	 */
	public String selectedBlockInformation() {
		RayBoxIntersection r = calcSelectedBlock();
		Vector3f bp = r.getBlockPos();
		byte blockType = _parent.getBlock((int) bp.x, (int) bp.y, (int) bp.z);

		return String.format("%s (t: %d) ", r, blockType);
	}

	/**
	 * Places a block of a given type in front of the player.
	 *
	 * @param type The type of the block
	 */
	public void placeBlock(byte type) {
		if (getParent() != null) {
			RayBoxIntersection is = calcSelectedBlock();
			if (is != null) {
				Vector3f blockPos = is.calcAdjacentBlockPos();

				// Prevent players from placing blocks inside their bounding boxes
				if (Block.AABBForBlockAt((int) blockPos.x, (int) blockPos.y, (int) blockPos.z).overlaps(getAABB())) {
					return;

				}
				getParent().setBlock((int) blockPos.x, (int) blockPos.y, (int) blockPos.z, type, true, false);

			}
		}
	}

	/**
	 * Plants a tree of a given type in front of the player.
	 *
	 * @param type The type of the tree
	 */
	public void plantTree(int type) {
		RayBoxIntersection is = calcSelectedBlock();
		if (is != null) {
			Vector3f blockPos = is.getBlockPos();

			if (type == 0) {
				_parent.getGeneratorTree().generate((int) blockPos.x, (int) blockPos.y, (int) blockPos.z, true);
			} else {
				_parent.getGeneratorPineTree().generate((int) blockPos.x, (int) blockPos.y, (int) blockPos.z, true);
			}
		}
	}

	/**
	 * Removes a block.
	 */
	void removeBlock() {
		if (getParent() != null) {
			RayBoxIntersection is = calcSelectedBlock();
			if (is != null) {
				Vector3f blockPos = is.getBlockPos();
				getParent().setBlock((int) blockPos.x, (int) blockPos.y, (int) blockPos.z, (byte) 0x0, true, true);
			}
		}
	}

	/**
	 * Processes the keyboard input.
	 *
	 * @param key         Pressed key on the keyboard
	 * @param state       The state of the key
	 * @param repeatEvent True if repeat event
	 */
	public void processKeyboardInput(int key, boolean state, boolean repeatEvent) {
		switch (key) {
			case Keyboard.KEY_E:
				if (state && !repeatEvent) {
					placeBlock(_selectedBlockType);
				}
				break;
			case Keyboard.KEY_Q:
				if (state && !repeatEvent) {
					removeBlock();
				}
				break;
			case Keyboard.KEY_UP:
				if (!repeatEvent && state) {
					cycleBlockTypes(1);
				}
				break;
			case Keyboard.KEY_DOWN:
				if (!repeatEvent && state) {
					cycleBlockTypes(-1);
				}
				break;
			case Keyboard.KEY_SPACE:
				if (!repeatEvent && state) {
					jump();
				}
				break;
		}
	}

	/**
	 * Processes the mouse input.
	 *
	 * @param button Pressed mouse button
	 * @param state  State of the mouse button
	 */
	public void processMouseInput(int button, boolean state) {
		if (button == 0 && state) {
			placeBlock(_selectedBlockType);
		} else if (button == 1 && state) {
			removeBlock();
		}
	}

	/**
	 * Checks for pressed keys and executes the respective movement
	 * command.
	 */
	private void processMovement() {
		if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
			walkForward();
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
			walkBackwards();
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
			strafeLeft();
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
			strafeRight();
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
			_wSpeed = Configuration.getSettingNumeric("WALKING_SPEED") * Configuration.getSettingNumeric("RUNNING_FACTOR");
		} else {
			_wSpeed = Configuration.getSettingNumeric("WALKING_SPEED");
		}
	}

	/**
	 * Checks for blocks below and above the player.
	 *
	 * @param origin
	 * @return True if a vertical collision was detected
	 */
	private boolean verticalHitTest(Vector3f origin) {
		boolean result = false;
		FastList<BlockPosition> blockPositions = gatherAdjacentBlockPositions(origin);

		Vector3f dir = Vector3f.sub(origin, _position, null);

		if (dir.length() > 0) {
			dir.normalise();
		} else {
			return false;
		}

		for (FastList.Node<BlockPosition> n = blockPositions.head(), end = blockPositions.tail(); (n = n.getNext()) != end; ) {
			byte blockType1 = _parent.getBlockAtPosition(VectorPool.getVector(n.getValue().x, n.getValue().y, n.getValue().z));

			if (!Block.getBlockForType(blockType1).isPenetrable()) {
				while (getAABB().overlaps(Block.AABBForBlockAt(n.getValue().x, n.getValue().y, n.getValue().z))) {
					result = true;
					// If a collision was detected: reset the player's position
					_position.y += dir.y * 0.0001;
					_gravity = 0f;
				}
			}

		}
		return result;
	}

	/**
	 * @param origin
	 * @return
	 */
	private FastList<BlockPosition> gatherAdjacentBlockPositions(Vector3f origin) {
        /*
         * Gather the surrounding block positions
         * and order those by the distance to the originating point.
         */
		FastList<BlockPosition> blockPositions = new FastList<BlockPosition>();

		for (int x = -1; x < 2; x++) {
			for (int z = -1; z < 2; z++) {
				for (int y = -1; y < 2; y++) {
					int blockPosX = (int) (origin.x + x + 0.5f);
					int blockPosY = (int) (origin.y + y + 0.5f);
					int blockPosZ = (int) (origin.z + z + 0.5f);

					blockPositions.add(new BlockPosition(blockPosX, blockPosY, blockPosZ, origin));
				}
			}
		}

		// Sort the block positions
		Collections.sort(blockPositions);
		return blockPositions;
	}

	/**
	 * Checks for blocks around the player.
	 *
	 * @param origin
	 * @return
	 */
	private boolean horizontalHitTest(Vector3f origin) {
		boolean result = false;
		FastList<BlockPosition> blockPositions = gatherAdjacentBlockPositions(origin);

		// Check each block positions for collisions
		for (FastList.Node<BlockPosition> n = blockPositions.head(), end = blockPositions.tail(); (n = n.getNext()) != end; ) {
			byte blockType1 = _parent.getBlockAtPosition(VectorPool.getVector(n.getValue().x, n.getValue().y, n.getValue().z));

			if (!Block.getBlockForType(blockType1).isPenetrable()) {
				if (getAABB().overlaps(Block.AABBForBlockAt(n.getValue().x, n.getValue().y, n.getValue().z))) {
					result = true;
					Vector3f normal = Block.AABBForBlockAt(n.getValue().x, n.getValue().y, n.getValue().z).closestNormalToPoint(origin);
					// Find a vector parallel to the surface normal
					Vector3f slideVector = Vector3f.cross(normal, VectorPool.getVector(0, 1, 0), null);
					// Calculate the direction from the origin to the current position
					Vector3f direction = VectorPool.getVector(_position.x, 0f, _position.z);
					direction.x -= origin.x;
					direction.z -= origin.z;
					// Calculate the intensity of the diversion alongside the block
					float length = Vector3f.dot(slideVector, direction);
					_position.z = origin.z + length * slideVector.z;
					_position.x = origin.x + length * slideVector.x;

					VectorPool.putVector(normal);
					VectorPool.putVector(slideVector);
					VectorPool.putVector(direction);
				}
			}
		}
		return result;
	}

	/**
	 * Updates the position of the player.
	 * <p/>
	 * TODO: Fix easing-artifact
	 */
	private void updatePlayerPosition() {
		// Save the previous position before changing any of the values
		Vector3f oldPosition = VectorPool.getVector();
		oldPosition.set(_position);

		if (Configuration.getSettingBoolean("DEMO_FLIGHT") && Configuration.getSettingBoolean("GOD_MODE")) {
			_position.z += 0.1;
			return;
		}

        /*
         * Slowdown the speed of the player each time this method is called.
         */
		if (Math.abs(_acc.y) > 0f) {
			_acc.y += -1f * _acc.y * Configuration.getSettingNumeric("FRICTION");
		}

		if (Math.abs(_acc.x) > 0f) {
			_acc.x += -1f * _acc.x * Configuration.getSettingNumeric("FRICTION");
		}

		if (Math.abs(_acc.z) > 0f) {
			_acc.z += -1f * _acc.z * Configuration.getSettingNumeric("FRICTION");
		}

		if (Math.abs(_acc.x) > _wSpeed || Math.abs(_acc.z) > _wSpeed || Math.abs(_acc.z) > _wSpeed) {
			double max = Math.max(Math.max(Math.abs(_acc.x), Math.abs(_acc.z)), _acc.y);
			double div = max / _wSpeed;

			_acc.x /= div;
			_acc.z /= div;
			_acc.y /= div;
		}


        /*
         * Increase the speed of the player by adding the movement
         * vector to the acceleration vector.
         */
		_acc.x += _movement.x;
		_acc.y += _movement.y;
		_acc.z += _movement.z;

		if (_gravity > -Configuration.getSettingNumeric("MAX_GRAVITY") && !Configuration.getSettingBoolean("GOD_MODE")) {
			_gravity -= Configuration.getSettingNumeric("GRAVITY");

			if (_gravity < -Configuration.getSettingNumeric("MAX_GRAVITY")) {
				_gravity = -Configuration.getSettingNumeric("MAX_GRAVITY");
			}
		}

		getPosition().y += _acc.y;
		getPosition().y += _gravity;

		if (!Configuration.getSettingBoolean("GOD_MODE")) {
			boolean vHit = verticalHitTest(oldPosition);
			if (vHit) {
				// Jumping is only possible, if the player is standing on ground
				if (_jump) {
					_jump = false;
					_gravity = Configuration.getSettingNumeric("JUMP_INTENSITY");
				}

				_playerIsTouchingGround = true;
			} else {

				_playerIsTouchingGround = false;
			}
		} else {
			_gravity = 0f;
		}

		oldPosition.set(_position);

        /*
         * Update the position of the player
         * according to the acceleration vector.
         */
		getPosition().x += _acc.x;
		getPosition().z += _acc.z;

        /*
         * Check for horizontal collisions __after__ checking for vertical
         * collisions.
         */
		if (!Configuration.getSettingBoolean("GOD_MODE")) {
			if (horizontalHitTest(oldPosition)) {
				// Do something while the player is colliding
			}
		}

		VectorPool.putVector(oldPosition);
	}

	/**
	 * Resets the player's attributes.
	 */
	public void resetPlayer() {
		_acc.set(0, 0, 0);
		_movement.set(0, 0, 0);
		_gravity = 0.0f;
	}

	/**
	 * Returns the parent world.
	 *
	 * @return The parent world
	 */
	World getParent() {
		return _parent;
	}

	/**
	 * Sets the parent world an resets the player.
	 *
	 * @param parent The parent world
	 */
	public void setParent(World parent) {
		this._parent = parent;
	}

	/**
	 * @return
	 */
	public Vector3f getViewDirection() {
		return _viewingDirection;
	}

	/**
	 * Cycles the selected block type.
	 *
	 * @param upDown Cycling direction
	 */
	void cycleBlockTypes(int upDown) {
		_selectedBlockType += upDown;

		if (_selectedBlockType >= Block.getBlockCount()) {
			_selectedBlockType = 0;
		} else if (_selectedBlockType < 0) {
			_selectedBlockType = (byte) (Block.getBlockCount() - 1);
		}
	}

	/**
	 * Returns some information about the player as a string.
	 *
	 * @return
	 */
	@Override
	public String toString() {
		return String.format("player (x: %.2f, y: %.2f, z: %.2f | x: %.2f, y: %.2f, z: %.2f | b: %d | gravity: %.2f | x: %.2f, y: %.2f, z:, %.2f)", _position.x, _position.y, _position.z, _viewingDirection.x, _viewingDirection.y, _viewingDirection.z, _selectedBlockType, _gravity, _movement.x, _movement.y, _movement.z);
	}

	/**
	 * Returns player's AABB.
	 *
	 * @return
	 */
	AABB getAABB() {
		return new AABB(_position, VectorPool.getVector(.3f, 0.7f, .3f));
	}

	/**
	 * @return
	 */
	public double getYaw() {
		return _yaw;
	}

	/**
	 * @return
	 */
	public double getPitch() {
		return _pitch;
	}
}