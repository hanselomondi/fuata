#!/usr/bin/env bash

# Installs the fuata vcs to your local environment so that it can be used as a CLI tool
echo "Installing Fuata CLI..."

# Copy the fat JAR file to user's PATH
cp build/libs/fuata-1.0-SNAPSHOT.jar /usr/local/bin/fuata-1.0.jar

# Create the wrapper script
echo "#!/bin/usr/env bash" > /usr/local/bin/fuata
echo "java -jar /usr/local/bin/fuata-1.0.jar" >> /usr/local/bin/fuata

# Make the wrapper script executable
chmod +x /usr/local/bin/fuata

echo "Installation complete. You can now use Fuata in your CLI, using the 'fuata' command"