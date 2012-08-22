For Development environment:
===========
To run under development environment:
1) Check out the source code of org.eclipse.osgi plugin and import it into the workspace.
2) Set the VM argument "-Dosgi.framework.extensions=net.rim.ejde.preprocessing.hook".


For Production:
==========
1) Add the following property to the /eclipse/configuration/config.ini file:
osgi.framework.extensions=net.rim.ejde.preprocessing.hook
This line is supposed to be inserted to the config.ini automatically by Eclipse. 
