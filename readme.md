## A Library to Manage Web Archive Files in Cloud Storage

This project serves as a client library for manage files in Fedora 4 with cloud storage providers (e.g. Amazon S3, and etc)

* Operations:
 * Move a binary file into cloud storage
 * Restore a binary object from cloud storage

## Concept
<img src="http://ml.cc.vt.edu/concept.png"  height="400" />

## Build

To build this projects use this command
    
    $ cd fedora-cloud-tool
    $ mvn clean install


## Usage

1. Include fedora-cloud-tool/target/fedora-cloud-tool-1.0.1-SNAPSHOT.jar in your project path

2. Setup cloud provider credentials in your system environment
	```
	AWS:
	AWS_ACCESS_KEY_ID="xxxxxxxxxx"
	AWS_SECRET_ACCESS_KEY="xxxxxxxxxxxxxxxxxxxxxxx"
	```
[example code](https://github.com/ylchenvt/wadl/tree/master/example)

## Generate Standalone Javadocs

	$ cd fedora-cloud-tool
	$ mvn javadoc:javadoc
	$ open target/site/apidocs/index.html

