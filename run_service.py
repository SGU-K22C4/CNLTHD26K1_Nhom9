#!/usr/bin/env python3
import subprocess
import sys

jar_path = r"C:\Workspace\CNLTHD26K1_Nhom9\backend\services\product-service\target\product-service-1.0.0.jar"

try:
    process = subprocess.Popen(
        [sys.executable, "-m", "java", "-jar", jar_path],
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1
    )

    # Read up to 100 lines of output
    line_count = 0
    for line in process.stdout:
        print(line, end='')
        line_count += 1
        if line_count >= 100:
            break

    if process.poll() is None:
        process.terminate()

except Exception as e:
    print(f"Error: {e}")

