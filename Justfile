# describes available recipes
help: 
  just --list --unsorted

# run codenarc
lint:
  ./gradlew spotlessCheck codenarc

# run unit tests
test class="*":
  ./gradlew test --tests '{{class}}'

# publish a release
release version branch=`git branch --show-current`:
  #!/usr/bin/env bash
  if [[ ! "{{branch}}" == "main" ]]; then
    echo "You can only publish a release from the 'main' branch"
    echo "--> currently on the '{{branch}}' branch"
  fi

  # update build.gradle on main
  sed -i '' "s/^version.*/version = '{{version}}'/g" build.gradle
  git add build.gradle
  git commit -m "bump version to {{version}}"
  git push

  # cut a release branch
  git checkout -B release/{{version}}
  git push --set-upstream origin release/{{version}}

  # tag the release
  git tag {{version}}
  git push origin refs/tags/{{version}}

  # publish to gradle plugin portal
  ./gradlew publishPlugins
