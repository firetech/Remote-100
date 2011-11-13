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

package nu.firetech.android.remote100;

import java.util.Set;

import nu.firetech.android.joystick.OnJoystickMovedListener;
import nu.firetech.android.joystick.SplitJoystickView;
import nu.firetech.android.remote100.R;
import nu.firetech.android.remote100.backend.Car100;
import nu.firetech.android.remote100.backend.CarServer;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class Remote100Activity extends Activity {
	private static final int ABOUT_ID = Menu.FIRST;
	private static final int SETTINGS_ID = Menu.FIRST + 1;
	
	private static final String CAR_NAME = "CAR 100";
	
	private int currGear = 1;
	private byte lastStatus = -1;
	private AlertDialog bluetoothAlert = null;
	
	private AlertDialog aboutDialog;
	private static String aboutMessage = null;
	
	private SharedPreferences prefs;
	
	private CarServer server;
	
	// =========================================
	// Activity State Handling
	// =========================================
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        
        SeekBar gearBar = (SeekBar)findViewById(R.id.gearBar);
        GearChangeListener gearChangeListener = new GearChangeListener();
        gearBar.setOnSeekBarChangeListener(gearChangeListener);
        
        SplitJoystickView joy = (SplitJoystickView)findViewById(R.id.joystick);
        JoystickListener joyListener = new JoystickListener();
        joy.setOnJostickMovedListener(joyListener);
		
		if (aboutMessage == null) {
			String spacer = "\n\n";
			
			String versionName;
			try {
				versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			} catch (NameNotFoundException e) {
				versionName = "(unknown version)";
			}
			
			aboutMessage = new StringBuilder(getString(R.string.app_name))
			.append(" - ")
			.append(versionName)
			.append(spacer)
			.append("==================\n")
			.append("The Remote-100 App\n")
			.append("==================\n")
			.append("Copyright (c) 2011 Joakim Andersson")
			.append(spacer)
			.append("This program comes with ABSOLUTELY NO WARRANTY.\nThis " +
					"is free software, licensed under the GNU General Public" +
					" License; version 2.")
			.append(spacer)
			.append("====================\n")
			.append("The Joystick Library\n")
			.append("====================\n")
			.append("Copyright (c) 2011, olberg(at)gmail(dot)com, " +
					"http://mobile-anarchy-widgets.googlecode.com\n")
			.append("Copyright (c) 2011, Joakim Andersson\n")
			.append("All rights reserved.\n")
			.append("\n")
			.append("Redistribution and use in source and binary forms, with" +
					" or without modification, are permitted provided that " +
					"the following conditions are met:\n")
			.append("* Redistributions of source code must retain the above " +
					"copyright notice, this list of conditions and the " +
					"following disclaimer.\n")
			.append("* Redistributions in binary form must reproduce the " +
					"above copyright notice, this list of conditions and the" +
					" following disclaimer in the documentation and/or other" +
					" materials provided with the distribution.\n")
			.append("\n")
			.append("THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND " +
					"CONTRIBUTORS \"AS IS\" AND ANY EXPRESS OR IMPLIED " +
					"WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED " +
					"WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A " +
					"PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL " +
					"THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY " +
					"DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR " +
					"CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, " +
					"PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF " +
					"USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) " +
					"HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER " +
					"IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING " +
					"NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE " +
					"USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE " +
					"POSSIBILITY OF SUCH DAMAGE.")
			/* phew... */
			.toString();
		}
		
		aboutDialog = new AlertDialog.Builder(this)
		.setTitle(R.string.menu_about)
		.setMessage(aboutMessage)
		.setIcon(android.R.drawable.ic_dialog_info)
		.setPositiveButton(android.R.string.ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		})
		.create();
        
        setStatus(Car100.STATUS_NO_LINK);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();

        SplitJoystickView joy = (SplitJoystickView)findViewById(R.id.joystick);
        joy.setLeftControls(prefs.getBoolean(getString(R.string.key_left_controls), false));
        
    	BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    	if (adapter == null || !adapter.isEnabled()) {
    		bluetoothAlert = new AlertDialog.Builder(this)
    		.setTitle(R.string.bluetooth_off_title)
    		.setMessage(R.string.bluetooth_off_msg)
    		.setIcon(android.R.drawable.ic_dialog_alert)
    		.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
    			@Override
    			public void onClick(DialogInterface dialog, int which) {
    				dialog.dismiss();
    				Remote100Activity.this.finish();
    			}
    		})
    		.create();
    		bluetoothAlert.show();
    	} else {
    		Set<BluetoothDevice> devices = adapter.getBondedDevices();
    		String carAddress = null;
    		for (BluetoothDevice d : devices) {
    			if (d.getName().equals(CAR_NAME)) {
    				carAddress = d.getAddress();
    				break;
    			}
    		}
    		if (carAddress == null) {
        		bluetoothAlert = new AlertDialog.Builder(this)
        		.setTitle(R.string.no_car_title)
        		.setMessage(getString(R.string.no_car_msg, CAR_NAME))
        		.setIcon(android.R.drawable.ic_dialog_alert)
        		.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        			@Override
        			public void onClick(DialogInterface dialog, int which) {
        				dialog.dismiss();
        				Remote100Activity.this.finish();
        			}
        		})
        		.create();
        		bluetoothAlert.show();
    		} else {
    			
    			server = new CarServer(this, adapter, carAddress);
    		}
    	}
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	if (server != null) {
    		server.close();
    		server = null;
    	}
    	if (bluetoothAlert != null) {
    		bluetoothAlert.dismiss();
    		bluetoothAlert = null;
    	}
    }
	
	// =========================================
	// Menu Handling
	// =========================================

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, ABOUT_ID, 0, R.string.menu_about).setIcon(android.R.drawable.ic_menu_info_details);
		menu.add(0, SETTINGS_ID, 0, R.string.menu_settings).setIcon(android.R.drawable.ic_menu_preferences);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()) {
		case ABOUT_ID:
			aboutDialog.show();
			return true;
		case SETTINGS_ID:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}
	
	// =========================================
	// UI Modification Methods
	// =========================================
	
    public void setStatus(byte status) {
    	if (status != lastStatus) {
	    	int statusText, statusIcon;
	    	switch (status) {
	    	case Car100.STATUS_NO_LINK:
	    		statusText = R.string.status_no_link;
	    		statusIcon = R.drawable.status_no_link;
	    		break;
	    	case Car100.STATUS_CONNECTED:
	    		statusText = R.string.status_connected;
	    		statusIcon = R.drawable.status_connected;
	    		break;
	    	case Car100.STATUS_CHARGING:
	    		statusText = R.string.status_charging;
	    		statusIcon = R.drawable.status_charging;
	    		break;
	    	case Car100.STATUS_CHARGE_FULL:
	    		statusText = R.string.status_charge_full;
	    		statusIcon = R.drawable.status_charge_full;
	    		break;
	    	case Car100.STATUS_READY:
	    		statusText = R.string.status_ready;
	    		statusIcon = 0;
	    		break;
	    	default:
	    		throw new IllegalArgumentException("Unknown status: " + status);
	    	}    	
	    	((TextView)findViewById(R.id.status)).setText(statusText);
	    	ImageView statusIconView = (ImageView)findViewById(R.id.status_icon);
	    	if (statusIcon != 0) {
	    		statusIconView.setImageResource(statusIcon);
	    		statusIconView.setVisibility(View.VISIBLE);
	    	} else {
	    		statusIconView.setVisibility(View.GONE);
	    	}
	    	
	    	int visibility = View.GONE;
	    	if (status == Car100.STATUS_READY) {
	    		visibility = View.VISIBLE;
	    		
	    		/* Work around a bug with the SplitJoystickView not being
	    		 * rendered on visibility change until something else changes.
	    		 * :(
	    		 */
	    		findViewById(android.R.id.content).postDelayed(new Runnable() {
					@Override
					public void run() {
						resetUI();
					}
				}, 100);
	    	}
	    	for (int view : new int[] {R.id.gearLayout, R.id.joystick}) {
	    		findViewById(view).setVisibility(visibility);
	    	}
	    	if (status == Car100.STATUS_READY) {
	    	}
    	}
		lastStatus = status;
    }
    
    public void resetUI() {
    	currGear = 1;
    	((SeekBar)findViewById(R.id.gearBar)).setProgress(currGear);
    	((TextView)findViewById(R.id.gearText)).setText(String.valueOf(currGear + 1));
    }
	
	// =========================================
	// Event Listeners
	// =========================================
    
    private class GearChangeListener implements OnSeekBarChangeListener {
		@Override
		public void onProgressChanged(SeekBar seekBar, int gear,
				boolean fromUser) {
			if (!fromUser || Car100.setGear((byte)(gear + 1))) {
				currGear = gear;
			} else {
				seekBar.setProgress(currGear);
			}
			((TextView)findViewById(R.id.gearText)).setText(String.valueOf(currGear + 1));
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {}
    }
    
    private class JoystickListener implements OnJoystickMovedListener {
		@Override
		public void onMoved(int x, int y) {
			if (x < -5) {
				Car100.setDir(Car100.DIR_LEFT);
			} else if (x > 5) {
				Car100.setDir(Car100.DIR_RIGHT);
			} else {
				Car100.setDir(Car100.DIR_STRAIGHT);
			}
			
			if (y > 5) {
				Car100.setAcc(Car100.ACC_FORWARD);
			} else if (y < -5) {
				Car100.setAcc(Car100.ACC_REVERSE);
			} else {
				Car100.setAcc(Car100.ACC_STOP);
			}
		}
    }
}