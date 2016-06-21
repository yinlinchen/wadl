Fedora 4 Github: https://github.com/fcrepo4/fcrepo4

Setup a Fedora server: http://localhost:8080/rest/

```
git clone https://github.com/fcrepo4/fcrepo4.git
cd fcrepo4
MAVEN_OPTS="-Xmx1024m -XX:MaxMetaspaceSize=1024m" mvn install
cd fcrepo-webapp
MAVEN_OPTS="-Xmx512m" mvn jetty:run

```

Create a Fedora binary under a Fedora container 
```
curl -X PUT --upload-file img/concept.png -H"Content-Type: image/png" "http://localhost:8080/rest/parent/"
curl -X PUT --upload-file img/JCDL16.jpeg -H"Content-Type: image/jpeg" "http://localhost:8080/rest/parent/"
```

