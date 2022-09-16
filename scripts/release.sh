#!/bin/bash
set -e

export RELEASE_VERSION="${RELEASE_VERSION:-}"
export POST_RELEASE_VERSION="${POST_RELEASE_VERSION:-}"
export DEBUG="${DEBUG:-false}"

export VERSION="${RELEASE_VERSION}"
export FOO_SEC="${FOO_SEC:-}"
export FOO_PUB="${FOO_PUB:-}"
export FOO_PASSPHRASE="${FOO_PASSPHRASE:-}"
export SONATYPE_USER="${SONATYPE_USER:-}"
export SONATYPE_PASSWORD="${SONATYPE_PASSWORD:-}"

if [[ "${DEBUG}" == "true" ]];  then
  set -x
fi

temporaryDir="$(mktemp -d)"
trap "{ rm -rf ${temporaryDir}; }" EXIT

if echo $VERSION | egrep -q 'M|RC'; then
    echo Activating \"milestone\" profile for version=\"$VERSION\"
    echo $MAVEN_ARGS | grep -q milestone || MAVEN_ARGS="$MAVEN_ARGS -Pmilestone"
elif echo $VERSION | egrep -q 'SNAPSHOT'; then
    echo "Version is a snapshot one - will not add any additional profiles for version=\"$VERSION\" "
else
    echo Activating \"central\" profile for version=\"$VERSION\"
    echo $MAVEN_ARGS | grep -q milestone || MAVEN_ARGS="$MAVEN_ARGS -Pcentral"
fi

MAVEN_ARGS="${MAVEN_ARGS} -Dgpg.secretKeyring="$FOO_SEC" -Dgpg.publicKeyring="$FOO_PUB" -Dgpg.passphrase="$FOO_PASSPHRASE" -DSONATYPE_USER="$SONATYPE_USER" -DSONATYPE_PASSWORD="$SONATYPE_PASSWORD""

echo "Will set the following Maven Args [${MAVEN_ARGS}]"

echo -e "\n\nUpdate the project versions to ${RELEASE_VERSION}\n\n"

otelVersion="${RELEASE_VERSION}" && ./mvnw versions:set -DnewVersion="${otelVersion}" -DgenerateBackupPoms=false && pushd spring-cloud-sleuth-otel-dependencies && ../mvnw versions:set -DnewVersion="${otelVersion}" -DgenerateBackupPoms=false && popd && pushd benchmarks && ../mvnw versions:set -DnewVersion="${otelVersion}" -DgenerateBackupPoms=false && popd

echo -e "\n\nBuild the project\n\n"

./mvnw clean install $MAVEN_ARGS

echo -e "\n\nUpload artifacts\n\n"

./mvnw deploy $MAVEN_ARGS -DskipTests

echo -e "\n\nCommit and tag\n\n"

git commit -am "Bumped versions for the ${otelVersion} release" && git tag "v${otelVersion}"

echo -e "\n\nGenerate docs\n\n"

./mvnw clean install -Pdocs -pl docs && cp -r docs/target/generated-docs/* "${temporaryDir}" && git checkout gh-pages && git reset --hard origin/gh-pages && rm -rf "docs/${otelVersion}" && mkdir -p "docs/${otelVersion}" && cp -rf "${temporaryDir}"/* "docs/${otelVersion}/" && pushd docs && rm current && ln -s "${otelVersion}" current && git add . && git commit -m "Updated site" && git push origin gh-pages

echo -e "\n\nUpdate the project versions to a post release version ${POST_RELEASE_VERSION}\n\n"

otelVersion="${POST_RELEASE_VERSION}" && ./mvnw versions:set -DnewVersion="${otelVersion}" -DgenerateBackupPoms=false && pushd spring-cloud-sleuth-otel-dependencies && ../mvnw versions:set -DnewVersion="${otelVersion}" -DgenerateBackupPoms=false && popd && pushd benchmarks && ../mvnw versions:set -DnewVersion="${otelVersion}" -DgenerateBackupPoms=false && popd
