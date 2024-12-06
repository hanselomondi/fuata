#!/usr/bin/env bash

# Exit immediately if a command exits with a non-zero status
set -e

echo "Installing Fuata CLI..."

# Check if the JAR file exists
JAR_SOURCE="build/libs/fuata-1.0-SNAPSHOT.jar"
JAR_TARGET="/usr/local/bin/fuata-1.0.jar"
WRAPPER_SCRIPT="/usr/local/bin/fuata"

if [[ ! -f "$JAR_SOURCE" ]]; then
    echo "Error: JAR file '$JAR_SOURCE' not found. Aborting..."
    exit 1
fi

# Ensure the script is run with sufficient permissions
if [[ $EUID -ne 0 ]]; then
    echo "Error: This script must be run as root. Aborting..."
    exit 1
fi

# Copy the JAR file
echo "Copying JAR file to $JAR_TARGET..."
cp "$JAR_SOURCE" "$JAR_TARGET"

# Create the wrapper script
echo "Creating wrapper script..."
echo "#!/usr/bin/env bash" > "$WRAPPER_SCRIPT"
echo "java -jar $JAR_TARGET \"\$@\"" >> "$WRAPPER_SCRIPT"

# Make the wrapper script executable
echo "Making wrapper script executable..."
chmod +x "$WRAPPER_SCRIPT"

echo "Installation complete. You can now use Fuata in your CLI, using the 'fuata' command."
