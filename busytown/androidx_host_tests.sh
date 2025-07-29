#!/bin/bash
set -e

echo "Starting $0 at $(date)"
source "$(dirname "$0")/setup_build_env_vars.sh"
source "$(dirname "$0")/record_build_metrics.sh"

cd "$(dirname $0)"

setup_build_env_vars
start_time=$(initialize_start_time)

BUILD_EXIT_CODE=0
if ! ( \
    impl/build.sh test allHostTests zipOwnersFiles createModuleInfo \
        -Pandroidx.displayTestOutput=false \
        --continue \
        "$@" \
); then
    BUILD_EXIT_CODE=$?
fi

ONLY_TEST_TASK_FAILED_SIGNAL_FILE_PATH="$OUT_DIR/androidx-settings-plugins/only_test_task_failed_signal.txt"

if [ $BUILD_EXIT_CODE -ne 0 ]; then
    # If the build fails and the signal file does NOT exist, it's a genuine build failure.
    # The signal file is only created when test tasks are the sole cause of failure.
    if [ ! -f "$ONLY_TEST_TASK_FAILED_SIGNAL_FILE_PATH" ]; then
        echo "Gradle build failed (exit code $BUILD_EXIT_CODE)."
        exit $BUILD_EXIT_CODE
    fi
    # If the signal file DOES exist, it means only tests failed.
    # We proceed silently to allow ATP to collect and report test results.
fi

record_build_metrics "$start_time"

echo "Completing $0 at $(date)"
