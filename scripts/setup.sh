#!/bin/bash

# ---------------------------------------------------------
# CSYE 6225 Application Setup Script for Custom Image
# Purpose: Install dependencies and configure application
# Database: RDS (configured via environment variables)
# ---------------------------------------------------------

# Enable strict error handling
set -e

# Define variables
APP_USER="csye6225"
APP_GROUP="csye6225"
APP_DIR="/opt/csye6225"
APP_JAR="webapp.jar"

echo "[INFO] Starting application setup..."

# ---------------------------------------------------------
# 1. Update system packages
# ---------------------------------------------------------
echo "[INFO] Updating package lists..."
sudo apt-get update

echo "[INFO] Upgrading installed packages..."
sudo DEBIAN_FRONTEND=noninteractive apt-get upgrade -y

# ---------------------------------------------------------
# 2. Install Java 21 (OpenJDK)
# ---------------------------------------------------------
echo "[INFO] Installing Java 21..."
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-21-jdk

# Verify Java installation
java -version

# ---------------------------------------------------------
# 2.5 Install Java 17 (OpenJDK)
# ---------------------------------------------------------
echo "Installing Java 17..."
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-17-jdk

# Verify Java installation
java -version

# ---------------------------------------------------------
# 3. Install MySQL Server
# ---------------------------------------------------------
echo "[INFO] Installing MySQL Server..."
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y mysql-server

# Start and enable MySQL
echo "[INFO] Starting and enabling MySQL..."
sudo systemctl start mysql
sudo systemctl enable mysql

# Wait for MySQL to be ready
echo "[INFO] Waiting for MySQL to be ready..."
sleep 5

# Create database and application user
echo "[INFO] Creating database and application user..."
sudo mysql -e "CREATE DATABASE IF NOT EXISTS csye6225;"
sudo mysql -e "CREATE USER IF NOT EXISTS 'csye6225'@'localhost' IDENTIFIED BY 'YourStrongPassword123!';"
sudo mysql -e "GRANT ALL PRIVILEGES ON csye6225.* TO 'csye6225'@'localhost';"
sudo mysql -e "FLUSH PRIVILEGES;"

echo "[INFO] MySQL setup completed."

# ---------------------------------------------------------
# 5. Create Application Group (Idempotent)
# ---------------------------------------------------------
echo "[INFO] Creating application group..."
if ! getent group ${APP_GROUP} > /dev/null; then
    sudo groupadd ${APP_GROUP}
    echo "[INFO] Group ${APP_GROUP} created."
else
    echo "[INFO] Group ${APP_GROUP} already exists."
fi

# ---------------------------------------------------------
# 6. Create Application User (System user, no login)
# ---------------------------------------------------------
echo "[INFO] Creating application user..."
if ! id -u ${APP_USER} > /dev/null 2>&1; then
    # -r: system user, -g: primary group, -s: no login shell
    sudo useradd -r -g ${APP_GROUP} -s /usr/sbin/nologin ${APP_USER}
    echo "[INFO] User ${APP_USER} created."
else
    echo "[INFO] User ${APP_USER} already exists."
fi

# ---------------------------------------------------------
# 7. Create Application Directory
# ---------------------------------------------------------
echo "[INFO] Creating application directory..."
sudo mkdir -p ${APP_DIR}

# ---------------------------------------------------------
# 8. Copy Application JAR (Packer will provide this file)
# ---------------------------------------------------------
# Note: The JAR file should be present in /tmp/ when this script runs
# Packer will copy it there before executing this script

if [ -f "/tmp/${APP_JAR}" ]; then
    echo "[INFO] Copying application JAR to ${APP_DIR}..."
    sudo cp /tmp/${APP_JAR} ${APP_DIR}/${APP_JAR}
else
    echo "[WARN] Application JAR not found at /tmp/${APP_JAR}"
    echo "[WARN] This is expected if running setup manually"
fi

# ---------------------------------------------------------
# 9. Set File Permissions
# ---------------------------------------------------------
echo "[INFO] Setting file permissions..."
sudo chown -R ${APP_USER}:${APP_GROUP} ${APP_DIR}
sudo chmod -R 750 ${APP_DIR}

echo "[INFO] Application setup completed successfully!"

echo "Setup script completed successfully!"
