Eclipse-JDE
===========
# About the BlackBerry Java Plug-in for Eclipse

* These procedures and the code at this location are provided under the [EPL-1.0](http://opensource.org/licenses/eclipse-1.0.php).

The BlackBerry&reg; Java&reg; Plug-in for Eclipse&reg; lets you develop, test, and debug BlackBerry Java applications using the Eclipse IDE, in combination with the BlackBerry SDK appropriate for the device OS your app is targeting. 

You can download BlackBerry SDKs through Eclipse update sites maintained by RIM for Windows and Mac OS.<br/>
<table>
<tr><td>Windows:</td><td><b>http://www.blackberry.com/developers/jar/win/java</b><td/></tr>
<tr><td>Mac OS:    </td><td><b>http://www.blackberry.com/developers/jar/mac/java</b><td/></tr>
</table>
The BlackBerry Java Plug-in for Eclipse offers
 
 * Simple BlackBerry Java development
 * Effective testing and debugging
 * Powerful application profiling

## Useful links
You can find tools and documentation, including full installers for Eclipse Plug-in for BlackBerry Java development Windows and Mac OSX, and other reference materials here:

[Tools for BlackBerry Java Development](https://developer.blackberry.com/java/download)

[Support community for open source development](http://supportforums.blackberry.com/t5/Open-Source-Development/ct-p/os_dev)

-------------------------------------------------------------------------------------------------------
## Building the source code
You can build the plug-ins on any Git Bash, Ant, Maven, or Eclipse supported environment, however we recommend Windows 7 + MINGW32 + Eclipse 3.7.2 32b to build the full installer.

### Windows development environment
-Windows 7 (64b or 32b)

-Java SDK 6 SE Update26 (or newer) ( export /set JAVA_HOME=<path to Java SDK  install root> | PATH=%PATH%;%JAVA_HOME%\bin )

-MINGW32 / git 1.7.10 (or newer)

-Apache Maven 3.0.1 (or newer)  ( export /set M2_HOME=<path to Maven install root> | PATH=%PATH%;%M2_HOME%\bin )

-Apache Ant 1.8.2 (or newer) ( export /set ANT_HOME=<path to Ant install root> | PATH=%PATH%;%ANT_HOME%\bin )

-Eclipse classic 3.7.2 32b (export /set ECLIPSE_HOME=|Eclipse root path| )

To check the versions of Maven, Ant, and Java that you have installed, enter the following commands in the Command Line window:

<pre><code>
 mvn –version
 ant –version
 java –version
</code></pre>

### Mac OSX support to build the plug-in only
* The full installer cannot be built on the Mac OS tool development environment.

-Mac OSX 10.6 / 10.7
-Java Development Kit version 1.6 installed
-git 1.7.10 for Mac OSx

#### We recommend that you install Maven on Mac OSX using MacPorts
You can download MacPorts from http://www.macports.org/install.php.
To verify that it is installed correctly, run the following command:
 <pre><code>sudo port install maven3
 mvn --version </code></pre>


#### To install Maven on Mac OSX without using MacPorts
   1) Download and extract the distribution archive apache-maven-3.0.3-bin.tar.gz to an install directory, for example /usr/local/apache-maven. The subdirectory apache-maven-3.0.3 will be created from the archive.

   2) In a command terminal, add the M2_HOME environment variable, for example, export M2_HOME=/usr/local/apache-maven/apache-maven-3.0.3.

   3) Add the M2 environment variable, for example, export M2=$M2_HOME/bin.

   4) Optional: Add the MAVEN_OPTS environment variable to specify JVM properties, for example, export MAVEN_OPTS="-Xms256m -Xmx512m".

   5) Add M2 environment variable to your path, for example, export PATH=$M2:$PATH.

   6) Make sure that JAVA_HOME is set to the location of your JDK, for example, export JAVA_HOME=/usr/java/jdk1.6.0_26 and that $JAVA_HOME/bin is in your PATH environment variable.

   7) Run mvn --version to verify that it is correctly installed.


-------------------------------------------------------------------------------------------------------

### Build the update site for the tool plug-in only

* To use the plug-in built using this procedure, you must install the required BlackBerry Java SDK separately.

From the command line, change to the root directory of the eJDE git repository and run the following command:
<pre><code> mvn clean package [-Declipse.target=|Eclipse Target|] </code></pre>
The Eclipse target is "helios" or "indigo" (default).
The first time the build is run it may take about 5 minutes to complete and requires an internet connection. Subsequent builds take around 2 minutes.

If the build is successful the update site can be found at ejde-update-site.win/site (for Windows) and ejde-udpate-site.mac/site (for Mac OS).

-------------------------------------------------------------------------------------------------------
### Build a complete update site (tool plug-in and BlackBerry Java SDK)
1) Set the "ECLIPSE_HOME" environment variable to the path of a valid indigo Eclipse standard installation
 <pre><code> export | set ECLIPSE_HOME=<path to Eclipse installation> </code></pre>
Note: This step is required to allow the org.eclipse.ant.core.antRunner to run the task p2.mirror to download the chosen BlackBerry SDKs.

2) (Optional) If you want to change the defaults, configure the following properties in root pom.xml, or they can also be provided as properties invoking<br/> <code>mvn ... -D|prop|=|value|</code>

<code>"jde.version.family"</code> to the BlackBerry SDK version family (e.g. 7.1.0)<br/>
<code>"jde.version"</code> to the BlackBerry SDK version (e.g. 7.1.0.10)<br/>
<code>"bb.sdk.local.repo.win | mac"</code>  to the correspondent folder containing the P2 mirror of the BlackBerry SDK artifacts<br/>

<code>"jde.keystore.path"</code> to your keystore you want to use ([Java Keytool](http://www.sslshopper.com/article-how-to-create-a-self-signed-certificate-using-java-keytool.html) to generate) to sign the built jars (default is ../keystore_foo.jks | Note that the path is pointing from one level beneath the root, from where it's being used but the .jks resides in the root of the project.)<br/>
<code>"jde.keystore.password"</code> to your kestore pass<br/>

3) From the command line, change to the root directory of the eJDE git repository and run the following commands:

 a) <code> mvn clean install –Dfull_site=true </code> (building 1st time required - this may take a bit longer as the BB SDKs for Windows and Mac OS are downloaded)</br>
 b) <code> mvn clean package –Dfull_site=true </code> (consecutive builds)<br/>

If the build is successful, the update site can be found at ejde-update-site.win/site (for Windows) and ejde-udpate-site.mac/site (for Mac OS). 

Note: If you plan to build the full installer, you must build step 3.a first.


-------------------------------------------------------------------------------------------------------
### Build the full installer for Windows & Mac OS
* You must build the full installer in a Windows development environment.

* The installer build scripts and custom Ant tasks are provided under the [Apache License Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

1) Build the full site as indicated in step 3.a in the procedure above.

2) Configure or review the "mount-point" property of the "mount-point-windows" profile in the root pom.xml to the folder that should contain 3rd party installer related artifacts:
 (default is /eJDE_install_artifacts )
The folder structure should look something like this:
<pre><code>
…
 Eclipse Platform/
  mac/
    indigo/
      eclipse-SDK-3.7.2-macosx-cocoa.tar.gz   (downloaded Eclipse 3.7.2 classic for Mac OSX archive)

  win/
    indigo/
      eclipse-SDK-3.7.2-win32.zip   (downloaded Eclipse 3.7.2 classic for Win 32b archive)

 InstallAnywhere 8.0 Enterprise/</code></pre>
 (valid IA8 installed dir; Visit [Flexera Software](http://www.flexerasoftware.com/products/installanywhere.htm) for license and terms)
<pre><code>
 Resources/
  [mac/]
  win/
    vcredist_x86.exe	</code></pre>
 ([MS VC++ 2008 Redistributable](http://www.microsoft.com/en-us/download/details.aspx?id=29) used by BlackBerry simulator 6.0 and up)

3) From the command line, change to the root directory of the eJDE git repository and run the following commands:

 <pre><code> mvn clean package -P installer </code></pre>

* Alternatively to the default /eJDE_install_artifacts structure, other parameters could be specified, like -Djde.eclipse.bundles="path to Eclipse platform dir"<br/>
If the build is successful, the installer <b>BlackBerry_JDE_PluginFull_2.0.0_indigo.exe</b> (for Windows) and <b>BlackBerry_JDE_PluginFull_2.0.0_indigo.zip</b> (for Mac) can be found at |eJDE repo root|/installer/target folder.

-------------------------------------------------------------------------------------------------------
### Eclipse development environment for this tool
To further develop the provided plug-in sources in Eclipse, you need to refer an instance of the org.eclipse.osgi plugin, which can be downloaded by invoking the <code>mvn clean -P get-osgi</code>, or run directly <code>cvs_checkout_org.eclipse.osgi.cmd</code>.

Then, import all of the projects, including org.eclipse.osgi, in your Eclipse 3.7 dev environment.

To launch the eJDE2.0 as an Eclipse application, set the following VM arguments:
<code>-Dosgi.framework.extensions=net.rim.ejde.preprocessing.hook -Dosgi.requiredJavaVersion=1.5 -Xms40m -Xmx512m -XX:MaxPermSize=512m</code>.

Once started, you must add a BlackBerry Java SDK to the runtime environment, either by using Help > Install New Software and point to the update site (local or web), or Preferences > Java > Installed JREs > Add > BlackBerry Execution Environment VM , and point to a local expanded BlackBerry Java SDK ../components/BlackBerry.ee file.
