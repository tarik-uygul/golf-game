#!/bin/bash
# Run the experiment and write results.csv in the project root.
# Usage:
#   ./run_experiment.sh           — 100 runs per config
#   ./run_experiment.sh 200       — 200 runs per config
#
# Progress messages go to the terminal; only CSV rows go into the file.

RUNS=${1:-100}
cd "$(dirname "$0")"

echo "Building..."
mvn compile -q

echo "Running experiment ($RUNS runs per config)..."
mvn exec:java -Dexec.args="$RUNS" > results.csv

echo "Done — results.csv written ($(wc -l < results.csv) lines)."
