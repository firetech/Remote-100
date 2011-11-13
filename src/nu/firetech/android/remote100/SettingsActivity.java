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

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState)	{
		super.onCreate(savedInstanceState);
		
		setTitle(getString(R.string.app_name) + " - " + getString(R.string.menu_settings));
		addPreferencesFromResource(R.layout.preferences);
	}
}
