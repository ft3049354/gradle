/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.nativebinaries.language.cpp
import org.gradle.ide.visualstudio.fixtures.ProjectFile
import org.gradle.ide.visualstudio.fixtures.SolutionFile
import org.gradle.integtests.fixtures.Sample
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativebinaries.language.cpp.fixtures.AbstractInstalledToolChainIntegrationSpec
import org.gradle.nativebinaries.language.cpp.fixtures.AvailableToolChains
import org.gradle.nativebinaries.language.cpp.fixtures.RequiresInstalledToolChain
import org.gradle.nativebinaries.test.cunit.CUnitTestResults
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition
import org.junit.Rule

import static org.gradle.nativebinaries.language.cpp.fixtures.ToolChainRequirement.VisualCpp

@Requires(TestPrecondition.CAN_INSTALL_EXECUTABLE)
class NativeSamplesIntegrationTest extends AbstractInstalledToolChainIntegrationSpec {
    @Rule public final Sample c = new Sample(temporaryFolder, 'native-binaries/c')
    @Rule public final Sample assembler = new Sample(temporaryFolder, 'native-binaries/assembler')
    @Rule public final Sample cpp = new Sample(temporaryFolder, 'native-binaries/cpp')
    @Rule public final Sample cppLib = new Sample(temporaryFolder, 'native-binaries/cpp-lib')
    @Rule public final Sample cppExe = new Sample(temporaryFolder, 'native-binaries/cpp-exe')
    @Rule public final Sample objectiveC = new Sample(temporaryFolder, 'native-binaries/objective-c')
    @Rule public final Sample objectiveCpp = new Sample(temporaryFolder, 'native-binaries/objective-cpp')
    @Rule public final Sample customLayout = new Sample(temporaryFolder, 'native-binaries/custom-layout')
    @Rule public final Sample multiProject = new Sample(temporaryFolder, 'native-binaries/multi-project')
    @Rule public final Sample flavors = new Sample(temporaryFolder, 'native-binaries/flavors')
    @Rule public final Sample variants = new Sample(temporaryFolder, 'native-binaries/variants')
    @Rule public final Sample toolChains = new Sample(temporaryFolder, 'native-binaries/tool-chains')
    @Rule public final Sample windowsResources = new Sample(temporaryFolder, 'native-binaries/windows-resources')
    @Rule public final Sample visualStudio = new Sample(temporaryFolder, 'native-binaries/visual-studio')
    @Rule public final Sample prebuilt = new Sample(temporaryFolder, 'native-binaries/prebuilt')
    @Rule public final Sample idl = new Sample(temporaryFolder, 'native-binaries/idl')
    @Rule public final Sample cunit = new Sample(temporaryFolder, 'native-binaries/cunit')

    def "assembler"() {
        given:
        sample assembler

        when:
        run "installMainExecutable"

        then:
        nonSkippedTasks.count { it.startsWith(":assembleMainExecutable") } == 1
        executedAndNotSkipped ":compileMainExecutableMainC", ":linkMainExecutable", ":mainExecutable"

        and:
        installation("native-binaries/assembler/build/install/mainExecutable").exec().out == "5 + 7 = 12\n"
    }

    def "c"() {
        given:
        sample c
        
        when:
        run "installMainExecutable"
        
        then:
        executedAndNotSkipped ":compileHelloSharedLibraryHelloC", ":linkHelloSharedLibrary", ":helloSharedLibrary",
                              ":compileMainExecutableMainC", ":linkMainExecutable", ":mainExecutable"

        and:
        installation("native-binaries/c/build/install/mainExecutable").exec().out == "Hello world!"
    }

    def "cpp"() {
        given:
        sample cpp

        when:
        run "installMainExecutable"

        then:
        executedAndNotSkipped ":compileHelloSharedLibraryHelloCpp", ":linkHelloSharedLibrary", ":helloSharedLibrary",
                              ":compileMainExecutableMainCpp", ":linkMainExecutable", ":mainExecutable"

        and:
        installation("native-binaries/cpp/build/install/mainExecutable").exec().out == "Hello world!\n"
    }

    @Requires(TestPrecondition.NOT_WINDOWS)
    def "objectiveC"() {
        given:
        sample objectiveC

        when:
        succeeds "installMainExecutable"

        then:
        executedAndNotSkipped ":compileMainExecutableMainObjc", ":linkMainExecutable", ":mainExecutable"

        and:
        executable("native-binaries/objective-c/build/binaries/mainExecutable/main").exec().out == "Hello world!\n"
    }

    @Requires(TestPrecondition.NOT_WINDOWS)
    def "objectiveCpp"() {
        given:
        sample objectiveCpp

        when:
        succeeds "installMainExecutable"

        then:
        executedAndNotSkipped ":compileMainExecutableMainObjcpp", ":linkMainExecutable", ":mainExecutable"

        and:
        executable("native-binaries/objective-cpp/build/binaries/mainExecutable/main").exec().out == "Hello world!\n"
    }

    def "exe"() {
        given:
        // Need to PATH to be set to find the 'strip' executable
        toolChain.initialiseEnvironment()

        and:
        sample cppExe

        when:
        run "installMain"

        then:
        executedAndNotSkipped ":compileMainExecutableMainCpp", ":linkMainExecutable", ":stripMainExecutable", ":mainExecutable"

        and:
        executable("native-binaries/cpp-exe/build/binaries/mainExecutable/main").exec().out == "Hello, World!\n"
        installation("native-binaries/cpp-exe/build/install/mainExecutable").exec().out == "Hello, World!\n"

        cleanup:
        toolChain.resetEnvironment()
    }

    def "lib"() {
        given:
        sample cppLib

        when:
        run "mainSharedLibrary"

        then:
        executedAndNotSkipped ":compileMainSharedLibraryMainCpp", ":linkMainSharedLibrary", ":mainSharedLibrary"

        and:
        sharedLibrary("native-binaries/cpp-lib/build/binaries/mainSharedLibrary/main").assertExists()

        when:
        sample cppLib
        run "mainStaticLibrary"

        then:
        executedAndNotSkipped ":compileMainStaticLibraryMainCpp", ":createMainStaticLibrary", ":mainStaticLibrary"

        and:
        staticLibrary("native-binaries/cpp-lib/build/binaries/mainStaticLibrary/main").assertExists()
    }


    @RequiresInstalledToolChain(VisualCpp)
    def "win rc"() {
        given:
        sample windowsResources

        when:
        run "installMainExecutable"

        then:
        executedAndNotSkipped ":compileHelloSharedLibraryHelloCpp", ":compileHelloSharedLibraryHelloRc",
                              ":linkHelloSharedLibrary", ":helloSharedLibrary",
                              ":compileMainExecutableMainCpp", ":linkMainExecutable", ":mainExecutable"

        and:
        installation("native-binaries/windows-resources/build/install/mainExecutable").exec().out == "Hello world!\n"

        when:
        executer.usingBuildScript(windowsResources.dir.file('build-resource-only-dll.gradle'))
        run "helloResSharedLibrary"

        then:
        file("native-binaries/windows-resources/build/binaries/helloResSharedLibrary/helloRes.dll").assertExists()
    }

    def "custom layout"() {
        given:
        sample customLayout

        when:
        run "installMainExecutable"

        then:
        executedAndNotSkipped ":compileHelloStaticLibraryHelloC", ":createHelloStaticLibrary", ":helloStaticLibrary",
                              ":compileMainExecutableMainCpp", ":linkMainExecutable", ":mainExecutable"

        and:
        installation("native-binaries/custom-layout/build/install/mainExecutable").exec().out == "Hello world!"
    }

    def flavors() {
        when:
        sample flavors
        run "installEnglishMainExecutable"

        then:
        executedAndNotSkipped ":compileEnglishHelloSharedLibraryLibCpp", ":linkEnglishHelloSharedLibrary", ":englishHelloSharedLibrary"
        executedAndNotSkipped ":compileEnglishMainExecutableExeCpp", ":linkEnglishMainExecutable", ":englishMainExecutable"

        and:
        executable("native-binaries/flavors/build/binaries/mainExecutable/english/main").assertExists()
        sharedLibrary("native-binaries/flavors/build/binaries/helloSharedLibrary/english/hello").assertExists()

        and:
        installation("native-binaries/flavors/build/install/mainExecutable/english").exec().out == "Hello world!\n"

        when:
        sample flavors
        run "installFrenchMainExecutable"

        then:
        executedAndNotSkipped ":compileFrenchHelloSharedLibraryLibCpp", ":linkFrenchHelloSharedLibrary", ":frenchHelloSharedLibrary"
        executedAndNotSkipped ":compileFrenchMainExecutableExeCpp", ":linkFrenchMainExecutable", ":frenchMainExecutable"

        and:
        executable("native-binaries/flavors/build/binaries/mainExecutable/french/main").assertExists()
        sharedLibrary("native-binaries/flavors/build/binaries/helloSharedLibrary/french/hello").assertExists()

        and:
        installation("native-binaries/flavors/build/install/mainExecutable/french").exec().out == "Bonjour monde!\n"
    }

    def variants() {
        when:
        sample variants
        run "buildExecutables"

        then:
        final debugX86 = executable("native-binaries/variants/build/binaries/mainExecutable/x86Debug/main")
        final releaseX86 = executable("native-binaries/variants/build/binaries/mainExecutable/x86Release/main")
        final debugX64 = executable("native-binaries/variants/build/binaries/mainExecutable/x64Debug/main")
        final releaseX64 = executable("native-binaries/variants/build/binaries/mainExecutable/x64Release/main")
        final debugIA64 = executable("native-binaries/variants/build/binaries/mainExecutable/itaniumDebug/main")
        final releaseIA64 = executable("native-binaries/variants/build/binaries/mainExecutable/itaniumRelease/main")

        debugX86.binaryInfo.arch.name == "x86"
        debugX86.assertDebugFileExists()
        debugX86.exec().out == "Hello world!\n"

        releaseX86.binaryInfo.arch.name == "x86"
        releaseX86.assertDebugFileDoesNotExist()
        releaseX86.exec().out == "Hello world!\n"

        // x86_64 binaries not supported on MinGW or cygwin
        if (toolChain.id == "mingw" || toolChain.id == "gcccygwin") {
            debugX64.assertDoesNotExist()
            releaseX64.assertDoesNotExist()
        } else {
            debugX64.binaryInfo.arch.name == "x86_64"
            releaseX64.binaryInfo.arch.name == "x86_64"
        }

        // Itanium not built
        debugIA64.assertDoesNotExist()
        releaseIA64.assertDoesNotExist()
    }

    def "tool chains"() {
        given:
        sample toolChains

        when:
        run "installMainExecutable"

        then:
        executable("native-binaries/tool-chains/build/binaries/mainExecutable/main").exec().out == "Hello from ${toolChain.typeDisplayName}!\n"
    }

    def multiProject() {
        given:
        sample multiProject

        when:
        run "installMainExecutable"

        then:
        ":exe:mainExecutable" in executedTasks

        and:
        sharedLibrary("native-binaries/multi-project/lib/build/binaries/mainSharedLibrary/main").assertExists()
        executable("native-binaries/multi-project/exe/build/binaries/mainExecutable/main").assertExists()
        installation("native-binaries/multi-project/exe/build/install/mainExecutable").exec().out == "Hello, World!\n"
    }

    def "visual studio"() {
        given:
        sample visualStudio

        when:
        run "mainVisualStudio"

        then:
        final solutionFile = new SolutionFile(visualStudio.dir.file("vs/mainExe.sln"))
        solutionFile.assertHasProjects("mainExe", "helloDll")
        solutionFile.content.contains "GlobalSection(SolutionNotes) = postSolution"
        solutionFile.content.contains "Text2 = The projects in this solution are [mainExe, helloDll]."

        final projectFile = new ProjectFile(visualStudio.dir.file("vs/helloDll.vcxproj"))
        projectFile.projectXml.PropertyGroup.find({it.'@Label' == 'Custom'}).ProjectDetails[0].text() == "Project is named helloDll"
    }

    def prebuilt() {
        given:
        inDirectory(prebuilt.dir.file("3rd-party-lib/util"))
        run "buildLibraries"

        and:
        sample prebuilt

        when:
        succeeds "buildExecutables"

        then:

        executable(prebuilt.dir.file("build/binaries/mainExecutable/debug/main")).exec().out ==
"""Built with Boost version: 1_55
Util build type: DEBUG
"""
        executable(prebuilt.dir.file("build/binaries/mainExecutable/release/main")).exec().out ==
"""Built with Boost version: 1_55
Util build type: RELEASE
"""
    }

    def "idl"() {
        given:
        sample idl

        when:
        run "installMainExecutable"

        then:
        executedAndNotSkipped ":idl", ":compileMainExecutableMainC", ":compileMainExecutableMainIdlOutput",
                              ":linkMainExecutable", ":mainExecutable"

        and:
        installation("native-binaries/idl/build/install/mainExecutable").exec().out == "Hello from generated source!!\n"
    }

    def "cunit"() {
        given:
        // CUnit prebuilt library only works for VS2010 on windows
        if (OperatingSystem.current().windows && !isVisualCpp2010()) {
            return
        }

        when:
        sample cunit
        succeeds "runPassing"

        then:
        executedAndNotSkipped ":operatorsTestCUnitLauncher",
                              ":compilePassingOperatorsTestCUnitExeOperatorsTestCunit", ":compilePassingOperatorsTestCUnitExeOperatorsTestCunitLauncher",
                              ":linkPassingOperatorsTestCUnitExe", ":passingOperatorsTestCUnitExe",
                              ":installPassingOperatorsTestCUnitExe", ":runPassingOperatorsTestCUnitExe"

        and:
        def passingResults = new CUnitTestResults(file("native-binaries/cunit/build/test-results/operatorsTestCUnitExe/passing/CUnitAutomated-Results.xml"))
        passingResults.suiteNames == ['operator tests']
        passingResults.suites['operator tests'].passingTests == ['test_plus', 'test_minus']
        passingResults.suites['operator tests'].failingTests == []
        passingResults.checkTestCases(2, 2, 0)
        passingResults.checkAssertions(6, 6, 0)

        when:
        sample cunit
        fails "runFailing"

        then:
        skipped ":operatorsTestCUnitLauncher"
        executedAndNotSkipped ":compileFailingOperatorsTestCUnitExeOperatorsTestCunit", ":compileFailingOperatorsTestCUnitExeOperatorsTestCunitLauncher",
                              ":linkFailingOperatorsTestCUnitExe", ":failingOperatorsTestCUnitExe",
                              ":installFailingOperatorsTestCUnitExe", ":runFailingOperatorsTestCUnitExe"

        and:
        def failingResults = new CUnitTestResults(file("native-binaries/cunit/build/test-results/operatorsTestCUnitExe/failing/CUnitAutomated-Results.xml"))
        failingResults.suiteNames == ['operator tests']
        failingResults.suites['operator tests'].passingTests == ['test_minus']
        failingResults.suites['operator tests'].failingTests == ['test_plus']
        failingResults.checkTestCases(2, 1, 1)
        failingResults.checkAssertions(6, 4, 2)
    }

    private static boolean isVisualCpp2010() {
        return (toolChain.visualCpp && (toolChain as AvailableToolChains.InstalledVisualCpp).version.major == "10")
    }

}