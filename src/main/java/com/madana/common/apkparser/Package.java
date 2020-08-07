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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

public class Package 
{
	private String arch, depends, origin, name, provides,
		providerPriority, timestamp, version;

	private static Connection connection;
	private static Statement statement;
	private static Instant lastUpdate;
	// update interval in hours
	private final static int updateInterval = 24;
	private static String alpineArch = "x86_64";
	private static String alpineVersion = "latest-stable";
	private static URL apkIndex;
	private static final File alpineIndexFolder= new File("APKINDEX");
       
	static {
		try {
			updateUrl();
		} catch(MalformedURLException ex) {}
	}

	private Package() {}

	private Package(ResultSet rs) throws SQLException
	{
		arch = rs.getString("arch");
		depends = rs.getString("depends");
		origin = rs.getString("origin");
		name = rs.getString("name");
		provides = rs.getString("provides");
		providerPriority = rs.getString("provider_priority");
		timestamp = rs.getString("timestamp");
		version = rs.getString("version");
	}

	private static void download() throws IOException, InterruptedException 
	{
		String file_path = "APKINDEX.tar.gz";
		InputStream in = null;
		try {
			in = new URL("http://dl-cdn.alpinelinux.org/alpine/"+
					alpineVersion+"/main/"+alpineArch+"/APKINDEX.tar.gz")
				.openStream();
		} catch(MalformedURLException ex) {}
		Files.copy(in, Paths.get(file_path), StandardCopyOption.REPLACE_EXISTING);
		extractTarGZ(new FileInputStream(file_path));
	}

	private static void parse() throws IOException, SQLException, InterruptedException
	{
		Package.download();
		String index_path = "APKINDEX";
		Package entry = new Package();
		Files.lines(Paths.get(index_path)).forEach(line -> {
			if (line.equals("")) {
				try {
					entry.apply();
				} catch (SQLException ex) {
					throw new RuntimeException(ex);
				}
				return;
			}

			entry.setValue(line.charAt(0), line.substring(2));
		});
	}

	private static void update() throws IOException, SQLException, InterruptedException
	{
		if (
			lastUpdate != null &&
			Duration.between(lastUpdate, Instant.now()).toHours()
				< updateInterval
		){
			return;
		}

		if (Package.connection != null)
			Package.connection.close();

		Package.connection = DriverManager.getConnection("jdbc:sqlite::memory:");
		Package.statement = Package.connection.createStatement();
		Package.statement.executeUpdate("drop table if exists package");
		Package.statement.executeUpdate("create table package (arch string, "
			+ "depends string, origin string, name string, "
			+ "provides string, provider_priority string, "
			+ "timestamp string, version string)");

		Package.parse();

		lastUpdate = Instant.now();
	}
	public static void extractTarGZ(InputStream in) throws IOException {
	    GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(in);
	    try (TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {
	        TarArchiveEntry entry;

	        while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
	            /** If the entry is a directory, create the directory. **/
	            if (entry.isDirectory()) {
	                File f = new File(entry.getName());
	                boolean created = f.mkdir();
	                if (!created) {
	                    System.out.printf("Unable to create directory '%s', during extraction of archive contents.\n",
	                            f.getAbsolutePath());
	                }
	            } else {
	                int count;
	                byte data[] = new byte[2048];
	                FileOutputStream fos = new FileOutputStream(entry.getName(), false);
	                try (BufferedOutputStream dest = new BufferedOutputStream(fos, 2048)) {
	                    while ((count = tarIn.read(data, 0, 2048)) != -1) {
	                        dest.write(data, 0, count);
	                    }
	                }
	            }
	        }

	    }
	}
	public static Package searchExact(String name) throws IOException, SQLException, InterruptedException
	{
		Package.update();

		ResultSet rs = Package.statement.executeQuery(
			String.format("select * from package where "
				+ "name = '%s'", name));

		if(rs.next())
			return new Package(rs);

		return null;
	}

	public static ArrayList<Package> search(String name) throws IOException, SQLException, InterruptedException
	{
		Package.update();

		ArrayList<Package> result = new ArrayList();

		ResultSet rs = Package.statement.executeQuery(
			String.format("select * from package where "
				+ "name like '%%%s%%'", name));

		while(rs.next())
		{
			result.add(new Package(rs));
		}
		return result;
	}

	private void apply() throws SQLException
	{
		validate();
		Package.statement.executeUpdate(
			String.format("insert into package values"
				+"('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')",
				arch, depends, origin, name, provides,
				providerPriority, timestamp, version));
		reset();
	}

	private void reset()
	{
		arch = depends = origin = name = provides = 
			providerPriority = timestamp = version = null;
	}

	private void validate() 
	{
		assert arch != null && name != null && version != null;
	}

	private void setValue(char type, String value)
	{
		switch(type) {
			case 'A':
				arch = value;
				break;
			case 'D':
				depends = value;
				break;
			case 'o':
				origin = value;
				break;
			case 'P':
				name = value;
				break;
			case 'p':
				provides = value;
				break;
			case 'k':
				providerPriority = value;
				break;
			case 't':
				timestamp = value;
				break;
			case 'V':
				version = value;
				break;
			default:
				break;
		}

	}

	private static void updateUrl() throws MalformedURLException
	{
		apkIndex = new URL("http://dl-cdn.alpinelinux.org/alpine/"+
			alpineVersion+"/main/"+alpineArch+"/APKINDEX.tar.gz");
		Package.lastUpdate = null;
	}

	public static void setAlpineArch(String alpineArch) throws MalformedURLException
	{
		Package.alpineArch = alpineArch;
		updateUrl();
	}

	public static void setAlpineVersion(String alpineVersion) throws MalformedURLException
	{
		Package.alpineVersion = alpineVersion;
		updateUrl();
	}

	public String getArch()
	{
		return arch;
	}

	public String getDepends()
	{
		return depends;
	}

	public String getOrigin()
	{
		return origin;
	}

	public String getName()
	{
		return name;
	}

	public String getProvides()
	{
		return provides;
	}

	public String getProviderPriority()
	{
		return providerPriority;
	}

	public String getTimestamp()
	{
		return timestamp;
	}

	public String getVersion()
	{
		return version;
	}

	public String toString() {
		return String.format(
			"Arch: %s, Depends: %s, Origin: %s, Name: %s, Provides: %s, " +
			"ProviderPriority: %s, Timestamp: %s, Version: %s",
			arch, depends, origin, name, provides,
			providerPriority, timestamp, version);
	}
}
