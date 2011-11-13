/*
 * Copyright (C) 2011 Joakim Andersson
 * 
 * This file is part of Remote-100, an Android application to 
 * control the Sony Ericsson CAR-100 accessory.
 * 
 * Remote-100 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * Remote-100 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Parts of this file were inspired by CarConnection.java, obtained from
 * the BluetoothCar example Midlet found in the Sony Ericsson SDK for the
 * Java ME platform, version 2.5.0.6.
 * 
 * 		/Joakim Andersson, 2011-11-11
 */

package nu.firetech.android.remote100.backend;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothSocket;

public class Car100 {
	// =========================================
	// Private Constants
	// =========================================

	// The input which says that the car is charging.
	private final String CAR_CHARGING		= "Charging";
	// The input which says that the car is in driving mode.
	private final String CAR_SPEED			= "Speed=2";
	// The input which says that the car is fully charged.
	private final String CAR_CHARGE_FULL	= "Ready";

	// The response to the car's output.
	private final String OK = "OK";

	// Car's commands.
	private final String FORWARD_PRESSED	= "+CKEV: u,1";
	private final String BACK_PRESSED		= "+CKEV: d,1";
	private final String SPEED3_PRESSED		= "+CKEV: #,1";
	private final String SPEED2_PRESSED		= "+CKEV: 0,1";
	private final String SPEED1_PRESSED		= "+CKEV: *,1";
	private final String STOP_PRESSED		= "+CKEV: :J,1";
	private final String LEFT_PRESSED		= "+CKEV: 2,1";
	private final String LEFT_RELEASED		= "+CKEV: 2,0";
	private final String RIGHT_PRESSED		= "+CKEV: 3,1";
	private final String RIGHT_RELEASED		= "+CKEV: 3,0";
	private final String NO_PRESSED			= "+CKEV: e,1";
	private final String NO_RELEASED		= "+CKEV: e,0";

	// Carriage return and line feed.
	private final byte[] CRLF = {13, 10};


	// =========================================
	// Public Constants
	// =========================================

	// Possible car statuses.
	public static final byte STATUS_NO_LINK		= 0;
	public static final byte STATUS_CONNECTED	= 1;
	public static final byte STATUS_CHARGING	= 2;
	public static final byte STATUS_CHARGE_FULL	= 3;
	public static final byte STATUS_READY		= 4;

	// Possible car accelerations.
	public static final byte ACC_REVERSE	= -1;
	public static final byte ACC_STOP		= 0;
	public static final byte ACC_FORWARD	= 1;

	// Possible car directions.
	public static final byte DIR_LEFT		= -1;
	public static final byte DIR_STRAIGHT	= 0;
	public static final byte DIR_RIGHT		= 1;


	// =========================================
	// Private Members
	// =========================================

	// The current instance of this class.
	private static Car100 instance = null;

	// The BluetoothSocket.
	private BluetoothSocket mSocket;

	// The current status of the car.
	private byte mCurrStatus = STATUS_NO_LINK;

	// The last time (System.currentTimeMillis()) we got or sent data to the car.
	private long mLastData;

	// The current gear in the car (1, 2 or 3).
	private byte mCurrGear;

	// The current acceleration of the car (ACC_REVERSE, ACC_STOP or ACC_FORWARD).
	private byte mCurrAcc;

	// The current direction of the car (DIR_LEFT, DIR_STRAIGHT or DIR_RIGHT).
	private byte mCurrDir;


	// =========================================
	// Public Methods
	// =========================================

	/**
	 * Get car status.
	 * 
	 * Will check for any new input and return the latest status update
	 * available. Run this method often. ;)
	 * 
	 * The return value will be one of:
	 * * STATUS_NO_LINK - Default status, will be set on I/O errors.
	 * * STATUS_CHARGING - The car is charging, and can't be controlled.
	 * * STATUS_CHARGE_FULL - The car is fully charged, but still docked to a charger (and can't be controlled).
	 * * STATUS_READY - The car is ready for your commands, master.
	 * 
	 * @return The car's current status, expressed as mentioned above.
	 */
	public static byte getStatus() throws IOException {
		return (instance != null ? instance.mGetStatus() : STATUS_NO_LINK);
	}

	/**
	 * Set the gear of the car.
	 * 
	 * Gear should be in the range [1, 3].
	 * 
	 * @param gear Desired car gear, in the range [1, 3]
	 * @return true if setting was successful, false otherwise.
	 */
	public static boolean setGear(byte gear) {
		if (instance == null) {
			return false;
		}

		try {
			instance.mSetGear(gear);
		} catch (IOException e) {
			instance.mCurrStatus = STATUS_NO_LINK;
			return false;
		}

		return true;
	}

	/**
	 * Set the acceleration of the car.
	 * 
	 * Use ACC_REVERSE, ACC_STOP or ACC_FORWARD as input.
	 * 
	 * @param acc One of ACC_REVERSE, ACC_STOP or ACC_FORWARD.
	 * @return true if setting was successful, false otherwise.
	 */
	public static boolean setAcc(byte acc) {
		if (instance == null) {
			return false;
		}

		try {
			instance.mSetAcc(acc);
		} catch (IOException e) {
			instance.mCurrStatus = STATUS_NO_LINK;
			return false;
		}

		return true;
	}

	/**
	 * Set the direction (steering) of the car.
	 * 
	 * Use DIR_LEFT, DIR_STRAIGHT or DIR_RIGHT as input.
	 * 
	 * @param dir One of DIR_LEFT, DIR_STRAIGHT or DIR_RIGHT.
	 * @return true if setting was successful, false otherwise.
	 */
	public static boolean setDir(byte dir) {
		if (instance == null) {
			return false;
		}

		try {
			instance.mSetDir(dir);
		} catch (IOException e) {
			instance.mCurrStatus = STATUS_NO_LINK;
			return false;
		}

		return true;
	}

	/**
	 * Closes the connection to the car.
	 * 
	 * First tells the car that we want to quit by pressing the NO button,
	 * then closes the socket.
	 * 
	 * @return true if closing was successful, false otherwise.
	 */
	public static boolean close() {
		if (instance == null) {
			return false;
		}

		try {
			instance.mClose();
		} catch (IOException e) {
			instance.mCurrStatus = STATUS_NO_LINK;
			return false;
		}

		instance = null;

		return true;
	}

	// =========================================
	// Helper Methods
	// =========================================

	private Car100(BluetoothSocket socket) throws IOException {
		this.mSocket = socket;
		this.mCurrStatus = STATUS_CONNECTED;
		this.mLastData = System.currentTimeMillis();
		mReset();
		mGetStatus();
	}

	/* Connect to car. */
	/* package */ static void connect(BluetoothSocket socket) throws IOException {
		close(); //Close any previous instance.
		try {
			socket.connect();
			instance = new Car100(socket);
		} catch (IOException e) {
			instance = null;
			throw e;
		}
	}

	/* Reset parameters. */
	private void mReset() {
		mCurrDir = DIR_STRAIGHT;
		mCurrGear = 2;
		mCurrAcc = ACC_STOP;
	}

	/* Check if car has sent any new status and return current or new status. */
	private byte mGetStatus() throws IOException {
		if (mCurrStatus == STATUS_NO_LINK) {
			return STATUS_NO_LINK;
		}

		byte oldStatus = mCurrStatus;

		try {
			InputStream is = mSocket.getInputStream();
			int availableBytes = is.available();
			if (availableBytes <= 0 && (System.currentTimeMillis() - mLastData) > 10000) {
				// Check if the stream still is alive.
				mSend(null);
			}
			while (availableBytes > 0) {
				byte[] byteBuffer = new byte[availableBytes];
				int readBytes = is.read(byteBuffer);
				mLastData = System.currentTimeMillis();
				if (readBytes == -1) {
					throw new IOException("EOF reached");
				}
				String str = new String(byteBuffer);

				// Set the car status.
				if (str.indexOf(CAR_SPEED) != -1) {
					mCurrStatus = STATUS_READY;
				} else if (str.indexOf(CAR_CHARGING) != -1) {
					mCurrStatus = STATUS_CHARGING;
				} else if (str.indexOf(CAR_CHARGE_FULL) != -1) {
					mCurrStatus = STATUS_CHARGE_FULL;
				}

				// Answer with OK.
				mSend(OK);

				// Any more input?
				availableBytes = is.available();
			}
		} catch (IOException e) {
			mCurrStatus = STATUS_NO_LINK;
			throw e;
		}

		if (oldStatus != mCurrStatus) {
			mReset();
		}

		return mCurrStatus;
	}

	/* Set the gear of the car. */
	private void mSetGear(byte gear) throws IOException {
		if (mCurrGear != gear) {
			switch (gear) {
			case 1:
				mSend(SPEED1_PRESSED);
				break;
			case 2:
				mSend(SPEED2_PRESSED);
				break;
			case 3:
				mSend(SPEED3_PRESSED);
				break;
			default:
				throw new IllegalArgumentException("Unknown gear: " + gear);
			}
			mCurrGear = gear;
		}
	}

	/* Set the acceleration of the car. */
	private void mSetAcc(byte acc) throws IOException {
		if (mCurrAcc != acc) {
			switch (acc) {
			case ACC_STOP:
				mSend(STOP_PRESSED);
				break;
			case ACC_FORWARD:
				mSend(FORWARD_PRESSED);
				break;
			case ACC_REVERSE:
				mSend(BACK_PRESSED);
				break;
			default:
				throw new IllegalArgumentException("Unknown acceleration: " + acc);
			}
			mCurrAcc = acc;
		}
	}

	/* Set the direction (steering) of the car. */
	private void mSetDir(byte dir) throws IOException {
		if (mCurrDir != dir) {
			// Release old direction (if necessary)
			switch (mCurrDir) {
			case DIR_LEFT:
				mSend(LEFT_RELEASED);
				break;
			case DIR_RIGHT:
				mSend(RIGHT_RELEASED);
				break;
			}
			// Press new direction (if necessary)
			switch (dir) {
			case DIR_LEFT:
				mSend(LEFT_PRESSED);
				break;
			case DIR_RIGHT:
				mSend(RIGHT_PRESSED);
				break;
			case DIR_STRAIGHT:
				break;
			default:
				throw new IllegalArgumentException("Unknown direction: " + dir);
			}
			mCurrDir = dir;
		}
	}

	/* Tell the car we've stopped playing and close the socket. */
	private void mClose() throws IOException {
		mSend(NO_PRESSED);
		mSend(NO_RELEASED);
		mCurrStatus = STATUS_NO_LINK;
		mSocket.close();
	}

	/* Write and flush the specified string to the socket. */
	private synchronized void mSend(String data) throws IOException {
		OutputStream os = mSocket.getOutputStream();
		os.write(CRLF);
		if (data != null) {
			os.write(data.getBytes());
			os.write(CRLF);
		}
		mLastData = System.currentTimeMillis();
	}
}
