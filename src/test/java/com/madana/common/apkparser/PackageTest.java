/*******************************************************************************
 * Copyright (C) 2020 MADANA
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * @organization:MADANA
 * @author:Frieder Paape
 * @contact:dev@madana.io
 ******************************************************************************/

package com.madana.common.apkparser;

import org.junit.Test;
import java.util.ArrayList;

public class PackageTest 
{
    @Test
    public void testSearchExact() throws Exception
    {
	try {
		Package mdb = Package.searchExact("mariadb");
		assert mdb.getName().equals("mariadb");
	} catch(Exception ex) {
		ex.printStackTrace();
		throw ex;
	}
    }

    @Test
    public void testSearch() throws Exception
    {
	try {
		ArrayList<Package> results = Package.search("mariadb");
		// leaning out the window here assuming mariadb search always returns more than one value
		assert results.size() > 1;
		for (Package pack: results) {
			assert pack.getName().contains("mariadb");
		}
	} catch(Exception ex) {
		ex.printStackTrace();
		throw ex;
	}
    }
    

    @Test
    public void testSetArchAndVersion() throws Exception
    {
	try {
		Package mdb = Package.searchExact("mariadb");
		assert mdb.getArch().equals("x86_64");
		Package.setAlpineArch("aarch64");
		mdb = Package.searchExact("mariadb");
		assert mdb.getArch().equals("aarch64");
		Package.setAlpineVersion("edge");
		Package mdb0 = Package.searchExact("mariadb");
		assert !mdb.getVersion().equals(mdb0.getVersion());
	} catch(Exception ex) {
		ex.printStackTrace();
		throw ex;
	}
    }

    @Test
    public void testSearchExactCommunity() throws Exception
    {
	try {
		Package mdb = Package.searchExact("nyancat");
		assert mdb.getName().equals("nyancat");
		assert mdb.getRepo().equals("community");
	} catch(Exception ex) {
		ex.printStackTrace();
		throw ex;
	}
    }
}
