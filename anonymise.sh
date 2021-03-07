#!/bin/bash
set -e
echo "Replacing all occurrences of $1 with $2";
sleep 1;
find MobileApps -type f -exec sed -i "s/$1/$2/gi" {} +
find SGX -type f -exec sed -i "s/$1/$2/gi" {} +
find Simulators -type f -exec sed -i "s/$1/$2/gi" {} +
find WebApi -type f -exec sed -i "s/$1/$2/gi" {} +