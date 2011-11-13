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

package nu.firetech.android.remote100.backend;

import java.io.IOException;
import java.util.UUID;

import nu.firetech.android.remote100.Remote100Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

public class CarServer extends Thread {
	private static final String LOG_TAG = "[Remote100] CarServer";
	private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
	
	private boolean run = true;
	
	private Remote100Activity activity;
	private BluetoothAdapter adapter;
	private String carAddress;
	
	public CarServer(Remote100Activity act, BluetoothAdapter adapter, String carAddress) {
		this.activity = act;
		this.adapter = adapter;
		this.carAddress = carAddress;
		this.start();
	}
	
	public void run() {
		while (run) {
			try {
				setStatus(Car100.STATUS_NO_LINK);
				
				Log.i(LOG_TAG, "Connecting to car...");
				BluetoothDevice car = adapter.getRemoteDevice(carAddress);
				Car100.connect(car.createRfcommSocketToServiceRecord(SPP_UUID));
				
				byte status = Car100.getStatus();
				if (status != Car100.STATUS_NO_LINK) {
					Log.i(LOG_TAG, "Connected.");
				}
				
				// Wait for current status. (After connection we get a burst of input).
				while (status == Car100.STATUS_CONNECTED && run) {
					simpleSleep(100);
					status = Car100.getStatus();
					setStatus(status);
				}
				
				// Keep checking and updating status
				while (status != Car100.STATUS_NO_LINK && run) {
					simpleSleep(1000);
					status = Car100.getStatus();
					setStatus(status);
				}
				
				if (run) {
					Log.i(LOG_TAG, "Connection lost.");
				}
				
			} catch (IOException e) {
				setStatus(Car100.STATUS_NO_LINK);
				Log.i(LOG_TAG, "Connection lost.", e);
			}
			if (run) {
				simpleSleep(2500);
			}
		}
	}
	
	private void setStatus(final byte status) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				activity.setStatus(status);
			}
		});
	}
	
	private void simpleSleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e1) {}
	}
	
	public void close() {
		run = false;
		Car100.close();
		Log.i(LOG_TAG, "Stopped.");
	}
}
