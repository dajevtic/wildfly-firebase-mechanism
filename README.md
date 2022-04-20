# Elytron security POC using firebase admin 

This code is a proof-of-concept using Firebase Admin to authenticate users via a configured Firebase project. It has been tested on wildfly-preview-26.1.0.Final (EE9) and wildfly-26.1.0.Final (EE8).
Have your firebase_credentials.json file ready and copy it into the configuration folder of your wildfly instance. Make sure its path matches the path in the configuration of the realm below.

## The following steps are required to install the firebase realm:

### Compile and build a jar which includes all dependencies

```
mvn clean compile assembly:single 
```

### cd into your target folder where your compiled jar resides and using `jboss-cli.sh --connect` (while Wildfly server is running) enter

```
module add --name=de.elb.wildfly.firebase --resources=firebase-mechanism-1.0.0-SNAPSHOFT-jar-with-dependencies.jar --dependencies=org.wildfly.security.elytron,org.wildfly.extension.elytron,org.slf4j

/subsystem=elytron/custom-realm=firebase-realm:add(module=de.elb.wildfly.firebase, class-name=de.elb.wildfly.firebase.FirebaseRealm, configuration={"credentials" => "/opt/wildfly-preview-26.1.0.Final/standalone/configuration/firebase_credentials.json"})

/subsystem=elytron/security-domain=firebase-domain:add(realms=[{realm=firebase-realm}], default-realm=firebase-realm, permission-mapper=default-permission-mapper)

/subsystem=elytron/http-authentication-factory=firebase-http-auth:add(http-server-mechanism-factory=global, security-domain=firebase-domain, mechanism-configurations=[{mechanism-name=BASIC, mechanism-realm-configurations=[{realm-name=firebase-domain}]},{mechanism-name=FORM, mechanism-realm-configurations=[{realm-name=firebase-domain}]}])

/subsystem=undertow/application-security-domain=firebase-domain:add(http-authentication-factory=firebase-http-auth)
```

Your war should target this domain in WEB-INF/jboss-web.xml:

```
<jboss-web>
    <security-domain>firebase-domain</security-domain>
</jboss-web>
```

You can configure BASIC silent additionally to form based authentication in your web.xml, e.g.:

```
<login-config>
    <auth-method>BASIC?silent=true,FORM</auth-method>
    <realm-name>firebase-domain</realm-name>
    <form-login-config>
        <form-login-page>/login/login.html</form-login-page>
        <form-error-page>/login/logine.html</form-error-page>
    </form-login-config>
</login-config>
```

If your war requires access to firebase admin, e.g. for messaging and other features, there is no need to include it in the war, as it is available as a wildfly module:

```
<dependency>
    <groupId>com.google.firebase</groupId>
    <artifactId>firebase-admin</artifactId>
    <version>${firebase.admin.version}</version>
    <scope>provided</scope>
</dependency>
```

Just add the following to your WEB-INF/jboss-deployment-structure.xml:

```
<jboss-deployment-structure>
    <deployment>
        <dependencies>
            <module name="de.elb.wildfly.firebase" />
        </dependencies>
    </deployment>
</jboss-deployment-structure>
```

You can then access Firebase Admin via:
```
FirebaseApp.getInstance()
```
