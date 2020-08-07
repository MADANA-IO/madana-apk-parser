### Usage

```
import com.madana.common.apkparser.Package

...

// set alpine arch; default is 'x86_64'
Package.setAlpineArch("aarch64");

// set alpine version; default is 'latest_stable'
Package.setAlpineArch("3.12");

// search for exact match
Package mariadb = Package.searchExact("mariadb");

// search for partial match; this returns a list of packages whos names contain 'mariadb'
ArrayList<Package> mariadbPackages = Package.search("mariadb");
```

### Good to know

 * package list is automatically update if a search is executed and if 24 hours have surpased
