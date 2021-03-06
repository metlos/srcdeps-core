/**
 * Copyright 2015-2016 Maven Source Dependencies
 * Plugin contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.srcdeps.core.impl.builder;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.srcdeps.core.BuildException;
import org.srcdeps.core.BuildRequest;
import org.srcdeps.core.BuildRequest.Verbosity;
import org.srcdeps.core.config.Maven;
import org.srcdeps.core.shell.Shell;
import org.srcdeps.core.shell.Shell.CommandResult;
import org.srcdeps.core.shell.ShellCommand;

/**
 * A base for {@link MvnBuilder} and {@link MvnwBuilder}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public abstract class AbstractMvnBuilder extends ShellBuilder {
    protected static final List<String> MVN_DEFAULT_ARGS = Collections
            .unmodifiableList(Arrays.asList("clean", "install"));
    protected static final List<String> MVNW_FILE_NAMES = Collections
            .unmodifiableList(Arrays.asList("mvnw", "mvnw.cmd"));

    protected static final List<String> POM_FILE_NAMES = Collections.unmodifiableList(
            Arrays.asList("pom.xml", "pom.atom", "pom.clj", "pom.groovy", "pom.rb", "pom.scala", "pom.yml"));
    protected static final List<String> SKIP_TESTS_ARGS = Collections.singletonList("-DskipTests");

    /**
     * @return the default build arguments used in Maven builds of source dependencies
     */
    public static List<String> getMvnDefaultArgs() {
        return MVN_DEFAULT_ARGS;
    }

    /**
     * @return the list of file names whose presence signals that the project can be built with {@link MvnwBuilder}
     */
    public static List<String> getMvnwFileNames() {
        return MVNW_FILE_NAMES;
    }

    /**
     * @return the list of file names that can store Maven Project Object Model. NB: there is not only {@code pom.xml}
     */
    public static List<String> getPomFileNames() {
        return POM_FILE_NAMES;
    }

    /**
     * @return the {@link List} of arguments to use when no tests should be run during the build of a source dependency
     */
    public static List<String> getSkipTestsArgs() {
        return SKIP_TESTS_ARGS;
    }

    public static boolean hasMvnwFile(Path directory) {
        for (String fileName : MVNW_FILE_NAMES) {
            if (directory.resolve(fileName).toFile().exists()) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasPomFile(Path directory) {
        for (String fileName : POM_FILE_NAMES) {
            if (directory.resolve(fileName).toFile().exists()) {
                return true;
            }
        }
        return false;
    }

    public AbstractMvnBuilder(String executable) {
        super(executable);
    }

    @Override
    protected List<String> getDefaultBuildArguments() {
        String settingsPath = System.getProperty(Maven.getSrcdepsMavenSettingsProperty());
        if (settingsPath != null) {
            List<String> result = new ArrayList<>(MVN_DEFAULT_ARGS.size() + 2);
            result.addAll(MVN_DEFAULT_ARGS);
            result.add("-s");
            result.add(settingsPath);
            return Collections.unmodifiableList(result);
        } else {
            return MVN_DEFAULT_ARGS;
        }
    }

    @Override
    protected List<String> getSkipTestsArguments(boolean skipTests) {
        return skipTests ? SKIP_TESTS_ARGS : Collections.<String> emptyList();
    }

    @Override
    protected List<String> getVerbosityArguments(Verbosity verbosity) {
        switch (verbosity) {
        case trace:
        case debug:
            return Collections.singletonList("--debug");
        case info:
            return Collections.emptyList();
        case warn:
        case error:
            return Collections.singletonList("--quiet");
        default:
            throw new IllegalStateException("Unexpected " + Verbosity.class.getName() + " value [" + verbosity + "]");
        }
    }

    @Override
    public void setVersions(BuildRequest request) throws BuildException {
        final List<String> args = new ArrayList<>();
        args.add("org.codehaus.mojo:versions-maven-plugin:" + request.getVersionsMavenPluginVersion() + ":set");
        args.add("-DnewVersion=" + request.getSrcVersion().toString());
        args.add("-DartifactId=*");
        args.add("-DgroupId=*");
        args.add("-DoldVersion=*");
        args.add("-DgenerateBackupPoms=false");
        args.addAll(getVerbosityArguments(request.getVerbosity()));

        ShellCommand cliRequest = ShellCommand.builder() //
                .executable(locateExecutable(request)).arguments(args) //
                .workingDirectory(request.getProjectRootDirectory()) //
                .environment(request.getBuildEnvironment()) //
                .ioRedirects(request.getIoRedirects()) //
                .timeoutMs(request.getTimeoutMs()) //
                .build();
        CommandResult result = Shell.execute(cliRequest).assertSuccess();
        this.restTimeoutMs = request.getTimeoutMs() - result.getRuntimeMs();
    }

}
