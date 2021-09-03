# DoorkeeperEncryption
Doorkeeper action for encrypting files before ingesting them into Fedora

## Installation

### JAR
To install, go into the `EncryptResources` folder and run `mvn clean install`.
Then move the built JAR (usually found in the target folder) to your tomcat flat webapp
libs folder: `/<tomcat_root_folder>/webapps/flat/WEB-INF/lib`.

### TLA-Encrypt Manager
This action uses the TLA-Encrypt Manager. To install, see https://github.com/thelanguagearchive/flat_encryption/tree/develop/java

### KMS
This action uses HashiCorp Vault as the KMS (Key Management System). For installation
instructions, see https://github.com/thelanguagearchive/flat_encryption/tree/develop/docker

## Configuration
To enable this action, you should add the following configuration to your `flat-deposit.xml` file.

```xml
<action name="encrypt resources" class="nl.mpi.tla.flat.deposit.action.EncryptResources">
    <parameter name="dir" value="{$work}/resources"/>
    <parameter name="credentials" value="/app/flat/encryption/credentials.json"/>
    <parameter name="outputDir" value="{$work}/encrypted"/>
</action>
```
