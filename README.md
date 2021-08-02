
The UID 2 Project is subject to Tech Lab IPR’s Policy and is managed by the IAB Tech Lab Addressability Working Group and Privacy & Rearc Commit Group. Please review the governance rules [here](https://github.com/IABTechLab/uid2-core/blob/master/Software%20Development%20and%20Release%20Procedures.md)

## Build & Package

```
    mvn package
```

## Install

1. add reference to uid2-client-1.0.0-jar-with-dependencies.jar generated from build & package setup in your project

2. install the library locally and reference via maven

```
    mvn install:install-file -Dfile="./target/uid2-client-1.0.0-jar-with-dependencies.jar" -DgroupId="com.uid2" -DartifactId="uid2-client" -Dpackaging=jar -Dversion="1.0.0"
```

this will install the jar into your local .m2 repository, you can set your desired target repo with `-DlocalRepositoryPath=path-to-specific-local-repo`

then on your maven project's pom.xml, add

```
        <dependency>
            <groupId>com.uid2</groupId>
            <artifactId>uid2-client</artifactId>
            <version>1.0.0</version>
        </dependency>
```

## Examples

See com.uid2.client.test.IntegrationExamples for example usage.



