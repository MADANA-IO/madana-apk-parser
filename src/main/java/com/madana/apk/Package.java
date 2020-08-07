package com.madana.apk;

import java.util.ArrayList;
import java.time.Instant;
import java.time.Duration;
import java.io.InputStream;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.net.MalformedURLException;
import java.io.IOException;
import java.lang.InterruptedException;
import java.lang.ProcessBuilder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
		String file_path = "/tmp/APKINDEX.tar.gz";
		InputStream in = null;
		try {
			in = new URL("http://dl-cdn.alpinelinux.org/alpine/"+
					alpineVersion+"/main/"+alpineArch+"/APKINDEX.tar.gz")
				.openStream();
		} catch(MalformedURLException ex) {}
		Files.copy(in, Paths.get(file_path), StandardCopyOption.REPLACE_EXISTING);

		ProcessBuilder builder = new ProcessBuilder();
		builder.command("sh", "-c", String.format("tar xfz %s -C %s", file_path, "/tmp/"));
		builder.directory(new File("/tmp"));
		int exitCode = builder.start().waitFor();
		assert exitCode == 0;
	}

	private static void parse() throws IOException, SQLException, InterruptedException
	{
		Package.download();
		String index_path = "/tmp/APKINDEX";
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
