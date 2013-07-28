Server Compare
==============

{{servercompare}} is a utility to compare configurations between two remote servers.

It takes to hostnames and a directory as a parameter, and compares that directory on the two machines. It will list files that are only on one of the two machines, and can open a diff tool to show the differences between files that exist on both but are different.

Usage
=====

Example (from `sbt`):

    run app1.production.example.org app1.acceptance.example.org /etc/apache2/
    
For more usage information, run

    run --help
    
Prerequisites
=============

* Passwordless SSH-key access to both machines
* Agent forwarding setup, so that from the reference machine, you can access the test machine without password (because we use `rsync` between these machines to check for file differences)
* Your user must be able to read the directory that is compared on both machines

Contributing
============

Feel free to open a pull request against this utility :)


