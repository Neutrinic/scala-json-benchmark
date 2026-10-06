#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
results_dir="${RESULTS_DIR:-results}"
benchmark_filter="${BENCHMARK_FILTER:-benchmarks.*Benchmark.*}"
libraries="${BENCHMARK_LIBRARIES:-argonaut,circe,circebooster,json4s,jsoniter,jawnfacade,lift,play,spray}"
operations="${BENCHMARK_OPERATIONS:-decode,encode}"
mkdir -p .tools "$results_dir"
if ! test -f .tools/sbt-launch.jar; then
  curl -fL https://repo.maven.apache.org/maven2/org/scala-sbt/sbt-launch/2.0.10/sbt-launch-2.0.10.jar -o .tools/sbt-launch.jar
fi
cat > .tools/repositories <<'REPOS'
[repositories]
local
maven-central: https://repo.maven.apache.org/maven2
REPOS
{
  date -u
  "$JAVA_HOME/bin/java" -version
  lscpu
  free -h
  sha256sum src/main/resources/birds.data
  git rev-parse HEAD 2>/dev/null || true
  git status --short 2>/dev/null || true
} > "$results_dir/environment.txt" 2>&1
find src/main/scala -type f -name '*.scala' -print0 | sort -z | xargs -0 sha256sum > "$results_dir/source.sha256"
sha256sum build.sbt project/plugins.sbt project/build.properties >> "$results_dir/source.sha256"
java_cmd=("$JAVA_HOME/bin/java" -Xms512m -Xmx2g -XX:ActiveProcessorCount=4
  -Dsbt.override.build.repos=true -Dsbt.repository.config="$PWD/.tools/repositories"
  -Dsbt.supershell=false -Dsbt.log.noformat=true -jar .tools/sbt-launch.jar)
"${java_cmd[@]}" 'compile' 'runMain benchmarks.Validate' 'Jmh/compile' > "$results_dir/validation.log" 2>&1
"${java_cmd[@]}" "Jmh/run -wi 5 -i 5 -w 2s -r 2s -f 2 -t 1 -prof gc -foe true -jvmArgs \"-Xms2g -Xmx2g -XX:+UseG1GC\" -rf json -rff $results_dir/jmh.json $benchmark_filter" > "$results_dir/jmh.log" 2>&1
"${java_cmd[@]}" 'show Compile/dependencyClasspath' > "$results_dir/dependencies.txt" 2>&1
python3 scripts/summarize.py --results-dir "$results_dir" --libraries "$libraries" --operations "$operations"
