Server Compare
==============

`servercompare` is a Scala utility to compare configurations between two remote servers.

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

Tricks
======

Some things are not readily compared with this tool, but if you can manage to get the changes into a file somehow, you can use this tool to compare.

For example, run the following commands in a temporary directory on multiple machines, and then run this tool on that directory:

    dpkg --get-selections > packages
    dpkg-query -W -f='${Conffiles}\n' '*' | awk 'OFS="  "{print $2,$1}' | LANG=C md5sum -c 2>/dev/null | awk -F': ' '$2 !~ /OK/{print $1}' | sort > changed-config-files 
    xargs md5sum < changed-config-files > changed-configs-md5sums



Contributing
============

Feel free to open a pull request against this utility :)


