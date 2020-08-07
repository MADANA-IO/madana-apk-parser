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
}
