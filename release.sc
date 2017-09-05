import $ivy.`org.eclipse.jgit:org.eclipse.jgit:4.8.0.201706111038-r`

import org.eclipse.jgit._
import org.eclipse.jgit.api._

def clone(organization: String, repo: String) = {
    val git: Git = Git.cloneRepository()
      .setURI(s"https://github.com/$organization/$repo.git")
      .setDirectory("$repo")
      .setBranchesToClone( singleton( "refs/heads/develop" ) )
      .setBranch( "refs/heads/develop" )
      .call();
}

 /*
 # Clone repo and checkout develop
 git clone git@github.com:${organization}/${repo}.git
 cd ${repo}
 git checkout develop
 git pull --rebase

 # Expect the version number on develop to be the version TO BE RELEASED

 echo "Updating dependencies"
 ${sep='\n' dependencyCommands}

 git add .
 # If there is nothing to commit, git commit will return 1 which will fail the script.
 # This ensures we only commit if build.sbt was effectively updated
 git diff-index --quiet HEAD || git commit -m "Update ${repo} version to ${releaseV}"

 # wdl4s needs a scala docs update
 if [ ${repo} == "wdl4s" ]; then

   # Generate new scaladoc
   sbt 'set scalacOptions in (Compile, doc) := List("-skip-packages", "better")' doc
   git checkout gh-pages
   for subproj in cwl wdl wom; do
     API_SRC_DIR=$subproj/target/scala-2.12/api
     API_DST_DIR=${releaseV}/$subproj
     if [ -d $API_SRC_DIR ]; then
       mkdir -p $API_DST_DIR
       mv $API_SRC_DIR $API_DST_DIR
     fi
   done
   git add ${releaseV}

   # Update latest pointer
   git rm --ignore-unmatch latest

   ln -s ${releaseV}/wom/api latest
   git add latest

   git diff-index --quiet HEAD || git commit -m "Update Scaladoc"
   git push origin gh-pages

   # Update badges on README
   git checkout develop
   curl -o scaladoc.png https://img.shields.io/badge/scaladoc-${releaseV}-blue.png
   curl -o version.png https://img.shields.io/badge/version-${releaseV}-blue.png

   git add scaladoc.png
   git add version.png

   git diff-index --quiet HEAD || git commit -m "Update README badges"
   git push origin develop
 fi

 # Merge develop into master
 git checkout master
 git pull --rebase
 git merge develop --no-edit

 # Make sure tests pass
 sbt update
 JAVA_OPTS=-XX:MaxMetaspaceSize=1024m sbt test

 # Tag the release
 git tag ${releaseV}

 # Push master and push the tags
 git push origin master
 git push --tags

 # Create and push the hotfix branch
 git checkout -b ${releaseV}_hotfix

 # Pin centaur for cromwell
 if [ ${repo} == "cromwell" ]; then
    centaurDevelopHEAD=$(git ls-remote git://github.com/${organization}/centaur.git | grep refs/heads/develop | cut -f 1)
    sed -i '' s/CENTAUR_BRANCH=./CENTAUR_BRANCH="$centaurDevelopHEAD"/g .travis.yml
    git add .travis.yml
    git commit -m "Pin release to centaur branch"
 fi

 git push origin ${releaseV}_hotfix

 # Assemble jar for cromwell
 if [ ${repo} == "cromwell" ]; then
    sbt -Dproject.version=${releaseV} -Dproject.isSnapshot=false assembly
 fi

 # Update develop to point to next release version
 git checkout develop
 ${updateVersionCommand}
 git add .
 git diff-index --quiet HEAD || git commit -m "Update ${repo} version from ${releaseV} to ${nextV}"
 git push origin develop

 pwd > executionDir.txt
 */
