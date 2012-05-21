encoding-enforcer-maven-plugin
==============================

A maven plugin to enforce the source code encoding for all project files.

This plugin validates all .java files in a maven project with the specified source encoding.

The source encoding must be defined as maven project property `project.build.sourceEncoding`.

[![Build Status](https://buildhive.cloudbees.com/job/kreyssel/job/encoding-enforcer-maven-plugin/badge/icon)](https://buildhive.cloudbees.com/job/kreyssel/job/encoding-enforcer-maven-plugin/)

You should clone the repository locally and run `mvn clean install`.

Then add the plugin to your pom:

	<project>
	
		<properties>
			<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
		</properties>
	
		<build>
			<plugins>
				...
				<plugin>
					<groupId>org.kreyssel.maven.plugins</groupId>
					<artifactId>encoding-detector-maven-plugin</artifactId>
					<version>0.0.1-SNAPSHOT</version>
					<executions>
						<execution>
							<id>ensure-encoding</id>
							<goals>
								<goal>check</goal>
							</goals>
						</execution>
					</executions>
				</plugin>
				...
			</plugins>
		</build>
	</project>