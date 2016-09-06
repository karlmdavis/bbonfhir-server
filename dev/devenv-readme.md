Development Environment Setup
=============================

Thinking of contributing to this project? Great! This document provides some help on getting a development environment setup for that work.

## Common/Shared Instructions

Most of the setup and configuration for this project is the same as for the other Java-based Blue Button projects. Accordingly, please be sure to follow all of the instructions documented here, first: [bluebutton-parent-pom: Development Environment Setup](https://github.com/HHSIDEAlab/bluebutton-parent-pom/blob/devenv-instructions/dev/devenv-readme.md).

## Build Dependencies

This project depends on the [HAPI FHIR](https://github.com/jamesagnew/hapi-fhir) project. Releases of that project are available in the Maven Central repository, which generally makes things pretty simple: our Maven builds will pick up theirs.

Unfortunately, this project will sometimes need to depend on an interim/snapshot build of HAPI FHIR. When that's the case, developers will first need to locally checkout and `mvn install` that interim version themselves, manually. To keep this simpler, a fork of HAPI FHIR is maintained in the [HHSIDEAlab/hapi-fhir](https://github.com/HHSIDEAlab/hapi-fhir) repository on GitHub, which will always point to whatever version of HAPI FHIR this one depends on. You can checkout and build that fork, as follows:

    $ git clone https://github.com/HHSIDEAlab/hapi-fhir.git hhsidealab-hapi-fhir.git
    $ cd hhsidealab-hapi-fhir.git
    $ mvn clean install -DskipITs=true -DskipTests=true

Once the build is done, the HAPI FHIR artifacts will be placed into your user's local Maven repository (`~/.m2/repository`), available for use by this project or others.

## Jetty

References:

* [Jetty Docs: Configuring SSL/TLS](http://www.eclipse.org/jetty/documentation/current/configuring-ssl.html)
* [Jetty Docs: jetty.xml](http://www.eclipse.org/jetty/documentation/9.3.x/jetty-xml-config.html)
* [Jetty Docs: Configuring the Jetty Maven Plugin](https://www.eclipse.org/jetty/documentation/9.3.x/jetty-maven-plugin.html#jetty-stop-goal)

In the HealthAPT dev, test, and prod environments in AWS, this application is deployed to a Jetty container. The `bbonfhir-server-app` module also attaches to its build the Jetty XML configuration that should be used to run the application

TODO: document how to run Jetty in prod

Note that, because the Jetty client SSL configuration has "`NeedClientAuth`" set to "`true`", you will be unable to access HAPI's testing UI in your web browser unless you first deploy the `client.pfx` file to your browser (temporarily!). All API access will also require a trusted client certificate, as well. If you just want to poke around in the Testing UI, you can temporarily adjust "`NeedClientAuth`" "`false`". Note, though, that this **must not** be done in production, as it completely disables the application's authentication requirements.

### SSL Development Keystores

For your convenience, a dev-only-really-don't-use-these-anywhere-else server keystore and client truststore (with certs) have been generated and saved in this project's `dev/ssl-stores` directory. Originally, these were generated as follows:

1. Generate a new server keypair that's valid for `localhost` and `127.0.0.1` and a new keystore for it using Java's `keytool` in the WildFly server's `standalone/configuration` directory, e.g.:
    
    ```
    $ keytool -genkeypair -alias server -keyalg RSA -keysize 4096 -dname "cn=localhost" -ext "san=ip:127.0.0.1" -validity 3650 -keypass changeit -keystore bbonfhir-server.git/dev/ssl-stores/server.keystore -storepass changeit
    $ cp bbonfhir-server.git/dev/ssl-stores/server.keystore ~/workspaces/tools/wildfly-10.1.0.Final/standalone/configuration/
    ```
    
1. Generate a new client certificate that can be used in tests and place it in a new server truststore:
    
    ```
    $ keytool -genkeypair -alias client-local-dev -keyalg RSA -keysize 4096 -dname "cn=client-local-dev" -validity 3650 -keypass changeit -keystore bbonfhir-server.git/dev/ssl-stores/client.keystore -storepass changeit
    $ keytool -exportcert -alias client-local-dev -file bbonfhir-server.git/dev/ssl-stores/client.cer -keystore bbonfhir-server.git/dev/ssl-stores/client.keystore -storepass changeit
    $ keytool -importcert -noprompt -trustcacerts -alias client-local-dev -file bbonfhir-server.git/dev/ssl-stores/client.cer -keypass changeit -keystore bbonfhir-server.git/dev/ssl-stores/server.truststore -storepass changeit
    $ cp bbonfhir-server.git/dev/ssl-stores/server.truststore ~/workspaces/tools/wildfly-10.1.0.Final/standalone/configuration/
    ```
    
1. Export the client certificate to a PFX file that you can use in your browser, if need be:
    
    ```
    $ keytool -importkeystore -srckeystore bbonfhir-server.git/dev/ssl-stores/client.keystore -destkeystore bbonfhir-server.git/dev/ssl-stores/client.pfx -deststoretype PKCS12 -srcstorepass changeit -deststorepass changeit -srcalias client-local-dev
    ```
    